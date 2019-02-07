package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

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
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.util.indexname.IndexNameHelper;

/**
 * A Runnable which consumes some of the alarm configuration messages and records them to an
 * elastic index. 
 *
 * @author Kunal Shroff
 *
 */
public class AlarmConfigLogger implements Runnable {

    private static final String INDEX_FORMAT = "_alarms_config";
    private final String topic;
    private final Serde<AlarmConfigMessage> alarmConfigMessageSerde;

    
    private IndexNameHelper indexNameHelper;

    /**
     * Create a alarm configuration message logger for the given topic.
     * @param topic
     * @throws Exception
     */
    public AlarmConfigLogger(String topic) throws Exception {
        super();
        this.topic = topic;

        MessageParser<AlarmConfigMessage> messageParser = new MessageParser<AlarmConfigMessage>(AlarmConfigMessage.class);
        alarmConfigMessageSerde = Serdes.serdeFrom(messageParser, messageParser);
    }

    @Override
    public void run() {

        logger.info("Starting the config stream consumer for " + topic);

        Properties props = new Properties();
        props.putAll(PropertiesHelper.getProperties());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-" + topic + "-alarm-config");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmConfigMessage> alarms = builder.stream(topic, Consumed
                .with(Serdes.String(), alarmConfigMessageSerde)
                .withTimestampExtractor(new TimestampExtractor() {

                    @Override
                    public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
                        return record.timestamp();
                    }
                }));

        final String indexDateSpanUnits = props.getProperty("date_span_units");
        final Integer indexDateSpanValue = Integer.parseInt(props.getProperty("date_span_value"));

        try {
            indexNameHelper = new IndexNameHelper(topic + INDEX_FORMAT , indexDateSpanUnits, indexDateSpanValue);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Time based index creation failed.", ex);
        }
        KStream<String, AlarmConfigMessage> timeStampedAlarms = alarms.transform(new TransformerSupplier<String, AlarmConfigMessage, KeyValue<String,AlarmConfigMessage>>() {

            @Override
            public Transformer<String, AlarmConfigMessage, KeyValue<String, AlarmConfigMessage>> get() {
                return new Transformer<String, AlarmConfigMessage, KeyValue<String, AlarmConfigMessage>>() {
                    private ProcessorContext context;
                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                    }

                    @Override
                    public KeyValue<String, AlarmConfigMessage> transform(String key, AlarmConfigMessage value) {
                        key = key.replace("\\", "");
                        if(value != null) {
                            value.setConfig(key);
                            value.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                            return new KeyValue<String, AlarmConfigMessage>(key, value);
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
        timeStampedAlarms.foreach((k, v) -> {
            String topic_name = indexNameHelper.getIndexName(v.getMessage_time());
            ElasticClientHelper.getInstance().indexAlarmConfigDocument(topic_name, v);
        });
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-" + topic + "-alarm-config-shutdown-hook") {
            @Override
            public void run() {
                streams.close(10, TimeUnit.SECONDS);
                System.out.println("\nShutting config streams Done.");
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

}
