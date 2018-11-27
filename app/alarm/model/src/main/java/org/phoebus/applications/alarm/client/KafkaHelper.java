/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;

/** Alarm client model
 *
 *  <p>Given an alarm configuration name like "Accelerator",
 *  subscribes to the "Accelerator" topic for configuration updates
 *  and the "AcceleratorState" topic for alarm state updates.
 *
 *  <p>Updates from either topic are merged into an in-memory model
 *  of the complete alarm information,
 *  updating listeners with all changes.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class KafkaHelper
{
    /** Create a consumer for alarm-type topics
     *
     *  <p>De-serialize as strings.
     *
     *  @param kafka_servers Servers to read
     *  @param topics Topics to which to subscribe
     *  @param from_beginning Topics to read from the beginning
     *  @return {@link Consumer}
     */
    public static Consumer<String, String> connectConsumer(final String kafka_servers, final List<String> topics, final List<String> from_beginning)
    {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        // API requires for Consumer to be in a group.
        // Each alarm client must receive all updates,
        // cannot balance updates across a group
        // --> Use unique group for each client
        final String group_id = "Alarm-" + UUID.randomUUID();
        props.put("group.id", group_id);

        logger.fine(group_id + " subscribes to " + kafka_servers + " for " + topics);

        // Read key, value as string
        final Deserializer<String> deserializer = new StringDeserializer();
        final Consumer<String, String> consumer = new KafkaConsumer<>(props, deserializer, deserializer);

        // Rewind whenever assigned to partition
        final ConsumerRebalanceListener crl = new ConsumerRebalanceListener()
        {
            @Override
            public void onPartitionsAssigned(final Collection<TopicPartition> parts)
            {
                // For 'configuration', start reading all messages.
                // For 'commands', OK to just read commands from now on.
                for (TopicPartition part : parts)
                    if (from_beginning.contains(part.topic()))
                    {
                        consumer.seekToBeginning(List.of(part));
                        logger.info("Reading from start of " + part.topic());
                    }
                    else
                        logger.info("Reading updates for " + part.topic());
            }

            @Override
            public void onPartitionsRevoked(final Collection<TopicPartition> parts)
            {
                // Ignore
            }
        };
        consumer.subscribe(topics, crl);

        return consumer;
    }

    /** Create producer for alarm information
     *  @param kafka_servers
     *  @return {@link Producer}
     */
    public static Producer<String, String> connectProducer(final String kafka_servers)
    {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        // Collect messages for 20ms until sending them out as a batch
        props.put("linger.ms", 20);

        // Write String key, value
        final Serializer<String> serializer = new StringSerializer();

        final Producer<String, String> producer = new KafkaProducer<>(props, serializer, serializer);

        return producer;
    }

    /**
     * Aggregate multiple topics into a single topic using KafkaStreams.
     * @param kafka_servers - Sever to connect to.
     * @param topics List of topics to aggregate.
     * @param aggregate_topic - Name of topic to aggregate to.
     * @return aggregate_stream - KafkaStreams
     * @author Evan Smith
     */
    public static KafkaStreams aggregateTopics(String kafka_servers, List<String> topics, String aggregate_topic)
    {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "Stream-To-Long-Term");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kafka_servers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        // Aggregate the topics by mapping the topic key value pairs one to one into the aggregate topic.
        builder.<String, String>stream(topics).mapValues(pair -> pair).to(aggregate_topic);

        return new KafkaStreams(builder.build(), props);
    }
}
