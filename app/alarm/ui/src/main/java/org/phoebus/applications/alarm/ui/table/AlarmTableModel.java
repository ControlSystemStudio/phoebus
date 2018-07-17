/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.phoebus.applications.alarm.client.AlarmClientLeaf;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Alarm Table Model, i.e. PVs in alarm or acknowledged
 *  @author Kay Kasemir
 */
public class AlarmTableModel
{
    /** Active alarms and their last known state */
    private final ConcurrentHashMap<AlarmClientLeaf, ClientState> active = new ConcurrentHashMap<>();

    /** Ack'ed alarms and their last known state */
    private final ConcurrentHashMap<AlarmClientLeaf, ClientState> acknowledged = new ConcurrentHashMap<>();

    /** @param item Item for which update has been received
     *  @return <code>true</code> If this changed the alarm table
     */
    public boolean handleUpdate(final AlarmTreeItem<?> item)
    {
        if (! (item instanceof AlarmClientLeaf))
            return false;

        boolean changes = false;
        // Following depends on "|=" NOT being short-cutting,
        // i.e. skipping the right-side evaluation when changes is already true.
        // AlarmTableModelTest would catch problems.
        final AlarmClientLeaf leaf = (AlarmClientLeaf) item;
        final ClientState state = leaf.getState();
        final SeverityLevel severity = state.severity;
        if (!leaf.isEnabled()  ||  severity == SeverityLevel.OK)
        {   // Clear from table
            changes |= active.remove(leaf) != null;
            changes |= acknowledged.remove(leaf) != null;
        }
        else if (severity.isActive())
        {   // Active alarm
            changes |= !state.equals(active.put(leaf, state));
            changes |= acknowledged.remove(leaf) != null;
        }
        else
        {   // Acknowledged alarm
            changes |= active.remove(leaf) != null;
            changes |= !state.equals(acknowledged.put(leaf, state));
        }

        return changes;
    }

    /** @param item Item for which removal has been received
     *  @return <code>true</code> If this changed the alarm table
     */
    public boolean remove(final AlarmTreeItem<?> item)
    {
        if (! (item instanceof AlarmClientLeaf))
            return false;
        final AlarmClientLeaf leaf = (AlarmClientLeaf) item;

        boolean changes = false;
        // Following depends on "|=" NOT being short-cutting,
        // i.e. skipping the right-side evaluation when changes is already true.
        // Remove alarm from both active and ack'ed list
        changes |= active.remove(leaf) != null;
        changes |= acknowledged.remove(leaf) != null;

        return changes;
    }

    /** @return Active alarms */
    public Set<AlarmClientLeaf> getActiveAlarms()
    {
        return active.keySet();
    }

    /** @return Acknowledged alarms */
    public Set<AlarmClientLeaf> getAcknowledgedAlarms()
    {
        return acknowledged.keySet();
    }
}
