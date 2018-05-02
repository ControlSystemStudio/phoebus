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
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.phoebus.applications.alarm.model.AlarmClientLeaf;
import org.phoebus.applications.alarm.model.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.json.JsonModelReader;
import org.phoebus.applications.alarm.model.json.JsonModelWriter;

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
public class AlarmClient
{
    private final String config_topic, state_topic, command_topic;
    private final CopyOnWriteArrayList<AlarmClientListener> listeners = new CopyOnWriteArrayList<>();
    private final AlarmClientNode root;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    private final Producer<String, String> producer;
    private final Thread thread;

    /** @param kafka_servers Servers
     *  @param config_name Name of alarm tree root
     */
    public AlarmClient(final String kafka_servers, final String config_name)
    {
        Objects.requireNonNull(kafka_servers);
        Objects.requireNonNull(config_name);

        config_topic = config_name;
        state_topic = config_name + AlarmSystem.STATE_TOPIC_SUFFIX;
        command_topic = config_name + AlarmSystem.COMMAND_TOPIC_SUFFIX;

        root = new AlarmClientNode(null, config_name);
        consumer = connectConsumer(kafka_servers, config_name);
        producer = connectProducer(kafka_servers, config_name);

        thread = new Thread(this::run, "AlarmClientModel");
        thread.setDaemon(true);
    }

    /** @param listener Listener to add */
    public void addListener(final AlarmClientListener listener)
    {
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final AlarmClientListener listener)
    {
        if (! listeners.remove(listener))
            throw new IllegalStateException("Unknown listener");
    }

    /** Start client
     *  @see #shutdown()
     */
    public void start()
    {
        thread.start();
    }

    /** @return <code>true</code> if <code>start()</code> had been called */
    public boolean isRunning()
    {
        return thread.isAlive();
    }

    public AlarmClientNode getRoot()
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
        final String group_id = "AlarmClientModel-" + UUID.randomUUID();
        props.put("group.id", group_id);

        final List<String> topics = List.of(config_topic, state_topic);
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
                consumer.seekToBeginning(parts);
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

        // Write String key, value
        final Serializer<String> serializer = new StringSerializer();

        final Producer<String, String> producer = new KafkaProducer<>(props, serializer, serializer);

        return producer;
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
                logger.log(Level.SEVERE, "Alarm client model error", ex);
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
            final String path = record.key();
            final String node_config = record.value();
            try
            {
                // System.out.printf("\n%s - %s:\n", path, node_config);
                if (node_config == null)
                {   // No config -> Delete node
                    // Message may actually come from either config topic,
                    // where some client originally requested the removal,
                    // or the state topic, where running alarm server
                    // replaced the last state update with an empty one.
                    final AlarmTreeItem<?> node = deleteNode(path);
                    // If this was a known node, notify listeners
                    if (node != null)
                        for (AlarmClientListener listener : listeners)
                            listener.itemRemoved(node);
                }
                else
                {
                    // Get node_config as JSON map to check for "pv" key
                    final Object json = JsonModelReader.parseAlarmItemConfig(node_config);
                    AlarmTreeItem<?> node = findNode(path);
                    // Only update listeners if this is a new node or the config changed
                    if (node == null)
                        node = findOrCreateNode(path, JsonModelReader.isLeafConfigOrState(json));
                    boolean need_update = JsonModelReader.updateAlarmItemConfig(node, json)  ||
                                          JsonModelReader.updateAlarmState(node, json);
                    // If there were changes, notify listeners
                    if (need_update)
                        for (AlarmClientListener listener : listeners)
                            listener.itemUpdated(node);
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

    /** Find existing node
     *
     *  @param path Path to node
     *  @return Node, <code>null</code> if model does not contain the node
     *  @throws Exception on error
     */
    private AlarmTreeItem<?> findNode(final String path) throws Exception
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
        node.detachFromParent();
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
                {
                    node = new AlarmClientLeaf(parent, name);
                    for (AlarmClientListener listener : listeners)
                        listener.itemAdded(node);
                    return node;
                }
                else
                {
                    node = new AlarmClientNode(parent, name);
                    for (AlarmClientListener listener : listeners)
                        listener.itemAdded(node);
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

    /** Add a component to the alarm tree
     *  @param parent Root or parent component under which to add the component
     *  @param name Name of the new component
     */
    public void addComponent(final AlarmTreeItem<?> parent, final String new_name)
    {
        try
        {
            sendNewItemInfo(parent, new_name, new AlarmClientNode(null, new_name));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot add component " + new_name + " to " + parent.getPathName(), ex);
        }
    }

    /** Add a component to the alarm tree
     *  @param parent Root or parent component under which to add the component
     *  @param name Name of the new component
     */
    public void addPV(final AlarmTreeItem<?> parent, final String new_name)
    {
        try
        {
            sendNewItemInfo(parent, new_name, new AlarmClientLeaf(null, new_name));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot add pv " + new_name + " to " + parent.getPathName(), ex);
        }
    }

    private void sendNewItemInfo(final AlarmTreeItem<?> parent, final String new_name, final AlarmTreeItem<?> content) throws Exception
    {
        // Send message about new component.
        // All clients, including this one, will receive and then add the new component.
        final String new_path = AlarmTreePath.makePath(parent.getPathName(), new_name);
        sendItemConfigurationUpdate(new_path, content);
    }

    /** Send item configuration
     *
     *  <p>All clients, including this one, will update when they receive the message
     *
     *  @aram path Path to the item
     *  @param config A prototype item (path is ignored) that holds the new configuration
     *  @throws Exception on error
     */
    public void sendItemConfigurationUpdate(final String path, final AlarmTreeItem<?> config) throws Exception
    {
        final String json = new String(JsonModelWriter.toJsonBytes(config));
        final ProducerRecord<String, String> record = new ProducerRecord<>(config_topic, path, json);
        producer.send(record);
    }

    /** Remove a component (and sub-items) from alarm tree
     *  @param item Item to remove
     */
    public void removeComponent(final AlarmTreeItem<?> item)
    {
        // Send message about item to remove
        // All clients, including this one, will receive and then remove the item.
        try
        {
            // Remove from configuration
            final ProducerRecord<String, String> record = new ProducerRecord<>(config_topic, item.getPathName(), null);
            producer.send(record);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot remove component " + item, ex);
        }
    }

    /** @param item Item for which to acknowledge alarm
     *  @param acknowledge <code>true</code> to acknowledge, else un-acknowledge
     */
    public void acknowledge(final AlarmTreeItem<?> item, final boolean acknowledge)
    {
        try
        {
            final String cmd = acknowledge ? "acknowledge" : "unacknowledge";
            final ProducerRecord<String, String> record = new ProducerRecord<>(command_topic, cmd, item.getPathName());
            producer.send(record);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot acknowledge component " + item, ex);
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
            logger.log(Level.WARNING, "Alarm client thread doesn't shut down", ex);
        }
        logger.info(thread.getName() + " shut down");

    }
}
