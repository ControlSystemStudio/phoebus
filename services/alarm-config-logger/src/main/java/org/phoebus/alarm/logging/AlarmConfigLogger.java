package org.phoebus.alarm.logging;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.MessageParser;

public class AlarmConfigLogger implements Runnable {

    private final String topic;

    private final Pattern pattern = Pattern.compile("(\\w*://\\S*)");
    private Properties props;

    private MessageParser<AlarmConfigMessage> messageParser = new MessageParser<AlarmConfigMessage>(
            AlarmConfigMessage.class);
    private final Serde<AlarmConfigMessage> alarmConfigMessageSerde = Serdes.serdeFrom(messageParser, messageParser);

    // TODO convert this to a preference.
    private final String location = "C:\\AlarmConfig";
    private final File root;

    public AlarmConfigLogger(String topic) {
        super();
        this.topic = topic;

        props = PropertiesHelper.getProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-" + topic + "-alarm-config");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        final String group_id = "Alarm-" + UUID.randomUUID();
        props.put("group.id", group_id);
        // make sure to consume the complete topic via "auto.offset.reset = earliest"
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        root = new File(location, topic);
        root.mkdirs();

        initialize();
    }

    private void initialize() {
        // TODO Auto-generated method stub
        logger.info("Initialize the alarm config logger server");
    }

    KafkaStreams streams = null;

    private Consumer<String, AlarmConfigMessage> consumer;

    @Override
    public void run() {

        try {
            consumer = new KafkaConsumer<>(props, Serdes.String().deserializer(), alarmConfigMessageSerde.deserializer());

            // Rewind whenever assigned to partition
            final ConsumerRebalanceListener crl = new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsAssigned(final Collection<TopicPartition> parts) {
                    consumer.beginningOffsets(parts).entrySet().forEach((entry) -> {
                        System.out.println("beginnings: " + entry.getKey() + " - " + entry.getValue());
                    });
                    consumer.endOffsets(parts).entrySet().forEach((entry) -> {
                        System.out.println("ending: " + entry.getKey() + " - " + entry.getValue());
                    });
                    parts.forEach(part -> {
                        System.out.println("before" + part + " " + consumer.position(part));
                    });
                    consumer.seekToBeginning(parts);
                    parts.forEach(part -> {
                        System.out.println("after" + part + " " + consumer.position(part));
                    });
                    logger.info("Reading from start of ");
                    parts.stream().forEach(System.out::println);
                }

                @Override
                public void onPartitionsRevoked(final Collection<TopicPartition> parts) {
                    // Ignore
                }
            };
            consumer.subscribe(List.of(this.topic), crl);
            final ConsumerRecords<String, AlarmConfigMessage> records = consumer.poll(1000);
            recreateAlarmConfig(records);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            StreamsBuilder builder = new StreamsBuilder();

            KStream<String, AlarmConfigMessage> alarms = builder.stream(topic,
                    Consumed.with(Serdes.String(), alarmConfigMessageSerde));
            alarms.process(new ProcessorSupplier<String, AlarmConfigMessage>() {
                @Override
                public Processor<String, AlarmConfigMessage> get() {
                    return new ProcessAlarmConfigMessage();
                }
            });

            Topology topology = builder.build();
            System.out.println(topology.describe());
            // props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-2" + topic +
            // "-alarm-config");
            streams = new KafkaStreams(topology, new StreamsConfig(props));
            System.out.println("ssss");
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-" + topic + "-alarm-shutdown-hook") {
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

    private synchronized void recreateAlarmConfig(ConsumerRecords<String, AlarmConfigMessage> messages) {

        for (final ConsumerRecord<String, AlarmConfigMessage> record : messages) {
            final String path = record.key();
            final AlarmConfigMessage node_config = record.value();
            logger.log(Level.INFO, "processing message:" + path + ":" + node_config);
            if (node_config != null) {
                File node = Paths.get(root.getParent(), path).toFile();
                if (node_config.isLeaf()) {
                    System.out.println("pv");
                    try (FileWriter fo = new FileWriter(node)) {
                        fo.write(node_config.toString());
                    } catch (IOException e) {
                        logger.log(Level.WARNING,
                                "Alarm config logging failed for path " + path + ", config " + node_config, e);
                    }
                } else {
                    if (!node.mkdirs()) {
                        logger.log(Level.WARNING,
                                "Alarm config logging failed for path " + path + ", config " + node_config);
                    }
                    if (node_config != null ) {
                        File node_info = new File(node, ".node_config");
                        try (FileWriter fo = new FileWriter(node_info)) {
                            fo.write(node_config.toString());
                        } catch (IOException e) {
                            logger.log(Level.WARNING,
                                    "Alarm config logging failed for path " + path + ", config " + node_config, e);
                        }
                    }
                }
            }
            try {
                System.out.println(path + " " + node_config);
            } catch (final Exception ex) {
                logger.log(Level.WARNING, "Alarm state check error for path " + path + ", config " + node_config, ex);
            }

        }

    }

    private static class ProcessAlarmConfigMessage implements Processor<String, AlarmConfigMessage> {

        @Override
        public void init(ProcessorContext context) {
            // TODO Auto-generated method stub

        }

        @Override
        public synchronized void process(String key, AlarmConfigMessage value) {
            System.out.println(key + " : " + value);
        }

        @Override
        public void punctuate(long timestamp) {
            // TODO Auto-generated method stub

        }

        @Override
        public void close() {
            // TODO Auto-generated method stub

        }

    }

}
