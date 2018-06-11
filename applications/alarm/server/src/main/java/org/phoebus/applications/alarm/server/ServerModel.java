/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreePath;
import org.phoebus.applications.alarm.model.BasicState;
import org.phoebus.applications.alarm.model.SeverityLevel;
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

    private final ConcurrentHashMap<String, ClientState> initial_states;

    private final String config_topic, command_topic, state_topic, talk_topic;
    private final ServerModelListener listener;
    private final AlarmServerNode root;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    private final Producer<String, String> producer;
    private final Thread thread;

    /** @param kafka_servers Servers
     *  @param config_name Name of alarm tree root
     * @param initial_states
     *  @throws Exception on error
     */
    public ServerModel(final String kafka_servers, final String config_name,
                       final ConcurrentHashMap<String, ClientState> initial_states,
                       final ServerModelListener listener)
    {
        this.initial_states = initial_states;
        // initial_states.entrySet().forEach(state ->
        //    System.out.println("Initial state for " + state.getKey() + " : " + state.getValue()));

        config_topic = Objects.requireNonNull(config_name);
        command_topic = config_name + AlarmSystem.COMMAND_TOPIC_SUFFIX;
        state_topic = config_name + AlarmSystem.STATE_TOPIC_SUFFIX;
        talk_topic = config_name + AlarmSystem.TALK_TOPIC_SUFFIX;
        this.listener = Objects.requireNonNull(listener);

        root = new AlarmServerNode(this, null, config_name);

        consumer = KafkaHelper.connectConsumer(Objects.requireNonNull(kafka_servers),
                                               List.of(config_topic, command_topic),
                                               List.of(config_topic));
        producer = KafkaHelper.connectProducer(kafka_servers);

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


    /** Find existing PV
     *
     *  @param name PV name
     *  @return Node, <code>null</code> if model does not contain the PV
     *  @throws Exception on error
     */
    public AlarmServerPV findPV(final String name) throws Exception
    {
        return findPV(name, root);
    }

    private AlarmServerPV findPV(final String name, final AlarmTreeItem<?> node)
    {
        if (node instanceof AlarmServerPV)
        {
            if (node.getName().equalsIgnoreCase(name))
                return (AlarmServerPV) node;
        }
        else
            for (AlarmTreeItem<?> child : node.getChildren())
            {
                final AlarmServerPV pv = findPV(name, child);
                if (pv != null)
                    return pv;
            }
        return null;
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
                // Use the known initial state, but only once (remove from map)
                if (last &&  is_leaf)
                    return new AlarmServerPV(this, parent, name, initial_states.remove(path));
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

    public void sentAnnunciatorMessage(final SeverityLevel level, final String description)
    {
        try
        {
            String severity = level.toString();
            final ProducerRecord<String, String> record = new ProducerRecord<>(talk_topic, severity, description);
            producer.send(record);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot send talk message for " + description, ex);
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
