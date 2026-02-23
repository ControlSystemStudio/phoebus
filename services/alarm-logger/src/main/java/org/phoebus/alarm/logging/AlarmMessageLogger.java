package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.phoebus.applications.alarm.AlarmSystemConstants;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.AlarmMessage;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.util.indexname.IndexNameHelper;

public class AlarmMessageLogger implements Runnable {

    private final String topic;
    private final Serde<AlarmMessage> alarmMessageSerde;

    private final Pattern pattern = Pattern.compile("(\\w*://\\S*)");

    private IndexNameHelper stateIndexNameHelper;
    private IndexNameHelper configIndexNameHelper;
    private static final String CONFIG_INDEX_FORMAT = "_alarms_config";
    private static final String STATE_INDEX_FORMAT = "_alarms_state";

    private volatile boolean shouldReconnect = true;
    private volatile KafkaStreams currentStreams = null;
    private final long reconnectDelayMs;
    private Thread shutdownHook = null;

    /**
     * Create a alarm logger for the alarm messages (both state and configuration)
     * for a given alarm server topic.
     * This runnable will create the kafka streams for the given alarm messages which match the format 'topic'
     * 
     * @param topic - the alarm topic in kafka
     */
    public AlarmMessageLogger(String topic) {
        super();
        this.topic = topic;

        MessageParser<AlarmMessage> messageParser = new MessageParser<>(AlarmMessage.class);
        alarmMessageSerde = Serdes.serdeFrom(messageParser, messageParser);

        // Read reconnect delay from system property, default to 30 seconds
        this.reconnectDelayMs = Long.parseLong(
            System.getProperty("kafka.reconnect.delay.ms", "30000")
        );
    }

