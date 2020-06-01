/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
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
 *  <p>This helper awaits such a pause in updates.
 *
 *  @author Kay Kasemir
 *  @author Evan Smith
 */
@SuppressWarnings("nls")
public class AlarmConfigMonitor
{
    private final AlarmClient client;
    private final ResettableTimeout timer;
    private final AtomicInteger updates = new AtomicInteger();

    private final AlarmClientListener updateListener = new AlarmClientListener()
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
            // Reset the timer when receiving update
            timer.reset();
            updates.incrementAndGet();
        }

        @Override
        public void itemRemoved(final AlarmTreeItem<?> item)
        {
            // Reset the timer when receiving update
            timer.reset();
            updates.incrementAndGet();
        }

        @Override
        public void itemUpdated(final AlarmTreeItem<?> item)
        {
            //NOP
        }
    };

    /** @param idle_secs Seconds after which we decide that there's a pause in configuration updates
     *  @param client AlarmClient to check for a pause in updates
     */
    public AlarmConfigMonitor(final long idle_secs, final AlarmClient client)
    {
        this.client = client;
        timer = new ResettableTimeout(idle_secs);
    }

    /** Wait for a pause in configuration updates
     *  @param timeout If there is no pause in configuration updates for this time, give up
     *  @throws Exception on timeout, i.e. could not detect a pause in updates
     */
    public void waitForPauseInUpdates(final long timeout) throws Exception
    {
        client.addListener(updateListener);
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
        client.removeListener(updateListener);
        timer.shutdown();
    }
}
