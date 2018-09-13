package org.phoebus.alarm.logging;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

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
import org.epics.pvdata.property.Alarm;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.messages.MessageParser;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AlarmConfigLogger implements Runnable {

    private final String topic;
    private Properties props;

    private MessageParser<AlarmConfigMessage> messageParser = new MessageParser<AlarmConfigMessage>(AlarmConfigMessage.class);
    private final Serde<AlarmConfigMessage> alarmConfigMessageSerde = Serdes.serdeFrom(messageParser, messageParser);

    // TODO convert this to a preference.
    private final String location = "C:\\AlarmConfig";
    private final File root;
    private final String group_id;

    // The alarm tree model which holds the current state of the alarm server
    private final AlarmClient model;

    public AlarmConfigLogger(String topic) {
        super();
        this.topic = topic;

        group_id = "Alarm-" + UUID.randomUUID();

        props = PropertiesHelper.getProperties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-" + this.topic + "-alarm-config");
        if (!props.containsKey(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        props.put("group.id", group_id);
        // make sure to consume the complete topic via "auto.offset.reset = earliest"
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        root = new File(location, this.topic);
        root.mkdirs();

        model = new AlarmClient(props.getProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG), this.topic);
    }

    KafkaStreams streams = null;

    private Consumer<String, String> consumer;

    @Override
    public void run() {

        try {
            consumer = new KafkaConsumer<>(props, Serdes.String().deserializer(), Serdes.String().deserializer());

            // Rewind whenever assigned to partition
            final ConsumerRebalanceListener crl = new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsAssigned(final Collection<TopicPartition> parts) {
                    // Ignore
                }

                @Override
                public void onPartitionsRevoked(final Collection<TopicPartition> parts) {
                    // Ignore
                }
            };
            consumer.subscribe(List.of(this.topic), crl);
            final ConsumerRecords<String, String> records = consumer.poll(1000);
            syncAlarmConfigRepository(records);
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            StreamsBuilder builder = new StreamsBuilder();

            KStream<String, String> alarms = builder.stream(topic, Consumed.with(Serdes.String(), Serdes.String()));
            alarms.process(new ProcessorSupplier<String, String>() {
                @Override
                public Processor<String, String> get() {
                    return new ProcessAlarmConfigMessage();
                }
            });

            Topology topology = builder.build();
            logger.config(topology.describe().toString());
            streams = new KafkaStreams(topology, new StreamsConfig(props));
        } catch (Exception e1) {
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

    ObjectMapper objectMapper = new ObjectMapper();
    /**
     * Process a single alarm configuration event
     * 
     * @param path
     * @param alarm_config
     */
    private synchronized void processAlarmConfigMessages(String path, String alarm_config) {
        try {
            logger.log(Level.INFO, "processing message:" + path + ":" + alarm_config);
            objectMapper.readValue(alarm_config, AlarmConfigMessage.class);
            if (alarm_config != null) {
                path = path.replace(":", "");
                File node = Paths.get(root.getParent(), path).toFile();
                if (!node.mkdirs()) {
                    logger.log(Level.WARNING, "Alarm config logging failed for path " + path + ", config " + alarm_config);
                }
                File node_info = new File(node, "alarm_config.json");
                try (FileWriter fo = new FileWriter(node_info)) {
                    fo.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readValue(alarm_config, Object.class)));
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                            "Alarm config logging failed for path " + path + ", config " + alarm_config, e);
                }
            }
            System.out.println(path + " " + alarm_config);
        } catch (final Exception ex) {
            logger.log(Level.WARNING, "Alarm state check error for path " + path + ", config " + alarm_config, ex);
        }
    }

    /**
     * Sync the local git repository with the config state as calculated from the
     * consumer records
     * 
     * @param messages
     */
    private synchronized void syncAlarmConfigRepository(ConsumerRecords<String, String> messages) {
        for (final ConsumerRecord<String, String> record : messages) {
            processAlarmConfigMessages(record.key(), record.value());
        }
    }

    private class ProcessAlarmConfigMessage implements Processor<String, String> {

        @Override
        public void init(ProcessorContext context) {
        }

        @Override
        public synchronized void process(String key, String value) {
            processAlarmConfigMessages(key, value);
        }

        @Override
        public void punctuate(long timestamp) {
        }

        @Override
        public void close() {
        }

    }

}
