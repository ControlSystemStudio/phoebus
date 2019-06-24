/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.ResettableTimeout;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.client.KafkaHelper;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.json.JsonModelReader;

/** Fetch initial alarm state
 *
 *  <p>At runtime, the alarm server publishes state updates.
 *  At startup, it acts similar to a client by listening
 *  to past state updates to have a smooth transition,
 *  most important keeping previously acknowledged alarms ack'ed.
 *
 *  <p>Since the state information is a stream, we cannot know
 *  for sure that we received _all_ states,
 *  but we can wait for a break in the initial flurry of updates
 *  and then assume that we have a good snapshot when there are
 *  no more state updates for a while.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmStateInitializer
{
    private final ResettableTimeout timer = new ResettableTimeout(4);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Consumer<String, String> consumer;
    private final Thread thread;
    private final ConcurrentHashMap<String, ClientState> inititial_severity = new ConcurrentHashMap<>();

    /** @param server Kafka Server host:port
     *  @param config_name Name of alarm tree root
     */
    public AlarmStateInitializer(final String server, final String config_name)
    {
        consumer = KafkaHelper.connectConsumer(server, List.of(config_name), List.of(config_name));

        thread = new Thread(this::run, "AlarmStateInitializer");
        thread.setDaemon(true);
        thread.start();
    }

    /** Background thread loop that checks for alarm tree updates */
    private void run()
    {
        try
        {
            while (running.get())
                checkUpdates();
        }
        catch (final Throwable ex)
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
        final ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
        for (final ConsumerRecord<String, String> record : records)
        {
            if (record.key().length() < 2)
            {
                logger.log(Level.WARNING, "Invalid key, expecting type:path, got " + record.key());
                continue;
            }
            final String type = record.key().substring(0, 2);
            // Only handle state updates
            if (type.equals(AlarmSystem.STATE_PREFIX))
            {
                final String path = record.key().substring(3);
                final String node_config = record.value();
                try
                {
                    // System.out.printf("\n%s - %s:\n", path, node_config);
                    if (node_config == null)
                    {   // No config -> Delete node
                        inititial_severity.remove(path);
                        timer.reset();
                    }
                    else
                    {
                        // Get node_config as JSON map to check for "pv" key
                        final Object json = JsonModelReader.parseJsonText(node_config);
                        final ClientState state = JsonModelReader.parseClientState(json);
                        if (state != null)
                        {
                            // Delete when PV was OK, or track non-OK severity.
                            if (state.severity == SeverityLevel.OK)
                                inititial_severity.remove(path);
                            else
                                inititial_severity.put(path, state);
                            timer.reset();
                        }
                    }
                }
                catch (final Exception ex)
                {
                    logger.log(Level.WARNING,
                               "Alarm state check error for path " + path +
                               ", config " + node_config, ex);
                }
            }
        }
    }

    /** Wait for state updates to stop, assuming that we then have a complete snapshot
     *  @return <code>true</code> if there was a pause in state updates
     */
    public boolean awaitCompleteStates()
    {
        return timer.awaitTimeout(30);
    }

    /** Stop the state initializer
     *  @return Map of alarm paths and non-OK alarm state
     */
    public ConcurrentHashMap<String, ClientState> shutdown()
    {
        running.set(false);
        try
        {
            thread.join();
        }
        catch (InterruptedException ex)
        {
            // Ignore
        }
        return inititial_severity;
    }
}
