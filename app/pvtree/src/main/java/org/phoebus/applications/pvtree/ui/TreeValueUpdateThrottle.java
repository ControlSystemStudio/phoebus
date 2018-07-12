/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.phoebus.applications.pvtree.Settings;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;

/** Throttle for tree UI 'update' of a tree item value
 *
 *  <p>Structure (new links) need to be represented
 *  to allow reacting to updates of their values,
 *  but the value updates itself can be throttled.
 *
 *  @author Kay Kasemir
 */
public class TreeValueUpdateThrottle<T>
{
    private final Consumer<Collection<T>> updater;
    private final UpdateThrottle throttle;
    private final Set<T> updateable = new LinkedHashSet<>();

    /** @param updater Will be called with accumulated fields to update */
    public TreeValueUpdateThrottle(final Consumer<Collection<T>> updater)
    {
        this.updater = updater;
        final long update_period_ms = Math.round(Settings.max_update_period * 1000);
        throttle = new UpdateThrottle(update_period_ms, TimeUnit.MILLISECONDS, this::doRun);
    }

    /** Request a tree viewer 'update'
     *  @param item Item to update
     */
    public void scheduleUpdate(final T item)
    {
        synchronized (updateable)
        {
            updateable.add(item);
        }
        throttle.trigger();
    }

    private void doRun()
    {
        // Create thread-safe copy of accumulated items for `updater`
        final List<T> items = new ArrayList<>();
        synchronized (updateable)
        {
            items.addAll(updateable);
            updateable.clear();
        }
        // Perform actual updates on UI thread
        Platform.runLater(() -> updater.accept(items));
    }

    public void shutdown()
    {
        throttle.dispose();
    }
}
