/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.AlarmClientNode;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.ClientState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VType;

/** Alarm tree leaf
 *
 *  <p>Has PV,
 *  alarm state (which latches etc.)
 *  as well as the 'current' state of the PV.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmServerPV extends AlarmTreeItem<AlarmState> implements AlarmTreeLeaf, PVListener
{
    private final AtomicReference<PV> pv = new AtomicReference<>();
    private final ServerModel model;
    private volatile String description = "";
    private volatile AlarmState current;

    public AlarmServerPV(final ServerModel model, final AlarmClientNode parent, final String name)
    {
        super(parent, name, Collections.emptyList());
        this.model = model;
        description = name;
        state = new AlarmState(SeverityLevel.OK, "", "", Instant.now());
        current = state;
    }

    @Override
    public AlarmServerNode getParent()
    {
        return (AlarmServerNode) parent;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public synchronized boolean setDescription(final String description)
    {
        if (this.description.equals(description))
            return false;
        this.description = description;
        return true;
    }


    // TODO Use enabled setting of AlarmLogic
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    @Override
    public boolean isEnabled()
    {
        return enabled.get();
    }

    @Override
    public boolean setEnabled(boolean enable)
    {
        return enabled.compareAndSet(! enable, enable);
    }

    // TODO Get/set from AlarmLogic
    @Override
    public boolean isLatching()
    {
        return false;
    }

    @Override
    public boolean setLatching(boolean latch)
    {
        return false;
    }

    // TODO Get/set from AlarmLogic
    @Override
    public boolean isAnnunciating()
    {
        return false;
    }

    @Override
    public boolean setAnnunciating(boolean annunciate)
    {
        return false;
    }

    // TODO Get/set from AlarmLogic
    @Override
    public int getDelay()
    {
        return 0;
    }

    @Override
    public boolean setDelay(int seconds)
    {
        return false;
    }

    // TODO Get/set from AlarmLogic
    @Override
    public int getCount()
    {
        return 0;
    }

    @Override
    public boolean setCount(int times)
    {
        return false;
    }

    // TODO Get/set from AlarmLogic
    @Override
    public String getFilter()
    {
        return null;
    }

    @Override
    public boolean setFilter(String expression)
    {
        return false;
    }


    public void start()
    {
        if (! isEnabled())
            return;
        try
        {
            final PV new_pv = PVPool.getPV(getName());
            logger.log(Level.FINE, "Start " + new_pv.getName());
            final PV previous = pv.getAndSet(new_pv);
            if (previous != null)
                throw new IllegalStateException("Alarm tree leaf " + getPathName() + " already started for " + previous);
            new_pv.addListener(this);
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot create PV for " + getPathName(), ex);
            return;
        }
    }

    public void stop()
    {
        if (! isEnabled())
            return;
        try
        {
            final PV the_pv = pv.getAndSet(null);
            if (the_pv == null)
                throw new IllegalStateException("Alarm tree leaf " + getPathName() + " has no PV");
            the_pv.removeListener(this);
            PVPool.releasePV(the_pv);
            logger.log(Level.FINE, "Stop " + the_pv.getName());
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot create PV for " + getPathName(), ex);
            return;
        }
    }

    // PVListener
    @Override
    public void valueChanged(final PV pv, final VType value)
    {
        logger.log(Level.FINE, getPathName() + " = " + value);

        final SeverityLevel old_severity = getState().severity;

        // TODO Decouple handling of received value from PV thread
        // TODO Use actual alarm logic
        // TODO Send updates for state up to the alarm tree root
        final ClientState new_state;
        if (value == null)
            new_state = new ClientState(SeverityLevel.UNDEFINED, "disconnected", null, Instant.now(), SeverityLevel.UNDEFINED, "disconnected");
        else if (value instanceof Alarm)
        {
            final Alarm alarm = (Alarm) value;
            final SeverityLevel severity = alarm.getAlarmSeverity() == AlarmSeverity.NONE
                                         ? SeverityLevel.OK
                                         : SeverityLevel.values()[SeverityLevel.UNDEFINED_ACK.ordinal() + alarm.getAlarmSeverity().ordinal()];
            new_state = new ClientState(severity, alarm.getAlarmName(), value.toString(), Instant.now(), severity, alarm.getAlarmName());
        }
        else
            new_state = new ClientState(SeverityLevel.UNDEFINED, "undefined", null, Instant.now(), SeverityLevel.UNDEFINED, "undefined");
        setState(new_state);
        model.sentStateUpdate(getPathName(), new_state);

        // Whenever logic computes new state, maximize up parent tree
        if (new_state.severity != old_severity)
            getParent().maximizeSeverity();
    }

    // PVListener
    @Override
    public void disconnected(final PV pv)
    {
        logger.log(Level.FINE, getPathName() + " disconnected");
        valueChanged(pv, null);
    }
}