    @Override
    public void run() {
        logger.info("Starting the alarm messages stream consumer for " + topic);

        Properties props = new Properties();
        props.putAll(PropertiesHelper.getProperties());

        final String indexDateSpanUnits = props.getProperty("date_span_units");
        final boolean useDatedIndexNames = Boolean.parseBoolean(props.getProperty("use_dated_index_names"));

        try {
            stateIndexNameHelper = new IndexNameHelper(topic + STATE_INDEX_FORMAT, useDatedIndexNames, indexDateSpanUnits);
            configIndexNameHelper = new IndexNameHelper(topic + CONFIG_INDEX_FORMAT , useDatedIndexNames, indexDateSpanUnits);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Time based index creation failed.", ex);
        }

        // Register shutdown hook once before retry loop
        shutdownHook = new Thread("streams-"+topic+"-alarm-messages-shutdown-hook") {
            @Override
            public void run() {
                logger.info("Shutdown hook triggered for topic " + topic);
                shouldReconnect = false;
                if (currentStreams != null) {
                    logger.info("Closing Kafka Streams for topic " + topic);
                    currentStreams.close(Duration.of(10, ChronoUnit.SECONDS));
                    currentStreams = null;
                }
                logger.info("Shutting streams down for topic " + topic);
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        // Retry loop for handling missing topics
        while (shouldReconnect) {
            try {
                startKafkaStreams(props);
                // If we get here, streams shut down normally
                break;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to start Kafka Streams for topic " + topic +
                    ", will retry in " + reconnectDelayMs + "ms", e);

                if (!shouldReconnect) {
                    break;
                }

                try {
                    Thread.sleep(reconnectDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.info("Reconnection loop interrupted for topic " + topic);
                    break;
                }
            }
        }

        // Clean up shutdown hook when we're done
        try {
            if (shutdownHook != null) {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                shutdownHook = null;
            }
        } catch (IllegalStateException e) {
            // Ignore - shutdown already in progress
        }

        logger.info("Alarm message logger for topic " + topic + " has shut down");
    }

    private void startKafkaStreams(Properties props) throws Exception {
        logger.info("Attempting to start Kafka Streams for topic " + topic);

        Properties kafkaProps = KafkaHelper.loadPropsFromFile(props.getProperty("kafka_properties",""));
        kafkaProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-"+topic+"-alarm-messages");

        kafkaProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                props.getOrDefault(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"));

        // API requires for Consumer to be in a group.
        // Each alarm client must receive all updates,
        // cannot balance updates across a group
        // --> Use unique group for each client
        final String group_id = "Alarm-" + UUID.randomUUID();
        kafkaProps.put("group.id", group_id);

        AlarmSystemConstants.logger.fine(kafkaProps.getProperty("group.id") + " subscribes to "
                + kafkaProps.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG) + " for " + topic);
        AlarmSystemConstants.logger.fine(kafkaProps.getProperty("group.id") + " subscribes to "
                + kafkaProps.get(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG) + " for " + topic);

        // Attach a message time stamp.
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, AlarmMessage> alarms = builder.stream(topic,
                Consumed.with(Serdes.String(), alarmMessageSerde).withTimestampExtractor(new TimestampExtractor() {

                    @Override
                    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
                        return record.timestamp();
                    }
                }));

        alarms = alarms.filter((k, v) -> {
            return v != null;
        });

        alarms = alarms.map((key, value) -> {
//            logger.config("Processing alarm message with key : " + key != null ? key
//                    : "null" + " " + value != null ? value.toString() : "null");
            value.setKey(key);
            return new KeyValue<String, AlarmMessage>(key, value);
        });

        alarms.split(Named.as("alarm-"))
                .branch((k, v) -> k.startsWith("state"),
                        Branched.withConsumer(alarmStateStream -> processAlarmStateStream(alarmStateStream)))
                .branch((k, v) -> k.startsWith("config"),
                        Branched.withConsumer(alarmConfigStream -> processAlarmConfigurationStream(alarmConfigStream)))
                .defaultBranch(Branched.withConsumer(stream -> {
                    // Log each unmatched key in the default branch
                    stream.foreach((k, v) -> logger.warning("Unknown alarm message type for key: " + k));
                }));

        final KafkaStreams streams = new KafkaStreams(builder.build(), kafkaProps);

        // Store reference for cleanup (volatile ensures visibility across threads)
        currentStreams = streams;

        streams.setUncaughtExceptionHandler(exception -> {
            logger.log(Level.SEVERE, "Stream exception encountered for topic " + topic + ": " +
                exception.getMessage(), exception);

            // Check if it's a missing source topic exception
            if (exception.getCause() instanceof org.apache.kafka.streams.errors.MissingSourceTopicException ||
                exception instanceof org.apache.kafka.streams.errors.MissingSourceTopicException) {
                logger.log(Level.WARNING, "Missing source topic detected for " + topic +
                    ". Will retry connection in " + reconnectDelayMs + "ms");
                return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
            }

            // For other exceptions, stop retry
            logger.log(Level.SEVERE, "Unrecoverable stream exception for topic " + topic, exception);
            shouldReconnect = false;
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
        });

        // Simple latch to wait for streams to stop
        final CountDownLatch latch = new CountDownLatch(1);
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.NOT_RUNNING || newState == KafkaStreams.State.ERROR) {
                latch.countDown();
            }
        });

        try {
            streams.start();
            logger.info("Kafka Streams started for topic " + topic);

            // Wait for streams to stop (either due to exception or shutdown)
            latch.await();

            // If stopped due to error, throw to trigger retry
            if (streams.state() == KafkaStreams.State.ERROR) {
                throw new Exception("Streams stopped with ERROR state");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new Exception("Interrupted", e);
        } finally {
            if (currentStreams != null) {
                currentStreams.close(Duration.of(10, ChronoUnit.SECONDS));
                currentStreams = null;
            }
        }
    }

    private void processAlarmStateStream(KStream<String, AlarmMessage> alarmStateBranch) {

        KStream<String, AlarmStateMessage> transformedAlarms = alarmStateBranch
                .transform(new TransformerSupplier<String, AlarmMessage, KeyValue<String, AlarmStateMessage>>() {

                    @Override
                    public Transformer<String, AlarmMessage, KeyValue<String, AlarmStateMessage>> get() {
                        return new Transformer<>() {
                            private ProcessorContext context;

                            @Override
                            public KeyValue<String, AlarmStateMessage> transform(String key, AlarmMessage value) {
                                key = key.replace("\\", "");
                                AlarmStateMessage newValue = value.getAlarmStateMessage();
                                Matcher matcher = pattern.matcher(key);
                                newValue.setConfig(key);
                                matcher.find();
                                String[] tokens = AlarmTreePath.splitPath(key);
                                final String pv = tokens[tokens.length - 1];
                                newValue.setPv(pv);

                                newValue.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                                return new KeyValue<>(key, newValue);
                            }

                            @Override
                            public void init(ProcessorContext context) {
                                this.context = context;
                            }

                            @Override
                            public void close() {
                                // TODO Auto-generated method stub

                            }
                        };
                    }
                });

        KStream<String, AlarmStateMessage> filteredAlarms = transformedAlarms.filter((k, v) -> {
            return v != null ? v.isLeaf() : false;
        });

        // Commit to elastic
        filteredAlarms.foreach((k, v) -> {
            ElasticClientHelper.getInstance().indexAlarmStateDocuments(stateIndexNameHelper.getIndexName(v.getMessage_time()), v);
        });

    }

    private void processAlarmConfigurationStream(KStream<String, AlarmMessage> alarmConfigBranch) {
        KStream<String, AlarmConfigMessage> alarmConfigMessages = alarmConfigBranch.transform(new TransformerSupplier<String, AlarmMessage, KeyValue<String,AlarmConfigMessage>>() {

            @Override
            public Transformer<String, AlarmMessage, KeyValue<String, AlarmConfigMessage>> get() {
                return new Transformer<String, AlarmMessage, KeyValue<String, AlarmConfigMessage>>() {
                    private ProcessorContext context;
                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                    }

                    @Override
                    public KeyValue<String, AlarmConfigMessage> transform(String key, AlarmMessage value) {
                        
                        key = key.replace("\\", "");
                        if(value != null) {
                            AlarmConfigMessage newValue = value.getAlarmConfigMessage();
                            newValue.setConfig(key);
                            newValue.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                            return new KeyValue<String, AlarmConfigMessage>(key, newValue);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public void close() {
                        
                    }
                    
                };
            }
        });

        // Commit to elastic
        alarmConfigMessages.foreach((k, v) -> {
            ElasticClientHelper.getInstance().indexAlarmConfigDocuments(configIndexNameHelper.getIndexName(v.getMessage_time()), v);
        });
    }

}
