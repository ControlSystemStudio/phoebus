/**
 *
 */
package org.phoebus.alarm.logging;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.Jackson3JsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest5_client.Rest5ClientTransport;
import co.elastic.clients.transport.rest5_client.low_level.Request;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.ResponseException;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.URISyntaxException;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

/**
 * A Utility service to allow for batched indexing of alarm state, config, and command messages to an elastic backend
 *
 * @author Kunal Shroff {@literal <kunalshroff9@gmail.gov>}
 */
public class ElasticClientHelper {
    Properties props = PropertiesHelper.getProperties();

    private static Rest5Client restClient;

    private static ElasticsearchTransport transport;

    private static ElasticsearchClient client;
    private static final AtomicReference<ElasticClientHelper> instance = new AtomicReference<>();
    private static final AtomicBoolean esInitialized = new AtomicBoolean();

    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(4);
    ScheduledFuture<?> job;
    // State messages to be indexed
    BlockingQueue<SimpleImmutableEntry<String, AlarmStateMessage>> stateMessagedQueue = new LinkedBlockingDeque<>();
    // State messages to be indexed
    BlockingQueue<SimpleImmutableEntry<String, AlarmConfigMessage>> configMessagedQueue = new LinkedBlockingDeque<>();

    BlockingQueue<SimpleImmutableEntry<String, AlarmCommandMessage>> commandMessagedQueue = new LinkedBlockingDeque<>();

