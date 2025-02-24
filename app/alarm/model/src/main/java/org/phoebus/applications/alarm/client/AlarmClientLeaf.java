/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.phoebus.applications.alarm.model.AlarmTreeItemWithState;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Alarm tree leaf
 *
 *  <p>No further child nodes, holds PV name and full {@link ClientState}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmClientLeaf extends AlarmTreeItemWithState<ClientState> implements AlarmTreeLeaf
{
    private volatile String description;

    private final AtomicBoolean latching = new AtomicBoolean(true);
    private final AtomicBoolean annunciating = new AtomicBoolean(true);

    private final AtomicInteger delay = new AtomicInteger(0);
    private final AtomicInteger count = new AtomicInteger(0);

    private volatile String filter = "";
    private volatile EnabledState enabled = new EnabledState(true);

    /** @param parent_path Path to parent
     *  @param name Name of this leaf
     */
    public AlarmClientLeaf(final String parent_path, final String name)
    {
        super(parent_path, name, Collections.emptyList());
        description = name;
        state = new ClientState(SeverityLevel.OK, "", "", Instant.now(), SeverityLevel.OK, "");
    }

    /** When requesting a configuration update,
     *  a detached copy is used to send the request.
     *  @return {@link AlarmClientLeaf} with same configuration, but no parent
     */
    public AlarmClientLeaf createDetachedCopy()
    {
        final AlarmClientLeaf pv = new AlarmClientLeaf(null, getName());
        pv.setDescription(getDescription());
        pv.setEnabled(getEnabled());
        pv.setLatching(isLatching());
        pv.setAnnunciating(isAnnunciating());
        pv.setDelay(getDelay());
        pv.setCount(getCount());
        pv.setFilter(getFilter());
        pv.setGuidance(new ArrayList<>(getGuidance()));
        pv.setDisplays(new ArrayList<>(getDisplays()));
        pv.setCommands(new ArrayList<>(getCommands()));
        pv.setActions(new ArrayList<>(getActions()));
        return pv;
    }

    /** @return Return description */
    @Override
    public String getDescription()
    {
        return description;
    }

    /** @param description Description
     *  @return <code>true</code> if this is a change
     */
    @Override
    public synchronized boolean setDescription(final String description)
    {
        if (this.description.equals(description))
            return false;
        this.description = description;
        return true;
    }

    /** @return <code>true</code> if alarms from PV are enabled */
    @Override
    public boolean isEnabled()
    {
        return enabled.enabled;
    }

    /** @param enable Enable the PV?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setEnabled(final boolean enable)
    {
        final EnabledState new_enabled_state = new EnabledState(enable);
        if (enabled.equals(new_enabled_state)) {
            return false;
        }
        enabled = new_enabled_state;
        return true;
    }

    /** @param enabled_state Enable the PV?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setEnabled(final EnabledState enabled_state)
    {
        if (enabled.equals(enabled_state)) {
            return false;
        }
        enabled = enabled_state;
        return true;
    }

    /** @param enabled_date (Re-)Enable the PV at some future date?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setEnabledDate(final LocalDateTime enabled_date)
    {
        final EnabledState new_enabled_state = new EnabledState(enabled_date);
        if ((enabled.equals(new_enabled_state)) || enabled_date.isBefore(LocalDateTime.now()) || enabled_date.isEqual(LocalDateTime.now())) {
            return false;
        }
        setEnabled(false);
        enabled = new_enabled_state;
        return true;
    }

    /** @return object representing enabled state */
    @Override
    public LocalDateTime getEnabledDate() {
        return enabled.enabled_date;
    }

    /** @return object representing enabled state */
    @Override
    public EnabledState getEnabled() {
        return enabled;
    }

    /** @return <code>true</code> if alarms from PV are latched */
    @Override
    public boolean isLatching()
    {
        return latching.get();
    }

    /** @param latch Latch alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setLatching(final boolean latch)
    {
        return latching.compareAndSet(! latch, latch);
    }

    /** @return <code>true</code> if alarms from PV are annunciated */
    @Override
    public boolean isAnnunciating()
    {
        return annunciating.get();
    }

    /** @param annunciate Annunciate alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setAnnunciating(final boolean annunciate)
    {
        return annunciating.compareAndSet(! annunciate, annunciate);
    }

    /** @return Alarm delay in seconds. */
    @Override
    public int getDelay()
    {
        return delay.get();
    }

    /** @param seconds Alarm delay
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setDelay(final int seconds)
    {
        return delay.getAndSet(seconds) != seconds;
    }

    /** @return Alarm count. Alarm needs to exceed this count within the delay */
    @Override
    public int getCount()
    {
        return count.get();
    }

    /** @param times Alarm when PV not OK more often than this count within delay
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setCount(final int times)
    {
        return count.getAndSet(times) != times;
    }

    /** @return Enabling filter expression. */
    @Override
    public String getFilter()
    {
        return filter;
    }

    /** @param expression Expression that enables the alarm
     *  @return <code>true</code> if this is a change
     */
    @Override
    public synchronized boolean setFilter(final String expression)
    {
        if (filter.equals(expression))
            return false;
        filter = expression;
        return true;
    }
}
