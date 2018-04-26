/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Alarm tree leaf
 *
 *  <p>No further child nodes, holds PV name and full {@link AlarmState}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeLeaf extends AlarmTreeItem<AlarmState>
{
    private volatile String description;

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean latching = new AtomicBoolean(true);
    private final AtomicBoolean annunciating = new AtomicBoolean(true);

    private final AtomicInteger delay = new AtomicInteger(0);
    private final AtomicInteger count = new AtomicInteger(0);

    private volatile String filter = "";

    public AlarmTreeLeaf(final AlarmTreeNode parent, final String name)
    {
        super(parent, name, Collections.emptyList());
        description = name;
        state = new AlarmState(SeverityLevel.OK, "", "", Instant.now(), SeverityLevel.OK, "");
    }

    /** @return Return description */
    public String getDescription()
    {
        return description;
    }

    /** @param description Description
     *  @return <code>true</code> if this is a change
     */
    public synchronized boolean setDescription(final String description)
    {
        if (this.description.equals(description))
            return false;
        this.description = description;
        return true;
    }

    /** @return <code>true</code> if alarms from PV are enabled */
    public boolean isEnabled()
    {
        return enabled.get();
    }

    /** @param enable Enable the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setEnabled(final boolean enable)
    {
        return enabled.compareAndSet(! enable, enable);
    }

    /** @return <code>true</code> if alarms from PV are latched */
    public boolean isLatching()
    {
        return latching.get();
    }

    /** @param latch Latch alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setLatching(final boolean latch)
    {
        return latching.compareAndSet(! latch, latch);
    }

    /** @return <code>true</code> if alarms from PV are annunciated */
    public boolean isAnnunciating()
    {
        return annunciating.get();
    }

    /** @param latch Annunciate alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setAnnunciating(final boolean annunciate)
    {
        return annunciating.compareAndSet(! annunciate, annunciate);
    }

    /** @return Alarm delay in seconds. */
    public int getDelay()
    {
        return delay.get();
    }

    /** @param seconds Alarm delay
     *  @return <code>true</code> if this is a change
     */
    public boolean setDelay(final int seconds)
    {
        return delay.getAndSet(seconds) != seconds;
    }

    /** @return Alarm count. Alarm needs to exceed this count within the delay */
    public int getCount()
    {
        return count.get();
    }

    /** @param times Alarm when PV not OK more often than this count within delay
     *  @return <code>true</code> if this is a change
     */
    public boolean setCount(final int times)
    {
        return count.getAndSet(times) != times;
    }

    /** @return Enabling filter expression. */
    public String getFilter()
    {
        return filter;
    }

    /** @param expression Expression that enables the alarm
     *  @return <code>true</code> if this is a change
     */
    public synchronized boolean setFilter(final String expression)
    {
        if (filter.equals(expression))
            return false;
        filter = expression;
        return true;
    }
}
