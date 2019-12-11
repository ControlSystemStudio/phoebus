package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.TimestampExtractor;
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

    }

    @Override
    public void run() {
        logger.info("Starting the alarm messages stream consumer for " + topic);

        Properties props = new Properties();
        props.putAll(PropertiesHelper.getProperties());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-"+topic+"-alarm-messages");

        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        
        
        final String indexDateSpanUnits = props.getProperty("date_span_units");
        final Integer indexDateSpanValue = Integer.parseInt(props.getProperty("date_span_value"));

        try {
            stateIndexNameHelper = new IndexNameHelper(topic + STATE_INDEX_FORMAT, indexDateSpanUnits, indexDateSpanValue);
            configIndexNameHelper = new IndexNameHelper(topic + CONFIG_INDEX_FORMAT , indexDateSpanUnits, indexDateSpanValue);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Time based index creation failed.", ex);
        }
        
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
            logger.config("Processing alarm message with key : " + key != null ? key
                    : "null" + " " + value != null ? value.toString() : "null");
            value.setKey(key);
            return new KeyValue<String, AlarmMessage>(key, value);
        });

        @SuppressWarnings("unchecked")
        KStream<String, AlarmMessage>[] alarmBranches = alarms.branch((k,v) -> k.startsWith("state"),
                                                                      (k,v) -> k.startsWith("config"),
                                                                      (k,v) -> false
                                                                     );

        processAlarmStateStream(alarmBranches[0], props);
        processAlarmConfigurationStream(alarmBranches[1], props);

        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-"+topic+"-alarm-messages-shutdown-hook") {
            @Override
            public void run() {
                streams.close(10, TimeUnit.SECONDS);
                System.out.println("\nShutting streams Done.");
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    private void processAlarmStateStream(KStream<String, AlarmMessage> alarmStateBranch, Properties props) {

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
            String topic_name = stateIndexNameHelper.getIndexName(v.getMessage_time());
            ElasticClientHelper.getInstance().indexAlarmStateDocument(topic_name, v);
        });

    }

    private void processAlarmConfigurationStream(KStream<String, AlarmMessage> alarmConfigBranch, Properties props) {
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
            String topic_name = configIndexNameHelper.getIndexName(v.getMessage_time());
            ElasticClientHelper.getInstance().indexAlarmConfigDocument(topic_name, v);
        });
    }

}
