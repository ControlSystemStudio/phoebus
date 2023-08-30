/**
 *
 */
package org.phoebus.alarm.logging;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.ExistsIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateRequest;
import co.elastic.clients.elasticsearch.indices.PutIndexTemplateResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

/**
 * A Utility service to allow for batched indexing of alarm state, config, and command messages to an elastic backend
 *
 * @author Kunal Shroff {@literal <kunalshroff9@gmail.gov>}
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
    BlockingQueue<SimpleImmutableEntry<String, AlarmStateMessage>> stateMessagedQueue = new LinkedBlockingDeque<>();
    // State messages to be indexed
    BlockingQueue<SimpleImmutableEntry<String, AlarmConfigMessage>> configMessagedQueue = new LinkedBlockingDeque<>();

    BlockingQueue<SimpleImmutableEntry<String, AlarmCommandMessage>> commandMessagedQueue = new LinkedBlockingDeque<>();

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
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Failed to close the elastic client.", ex);
                    }
                }
            }));

            // Create the low-level client
            restClient = RestClient.builder(
                    new HttpHost(props.getProperty("es_host"), Integer.parseInt(props.getProperty("es_port")))).build();

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
            job = scheduledExecutorService.scheduleAtFixedRate(new flush2Elastic(stateMessagedQueue, configMessagedQueue, commandMessagedQueue),
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

    /**
     * Index an alarm state message
     *
     * @param indexName Name of Elasticsearch index, e.g. myConfig_alarms_state_yyyy-MM-dd
     * @param alarmStateMessage Object holding alarm state message
     */
    public void indexAlarmStateDocuments(String indexName, AlarmStateMessage alarmStateMessage) {
        try {
            stateMessagedQueue.put(new SimpleImmutableEntry<>(indexName, alarmStateMessage));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmStateMessage + " to index " + indexName, e);
        }
    }

    /**
     * Index an alarm command message
     *
     * @param indexName Name of Elasticsearch index, e.g. myConfig_alarms_cmd_yyyy-MM-dd
     * @param alarmCommandMessage Object holding alarm command message
     */
    public void indexAlarmCmdDocument(String indexName, AlarmCommandMessage alarmCommandMessage) {
        try {
            commandMessagedQueue.put(new SimpleImmutableEntry<>(indexName, alarmCommandMessage));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log command message " + alarmCommandMessage + " to index " + indexName, e);
        }
    }

    /**
     * Index an alarm config message
     *
     * @param indexName Name of Elasticsearch index, e.g. myConfig_alarms_config_yyyy-MM-dd
     * @param alarmConfigMessage Object holding alarm config message
     */
    public void indexAlarmConfigDocuments(String indexName, AlarmConfigMessage alarmConfigMessage) {
        try {
            configMessagedQueue.put(new SimpleImmutableEntry<>(indexName, alarmConfigMessage));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "failed to log message " + alarmConfigMessage + " to index " + indexName, e);
        }
    }

    /**
     * A helper class which implements 2 queues for allowing bulk logging of state and config messages
     */
    private static class flush2Elastic implements Runnable {

        private final BlockingQueue<SimpleImmutableEntry<String, AlarmStateMessage>> stateMessagedQueue;
        private final BlockingQueue<SimpleImmutableEntry<String, AlarmConfigMessage>> configMessagedQueue;
        private final BlockingQueue<SimpleImmutableEntry<String, AlarmCommandMessage>> commandMessagedQueue;

        public flush2Elastic(BlockingQueue<SimpleImmutableEntry<String, AlarmStateMessage>> stateMessagedQueue,
                             BlockingQueue<SimpleImmutableEntry<String, AlarmConfigMessage>> configMessagedQueue,
                             BlockingQueue<SimpleImmutableEntry<String, AlarmCommandMessage>> commandMessagedQueue) {
            this.stateMessagedQueue = stateMessagedQueue;
            this.configMessagedQueue = configMessagedQueue;
            this.commandMessagedQueue = commandMessagedQueue;
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
            if (stateMessagedQueue.size() + configMessagedQueue.size() > 0) {
                logger.log(Level.INFO, "batch execution of : " + stateMessagedQueue.size() + " state messages and " + configMessagedQueue.size() + " config messages");
                BulkRequest.Builder bulkRequest = new BulkRequest.Builder().refresh(Refresh.True);
                Collection<SimpleImmutableEntry<String, AlarmStateMessage>> statePairs = new ArrayList<>();
                stateMessagedQueue.drainTo(statePairs);
                Collection<SimpleImmutableEntry<String, AlarmConfigMessage>> configPairs = new ArrayList<>();
                configMessagedQueue.drainTo(configPairs);
                Collection<SimpleImmutableEntry<String, AlarmCommandMessage>> commandPairs = new ArrayList<>();
                commandMessagedQueue.drainTo(commandPairs);
                statePairs.forEach(pair -> bulkRequest.operations(op -> op
                        .index(idx -> idx
                                .index(pair.getKey().toLowerCase())
                                .document(pair.getValue().sourceMap()))));
                configPairs.forEach(pair -> bulkRequest.operations(op -> op
                        .index(idx -> idx
                                .index(pair.getKey().toLowerCase())
                                .document(pair.getValue().sourceMap()))));
                commandPairs.forEach(pair -> bulkRequest.operations(op -> op
                        .index(idx -> idx
                                .index(pair.getKey().toLowerCase())
                                .document(pair.getValue().sourceMap()))));
                try {
                    BulkResponse bulkResponse = client.bulk(bulkRequest.build());
                    bulkResponse.items().forEach(item -> {
                                if (item.error() != null) {
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

        private static final Properties props = new Properties();

        static {
            props.putAll(PropertiesHelper.getProperties());
        }

        private final String ALARM_STATE_TEMPLATE = props.getProperty("elasticsearch.alarm.state.template", "alarms_state_template");
        private final String ALARM_STATE_TEMPLATE_PATTERN = props.getProperty("elasticsearch.alarm.state.template.pattern", "*_alarms_state*");

        private final String ALARM_CMD_TEMPLATE = props.getProperty("elasticsearch.alarm.cmd.template", "alarms_cmd_template");
        private final String ALARM_CMD_TEMPLATE_PATTERN = props.getProperty("elasticsearch.alarm.cmd.template.pattern", "*_alarms_cmd*");

        private final String ALARM_CONFIG_TEMPLATE = props.getProperty("elasticsearch.alarm.config.template", "alarms_config_template");
        private final String ALARM_CONFIG_TEMPLATE_PATTERN = props.getProperty("elasticsearch.alarm.config.template.pattern", "*_alarms_config*");

        /**
         * Check if the required templated for the phoebus alarm logs exists, if not create them.
         *
         * @throws IOException if Elasticsearch interaction fails
         */
        public void initializeIndices() throws IOException {
            // Create the alarm state messages index template
            boolean exists = client.indices().existsIndexTemplate(ExistsIndexTemplateRequest.of(i -> i.name(ALARM_STATE_TEMPLATE))).value();

            if (!exists) {
                try (InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_state_template.json")) {
                    PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                            .name(ALARM_STATE_TEMPLATE)
                            .indexPatterns(Arrays.asList(ALARM_STATE_TEMPLATE_PATTERN))
                            .withJson(is)
                            .priority(1)
                            .create(true)
                            .build();
                    PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                    putTemplateResponse.acknowledged();
                    logger.log(Level.INFO, "Created " + ALARM_STATE_TEMPLATE + " template.");
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to create template " + ALARM_STATE_TEMPLATE + " template.", e);
                }
            }

            // Create the alarm command messages index template
            exists = client.indices().existsIndexTemplate(ExistsIndexTemplateRequest.of(i -> i.name(ALARM_CMD_TEMPLATE))).value();

            if (!exists) {
                try (InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_cmd_template.json")) {
                    PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                            .name(ALARM_CMD_TEMPLATE)
                            .indexPatterns(Arrays.asList(ALARM_CMD_TEMPLATE_PATTERN))
                            .withJson(is)
                            .priority(2)
                            .create(true)
                            .build();
                    PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                    putTemplateResponse.acknowledged();
                    logger.log(Level.INFO, "Created " + ALARM_CMD_TEMPLATE + " template.");
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to create template " + ALARM_CMD_TEMPLATE + " template.", e);
                }
            }

            // Create the alarm config messages index template
            exists = client.indices().existsIndexTemplate(ExistsIndexTemplateRequest.of(i -> i.name(ALARM_CONFIG_TEMPLATE))).value();

            if (!exists) {
                try (InputStream is = ElasticClientHelper.class.getResourceAsStream("/alarms_config_template.json")) {
                    PutIndexTemplateRequest templateRequest = new PutIndexTemplateRequest.Builder()
                            .name(ALARM_CONFIG_TEMPLATE)
                            .indexPatterns(Arrays.asList(ALARM_CONFIG_TEMPLATE_PATTERN))
                            .withJson(is)
                            .priority(3)
                            .create(true)
                            .build();
                    PutIndexTemplateResponse putTemplateResponse = client.indices().putIndexTemplate(templateRequest);
                    putTemplateResponse.acknowledged();
                    logger.log(Level.INFO, "Created " + ALARM_CONFIG_TEMPLATE + " template.");
                } catch (Exception e) {
                    logger.log(Level.INFO, "Failed to create template " + ALARM_CONFIG_TEMPLATE + " template.", e);
                }
            }

        }
    }
}
