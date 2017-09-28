/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.ui;

import static org.phoebus.applications.pvtree.PVTreeApplication.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.phoebus.applications.pvtree.Settings;

/** Throttle for tree UI 'update' of a tree item value
 *
 *  <p>Structure (new links) need to be represented
 *  to allow reacting to updates of their values,
 *  but the value updates itself can be throttled.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TreeValueUpdateThrottle<T>
{
    private final static long update_period_ms = Math.round(Settings.max_update_period * 1000);
    private final Consumer<Collection<T>> updater;
    private final Set<T> updateable = new LinkedHashSet<>();
    private final Thread throttle_thread;
    private volatile boolean run = true;

    /** @param updater Will be called with accumulated fields to update */
    public TreeValueUpdateThrottle(final Consumer<Collection<T>> updater)
    {
        this.updater = updater;
        throttle_thread = new Thread(this::doRun);
        throttle_thread.setName("PVTreeUpdates");
        throttle_thread.setDaemon(true);
        throttle_thread.start();
    }

    /** Request a tree viewer 'update'
     *  @param item Item to update
     */
    public void scheduleUpdate(final T item)
    {
        synchronized (updateable)
        {
            updateable.add(item);
            updateable.notifyAll();
        }
    }

    public void clearPendingUpdates()
    {
        synchronized (updateable)
        {
            updateable.clear();
        }
    }

    private void doRun()
    {
        try
        {
            while (run)
            {   // Create thread-safe copy of accumulated items for `updater`
                final List<T> items = new ArrayList<>();
                synchronized (updateable)
                {
                    while (run  &&  updateable.isEmpty())
                        updateable.wait();
                    items.addAll(updateable);
                    updateable.clear();
                }
                if (! run)
                    return;

                updater.accept(items);

                // Suppress updates
                Thread.sleep(update_period_ms);
            }
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "PVTree update error", ex);
        }
    }

    public void shutdown()
    {
        run = false;
        synchronized (updateable)
        {
            updateable.notifyAll();
        }
        try
        {
            throttle_thread.join(2000);
        }
        catch (final InterruptedException ex)
        {
            // Ignore, closing down anyway
        }
        if (throttle_thread.isAlive())
            logger.log(Level.WARNING, "PVTree update throttle fails to terminate within 2 seconds");
    }
}
