/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Test;
import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.TitleDetail;
import org.phoebus.applications.alarm.model.json.JsonModelWriter;

/** Produce alarm config example updates
 *
 *  <p>Create topic:
 *  <pre>
 *  bin/kafka-topics.sh  --zookeeper localhost:2181 --create --replication-factor 1 --partitions 1 --topic Accelerator
 *  bin/kafka-configs.sh --zookeeper localhost:2181 --entity-type topics --alter --entity-name Accelerator
 *                       --add-config cleanup.policy=compact,segment.ms=10000,min.cleanable.dirty.ratio=0.01
 *  </pre>
 *
 *  <p>Monitor updates via
 *  <pre>
 *  bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --property print.key=true --property key.separator=": " --topic Accelerator --from-beginning
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmConfigProducerDemo
{
    @Test
    public void demoAlarmConfig() throws Exception
    {
        final Instant start = Instant.now();

        // Create alarm tree model
        AlarmClientNode root = new AlarmClientNode(null, "Accelerator");

        AlarmClientNode area = new AlarmClientNode(root, "Vacuum");
        area.setDisplays(List.of(new TitleDetail("A", "one.opi"), new TitleDetail("B", "other.opi")));

        AlarmClientNode system = new AlarmClientNode(area, "Sector000001");
        system.setGuidance(List.of(new TitleDetail("Contacts", "Call Jane")));

        AlarmClientLeaf pv1 = new AlarmClientLeaf(system, "SomePVName");
        pv1.setDisplays(List.of(new TitleDetail("X", "Specific.opi")));

        AlarmClientLeaf pv2 = new AlarmClientLeaf(system, "SomeOtherPVName");
        pv2.setEnabled(false);

        // 1000 'sections' of 10 subsections of 10 PVs  -> 100k PVs
        for (int s=0; s<1000; ++s)
        {
            final AlarmClientNode sys = s == 1 ? system : new AlarmClientNode(area, String.format("Sector%06d", s));
            for (int sub=0; sub<10; ++sub)
            {
                final AlarmClientNode subsys = new AlarmClientNode(sys, String.format("Sub%02d", sub));
                for (int i=0; i<10; ++i)
                {
                    // Create 1000 PVs that change, rest static
                    final int n = s*100 + sub*10 + i;
                    final AlarmClientLeaf pv = (n>0  &&  n <= 1000)
                        ? new AlarmClientLeaf(subsys, String.format("sim://sine(-%d, %d, 1)", n, n))
                        : new AlarmClientLeaf(subsys, String.format("loc://PV%06d(0)", n));
                    pv.setLatching(false);
                }
            }
        }

        System.out.println(root.getPathName() + " = " + JsonModelWriter.toJsonString(root));
        System.out.println(area.getPathName() + " = " + JsonModelWriter.toJsonString(area));
        System.out.println(system.getPathName() + " = " + JsonModelWriter.toJsonString(system));
        System.out.println(pv1.getPathName() + " = " + JsonModelWriter.toJsonString(pv1));
        System.out.println(pv2.getPathName() + " = " + JsonModelWriter.toJsonString(pv2));


        // Connect to Kafka
        final Properties props = new Properties();
        props.put("bootstrap.servers", AlarmDemoSettings.SERVERS);

        // Required to write String key, value
        final Serializer<String> key_serializer = new StringSerializer();

        // Example for custom serializer
        final Serializer<AlarmTreeItem<?>> value_serializer = new Serializer<>()
        {
            @Override
            public void configure(Map<String, ?> configs, boolean isKey)
            {
                // NOP
            }

            @Override
            public byte[] serialize(String topic, AlarmTreeItem<?> item)
            {
                if (item == null)
                    return null;
                try
                {
                    return JsonModelWriter.toJsonBytes(item);
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

        final String topic = AlarmDemoSettings.ROOT;

        Producer<String, AlarmTreeItem<?>> producer = new KafkaProducer<>(props, key_serializer, value_serializer);

        sendConfigHierarchy(producer, topic, root);

        final Instant end = Instant.now();
        System.out.println("Time to upload configuration to Kafka: " + Duration.between(start, end).toMillis() + " ms");

        // Add a few changes to /Accelerator/Vacuum/Sector000001/SomePVName: Disable, remove, add back in, ..
        for (int i=0; i<10; ++i)
        {
            switch (i%4)
            {
            case 0:
                pv2.setEnabled(true);
                sendItemConfig(producer, topic, pv2);
                break;
            case 1:
                pv2.setEnabled(false);
                sendItemConfig(producer, topic, pv2);
                break;
            default:
                sendItemRemoval(producer, topic, pv2.getPathName());
                break;
            }
            Thread.sleep(2000);
        }

        producer.close();
    }


    private void sendConfigHierarchy(final Producer<String, AlarmTreeItem<?>> producer, final String topic, final AlarmTreeItem<?> item)
    {
        sendItemConfig(producer, topic, item);

        for (AlarmTreeItem<?> child : item.getChildren())
            sendConfigHierarchy(producer, topic, child);
    }

    // Place all keys (alarm tree paths) in the same partition
    private static final Integer partition = Integer.valueOf(0);

    private void sendItemConfig(final Producer<String, AlarmTreeItem<?>> producer,
            final String topic, final AlarmTreeItem<?> item)
    {
        final String key = AlarmSystem.CONFIG_PREFIX + item.getPathName();
        final ProducerRecord<String, AlarmTreeItem<?>> record = new ProducerRecord<>(topic, partition, key, item);
        producer.send(record);
    }

    private void sendItemRemoval(final Producer<String, AlarmTreeItem<?>> producer,
            final String topic, final String path)
    {
        final String key = AlarmSystem.CONFIG_PREFIX + path;
        final ProducerRecord<String, AlarmTreeItem<?>> record = new ProducerRecord<>(topic, partition, key, null);
        producer.send(record);
    }
}
