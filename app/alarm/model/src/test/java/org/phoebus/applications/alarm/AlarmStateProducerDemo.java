/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.json.JsonModelWriter;

/** Produce alarm state records
 *
 *  <p>Create topic:
 *  <pre>
 *  bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic AcceleratorState
 *  bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --alter --entity-name AcceleratorState
 *                       --add-config cleanup.policy=compact,segment.ms=10000,min.cleanable.dirty.ratio=0.01
 *  </pre>
 *
 *  <p>Monitor updates via
 *  <pre>
 *  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --property print.key=true --property key.separator=": " --topic AcceleratorState --from-beginning
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmStateProducerDemo
{
    public static void main(final String[] argv)
    {
        String root = AlarmDemoSettings.ROOT;

        if (argv.length == 2)
            root = argv[1];

        final Properties props = new Properties();
        props.put("bootstrap.servers", AlarmDemoSettings.SERVERS);
        // Assert that all replicas have been updated
        props.put("acks", "all");

        // Required to write String key, value
        final Serializer<String> key_serializer = new StringSerializer();

        // Example for custom serializer
        final Serializer<BasicState> value_serializer = new Serializer<>()
        {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey)
            {
                // NOP
            }

            @Override
            public byte[] serialize(String topic, BasicState state)
            {
                try
                {
                    return JsonModelWriter.toJsonBytes(state, false, false);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                return new byte[0];
            }

            @Override
            public void close()
            {
                // NOP
            }
        };

        final String topic = root + "State";

        System.out.println("Publishing to " + topic);

        @SuppressWarnings("resource")
        Producer<String, BasicState> producer = new KafkaProducer<>(props, key_serializer, value_serializer);
        // Place all keys (alarm tree paths) in the same partition
        Integer partition = Integer.valueOf(0);

        while (true)
        {
            for (int i=0; i<10; ++i)
            {
                String key;
                switch ((i/2) % 2)
                {
                case 0:  key = "/Accelerator/Vacuum/Sector000002/SomePVName";
                         break;
                default: key = "/Accelerator/Vacuum/Sector000002/SomeOtherPVName";
                }

                BasicState state = new ClientState(i % 2 == 0 ? SeverityLevel.MAJOR_ACK : SeverityLevel.MAJOR, "demo",
                                                  "-10",
                                                  Instant.now(),
                                                  SeverityLevel.MAJOR, "demo");

                try
                {
                    for (int path=0; path<3; ++path)
                    {
                        final ProducerRecord<String, BasicState> record = new ProducerRecord<>(topic, partition, key, state);
                        // System.out.println(producer.send(record).get());
                        producer.send(record);
                        final int sep = key.lastIndexOf('/');
                        key = key.substring(0, sep);
                        state = new BasicState(state.severity);
                    }
                    // Results in just under 300 messages/sec
                    Thread.sleep(10);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            producer.flush();
            // Thread.sleep(5000);
        }
        // producer.close();
    }
}
