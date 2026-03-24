package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.framework.preferences.PreferencesReader;
import org.phoebus.util.indexname.IndexNameHelper;

/**
 * A Runnable which consumes the alarm command messages and records them to an
 * elastic index. 
 *
 * @author Kunal Shroff
 *
 */
public class AlarmCmdLogger implements Runnable {

    private static final PreferencesReader prefs = new PreferencesReader(AlarmLoggingService.class, "/application.properties");

    private static final String INDEX_FORMAT = "_alarms_cmd";
    private final String topic;
    private final Serde<AlarmCommandMessage> alarmCommandMessageSerde;

    
    private IndexNameHelper indexNameHelper;

    private volatile boolean shouldReconnect = true;
    private volatile KafkaStreams currentStreams = null;
    private final long reconnectDelayMs;
    private Thread shutdownHook = null;

    /**
     * Create a alarm command message logger for the given topic. 
     * This runnable will create the kafka streams for the given alarm messages which match the format 'topicCommand'
     * @param topic the alarm topic
     * @throws Exception - parsing the alarm command messages
     */
    public AlarmCmdLogger(String topic) throws Exception {
        super();
        this.topic = topic;

        MessageParser<AlarmCommandMessage> messageParser = new MessageParser<AlarmCommandMessage>(AlarmCommandMessage.class);
        alarmCommandMessageSerde = Serdes.serdeFrom(messageParser, messageParser);

        // Read reconnect delay from system property, default to 30 seconds
        this.reconnectDelayMs = Long.parseLong(
            System.getProperty("kafka.reconnect.delay.ms", "30000")
        );
    }

    @Override
    public void run() {
        logger.info("Starting the cmd stream consumer for " + topic);

        Properties props = new Properties();
        props.putAll(PropertiesHelper.getProperties());

        final String indexDateSpanUnits = props.getProperty("date_span_units");
        final boolean useDatedIndexNames = Boolean.parseBoolean(props.getProperty("use_dated_index_names"));

        try {
            indexNameHelper = new IndexNameHelper(topic + INDEX_FORMAT, useDatedIndexNames, indexDateSpanUnits);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Time based index creation failed.", ex);
        }

        // Register shutdown hook once before retry loop
        shutdownHook = new Thread("streams-" + topic + "-alarm-cmd-shutdown-hook") {
            @Override
            public void run() {
                logger.info("Shutdown hook triggered for topic " + topic);
                shouldReconnect = false;
                if (currentStreams != null) {
                    logger.info("Closing Kafka Streams for topic " + topic);
                    currentStreams.close(Duration.of(10, ChronoUnit.SECONDS));
                    currentStreams = null;
                }
                logger.info("Shutting cmd streams down for topic " + topic);
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

        logger.info("Alarm cmd logger for topic " + topic + " has shut down");
    }

    private void startKafkaStreams(Properties props) throws Exception {
        logger.info("Attempting to start Kafka Streams for topic " + topic);

        Properties kafkaProps = KafkaHelper.loadPropsFromFile(props.getProperty("kafka_properties",""));
        kafkaProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-" + topic + "-alarm-cmd");

        kafkaProps.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                props.getOrDefault(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092"));

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmCommandMessage> alarms = builder.stream(topic + "Command", Consumed
                .with(Serdes.String(), alarmCommandMessageSerde)
                .withTimestampExtractor(new TimestampExtractor() {

                    @Override
                    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
                        return record.timestamp();
                    }
                }));

        KStream<String, AlarmCommandMessage> timeStampedAlarms = alarms.transform(new TransformerSupplier<String, AlarmCommandMessage, KeyValue<String,AlarmCommandMessage>>() {

            @Override
            public Transformer<String, AlarmCommandMessage, KeyValue<String, AlarmCommandMessage>> get() {
                return new Transformer<String, AlarmCommandMessage, KeyValue<String, AlarmCommandMessage>>() {
                    private ProcessorContext context;
                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                    }

                    @Override
                    public KeyValue<String, AlarmCommandMessage> transform(String key, AlarmCommandMessage value) {
                        key = key.replace("\\", "");
                        value.setConfig(key);
                        value.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                        return new KeyValue<String, AlarmCommandMessage>(key, value);
                    }

                    @Override
                    public void close() {
                        
                    }
                    
                };
            }
        });

        // Commit to elastic
        timeStampedAlarms.foreach((k, v) -> {
            String topic_name = indexNameHelper.getIndexName(v.getMessage_time());
            ElasticClientHelper.getInstance().indexAlarmCmdDocument(topic_name, v);
        });

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

}
