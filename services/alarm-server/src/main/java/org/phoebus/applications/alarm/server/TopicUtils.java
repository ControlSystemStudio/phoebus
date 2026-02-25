/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.alarm.server;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;
import org.phoebus.applications.alarm.client.KafkaHelper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Provides a utility to create the Kafka topics if needed, and then
 * configure them based on preferences.
 */
public class TopicUtils {

    private static final Logger logger = Logger.getLogger(TopicUtils.class.getName());

    /**
     * Ensure that the required Kafka topics exist and are correctly configured.
     * <p>
     * Creates and configures the main alarm topic (compacted) and command/talk topics (deleted).
     * For more details on alarm topic configuration, see:
     * Refer to <a href="https://github.com/ControlSystemStudio/phoebus/tree/master/app/alarm#configure-alarm-topics">Configure Alarm Topics</a>
     *
     * @param server           Kafka server
     * @param topic            Base topic name
     * @param kafkaPropsFile Extra Kafka properties file
     * @throws Exception If for instance an admin client could not be created or
     * if the request to Kafka times out.
     */
    public static void ensureKafkaTopics(String server, String topic, String kafkaPropsFile) throws Exception {
        var kafkaProps = KafkaHelper.loadPropsFromFile(kafkaPropsFile);
        kafkaProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, server);
        try (AdminClient admin = AdminClient.create(kafkaProps)) {
            Set<String> topics = admin.listTopics().names().get(60, TimeUnit.SECONDS);
            // Compacted topic
            if (!topics.contains(topic)) {
                createTopic(admin, topic);
            }
            setCompactedConfig(admin, topic);

            // Deleted topics
            for (String suffix : List.of("Command", "Talk")) {
                String deletedTopic = topic + suffix;
                if (!topics.contains(deletedTopic)) {
                    createTopic(admin, deletedTopic);
                }
                setDeletedConfig(admin, deletedTopic);
            }
        }
    }

    /**
     * Create topics
     *
     * @param admin Admin client
     * @param topic Topic name
     * @throws Exception If topic could not be created
     */
    private static void createTopic(AdminClient admin, String topic) throws Exception {
        NewTopic newTopic = new NewTopic(topic, 1, (short) 1);
        try {
            admin.createTopics(List.of(newTopic)).all().get();
            logger.info("Created topic: " + topic);
        } catch (Exception e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                logger.info("Topic already exists: " + topic);
            } else {
                throw e;
            }
        }
    }

    /**
     * Configure topic for alarm state storage with compaction to retain latest state.
     * For configuration information, see:
     * <p>
     * Refer to <a href="https://github.com/ControlSystemStudio/phoebus/tree/master/app/alarm#configure-alarm-topics">Configure Alarm Topics</a>
     *
     * @param admin Admin client
     * @param topic Topic name
     * @throws Exception If topic could not be configured
     */
    private static void setCompactedConfig(AdminClient admin, String topic) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        List<AlterConfigOp> configOps = List.of(
                new AlterConfigOp(new ConfigEntry("cleanup.policy", "compact"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("segment.ms", "10000"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("min.cleanable.dirty.ratio", "0.01"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("min.compaction.lag.ms", "1000"), AlterConfigOp.OpType.SET)
        );
        admin.incrementalAlterConfigs(Map.of(resource, configOps)).all().get();
        logger.info("Set compacted config for topic: " + topic);
    }

    /**
     * Configure topic for command/talk messages with time-based deletion.
     * For configuration information, see:
     * <p>
     * Refer to <a href="https://github.com/ControlSystemStudio/phoebus/tree/master/app/alarm#configure-alarm-topics">Configure Alarm Topics</a>
     *
     * @param admin Admin client
     * @param topic Topic name
     * @throws Exception If topic could not be configured
     */
    private static void setDeletedConfig(AdminClient admin, String topic) throws Exception {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
        List<AlterConfigOp> configOps = List.of(
                new AlterConfigOp(new ConfigEntry("cleanup.policy", "delete"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("segment.ms", "10000"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("min.cleanable.dirty.ratio", "0.01"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("min.compaction.lag.ms", "1000"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("retention.ms", "20000"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("delete.retention.ms", "1000"), AlterConfigOp.OpType.SET),
                new AlterConfigOp(new ConfigEntry("file.delete.delay.ms", "1000"), AlterConfigOp.OpType.SET)
        );
        admin.incrementalAlterConfigs(Map.of(resource, configOps)).all().get();
        logger.info("Set deleted config for topic: " + topic);
    }
}
