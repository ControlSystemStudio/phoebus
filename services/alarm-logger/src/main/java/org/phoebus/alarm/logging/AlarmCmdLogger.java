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
import org.phoebus.applications.alarm.messages.AlarmCommandMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.util.indexname.IndexNameHelper;

/**
 * A Runnable which consumes the alarm command messages and records them to an
 * elastic index. 
 *
 * @author Kunal Shroff
 *
 */
public class AlarmCmdLogger implements Runnable {

    private static final String INDEX_FORMAT = "_alarms_cmd";
    private final String topic;
    private final Serde<AlarmCommandMessage> alarmCommandMessageSerde;

    
    private IndexNameHelper indexNameHelper;

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
    }

    @Override
    public void run() {

        logger.info("Starting the cmd stream consumer for " + topic);

        Properties props = new Properties();
        props.putAll(PropertiesHelper.getProperties());
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-" + topic + "-alarm-cmd");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmCommandMessage> alarms = builder.stream(topic + "Command", Consumed
                .with(Serdes.String(), alarmCommandMessageSerde)
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
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-" + topic + "-alarm-cmd-shutdown-hook") {
            @Override
            public void run() {
                streams.close(10, TimeUnit.SECONDS);
                System.out.println("\nShutting cmd streams Done.");
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
