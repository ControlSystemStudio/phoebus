/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import java.util.concurrent.atomic.AtomicInteger;

import org.phoebus.applications.alarm.ResettableTimeout;
import org.phoebus.applications.alarm.model.AlarmTreeItem;

/** Check for a pause in alarm client updates.
 *
 *  <p>When starting up, the alarm client reads all the past configuration
 *  updates.
 *  There is no perfect way to determine that we received "all" configuration
 *  information, because further updates could happen at any time,
 *  whenever a user decides to modify the alarm tree.
 *
 *  <p>The best approach is to check for a pause.
 *  Not receiving any alarm client updates for a while
 *  likely means that we have a stable configuration.
 *
 *  <p>This helper first waits for an initial config message,
 *  allowing for the connection to take some time.
 *  Based on past experience, we then receive a flurry of
 *  config messages.
 *  By then awaiting a pause in configuration updates,
 *  we assume that a complete configuration snapshot has been received.
 *
 *  @author Kay Kasemir
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmConfigMonitor
{
    private final AlarmClient client;
    private final ResettableTimeout timer;
    private final long idle_secs;
    private final AtomicInteger updates = new AtomicInteger();

    /** Listener to messages, resetting timer on config messages */
    private final AlarmClientListener config_listener = new AlarmClientListener()
    {
        @Override
        public void serverStateChanged(final boolean alive)
        {
            //NOP
        }

        @Override
        public void serverModeChanged(boolean maintenance_mode)
        {
            //NOP
        }

        @Override
        public void serverDisableNotifyChanged(boolean disable_notify)
        {
            //NOP
        }

        @Override
        public void itemAdded(final AlarmTreeItem<?> item)
        {
            // Reset the timer when receiving config update
            timer.reset(idle_secs);
            updates.incrementAndGet();
        }

        @Override
        public void itemRemoved(final AlarmTreeItem<?> item)
        {
            // Reset the timer when receiving config update
            timer.reset(idle_secs);
            updates.incrementAndGet();
        }

        @Override
        public void itemUpdated(final AlarmTreeItem<?> item)
        {
            //NOP
        }
    };

    /** @param initial_secs Seconds to wait for the initial config message (a 'connection' timeout)
     *  @param idle_secs Seconds after which we decide that there's a pause in configuration updates (assuming we received complete config snapshot)
     *  @param client AlarmClient to check for a pause in updates
     */
    public AlarmConfigMonitor(final long initial_secs, final long idle_secs, final AlarmClient client)
    {
        this.client = client;
        this.idle_secs = idle_secs;
        timer = new ResettableTimeout(initial_secs);
    }

    /** Wait for a pause in configuration updates
     *  @param timeout If there is no pause in configuration updates for this time, give up
     *  @throws Exception on timeout, i.e. could not detect a pause in updates
     */
    public void waitForPauseInUpdates(final long timeout) throws Exception
    {
        client.addListener(config_listener);
        if (! timer.awaitTimeout(timeout))
             throw new Exception(timeout + " seconds have passed, I give up waiting for updates to subside.");
        // Reset the counter to count any updates received after we decide to continue.
        updates.set(0);
    }

    /** @return Updates that were received after <code>waitForPauseInUpdates</code> */
    public int getCount()
    {
        return updates.get();
    }

    /** Call when no longer interested in checking updates */
    public void dispose()
    {
        client.removeListener(config_listener);
        timer.shutdown();
    }
}
