package org.phoebus.alarm.logging;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.util.FS;
import org.phoebus.applications.alarm.client.AlarmClient;
import org.phoebus.applications.alarm.messages.AlarmConfigMessage;
import org.phoebus.applications.alarm.model.xml.XmlModelWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AlarmConfigLogger implements Runnable {

    private final String topic;
    private Properties props;

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
        model.start();

        initialize();
    }

    private void initialize() {
        // Check if the local git repository exists.
        if (!root.isDirectory()) {
            root.mkdirs();
        }
        if (!RepositoryCache.FileKey.isGitRepository(root, FS.detect())) {
            // Not present or not a Git repository. Create a new git repo
            try (Git git = Git.init().setDirectory(root).setBare(false).call()) {
                logger.log(Level.INFO, "Created repository: " + git.getRepository().getDirectory());
            } catch (IllegalStateException | GitAPIException e) {
                e.printStackTrace();
            }
        }

        writeAlarmModel();
        try (Consumer<String, String> consumer = new KafkaConsumer<String, String>(props,
                Serdes.String().deserializer(), Serdes.String().deserializer());) {

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
            e1.printStackTrace();
        }
        // Commit the initialized git repo
        try (Git git = Git.open(root)) {
            git.add().addFilepattern(".").call();
            git.commit().setAll(true).setMessage("Dump of the alarm configuration of the server").call();
        } catch (GitAPIException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    KafkaStreams streams = null;

    @Override
    public void run() {

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
     * @param commit
     */
    private synchronized void processAlarmConfigMessages(String path, String alarm_config, boolean commit) {
        try {
            logger.log(Level.INFO, "processing message:" + path + ":" + alarm_config);
            if (alarm_config != null) {
//                objectMapper.readValue(alarm_config, AlarmConfigMessage.class);
                path = path.replace(":\\/\\/", "_");
                File node = Paths.get(root.getParent(), path).toFile();
                node.mkdirs();
                File node_info = new File(node, "alarm_config.json");
                try (FileWriter fo = new FileWriter(node_info)) {
                    fo.write(objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(objectMapper.readValue(alarm_config, Object.class)));
                } catch (IOException e) {
                    logger.log(Level.WARNING,
                            "Alarm config logging failed for path " + path + ", config " + alarm_config, e);
                }
            } else {
                path = path.replace(":\\/\\/", "_");
                Path directory = Paths.get(root.getParent(), path);
                Files.walk(directory).map(Path::toFile).forEach(File::delete);
                directory.toFile().delete();
            }
            if(commit) {
             // Commit the initialized git repo
                try (Git git = Git.open(root)) {
                    git.add().addFilepattern(".").call();
                    git.commit().setAll(true).setMessage("Alarm config update "+path).call();
                } catch (GitAPIException | IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
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
            processAlarmConfigMessages(record.key(), record.value(), false);
        }
    }

    private class ProcessAlarmConfigMessage implements Processor<String, String> {

        @Override
        public void init(ProcessorContext context) {
        }

        @Override
        public synchronized void process(String key, String value) {
            processAlarmConfigMessages(key, value, true);
        }

        @Override
        public void punctuate(long timestamp) {
        }

        @Override
        public void close() {
        }

    }

    private synchronized void writeAlarmModel() {
        // Output the model to the restore-able scripts folder.
        File node = Paths.get(root.getPath(), ".restore-script").toFile();
        if (!node.mkdirs()) {
            logger.log(Level.WARNING, "Alarm config logging failed to create .restore-script folder");
        }
        File node_info = new File(node, "config.xml");
        try (OutputStream fo = Files.newOutputStream(node_info.toPath());
                XmlModelWriter modelWriter = new XmlModelWriter(fo);) {
            modelWriter.write(model.getRoot());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Alarm config logging failed to dump the alarm configuration to config.xml", e);
        }
    }

}
