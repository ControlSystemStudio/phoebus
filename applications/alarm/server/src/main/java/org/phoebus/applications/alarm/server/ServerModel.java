/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.model.json.JsonModelWriter;

/** Server's model of the alarm configuration
 *
 *  <p>Given an alarm configuration name like "Accelerator",
 *  subscribes to the "Accelerator" topic for configuration updates,
 *  reading all configuration updates from a presumably "compacted" topic.
 *
 *  <p>Also subscribes to "AcceleratorCommand" topic,
 *  listening for new commands (acknowledge, ..).
 *
 *  <p>Publishes alarm state updates to the "AcceleratorState" topic.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ServerModel
{
    // Related to the AlarmClient code, but different:
    // Creates AlarmTreePV instead of the clients' AlarmTreeLeaf.
    // While AlarmClient has API to create/delete entries,
    // this one only reads the configuration, but sends state updates.
    // In the AlarmClient, deleting a node simply removes it from the tree,
    // disposing all sub sections.
    // The alarm server needs to handle the removal of each PV in the sub tree.
    private final String config_topic, command_topic, state_topic;
    private final ServerModelListener listener;
    private final AlarmServerNode root;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    private final Producer<String, String> producer;
    private final Thread thread;

    /** @param kafka_servers Servers
     *  @param config_name Name of alarm tree root
     *  @throws Exception on error
     */
    public ServerModel(final String kafka_servers, final String config_name,
                       final ServerModelListener listener)
    {
        config_topic = Objects.requireNonNull(config_name);
        command_topic = config_name + AlarmSystem.COMMAND_TOPIC_SUFFIX;
        state_topic = config_name + AlarmSystem.STATE_TOPIC_SUFFIX;
        this.listener = Objects.requireNonNull(listener);

        root = new AlarmServerNode(this, null, config_name);
        consumer = connectConsumer(Objects.requireNonNull(kafka_servers), config_name);
        producer = connectProducer(kafka_servers, config_name);

        thread = new Thread(this::run, "ServerModel");
        thread.setDaemon(true);
    }

    /** Start client
     *  @see #shutdown()
     */
    public void start()
    {
        thread.start();
    }

    public AlarmServerNode getRoot()
    {
        return root;
    }

    private Consumer<String, String> connectConsumer(final String kafka_servers, final String config_name)
    {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        // API requires for Consumer to be in a group.
        // Each alarm client must receive all updates,
        // cannot balance updates across a group
        // --> Use unique group for each client
        final String group_id = "ServerModel-" + UUID.randomUUID();
        props.put("group.id", group_id);

        final List<String> topics = List.of(config_topic, command_topic);
        logger.info(group_id + " subscribes to " + kafka_servers + " for " + topics);

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
                    if (part.topic().equals(config_name))
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

    private Producer<String, String> connectProducer(final String kafka_servers, final String config_name)
    {
        final Properties props = new Properties();
        props.put("bootstrap.servers", kafka_servers);
        // Collect messages for 20ms until sending them out as a batch
        props.put("linger.ms", 20);

        // Write String key, value
        final Serializer<String> serializer = new StringSerializer();
        return new KafkaProducer<>(props, serializer, serializer);
    }

    /** Background thread loop that checks for alarm tree updates */
    private void run()
    {
        try
        {
            while (running.get())
                checkUpdates();
        }
        catch (Throwable ex)
        {
            if (running.get())
                logger.log(Level.SEVERE, "Server model error", ex);
            // else: Intended shutdown
        }
        finally
        {
            consumer.close();
        }
    }

    /** Perform one check for updates */
    private void checkUpdates()
    {
        final ConsumerRecords<String, String> records = consumer.poll(100);
        for (ConsumerRecord<String, String> record : records)
        {
            if (record.topic().equals(command_topic))
            {
                final String command = record.key();
                final String detail = record.value();
                listener.handleCommand(command, detail);
            }
            else
            {
                final String path = record.key();
                final String node_config = record.value();
                try
                {
                    // System.out.printf("\n%s - %s:\n", path, node_config);
                    if (node_config == null)
                    {   // No config -> Delete node
                        final AlarmTreeItem<?> node = deleteNode(path);
                        if (node != null)
                            stopPVs(node);

                        // If this was the configuration message where client
                        // removed an item, add a null state update.
                        // Otherwise, if there ever was a state update,
                        // that last state update would add the item back into the client alarm tree
                        if (record.topic().equals(config_topic))
                            sentStateUpdate(path, null);
                    }
                    else
                    {
                        // Get node_config as JSON map to check for "pv" key
                        final Object json = JsonModelReader.parseAlarmItemConfig(node_config);
                        AlarmTreeItem<?> node = findNode(path);

                        // New node? Create it.
                        final boolean new_node = node == null;
                        if (new_node)
                            node = findOrCreateNode(path, JsonModelReader.isLeafConfigOrState(json));

                        // If an existing (i.e. started) PV is about to be updated, stop it.
                        if (node instanceof AlarmServerPV   &&  !new_node)
                            ((AlarmServerPV)node).stop();

                        // Return value of update..() tells us if it really changed.
                        // It might not have been necessary to stop the PV, but hard to tell in advance...
                        JsonModelReader.updateAlarmItemConfig(node, json);

                        // A new PV, or an existing one that was stopped: Start it
                        if (node instanceof AlarmServerPV)
                        {
                            final AlarmServerPV pv = (AlarmServerPV) node;
                            // Update parents in case node was disabled
                            // (i.e. 'start()' won't do anything),
                            // and to reflect last known state ASAP
                            // before the PV connects
                            pv.getParent().maximizeSeverity();
                            pv.start();
                        }
                    }
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING,
                               "Alarm config update error for path " + path +
                               ", config " + node_config, ex);
                }
            }
        }
    }

    /** Find existing node
     *
     *  @param path Path to node
     *  @return Node, <code>null</code> if model does not contain the node
     *  @throws Exception on error
     */
    public AlarmTreeItem<?> findNode(final String path) throws Exception
    {
        final String[] path_elements = AlarmTreePath.splitPath(path);

        // Start of path must match the alarm tree root
        if (path_elements.length < 1  ||
            !root.getName().equals(path_elements[0]))
            throw new Exception("Invalid path for alarm configuration " + root.getName() + ": " + path);

        // Walk down the path
        AlarmTreeItem<?> node = root;
        for (int i=1; i<path_elements.length; ++i)
        {
            final String name = path_elements[i];
            node = node.getChild(name);
            if (node == null)
                return null;
        }
        return node;
    }

    /** Find an existing alarm tree item or create a new one
     *
     *  <p>Informs listener about created nodes,
     *  if necessary one notification for each created node along the path.
     *
     *  @param path Alarm tree path
     *  @param is_leaf Is this the path to a leaf?
     *  @return {@link AlarmTreeItem}
     *  @throws Exception on error
     */
    private AlarmTreeItem<?> findOrCreateNode(final String path, final boolean is_leaf) throws Exception
    {
        final String[] path_elements = AlarmTreePath.splitPath(path);

        // Start of path must match the alarm tree root
        if (path_elements.length < 1  ||
            !root.getName().equals(path_elements[0]))
            throw new Exception("Invalid path for alarm configuration " + root.getName() + ": " + path);

        // Walk down the path
        AlarmClientNode parent = root;
        for (int i=1; i<path_elements.length; ++i)
        {
            final String name = path_elements[i];
            final boolean last = i == path_elements.length-1;
            AlarmTreeItem<?> node = parent.getChild(name);
            // Create missing nodes
            if (node == null)
            {   // Done when creating leaf
                if (last &&  is_leaf)
                    return new AlarmServerPV(this, parent, name);
                else
                {
                    node = new AlarmServerNode(this, parent, name);
                    // No listener interested in changes to node?
                    // TODO Check for action to update 'severity PV'
                }
            }
            // Reached desired node?
            if (last)
                return node;
            // Found or created intermediate node; continue walking down the path
            parent = (AlarmClientNode) node;
        }

        // If path_elements.length == 1, loop never ran. Return root == parent
        return parent;
    }

    /** Delete node
     *
     *  <p>It's OK to try delete an unknown node:
     *  The node might have once existed, but was then deleted.
     *  The last entry in the configuration database is then the deletion hint.
     *  A new model that reads this node-to-delete information
     *  thus never knew the node.
     *
     *  @param path Path to node to delete
     *  @return Node that was removed, or <code>null</code> if model never knew that node
     *  @throws Exception on error
     */
    private AlarmTreeItem<?> deleteNode(final String path) throws Exception
    {
        final AlarmTreeItem<?> node = findNode(path);
        if (node == null)
            return null;

        // Node is known: Detach it
        final AlarmTreeItem<BasicState> parent = node.getParent();
        node.detachFromParent();

        // Removing a node that was in alarm can update the severity of the parent
        if (parent instanceof AlarmServerNode)
            ((AlarmServerNode)parent).maximizeSeverity();
        return node;
    }

    /** Stop PVs in a subtree of the alarm hierarchy
     *  @param node Node where to start
     */
    private void stopPVs(final AlarmTreeItem<?> node)
    {
        // If this was a known PV, notify listener
        if (node instanceof AlarmServerPV)
            ((AlarmServerPV) node).stop();
        else
            for (AlarmTreeItem<?> child : node.getChildren())
                stopPVs(child);
    }

    /** Send alarm state update
     *
     *  @param path Path of item that has a new state
     *  @param new_state That new state
     */
    public void sentStateUpdate(final String path, final BasicState new_state)
    {
        try
        {
            final String json = new_state == null ? null : new String(JsonModelWriter.toJsonBytes(new_state));
            final ProducerRecord<String, String> record = new ProducerRecord<>(state_topic, path, json);
            producer.send(record);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot send state update for " + path, ex);
        }
    }

    /** Stop client */
    public void shutdown()
    {
        running.set(false);
        consumer.wakeup();
        try
        {
            thread.join(2000);
        }
        catch (InterruptedException ex)
        {
            logger.log(Level.WARNING, "Server model thread doesn't shut down", ex);
        }
        logger.info(thread.getName() + " shut down");
    }
}
