/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.epics.vtype.AlarmSeverity;

/** Model of the PV Tree
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TreeModel
{
    private final AtomicReference<TreeModelItem> root = new AtomicReference<>();

    private final List<TreeModelListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean latch_alarm = false;

    private AtomicInteger item_count = new AtomicInteger();

    /** Number of items in the tree to resolve, where fields are still to be fetched */
    private AtomicInteger links_to_resolve = new AtomicInteger();

    /** 'latched' = value updates should be ignored */
    private final AtomicBoolean latched = new AtomicBoolean();

    /** @param listener Listener to add */
    public void addListener(final TreeModelListener listener)
    {
        listeners.add(listener);
    }

    /** Set a new root PV
     *
     *  <p>Caller should then get the new root element,
     *  represent it, and start it.
     *
     *  @param pv_name
     *  @throws Exception
     *  @see {@link #getRoot()}
     *  @see TreeModelItem#start()
     */
    public void setRootPV(final String pv_name)
    {
        item_count.set(1);
        final TreeModelItem new_root = new TreeModelItem(this, null, "PV", pv_name);
        final TreeModelItem old = root.getAndSet(new_root);
        if (old != null)
            old.dispose();

        links_to_resolve.set(0);

        if (latched.getAndSet(false))
            for (TreeModelListener listener : listeners)
                listener.latchStateChanged(false);
    }

    /** @return Root element */
    public TreeModelItem getRoot()
    {
        return root.get();
    }

    /** @return Number of items in tree */
    public int getItemCount()
    {
        return item_count.get();
    }

    /** @param size Additional links that a PV tree item starts to resolve */
    void incrementLinks(final int size)
    {
        links_to_resolve.addAndGet(size);
    }

    /** PVItem resolved another link
     *
     *  <p>Triggers tree expansion when no links left to resolve
     */
    void decrementLinks()
    {
        final int left = links_to_resolve.decrementAndGet();
        if (left > 0)
            return;

        for (TreeModelListener listener : listeners)
            listener.allLinksResolved();
    }

    boolean isLatched()
    {
        return latched.get();
    }

    /** @return Does the model latch updates when the root enters alarm state? */
    public boolean isLatchingOnAlarm()
    {
        return latch_alarm;
    }

    /** @param latch Should the model latch updates when the root enters alarm state? */
    public void latchOnAlarm(final boolean latch)
    {
        latch_alarm = latch;
        if (latched.getAndSet(false))
        {
            unlatch(getRoot());
            for (TreeModelListener listener : listeners)
                listener.latchStateChanged(false);
        }
    }

    /** @param node Recursively trigger value updates from this node on */
    private void unlatch(final TreeModelItem node)
    {
        node.updateValue();
        for (TreeModelItem link : node.getLinks())
            unlatch(link);
    }

    /** @return All items that are currently in alarm */
    public List<TreeModelItem> getAlarmItems()
    {
        final List<TreeModelItem> alarms = new ArrayList<>();
        final TreeModelItem node = getRoot();
        if (node != null)
            getAlarmItems(alarms, node);
        return alarms;
    }

    private void getAlarmItems(final List<TreeModelItem> alarms, final TreeModelItem node)
    {
        final AlarmSeverity severity = node.getSeverity();
        if (severity != null   &&  severity != AlarmSeverity.NONE)
            alarms.add(node);
        for (TreeModelItem link : node.getLinks())
            getAlarmItems(alarms, link);
    }

    /** Locate _another_ tree PV
     *  @param existing Item that describes the PV to locate,
     *                  ignoring that item itself
     *  @return Other item for same PV in model, <code>null</code> if there's no other
     */
    protected TreeModelItem findDuplicate(final TreeModelItem existing)
    {
        return findDuplicate(root.get(), existing);
    }

    private TreeModelItem findDuplicate(final TreeModelItem item, final TreeModelItem existing)
    {
        if (item != existing  &&
            item.getPVName().equals(existing.getPVName()))
            return item;
        for (TreeModelItem link : item.getLinks())
        {
            final TreeModelItem found = findDuplicate(link, existing);
            if (found != null)
                return found;
        }
        return null;
    }

    void itemUpdated(final TreeModelItem item)
    {
        if (latch_alarm  &&  item == root.get())
        {
            final AlarmSeverity severity = item.getSeverity();
            // Is this an alarm?
            if (severity != null  &&  severity != AlarmSeverity.NONE)
                // Entering latched state?
                if (latched.getAndSet(true) == false)
                    for (TreeModelListener listener : listeners)
                        listener.latchStateChanged(true);
        }

        for (TreeModelListener listener : listeners)
            listener.itemChanged(item);
    }

    void itemLinkAdded(final TreeModelItem item, final TreeModelItem new_item)
    {
        item_count.incrementAndGet();
        for (TreeModelListener listener : listeners)
            listener.itemLinkAdded(item, new_item);
    }

    /** Must be called to release PVs */
    public void dispose()
    {
        final TreeModelItem old = root.getAndSet(null);
        if (old != null)
            old.dispose();
    }
}