    private ElasticClientHelper() {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down the ElasticClientHelper.");
                if (client != null) {
                    try {
                        // Do not call client.shutdown() with Rest5 transport because some
                        // client versions assume RestClientTransport internally.
                        transport.close();
                        restClient.close();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, "Failed to close the elastic client.", ex);
                    }
                }
            }));

            // Create the low-level client
            final var esHost = props.getProperty("es_host", "");
            final var esPort = props.getProperty("es_port", "");
            final var esUrls = props.getProperty("es_urls", "");
            HttpHost[] esHttpHosts;
            if (esUrls.isEmpty()) {
                final var http_host = new HttpHost(
                        "http",
                        esHost.isEmpty() ? "localhost" : esHost,
                        esPort.isEmpty() ? 9200 : Integer.parseInt(esPort));
                esHttpHosts = new HttpHost[] {http_host};
            } else {
                if (!esHost.isEmpty() || !esPort.isEmpty()) {
                    logger.warning("Only one of es_urls or es_host and es_port can be specified, ignoring es_host and es_port.");
                }
                esHttpHosts = Arrays.stream(esUrls.split(","))
                        .map(url -> {
                            try {
                                return HttpHost.create(url);
                            } catch (URISyntaxException e) {
                                throw new IllegalArgumentException("Invalid URL in es_urls: " + url, e);
                            }
                        })
                        .toArray(HttpHost[]::new);
            }
            final var esAuthHeader = props.getProperty("es_auth_header", "");
            final var esAuthUsername = props.getProperty("es_auth_username", "");
            final var esAuthPassword = props.getProperty("es_auth_password", "");
            final Rest5ClientBuilder restClientBuilder = Rest5Client.builder(esHttpHosts);
            if (!esAuthHeader.isEmpty()) {
                if (!esAuthUsername.isEmpty() || !esAuthPassword.isEmpty()) {
                    logger.warning("Only one of es_auth_header or es_auth_username and es_auth_password can be specified. Ignoring es_auth_username and es_auth_password.");
                }
                restClientBuilder.setDefaultHeaders(
                        new Header[] {new BasicHeader("Authorization", esAuthHeader)});
            } else if (!esAuthUsername.isEmpty() || !esAuthPassword.isEmpty()) {
                final var credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                        new AuthScope(esHttpHosts[0]),
                        new UsernamePasswordCredentials(esAuthUsername, esAuthPassword.toCharArray()));
                restClientBuilder.setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
            }
            restClient = restClientBuilder.build();

            transport = new Rest5ClientTransport(
                    restClient,
                    new Jackson3JsonpMapper(new JsonMapper())
            );
            client = new ElasticsearchClient(transport);
            if (props.getProperty("es_sniff").equals("true")) {
                logger.log(Level.WARNING, "es_sniff=true is ignored because Rest5Client does not support the legacy sniffer API.");
            }
            // Initialize the elastic templates
            esInitialized.set(!Boolean.parseBoolean(props.getProperty("es_create_templates")));

            // Start the executor for periodically logging into es
            job = scheduledExecutorService.scheduleAtFixedRate(new flush2Elastic(stateMessagedQueue, configMessagedQueue, commandMessagedQueue),
                    0, 250, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            try {
                if (job != null) {
                    job.cancel(false);
                }
                if (transport != null) {
                    transport.close();
                }
                if (restClient != null) {
                    restClient.close();
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Failed to close the elastic client", ex);
            }
        }
    }

    public static ElasticClientHelper getInstance() {
        var helper = instance.get();
        if (helper == null) {
            // The helper instance is associated with static resources, so we
            // want to be certain that it is never created twice. In order to
            // ensure this, we have to create it inside a synchronized block,
            // but we only do this if we expect that there is no instance yet.
            // This looks like the double-checked-locking anti-pattern, but it
            // is not an anti-pattern here, because instance is an atomic
            // reference, so getting the value establishes a happens-before
            // relationship, and we can be sure that we won’t retrieve an
            // uninitialized object.
            synchronized (instance) {
                helper = instance.get();
                if (helper == null) {
                    helper = new ElasticClientHelper();
                    instance.set(helper);
                }
            }
        }
        return helper;
    }

    public ElasticsearchClient getClient() {
        return client;
    }

    public Rest5Client getRestClient() {
        return restClient;
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
            int stateSize = stateMessagedQueue.size();
            int configSize = configMessagedQueue.size();
            int commandSize = commandMessagedQueue.size();
            if (stateSize + configSize + commandSize > 0) {
                logger.log(Level.INFO, "batch execution of : " + stateSize + " state, " + configSize + " config, " + commandSize + " command messages");
                Collection<SimpleImmutableEntry<String, AlarmStateMessage>> statePairs = new ArrayList<>();
                stateMessagedQueue.drainTo(statePairs);
                Collection<SimpleImmutableEntry<String, AlarmConfigMessage>> configPairs = new ArrayList<>();
                configMessagedQueue.drainTo(configPairs);
                Collection<SimpleImmutableEntry<String, AlarmCommandMessage>> commandPairs = new ArrayList<>();
                commandMessagedQueue.drainTo(commandPairs);
                try {
                    performBulkIndex(statePairs, configPairs, commandPairs);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "failed to log messages to index ", e);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unexpected error during bulk indexing: " + e.getClass().getName() + " - " + e.getMessage(), e);
                }
            }
        }

        /**
         * Perform bulk indexing using low-level Rest5 API to avoid media-type header issues
         * with elasticsearch-java 9.x high-level client against ES 8.x backends.
         */
        private void performBulkIndex(Collection<SimpleImmutableEntry<String, AlarmStateMessage>> statePairs,
                                      Collection<SimpleImmutableEntry<String, AlarmConfigMessage>> configPairs,
                                      Collection<SimpleImmutableEntry<String, AlarmCommandMessage>> commandPairs)
                throws IOException {
            if (statePairs.isEmpty() && configPairs.isEmpty() && commandPairs.isEmpty()) {
                return;
            }

            StringBuilder bulkPayload = new StringBuilder();
            long successCount = 0L;

            // Add state pairs
            for (var pair : statePairs) {
                String indexName = pair.getKey().toLowerCase();
                bulkPayload.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");
                String jsonDoc = ElasticClientHelper.toJson(pair.getValue().sourceMap());
                bulkPayload.append(jsonDoc).append("\n");
                successCount++;
            }

            // Add config pairs
            for (var pair : configPairs) {
                String indexName = pair.getKey().toLowerCase();
                bulkPayload.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");
                String jsonDoc = ElasticClientHelper.toJson(pair.getValue().sourceMap());
                bulkPayload.append(jsonDoc).append("\n");
                successCount++;
            }

            // Add command pairs
            for (var pair : commandPairs) {
                String indexName = pair.getKey().toLowerCase();
                bulkPayload.append("{\"index\":{\"_index\":\"").append(indexName).append("\"}}\n");
                String jsonDoc = ElasticClientHelper.toJson(pair.getValue().sourceMap());
                bulkPayload.append(jsonDoc).append("\n");
                successCount++;
            }

            if (bulkPayload.isEmpty()) {
                return;
            }

            Request request = new Request("POST", "/_bulk");
            request.addParameter("refresh", "true");
            request.setJsonEntity(bulkPayload.toString());

            try {
                int statusCode = restClient.performRequest(request).getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    logger.log(Level.INFO, "Bulk indexing completed successfully: " + successCount + " items indexed");
                } else {
                    logger.log(Level.WARNING, "Bulk indexing returned HTTP " + statusCode + " but " + successCount + " items were sent");
                }
            } catch (ResponseException e) {
                logger.log(Level.SEVERE, "Bulk indexing failed with HTTP " + e.getResponse().getStatusCode(), e);
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
            createTemplateIfMissing(ALARM_STATE_TEMPLATE, ALARM_STATE_TEMPLATE_PATTERN, "/alarms_state_template.json", 1L);
            createTemplateIfMissing(ALARM_CMD_TEMPLATE, ALARM_CMD_TEMPLATE_PATTERN, "/alarms_cmd_template.json", 2L);
            createTemplateIfMissing(ALARM_CONFIG_TEMPLATE, ALARM_CONFIG_TEMPLATE_PATTERN, "/alarms_config_template.json", 3L);
        }

        private void createTemplateIfMissing(String templateName,
                                             String pattern,
                                             String resource,
                                             long priority) {
            try (InputStream is = ElasticClientHelper.class.getResourceAsStream(resource)) {
                if (is == null) {
                    throw new IOException("Template resource not found: " + resource);
                }
                final String templateJson = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                final String payload = toComposableTemplatePayload(templateJson, pattern, priority);
                final String endpoint = "/_index_template/" + URLEncoder.encode(templateName, StandardCharsets.UTF_8);

                Request request = new Request("PUT", endpoint);
                request.addParameter("create", "true");
                request.setJsonEntity(payload);

                int statusCode = restClient.performRequest(request).getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    logger.log(Level.INFO, "Created " + templateName + " template.");
                } else {
                    logger.log(Level.WARNING, "Template creation returned HTTP " + statusCode + " for " + templateName + ".");
                }
            } catch (ResponseException e) {
                if (e.getResponse().getStatusCode() == 409) {
                    logger.log(Level.FINE, "Template " + templateName + " already exists.");
                    return;
                }
                logger.log(Level.INFO, "Failed to create template " + templateName + " template.", e);
            } catch (Exception e) {
                logger.log(Level.INFO, "Failed to create template " + templateName + " template.", e);
            }

        }

        /**
         * Builds a composable template payload by combining existing template JSON
         * content with runtime index pattern and priority.
         */
        private String toComposableTemplatePayload(String templateJson, String pattern, long priority) throws IOException {
            String trimmed = templateJson.trim();
            if (!trimmed.startsWith("{")) {
                throw new IOException("Invalid template JSON content.");
            }
            String escapedPattern = pattern.replace("\\", "\\\\").replace("\"", "\\\"");
            return "{\"index_patterns\":[\"" + escapedPattern + "\"],\"priority\":" + priority + "," + trimmed.substring(1);
        }
    }

    /**
     * Serialize a map to JSON string using the mapper.
     */
    static String toJson(java.util.Map<String, String> map) {
        return new tools.jackson.databind.json.JsonMapper().writeValueAsString(map);
    }
}
