/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.phoebus.applications.alarm.AlarmSystem;

/** Create alarm topics
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class CreateTopics
{
    // Default configuration for each topic.
    private static final short REPLICATION_FACTOR = 1;
    private static final int PARTITIONS = 1;
    private static final String cleanup_policy = "cleanup.policy",
                                policy         = "compact",
                                segment_time   = "segment.ms",
                                time           = "10000",
                                dirty2clean    = "min.cleanable.dirty.ratio",
                                ratio          = "0.01";

    /** Discover the currently active Kafka topics, and creates any that are missing.
     *
     *  @param kafka_servers The network address for the kafka_servers. Example: 'localhost:9092'.
     *  @param config Alarm config, for example "Accelerator"
     */
    public static void discoverAndCreateTopics (final String kafka_servers, final String config)
    {
        // Connect to Kafka server.
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        final AdminClient client = AdminClient.create(props);

        final List<String> topics_to_create = discoverTopics(client, config);
        createTopics(client, topics_to_create);

        client.close();
    }

    /**
     * <p> Discover any currently active Kafka topics. Return a list of strings filled with any default topics that need to be created.
     * @param client
     * @return topics_to_create <code>List</code> of <code>Strings</code> with all the topic names that need to be created.
     *                           Returns <code>null</code> if none need to be created.
     */
    private static List<String> discoverTopics(final AdminClient client, final String config)
    {
        final List<String> topics_to_create = new ArrayList<>();

        // Discover what topics currently exist.
        try
        {
            final ListTopicsResult res = client.listTopics();
            final KafkaFuture<Set<String>> topics = res.names();
            final Set<String> topic_names = topics.get();

            if (! topic_names.contains(config))
                topics_to_create.add(config);
            if (! topic_names.contains(config + AlarmSystem.STATE_TOPIC_SUFFIX))
                topics_to_create.add(config + AlarmSystem.STATE_TOPIC_SUFFIX);
            if (! topic_names.contains(config + AlarmSystem.COMMAND_TOPIC_SUFFIX))
                topics_to_create.add(config + AlarmSystem.COMMAND_TOPIC_SUFFIX);
            if (! topic_names.contains(config + AlarmSystem.TALK_TOPIC_SUFFIX))
                topics_to_create.add(config + AlarmSystem.TALK_TOPIC_SUFFIX);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Unable to list topics. Automatic topic detection failed.", ex);
        }

        return topics_to_create;
    }

    /** Create a topic for each of the topics in the passed list.
     *  @param client {@link AdminClient}
     *  @param topics_to_create {@link List} of {@link String}s filled with the names of topics to create.
     */
    private static void createTopics(final AdminClient client, final List<String> topics_to_create)
    {
        // Create the new topics locally.
        final List<NewTopic> new_topics = new ArrayList<>();
        for (String topic : topics_to_create)
        {
                logger.info("Creating topic '" + topic + "'");
                new_topics.add(createTopic(client, topic));
        }
        // Create the new topics in the Kafka server.
        try
        {
            final CreateTopicsResult res = client.createTopics(new_topics);
            final KafkaFuture<Void> future = res.all();
            future.get();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Attempt to create topics failed", ex);
        }
    }

    /** Create a Kafka topic with the passed name.
     *  @param client {@link AdminClient}
     *  @param topic_name Name of the topic to be created.
     *  @return new_topic The newly created topic.
     */
    private static NewTopic createTopic(final AdminClient client, final String topic_name)
    {
        final NewTopic new_topic = new NewTopic(topic_name, PARTITIONS, REPLICATION_FACTOR);
        final Map<String, String> configs = new HashMap<>();
        configs.put(cleanup_policy, policy);
        configs.put(segment_time, time);
        configs.put(dirty2clean, ratio);
        return new_topic.configs(configs);
    }
}
