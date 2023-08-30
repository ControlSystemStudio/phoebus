/*******************************************************************************
 * Copyright (c) 2018-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.epics.vtype.VType;
import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.Messages;
import org.phoebus.applications.alarm.client.ClientState;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.applications.alarm.model.AlarmTreeLeaf;
import org.phoebus.applications.alarm.model.EnabledState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.applications.alarm.model.TitleDetailDelay;
import org.phoebus.applications.alarm.server.actions.AutomatedActions;
import org.phoebus.applications.alarm.server.actions.AutomatedActionsHelper;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.disposables.Disposable;

/** Alarm tree leaf
 *
 *  <p>Has PV,
 *  alarm state (which latches etc.)
 *  as well as the 'current' state of the PV.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmServerPV extends AlarmTreeItem<AlarmState> implements AlarmTreeLeaf
{
    /** Timer used to check for the initial connection */
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setName("PVConnectionTimeout");
        thread.setDaemon(true);
        return thread;
    });

    private final ServerModel model;

    private volatile String description = "";

    private final AlarmLogic logic;

    private final AtomicReference<PV> pv = new AtomicReference<>();

    private volatile Disposable pv_flow;

    /** Track connection state */
    private volatile boolean is_connected = false;

    private volatile ScheduledFuture<?> connection_timeout_task = null;

    private final AtomicReference<AutomatedActions> automated_actions = new AtomicReference<>();

    /** Filter that might be used to compute 'enabled' state;
     *  can be <code>null</code>
     */
    private volatile Filter filter = null;
    private volatile EnabledState enabled = new EnabledState(true);
    private volatile EnabledDateTimeFilter enabled_datetime_filter = null;

    public AlarmServerPV(final ServerModel model, final String parent_path, final String name, final ClientState initial)
    {
        super(parent_path, name, Collections.emptyList());
        this.model = model;
        description = name;

        final AlarmState current_state;
        final AlarmState alarm_state;
        if (initial == null)
            current_state = alarm_state = new AlarmState(SeverityLevel.OK, "", "", Instant.now());
        else
        {
            current_state = new AlarmState(initial.current_severity, initial.current_message, "?", initial.time);
            alarm_state = new AlarmState(initial.severity, initial.message, initial.value, initial.time);
        }
        final AlarmLogicListener listener = new AlarmLogicListener()
        {
            @Override
            public void alarmStateChanged(final AlarmState current, final AlarmState alarm)
            {
                // Send alarm and current state to clients
                logger.log(Level.FINER, () -> getPathName() + " changes to " + current + ", " + alarm);
                final ClientState new_state = new ClientState(alarm,
                                                              current.severity,
                                                              current.message);
                model.sendStateUpdate(getPathName(), new_state);

                // Update automated actions
                AutomatedActionsHelper.update(automated_actions, alarm.severity);

                // Whenever logic computes new state, maximize up parent tree
                final AlarmServerNode parent = getParent();
                if (parent != null)
                    parent.maximizeSeverity();
                else
                    logger.log(Level.FINE, getPathName() + " ignores delayed change to " + current + ", " + alarm + " since no longer in alarm tree");
            }

            @Override
            public void annunciateAlarm(final SeverityLevel severity)
            {
                model.sendAnnunciatorMessage(getPathName(), severity, description);
            }
        };
        logic = new AlarmLogic(listener, true, true, 0, 0, current_state, alarm_state, 0);
    }

    @Override
    public AlarmState getState()
    {
        if (logic == null)
            throw new NullPointerException(getPathName() + " logic == null");
        return logic.getAlarmState();
    }

    public AlarmState getCurrentState()
    {
        return logic.getCurrentState();
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
        if (enabled_datetime_filter != null) {
            enabled_datetime_filter.cancel();
            enabled_datetime_filter = null;
        }
        logic.setEnabled(enable);
        return true;
    }

    /** @param enable Enable the PV?
     *  @return <code>true</code> if this is a change
     * Set as listener to enable
     */
    @Override
    public boolean setEnabled(final EnabledState enabled_state)
    {
        if (enabled.equals(enabled_state)) {
            return false;
        }

        enabled = enabled_state;
        logic.setEnabled(enabled.enabled);
        return true;
    }

    /** @param enable Enable the PV?
     *  @return <code>true</code> if this is a change
     */
    @Override
    public boolean setEnabledDate(final LocalDateTime enabled_date)
    {
        final EnabledState new_enabled_state = new EnabledState(enabled_date);
        if (enabled.equals(new_enabled_state)) {
            return false;
        }
        // if before current time, do not accept.
        if (enabled_date.isBefore(LocalDateTime.now())) {
            logger.log(Level.WARNING, "Enabled date is before current time.");
            return false;
        }

        enabled = new_enabled_state;
        logic.setEnabled(false);

        // cancel existing datetime filter and add new
        if (enabled_datetime_filter != null) {
            enabled_datetime_filter.cancel();
        }

        enabled_datetime_filter = new EnabledDateTimeFilter(enabled_date, this::enabledDateTimeFilterChanged);
        return true;
    }


    /** @return object representing enabled state */
    @Override
    public LocalDateTime getEnabledDate()
    {
        final LocalDateTime safe_copy = enabled.enabled_date;
        return safe_copy;
    }

    /** @return object representing enabled state */
    @Override
    public EnabledState getEnabled() {
        return enabled;
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


    @Override
    public boolean setActions(final List<TitleDetailDelay> actions)
    {
        if (super.setActions(actions))
        {
            AutomatedActionsHelper.configure(automated_actions, this,
                                             logic.getAlarmState().severity,
                                             isEnabled(), actions);
            return true;
        }
        return false;
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
            pv_flow = new_pv.onValueEvent(BackpressureStrategy.BUFFER)
                            .subscribe(this::handleValueUpdate);
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

    /** Listener to filter */
    private void enabledDateTimeFilterChanged(final boolean enabled)
    {
        setEnabled(enabled);

        // Alarm server should _listen_ to config changes, not send them, with one exception:
        // If a 'shelved' alarm gets re-enabled by the alarm server, it sends that out
        if (enabled)
            model.sendConfigUpdate(getPathName(), this);
    }

    public void stop()
    {
        try
        {
            // Cancel delayed
            logic.dispose();

            // Cancel ongoing actions, but don't set to null
            // because in case there's another start() it needs
            // to find & trigger the actions
            AutomatedActionsHelper.cancel(automated_actions);

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

            if (enabled_datetime_filter != null) {
                enabled_datetime_filter.cancel();
            }

            // Stop filter
            final Filter safe_copy = filter;
            if (safe_copy != null)
                safe_copy.stop();

            // Dispose PV
            pv_flow.dispose();
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

    /** @param value Value received from PV */
    private void handleValueUpdate(final VType value)
    {
        try
        {
            if (PV.isDisconnected(value))
            {
                disconnected();
                return;
            }
            // Inspect alarm state of received value
            is_connected = true;
            final SeverityLevel new_severity = SeverityLevelHelper.decodeSeverity(value);
            final String new_message = SeverityLevelHelper.getStatusMessage(value);
            final AlarmState received = new AlarmState(new_severity, new_message,
                                                       VTypeHelper.toString(value),
                                                       VTypeHelper.getTimestamp(value));
            // Update alarm logic
            logic.computeNewState(received);
            logger.log(Level.FINER, () -> getPathName() + " received " + value + " -> " + logic);
        }
        catch (Throwable ex)
        {
            throw new RuntimeException(getPathName() + " failed to handle update " + value, ex);
        }
    }

    /** Handle fact that PV disconnected */
    private void disconnected()
    {
        logger.log(Level.FINE, getPathName() + " disconnected");
        is_connected = false;
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
        {
            buf.append(" - ");
            if (getCount() > 0)
                buf.append(getCount()).append(" counts within ").append(getDelay()).append(" sec");
            else
                buf.append(getDelay()).append(" sec delay");
        }

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
