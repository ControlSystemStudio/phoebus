/**
 *
 */
package org.phoebus.alarm.logging;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import co.elastic.clients.elasticsearch.indices.*;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.AbstractMap.SimpleImmutableEntry;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

/**
 * @author Kunal Shroff {@literal <kunalshroff9@gmail.gov>}
 *
 */
public class ElasticClientHelper {
    Properties props = PropertiesHelper.getProperties();

    private static RestClient restClient;

    private static ElasticsearchTransport transport;

    private static ElasticsearchClient client;
    private static ElasticClientHelper instance;
    private static Sniffer sniffer;

    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);
    ScheduledFuture<?> job;
    // State messages to be indexed
    BlockingQueue<SimpleImmutableEntry<String,AlarmStateMessage>> stateMessagedQueue = new LinkedBlockingDeque<>();
    // State messages to be indexed
    BlockingQueue<SimpleImmutableEntry<String,AlarmConfigMessage>> configMessagedQueue = new LinkedBlockingDeque<>();

    private final ObjectMapper mapper = new ObjectMapper();

    private ElasticClientHelper() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down the ElasticClientHelper.");
                if (client != null) {
                    try {
                        client.shutdown();
                        transport.close();
                        restClient.close();
                    }
                    catch (IOException ex){
                        logger.log(Level.WARNING, "Failed to close the elastic client.", ex);
                    }
                }
            }));

            // Create the low-level client
            restClient = RestClient.builder(
                            new HttpHost(props.getProperty("es_host"),Integer.parseInt(props.getProperty("es_port")))).build();

            mapper.registerModule(new JavaTimeModule());
            transport = new RestClientTransport(
                    restClient,
                    new JacksonJsonpMapper(mapper)
            );
            client = new ElasticsearchClient(transport);
            if (props.getProperty("es_sniff").equals("true")) {
                sniffer = Sniffer.builder(restClient).build();
                logger.log(Level.INFO, "ES Sniff feature is enabled");
            }
            // Initialize the elastic templates
            esInitialized.set(!Boolean.parseBoolean(props.getProperty("es_create_templates")));

            // Start the executor for periodically logging into es
            job = scheduledExecutorService.scheduleAtFixedRate(new flush2Elastic(stateMessagedQueue, configMessagedQueue),
                    0, 250, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            try {
                job.cancel(false);
                sniffer.close();
                transport.close();
                restClient.close();
                client.shutdown();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close the elastic client", ex);
            }
        }

    }

    public static ElasticClientHelper getInstance() {
        if (instance == null) {
            instance = new ElasticClientHelper();
        }
        return instance;
    }

    public ElasticsearchClient getClient() {
        return client;
    }

    public void indexAlarmStateDocuments(String indexName, AlarmStateMessage alarmStateMessage) {
        try {
            stateMessagedQueue.put(new SimpleImmutableEntry<>(indexName,alarmStateMessage));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmStateMessage + " to index " + indexName, e);
        }
    }

    public boolean indexAlarmCmdDocument(String indexName, AlarmCommandMessage alarmCommandMessage) {
        IndexRequest<AlarmCommandMessage> indexRequest = new IndexRequest.Builder<AlarmCommandMessage>()
                .index(indexName.toLowerCase())
                .document(alarmCommandMessage)
                .build();
        try {
            IndexResponse indexResponse = client.index(indexRequest);
            return indexResponse.result().equals(Result.Created);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmCommandMessage + " to index " + indexName, e);
            return false;
        }
    }

    public void indexAlarmConfigDocuments(String indexName, AlarmConfigMessage alarmConfigMessage) {
        try {
            configMessagedQueue.put(new SimpleImmutableEntry<>(indexName,alarmConfigMessage));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmConfigMessage + " to index " + indexName, e);
        }
    }

    private static class flush2Elastic implements Runnable {

        private final BlockingQueue<SimpleImmutableEntry<String,AlarmStateMessage>> stateMessagedQueue;
        private final BlockingQueue<SimpleImmutableEntry<String,AlarmConfigMessage>> configMessagedQueue;

        public flush2Elastic(BlockingQueue<SimpleImmutableEntry<String,AlarmStateMessage>> stateMessagedQueue,
                             BlockingQueue<SimpleImmutableEntry<String,AlarmConfigMessage>> configMessagedQueue) {
            this.stateMessagedQueue = stateMessagedQueue;
            this.configMessagedQueue = configMessagedQueue;
        }

        @Override
        public void run() {
            if (esInitialized.compareAndSet(false, true)) {
                try {
                    initializeIndices();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "failed to create the alarm log indices ", e);
                }
            }
            if(stateMessagedQueue.size() + configMessagedQueue.size() > 0){
                logger.log(Level.INFO, "batch execution of : " + stateMessagedQueue.size() + " state messages and " + configMessagedQueue.size() + " config messages");
                BulkRequest.Builder bulkRequest = new BulkRequest.Builder().refresh(Refresh.True);
                Collection<SimpleImmutableEntry<String,AlarmStateMessage>> statePairs = new ArrayList<>();
                stateMessagedQueue.drainTo(statePairs);
                Collection<SimpleImmutableEntry<String,AlarmConfigMessage>> configPairs = new ArrayList<>();
                configMessagedQueue.drainTo(configPairs);
                statePairs.forEach( pair -> bulkRequest.operations(op -> op
                        .index(idx -> idx
                                .index(pair.getKey())
                                .document(pair.getValue()))));
                configPairs.forEach( pair -> bulkRequest.operations(op->op
                        .index(idx->idx
                                .index(pair.getKey())
                                .document(pair.getValue()))));
                try {
                    BulkResponse bulkResponse = client.bulk(bulkRequest.build());
                        bulkResponse.items().forEach(item -> {
                                    if (item.error()!=null) {
                                        logger.log(Level.SEVERE, "Failed while indexing to " + item.index() + " type "
                                                + item.operationType() + item.error().reason() + "]");
                                    }
                                }
                        );
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "failed to log messages to index ", e);
                }
            }
        }

        private static final String ALARM_STATE_TEMPLATE =  "alarms_state_template";
        private static final String ALARM_STATE_TEMPLATE_PATTERN =  "*_alarms_state*";

        private static final String ALARM_CMD_TEMPLATE =  "alarms_cmd_template";
        private static final String ALARM_CMD_TEMPLATE_PATTERN =  "*_alarms_cmd*";

        private static final String ALARM_CONFIG_TEMPLATE =  "alarms_config_template";
        private static final String ALARM_CONFIG_TEMPLATE_PATTERN =  "*_alarms_config*";

        public void initializeIndices() throws IOException {
            // Create the alarm state messages index template
            ExistsTemplateRequest request = new ExistsTemplateRequest.Builder()
                    .name(ALARM_STATE_TEMPLATE)
                    .build();
            boolean exists = client.indices().existsTemplate(request).value();

            if(!exists) {
                InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_state_template.json");
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                        .name(ALARM_STATE_TEMPLATE)
                        .indexPatterns(Arrays.asList(ALARM_STATE_TEMPLATE_PATTERN))
                        .withJson(is)
                        .create(true)
                        .build();
                PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                putTemplateResponse.acknowledged();
                logger.log( Level.INFO, "Created " + ALARM_STATE_TEMPLATE + " template.");
            }

            // Create the alarm command messages index template
            request = new ExistsTemplateRequest.Builder()
                    .name(ALARM_CMD_TEMPLATE)
                    .build();
            exists = client.indices().existsTemplate(request).value();

            if(!exists) {
                InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_cmd_template.json");
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                        .name(ALARM_CMD_TEMPLATE)
                        .indexPatterns(Arrays.asList(ALARM_CMD_TEMPLATE_PATTERN))
                        .withJson(is)
                        .create(true)
                        .build();
                PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                putTemplateResponse.acknowledged();
                logger.log( Level.INFO, "Created " + ALARM_STATE_TEMPLATE + " template.");
            }

            // Create the alarm config messages index template
            request = new ExistsTemplateRequest.Builder()
                    .name(ALARM_CONFIG_TEMPLATE)
                    .build();
            exists = client.indices().existsTemplate(request).value();

            if(!exists) {
                InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_cmd_template.json");
                PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                        .name(ALARM_CONFIG_TEMPLATE)
                        .indexPatterns(Arrays.asList(ALARM_CONFIG_TEMPLATE_PATTERN))
                        .withJson(is)
                        .create(true)
                        .build();
                PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                putTemplateResponse.acknowledged();
                logger.log( Level.INFO, "Created " + ALARM_CONFIG_TEMPLATE + " template.");
            }

        }
    }
}
