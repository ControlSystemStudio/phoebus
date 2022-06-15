/*******************************************************************************
 * Copyright (c) 2018-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
import org.phoebus.applications.alarm.model.AlarmState;
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

    private final String config_state_topic, command_topic, talk_topic;
    private final ServerModelListener listener;
    private final AlarmServerNode root;
    private volatile boolean running = true;
    private final Consumer<String, String> consumer;
    private final Producer<String, String> producer;
    private final Thread thread;
    private long last_state_update = 0;
    private long last_annunciation = 0;

    /** Time of last connectivity check */
    private long last_connection_check = System.currentTimeMillis();

    /** Did the last connectivity check fail? */
    private boolean connection_lost = false;


    /** @param kafka_servers Servers
     *  @param config_name Name of alarm tree root
     *  @param initial_states
     *  @param listener
     *  @param kafka_properties_file Additional properties to pass to the kafka client
     *  @throws Exception on error
     */
    public ServerModel(final String kafka_servers, final String config_name,
                       final ConcurrentHashMap<String, ClientState> initial_states,
                       final ServerModelListener listener,
                       final String kafka_properties_file) throws Exception
    {
        this.initial_states = initial_states;
        // initial_states.entrySet().forEach(state ->
        //    System.out.println("Initial state for " + state.getKey() + " : " + state.getValue()));

        config_state_topic = Objects.requireNonNull(config_name);
        command_topic  = config_name + AlarmSystem.COMMAND_TOPIC_SUFFIX;
        talk_topic     = config_name + AlarmSystem.TALK_TOPIC_SUFFIX;
        this.listener = Objects.requireNonNull(listener);

        root = new AlarmServerNode(this, null, config_name);

        consumer = KafkaHelper.connectConsumer(Objects.requireNonNull(kafka_servers),
                                               List.of(config_state_topic, command_topic),
                                               List.of(config_state_topic),
                                               kafka_properties_file);
        producer = KafkaHelper.connectProducer(kafka_servers, kafka_properties_file);

        thread = new Thread(this::run, "ServerModel");
        thread.setDaemon(true);
    }


    /** Start client
     *  @see #shutdown()
     */
    public void start()
    {
        thread.start();
        SeverityPVHandler.initialize();

        // Alarm server startup message
        sendAnnunciatorMessage(root.getPathName(), SeverityLevel.OK, "* Alarm server started. Everything is going to be all right.");
    }

    public AlarmServerNode getRoot()
    {
        return root;
    }

    /** Background thread
     *
     *  <p>Checks for alarm tree updates,
     *  emits idle or nag messages,
     *  validates connection
     */
    private void run()
    {
        try
        {
            while (running)
            {
                checkUpdates();
                final long now = System.currentTimeMillis();
                checkIdle(now);
                checkNag(now);
                checkConnectivity(now);
            }
        }
        catch (Throwable ex)
        {
            if (running)
                logger.log(Level.SEVERE, "Server model error", ex);
            // else: Intended shutdown
        }
        finally
        {
            consumer.close();
        }
    }

    /** Periodically check for Kafka connectivity
     *  @param now Current millisec
     */
    private void checkConnectivity(final long now)
    {
        if (AlarmSystem.connection_check_secs < 0  ||
            (now - last_connection_check)  <  AlarmSystem.connection_check_secs*1000)
            return;

        boolean connected = false;
        try
        {
            // There is no consumer.isConnected() type of API?
            // https://stackoverflow.com/questions/38103198/how-to-check-kafka-consumer-state
            // suggest calling listTopics with timeout
            logger.log(Level.FINE, "Testing Kafka connectitity");
            consumer.listTopics(Duration.ofSeconds(1));
            connected = true;
        }
        catch (Throwable ex)
        {
            logger.log(Level.FINE, "No Kafka connectitity", ex);
        }

        // While disconnected, the Kafka API still allows sending messages
        // but silently drops them, so clients will get out of sync,
        // and since Kafka is down, it won't track the most recent alarm state
        // for future clients...
        if (connected == false  &&  connection_lost == false)
            logger.log(Level.WARNING, "Lost Kafka connectitity");
        else if (connected &&  connection_lost)
        {
            logger.log(Level.WARNING, "Regained Kafka connectitity");
            // Update Kafka and thus clients with current state
            // as soon as connectivity is restored
            resend(getRoot());
            sendAnnunciatorMessage(root.getPathName(), SeverityLevel.OK, "* Alarm server re-connected");
        }

        connection_lost = ! connected;
        last_connection_check = now;
    }

    /** Perform one check for updates */
    private void checkUpdates()
    {
        final ConsumerRecords<String, String> records;
        try
        {
            records = consumer.poll(Duration.ofMillis(100));
        }
        catch (Throwable ex)
        {
            // This typically doesn't happen, 'poll' will simply not return any new
            // records while disconnected from Kafka...
            logger.log(Level.WARNING, "Error reading updates from Kafka", ex);
            return;
        }
        for (ConsumerRecord<String, String> record : records)
        {
            final int sep = record.key().indexOf(':');
            if (sep < 0)
            {
                logger.log(Level.WARNING, "Invalid key, expecting type:path, got " + record.key());
                continue;
            }

            final String type = record.key().substring(0, sep+1);
            final String path = record.key().substring(sep+1);
            if (type.equals(AlarmSystem.COMMAND_PREFIX)  ||  record.topic().equals(command_topic))
            {
                final String json = record.value();
                listener.handleCommand(path, json);
            }
            else if (type.equals(AlarmSystem.CONFIG_PREFIX))
            {
                final String node_config = record.value();
                try
                {
                    // System.out.printf("\n%s - %s:\n", path, node_config);
                    if (node_config == null)
                    {   // No config -> Delete node
                        final AlarmTreeItem<?> node = deleteNode(path);
                        if (node != null)
                            stopDeletedPVs(node);
                        // else: Deletion message for node we never created
                    }
                    else
                    {
                        // Get node_config as JSON map to check for "pv" key
                        final Object json = JsonModelReader.parseJsonText(node_config);

                        // Ignore 'delete' messages because they don't update the config
                        // and would result in superfluous PV stop() and re-start().
                        // The follow-up message with config == null will actually delete the AlarmServerPV
                        if (JsonModelReader.isConfigDeletion(json))
                            continue;

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

                            //check if using past disabled date
                            LocalDateTime enabled_date = pv.getEnabledDate();
                            if (enabled_date != null && enabled_date.isBefore(LocalDateTime.now())) {
                                pv.setEnabled(true);
                            }
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
            // else: Ignore state updates (which we sent ourselves)
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
                {
                    final AlarmServerPV pv = new AlarmServerPV(this, parent.getPathName(), name, initial_states.remove(path));
                    pv.addToParent(parent);
                    return pv;
                }
                else
                {
                    node = new AlarmServerNode(this, parent.getPathName(), name);
                    node.addToParent(parent);
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

        // Node is known.

        // Clear actions to cancel pending notifications
        node.setActions(Collections.emptyList());

        // Detach it
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
    private void stopDeletedPVs(final AlarmTreeItem<?> node)
    {
        if (node instanceof AlarmServerPV)
        {
            // Stop the PV, i.e. no longer react to value updates
            ((AlarmServerPV) node).stop();
            // Send a null "tombstone" status update
            sendStateUpdate(node.getPathName(), null);
        }
        else
            for (AlarmTreeItem<?> child : node.getChildren())
                stopDeletedPVs(child);
    }

    /** Send alarm update to 'state' topic
     *  @param path Path of item that has a new state
     *  @param new_state That new state
     */
    public void sendStateUpdate(final String path, final BasicState new_state)
    {
        try
        {
            final String json = new_state == null ? null : new String(JsonModelWriter.toJsonBytes(new_state, AlarmLogic.getMaintenanceMode(), AlarmLogic.getDisableNotify()));
            final ProducerRecord<String, String> record = new ProducerRecord<>(config_state_topic, AlarmSystem.STATE_PREFIX + path, json);
            producer.send(record);
            last_state_update = System.currentTimeMillis();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot send state update for " + path, ex);
        }
    }

    /** Send alarm update to 'config' topic
     *  @param path Path of item that has a new state
     *  @param new_state That new state
     */
    public void sendConfigUpdate(final String path, final AlarmTreeItem<AlarmState> config)
    {
        try
        {
            final String json = config == null ? null : new String(JsonModelWriter.toJsonBytes(config));
            final ProducerRecord<String, String> record = new ProducerRecord<>(config_state_topic, AlarmSystem.CONFIG_PREFIX + path, json);
            producer.send(record);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot send config update for " + path, ex);
        }
    }

    /** Send annunciation message to 'talk' topic
     *  @param path Path of item that has a new state
     *  @param severity Severity
     *  @param message Message
     */
    public void sendAnnunciatorMessage(final String path, final SeverityLevel severity, final String message)
    {
        try
        {
            last_annunciation = System.currentTimeMillis();

            final String json = JsonModelWriter.talkToString(severity, message);
            final ProducerRecord<String, String> record = new ProducerRecord<>(talk_topic, AlarmSystem.TALK_PREFIX + path, json);
            producer.send(record);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot send talk message for " + path, ex);
        }
    }

    /** Re-send current state (after e.g. network trouble)
     *  @param node Node from which on to send state update, recursively
     */
    public void resend(final AlarmTreeItem<?> node)
    {
        final BasicState state;
        if (node instanceof AlarmServerPV)
        {
            final AlarmServerPV pv = (AlarmServerPV) node;
            final AlarmState current = pv.getCurrentState();
            state = new ClientState(pv.getState(), current.getSeverity(), current.getMessage());
            sendStateUpdate(pv.getPathName(), state);
        }
        else
            state = node.getState();

        logger.log(Level.INFO, "Resend state:" + node.getPathName() + ": " + state);
        sendStateUpdate(node.getPathName(), state);

        for (AlarmTreeItem<?> child : node.getChildren())
            resend(child);
    }


    /** Check if 'idle' message should be sent since there were no state updates
     *  @param now Current millisec
     */
    private void checkIdle(final long now)
    {
        if (now - last_state_update  >  AlarmSystem.idle_timeout_ms)
            sendStateUpdate(root.getPathName(), root.getState());
    }

    /** Check if 'idle' message should be sent since there were no state updates
     *  @param now Current millisec
     */
    private void checkNag(final long now)
    {
        if (AlarmSystem.nag_period_ms > 0  &&
            now - last_annunciation  >  AlarmSystem.nag_period_ms)
        {
            final int active = countAlarmPVs(root);
            if (active == 1)
                sendAnnunciatorMessage(root.getPathName(), root.getState().severity, "* There is 1 active alarm");
            else if (active > 1)
                sendAnnunciatorMessage(root.getPathName(), root.getState().severity, "* There are " + active + " active alarms");
        }
    }

    /** There is at this time no caching of AlarmTreePVs,
     *  to recurse the alarm tree for PVs in active alarm
     *  @param item Item where to start recursion
     *  @return PVs found with active alarm
     */
    private int countAlarmPVs(final AlarmTreeItem<?> item)
    {
        // Only count enabled items
        if (item instanceof AlarmServerPV)
            return ((AlarmServerPV) item).isEnabled() &&  item.getState().severity.isActive() ? 1 : 0;
        int active = 0;
        for (AlarmTreeItem<?> child : item.getChildren())
            if (child.getState().severity.isActive())
                active += countAlarmPVs(child);
        return active;
    }

    private void clearActionsAndStopPVs(final AlarmTreeItem<?> node)
    {
        // Clear actions to cancel pending notifications
        node.setActions(Collections.emptyList());

        // Stop PV, or recurse to child nodes
        if (node instanceof AlarmServerPV)
            ((AlarmServerPV) node).stop();
        else
            for (AlarmTreeItem<?> child : node.getChildren())
                clearActionsAndStopPVs(child);
    }

    /** Stop client */
    public void shutdown()
    {
        SeverityPVHandler.stop();
        running = false;
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

        // Stop all the PVs
        clearActionsAndStopPVs(root);
        logger.info("Stopped all PVs");

        // Delete config
        root.getChildren().clear();
        root.maximizeSeverity();
        logger.info("Cleared configuration for " + root.getName());
    }
}
