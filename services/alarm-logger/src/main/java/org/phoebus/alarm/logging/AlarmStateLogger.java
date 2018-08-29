package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.kstream.TransformerSupplier;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.StateStore;
import org.apache.kafka.streams.processor.TimestampExtractor;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.util.indexname.IndexNameHelper;

public class AlarmStateLogger implements Runnable {

    private final String topic;
    private Map<String, Object> serdeProps;
    private final Serde<AlarmStateMessage> alarmStateMessageSerde;
    
    private final Pattern pattern = Pattern.compile("(\\w*://\\S*)");

    private IndexNameHelper indexNameHelper;

    public AlarmStateLogger(String topic) throws Exception {
        super();
        this.topic = topic;

        MessageParser<AlarmStateMessage> messageParser = new MessageParser<AlarmStateMessage>(AlarmStateMessage.class);
        alarmStateMessageSerde = Serdes.serdeFrom(messageParser, messageParser);
    }

    @Override
    public void run() {
        logger.info("Starting the stream consumer");

        Properties props = PropertiesHelper.getProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-"+topic+"-alarm-state");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, AlarmStateMessage> alarms = builder.stream(topic+"State",
                Consumed.with(Serdes.String(), alarmStateMessageSerde)
                        .withTimestampExtractor(new TimestampExtractor() {
                            
                            @Override
                            public long extract(ConsumerRecord<Object, Object> record, long previousTimestamp) {
                                return record.timestamp();
                            }
                        }));

        // Filter the alarms to only
        KStream<String, AlarmStateMessage> filteredAlarms = alarms.filter((k, v) -> {
            return v != null ? v.isLeaf() : false;
        });

        // transform the alarm messages, include the pv and config path
        // create store
        StoreBuilder<KeyValueStore<String,AlarmStateMessage>> keyValueStoreBuilder =
                Stores.keyValueStoreBuilder(Stores.lruMap(topic+"_state_store", 100),
                        Serdes.String(),
                        alarmStateMessageSerde);
        // register store
        builder.addStateStore(keyValueStoreBuilder);

        KStream<String, AlarmStateMessage> transformedAlarms = filteredAlarms.transform(new TransformerSupplier<String, AlarmStateMessage, KeyValue<String,AlarmStateMessage>>() {

            @Override
            public Transformer<String, AlarmStateMessage, KeyValue<String, AlarmStateMessage>> get() {
                return new Transformer<String, AlarmStateMessage, KeyValue<String, AlarmStateMessage>>() {
                    private ProcessorContext context;
                    private StateStore state;

                    @Override
                    public KeyValue<String, AlarmStateMessage> transform(String key, AlarmStateMessage value) {
                        key = key.replace("\\", "");
                        Matcher matcher = pattern.matcher(key);
                        value.setConfig(key);
                        matcher.find();
                        String[] tokens = AlarmTreePath.splitPath(key);
                        value.setPv(tokens[tokens.length - 1]);
                        value.setMessage_time(Instant.ofEpochMilli(context.timestamp()));
                        return new KeyValue<String, AlarmStateMessage>(key, value);
                    }

                    @Override
                    public void init(ProcessorContext context) {
                        this.context = context;
                        this.state = context.getStateStore(topic+"_state_store");
                    }

                    @Override
                    public KeyValue<String, AlarmStateMessage> punctuate(long timestamp) {
                        return null;
                    }

                    @Override
                    public void close() {
                        // TODO Auto-generated method stub
                        
                    }
                };
            }
        }, topic+"_state_store");

        final String indexDateSpanUnits = props.getProperty("date_span_units");
        final Integer indexDateSpanValue = Integer.parseInt(props.getProperty("date_span_value"));

        logger.info("Index " + topic + "* using date span units of '" + indexDateSpanUnits + "'.");
        logger.info("Index " + topic + "* using date span value of '" + indexDateSpanValue + "'.");
        
        try
        {
            indexNameHelper = new IndexNameHelper(topic + "_alarms", indexDateSpanUnits, indexDateSpanValue);
        } 
        catch (Exception ex)
        {
            logger.log(Level.SEVERE, "Time based index creation failed.", ex);
        }

        // Commit to elastic
        transformedAlarms.foreach((k, v) -> {
            String topic_name = indexNameHelper.getIndexName(v.getMessage_time());
            ElasticClientHelper.getInstance().indexAlarmStateDocument(topic_name, v);
        });
        final KafkaStreams streams = new KafkaStreams(builder.build(), props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-"+topic+"-alarm-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
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
