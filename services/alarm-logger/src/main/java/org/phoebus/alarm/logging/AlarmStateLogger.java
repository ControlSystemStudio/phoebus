package org.phoebus.alarm.logging;

import static org.phoebus.alarm.logging.AlarmLoggingService.logger;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.phoebus.applications.alarm.messages.AlarmStateMessage;
import org.phoebus.applications.alarm.messages.MessageParser;
import org.phoebus.applications.alarm.model.AlarmTreePath;

public class AlarmStateLogger implements Runnable {

    private final String topic;
    private Map<String, Object> serdeProps;
    private final Serde<AlarmStateMessage> alarmStateMessageSerde;
    
    private final Pattern pattern = Pattern.compile("(\\w*://\\S*)");

    public AlarmStateLogger(String topic) {
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
                Consumed.with(Serdes.String(), alarmStateMessageSerde));

        // Filter the alarms to only
        KStream<String, AlarmStateMessage> filteredAlarms = alarms.filter((k, v) -> {
            return v != null ? v.isLeaf() : false;
        });

        // transform the alarm messages, include the pv and config path
        KStream<String, AlarmStateMessage> transformedAlarms = filteredAlarms
                .map(new KeyValueMapper<String, AlarmStateMessage, KeyValue<String, AlarmStateMessage>>() {

                    @Override
                    public KeyValue<String, AlarmStateMessage> apply(String key, AlarmStateMessage value) {
                        key = key.replace("\\", "");
                        Matcher matcher = pattern.matcher(key);
                        value.setConfig(key);
                        matcher.find();
                        String[] tokens = AlarmTreePath.splitPath(key);
                        value.setPv(tokens[tokens.length - 1]);
                        return new KeyValue<String, AlarmStateMessage>(key, value);
                    }
                });
        // Commit to elastic
        transformedAlarms.foreach((k, v) -> {
            ElasticClientHelper.getInstance().indexAlarmStateDocument(topic + "_alarms", v);
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
