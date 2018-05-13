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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.Messages;
import org.phoebus.applications.alarm.client.AlarmClientNode;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
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
    /** Timer used to check for the initial connection */
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setName("PVConnectionTimeout");
        thread.setDaemon(true);
        return thread;
    });

    private volatile String description = "";

    private final AtomicBoolean enabled = new AtomicBoolean(true);

    private final AlarmLogic logic;

    private final AtomicReference<PV> pv = new AtomicReference<>();

    /** Track connection state */
    private volatile boolean is_connected = false;

    private volatile ScheduledFuture<?> connection_timeout_task = null;

    /** Filter that might be used to compute 'enabled' state;
     *  can be <code>null</code>
     */
    private volatile Filter filter = null;

    public AlarmServerPV(final ServerModel model, final AlarmClientNode parent, final String name)
    {
        super(parent, name, Collections.emptyList());
        description = name;

        final AlarmState initial = new AlarmState(SeverityLevel.OK, "", "", Instant.now());
        final AlarmLogicListener listener = new AlarmLogicListener()
        {
            @Override
            public void alarmStateChanged(AlarmState current, AlarmState alarm)
            {
                // Send alarm and current state to clients
                logger.log(Level.FINER, () -> getPathName() + " changes to " + current + ", " + alarm);
                final ClientState new_state = new ClientState(alarm,
                                                              current.severity,
                                                              current.message);
                model.sentStateUpdate(getPathName(), new_state);

                // Whenever logic computes new state, maximize up parent tree
                getParent().maximizeSeverity();
            }

            @Override
            public void annunciateAlarm(SeverityLevel level)
            {
                // TODO Send text to Kafka, so that annunciators can, well, annunciate
                // model.sentAnnunciationMessage(...)
            }
        };
        logic = new AlarmLogic(listener, true, true, 0, 0, initial, initial, 0);
    }

    @Override
    public AlarmState getState()
    {
        return logic.getAlarmState();
    }


    /** Acknowledge current alarm severity
     *  @param acknowledge Acknowledge or un-acknowledge?
     */
    public void acknowledge(final boolean acknowledge)
    {
        logic.acknowledge(acknowledge);
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

    @Override
    public boolean isEnabled()
    {
        return enabled.get();
    }

    @Override
    public boolean setEnabled(final boolean enable)
    {
        return enabled.compareAndSet(! enable, enable);
    }

    @Override
    public boolean isLatching()
    {
        return logic.isLatching();
    }

    @Override
    public boolean setLatching(final boolean latch)
    {
        return logic.setLatching(latch);
    }

    @Override
    public boolean isAnnunciating()
    {
        return logic.isAnnunciating();
    }

    @Override
    public boolean setAnnunciating(final boolean annunciate)
    {
        return logic.setAnnunciating(annunciate);
    }

    @Override
    public int getDelay()
    {
        return logic.getDelay();
    }

    @Override
    public boolean setDelay(final int seconds)
    {
        return logic.setDelay(seconds);
    }

    @Override
    public int getCount()
    {
        return logic.getCount();
    }

    @Override
    public boolean setCount(final int times)
    {
        return logic.setCount(times);
    }

    @Override
    public String getFilter()
    {
        final Filter safe_copy = filter;
        return safe_copy == null ? "" : safe_copy.getExpression();
    }

    @Override
    public boolean setFilter(final String expression)
    {
        if (pv.get() != null)
            throw new IllegalStateException("Cannot change filter while running for " + getPathName());
        try
        {
            if (expression == null  ||  expression.isEmpty())
            {
                if (filter == null)
                    return false;
                filter = null;
            }
            else
            {
                if (filter != null  &&  filter.getExpression().equals(expression))
                    return false;
                filter = new Filter(expression, this::filterChanged);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot set filter for " + getPathName() + " to " + expression, ex);
            return false;
        }
        return true;
    }

    public void start()
    {
        if (! isEnabled())
            return;
        try
        {
            connection_timeout_task = timer.schedule(this::checkConnection, AlarmSystem.connection_timeout, TimeUnit.SECONDS);

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

            final AlarmState received = new AlarmState(SeverityLevel.UNDEFINED, Messages.NoPV, "", Instant.now());
            logic.computeNewState(received);
        }

        try
        {
            final Filter safe_copy = filter;
            if (safe_copy != null)
                safe_copy.start();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot start filter for " + getPathName(), ex);
        }
    }

    private void checkConnection()
    {
        if (! isConnected())
        {
            logger.log(Level.WARNING, () -> getPathName() + " connection timed out");
            disconnected();
        }
    }

    /** Listener to filter */
    private void filterChanged(final double value)
    {
        final boolean new_enable_state = value > 0.0;
        logger.log(Level.FINE, () -> getPathName() + " " + filter + " value " + value);

        logic.setEnabled(new_enable_state);
    }

    public void stop()
    {
        try
        {
            final PV the_pv = pv.getAndSet(null);
            // Be lenient if already stopped,
            // or never started because ! isEnabled()
            if (the_pv == null)
                return;

            // Stop checking for initial connection
            final ScheduledFuture<?> conn_to = connection_timeout_task;
            if (conn_to != null)
            {
                conn_to.cancel(false);
                connection_timeout_task = null;
            }

            // Stop filter
            final Filter safe_copy = filter;
            if (safe_copy != null)
                safe_copy.stop();

            // Dispose PV
            the_pv.removeListener(this);
            PVPool.releasePV(the_pv);
            logger.log(Level.FINE, "Stop " + the_pv.getName());
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Cannot stop " + getPathName(), ex);
        }
        is_connected = false;
    }

    /** @return <code>true</code> if PV is connected */
    public boolean isConnected()
    {
        return is_connected;
    }

    // PVListener
    @Override
    public void valueChanged(final VType value)
    {
        // Inspect alarm state of received value
        is_connected = true;
        final SeverityLevel new_severity = VTypeHelper.decodeSeverity(value);
        final String new_message = VTypeHelper.getStatusMessage(value);
        final AlarmState received = new AlarmState(new_severity, new_message,
                                                   VTypeHelper.toString(value),
                                                   VTypeHelper.getTimestamp(value));
        // Update alarm logic
        logic.computeNewState(received);
        logger.log(Level.FINER, () -> getPathName() + " received " + value + " -> " + logic);
    }

    // PVListener
    @Override
    public void disconnected()
    {
        logger.log(Level.FINE, getPathName() + " disconnected");
        final AlarmState received = new AlarmState(SeverityLevel.UNDEFINED, Messages.Disconnected, "", Instant.now());
        logic.computeNewState(received);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(getPathName())
           .append(" [").append(getDescription()).append("]");

        final PV safe_pv = pv.get();
        if (safe_pv != null)
        {
            if (is_connected)
                buf.append(" - connected, ");
            else
                buf.append(" - disconnected, ");
            buf.append(safe_pv.read());
        }
        if (! isEnabled())
            buf.append(" - disabled");
        if (isAnnunciating())
            buf.append(" - annunciating");
        if  (isLatching())
            buf.append(" - latching");
        if (getDelay() > 0)
            buf.append(" - ").append(getDelay()).append(" sec delay");

        buf.append(" - ").append(logic.toString());

        final Filter safe_copy = filter;
        if (safe_copy != null)
        {
            if (logic.isEnabled())
               buf.append(" - dynamically enabled via ");
            else
                buf.append(" - dynamically disabled via ");
            buf.append(safe_copy);
        }

        return buf.toString();
    }
}
