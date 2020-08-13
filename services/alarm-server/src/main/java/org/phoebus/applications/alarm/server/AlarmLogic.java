/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.phoebus.applications.alarm.Messages;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Alarm handling logic.
 *  <p>
 *  Maintains the alarm state, which consists of:
 *  <ul>
 *  <li>Severity and message of the PV as far as the alarm system is
 *      concerned. This might be the latched, highest un-acknowledged
 *      severity/message. It might also be an acknowledged severity level.
 *  <li>"Current" severity of the PV. This severity may be lower than
 *      the latched alarm system severity.
 *  <li>Timestamp and Value that caused the Severity and message
 *  </ul>
 *  <p>
 *  Logic can 'latch' to the highest un-acknowledged alarm state
 *  and trigger annunciation when new alarm becomes active.
 *  <p>
 *  Abstract base of AlarmPV to allow tests independent from actual
 *  control system connection.
 *
 *  @see AlarmPV
 *  @see AlarmLogicUnitTest
 *  @author Kay Kasemir
 */
public class AlarmLogic // implements GlobalAlarmListener
{
    /** @see #getMaintenanceMode() */
    private static volatile boolean maintenance_mode = false;

    /** @see #getDisableNotify() */
    private static volatile boolean disable_notify = false;

    /** Listener to notify on alarm state changes */
    final private AlarmLogicListener listener;

    /** Is logic enabled, or only following the 'current' PV state
     *  without actually alarming?
     */
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    /** 'Current' state that was received while disabled.
     *  Is cached in case we get re-enabled, whereupon it is used
     */
    private volatile AlarmState disabled_state = null;

    /** Latch the highest received alarm severity/status?
     *  When <code>false</code>, the latched alarm state actually
     *  follows the current value of the PV without requiring
     *  acknowledgment.
     */
    private final AtomicBoolean latching = new AtomicBoolean(true);

    /** Annunciate alarms? */
    private final AtomicBoolean annunciating = new AtomicBoolean(true);

    /** Does this alarm have priority in maintenance mode, i.e.
     *  INVALID should still be annunciated in maintenance mode,
     *  and the annunciator will not suppress it within a flurry of
     *  alarms that are usually throttled/summarized
     */
    private volatile boolean has_priority = false;

    /** Require minimum time [seconds] in alarm before indicating alarm */
    private final AtomicInteger delay = new AtomicInteger(0);

    /** Pending delayed alarm state update or <code>null</code> */
    final private DelayedAlarmUpdate delayed_check = new DelayedAlarmUpdate(this::delayedStateUpdate);

    /** Alarm when PV != OK more often than this count within delay */
    private final AtomicReference<AlarmStateHistory> alarm_history = new AtomicReference<>();

    /** Current state of the control system channel */
    private volatile AlarmState current_state;

    /** Alarm logic state, with might be latched or delayed from current_state */
    private volatile AlarmState alarm_state;

    /** Delay [seconds] after which a 'global' alarm state update is sent */
    final private int global_delay;

    /** Pending global alarm state update or <code>null</code> */
//    final private GlobalAlarmUpdate global_check = new GlobalAlarmUpdate(this);

    /** Is there an active 'global' alarm, i.e. a global alarm notification
     *  has been sent?
     */
    private boolean in_global_alarm = false;

    /** Initialize
     *  @param listener {@link AlarmLogicListener}
     *  @param latching Latch the highest received alarm severity?
     *  @param annunciating Annunciate alarms?
     *  @param delay Minimum time in alarm before indicating alarm [seconds]
     *  @param count Alarm when PV != OK more often than this count within delay
     *  @param current_state Current alarm state of PV
     *  @param alarm_state Alarm logic state
     *  @param global_delay Delay [seconds] after which a 'global' notification is sent. 0 to disable
     */
    public AlarmLogic(final AlarmLogicListener listener,
            final boolean latching, final boolean annunciating,
            final int delay,
            final int count,
            final AlarmState current_state,
            final AlarmState alarm_state,
            final int global_delay)
    {
        this.listener = listener;
        this.latching.set(latching);
        this.annunciating.set(annunciating);
        this.delay.set(delay);
        if (count > 0)
            alarm_history.set(new AlarmStateHistory(count));
        this.current_state = current_state;
        this.alarm_state = alarm_state;
        this.global_delay = global_delay;
    }

    /** Set maintenance mode.
     *  @param maintenance_mode
     *  @see #getMaintenanceMode()
     */
    @SuppressWarnings("nls")
    public static void setMaintenanceMode(final boolean maintenance_mode)
    {
        AlarmLogic.maintenance_mode = maintenance_mode;
        logger.config("Maintenance Mode: " + maintenance_mode);
    }

    /** In maintenance mode, 'INVALID' alarms are suppressed by
     *  _not_ annunciating them, automatically acknowledging them
     *  and otherwise treating them like 'OK':
     *  When INVALID_ACK is followed by 'MAJOR', that is considered
     *  a new alarm since the INVALID didn't really count.
     *  @return <code>true</code> in maintenance mode
     */
    public static boolean getMaintenanceMode()
    {
        return maintenance_mode;
    }

    /** Set disable notify.
     *  @param disable_notify
     *  @see #getDisableNotify()
     */
    @SuppressWarnings("nls")
    public static void setDisableNotify(final boolean disable_notify)
    {
        AlarmLogic.disable_notify = disable_notify;
        logger.info("Disable Notify: " + disable_notify);
    }

    /** If disable_notify is true, email notifications are disabled
     *  @return <code>true</code> disable notify
     */
    public static boolean getDisableNotify()
    {
        return disable_notify;
    }

    /** @param enable Enable or disable the logic?
     *  @return <code>true</code> if this is a change
     */
    public boolean setEnabled(final boolean enable)
    {
        // Ignore if there's no change
        if (enabled.getAndSet(enable) == enable)
            return false;
        listener.alarmEnablementChanged(enable);
        if (!enable)
        {   // Disabled
            // Remember current PV state in case we're re-enabled
            disabled_state = current_state;
            // Otherwise pretend all is OK, using special message

            final AlarmState current = AlarmState.createClearState(current_state.value);
            final AlarmState alarm = new AlarmState(SeverityLevel.OK,
                                                    Messages.Disabled, "", Instant.now());
            current_state = current;
            alarm_state = alarm;
            listener.alarmStateChanged(current, alarm);
        }
        else
        {   // (Re-)enabled
            if (disabled_state != null)
            {
                computeNewState(disabled_state);
                disabled_state = null;
            }
        }
        return true;
    }

    /** @return Are alarms enabled */
    public boolean isEnabled()
    {
        return enabled.get();
    }

    /** @param latch Annunciate alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setAnnunciating(final boolean annunciate)
    {
        return annunciating.compareAndSet(! annunciate, annunciate);
    }

    /** @return <code>true</code> if configured to annunciate */
    public boolean isAnnunciating()
    {
        return annunciating.get();
    }

    /** @param has_priority Does this alarm have priority in maintenance mode? */
    public void setPriority(final boolean has_priority)
    {
        this.has_priority = has_priority;
    }

    /** @param latch Latch alarms from the PV?
     *  @return <code>true</code> if this is a change
     */
    public boolean setLatching(boolean latch)
    {
        return latching.compareAndSet(! latch, latch);
    }

    /** @return <code>true</code> for latching behavior */
    public boolean isLatching()
    {
        return latching.get();
    }

    /** @param seconds Alarm delay in seconds
     *  @return <code>true</code> if this is a change
     */
    public boolean setDelay(final int seconds)
    {
        return delay.getAndSet(seconds) != seconds;
    }

    /** @return Alarm delay in seconds */
    public int getDelay()
    {
        return delay.get();
    }

    /** @return count Alarm when PV != OK more often than this count within delay */
    public int getCount()
    {
        final AlarmStateHistory hist = alarm_history.get();
        if (hist == null)
            return 0;
        return hist.getCount();
    }

    /** Alarm when PV != OK more often than this count within delay
     *  @param count New count
     *  @return <code>true</code> if this is a change
     */
    public boolean setCount(final int count)
    {
        // Not 100% atomic.
        // Concurrent callers could set the same count,
        // and this would install the same history twice,
        // needlessly creating and then disposing one of them.
        // --> Fine.
        if (getCount() == count)
            return false;
        final AlarmStateHistory prev = count > 0
                ? alarm_history.getAndSet(new AlarmStateHistory(count))
                : alarm_history.getAndSet(null);
        if (prev != null)
            prev.dispose();
        return true;
    }

    /** @return Current state of PV */
    public AlarmState getCurrentState()
    {
        return current_state;
    }

    /** @return Alarm system state */
    public AlarmState getAlarmState()
    {
        return alarm_state;
    }

    /** Compute the new alarm state based on new data from control system.
     *  @param received_state Severity/Status received from control system
     */
    public void computeNewState(final AlarmState received_state)
    {
        final boolean return_to_ok;
        synchronized (this)
        {
            // When disabled, ignore...
            if (!enabled.get())
            {
                disabled_state = received_state;
                return;
            }
            // Remember what used to be the 'current' severity
            final SeverityLevel previous_severity = current_state.severity;
            final String previous_message = current_state.message;
            // Update current severity.
            current_state = received_state;
            // If there's no change to the current severity and message, we're done.
            if (received_state.severity == previous_severity  &&
                received_state.message.equals(previous_message))
                return;

            // Does this 'clear' an acknowledged severity? -> OK
            final boolean alarm_cleared =
                   (current_state.severity == SeverityLevel.OK  &&
                    !alarm_state.severity.isActive());
            // Are we in maintenance mode, and this is a new
            // alarm after an INVALID one that was suppressed?
            // -> At least briefly return to OK so that 'new' alarm
            //    will take effect
            final boolean maint_leaving_invalid =
             maintenance_mode &&
             // Current severity is better than INVALID & UNDEFINED
             current_state.severity.ordinal() < SeverityLevel.INVALID.ordinal()  &&
             // Alarm state was INVALID, INVALID_ACK, UNDEFINED, UNDEFINED_ACK
             alarm_state.severity.getAlarmUpdatePriority()
                          >= SeverityLevel.INVALID.getAlarmUpdatePriority();

            return_to_ok = alarm_cleared  ||  maint_leaving_invalid;
            if (return_to_ok)
                alarm_state = AlarmState.createClearState(received_state.value, received_state.time);
        }
        // Out of sync'ed section
        if (return_to_ok)
        {
            // If a delayed alarm timer was started, cancel it
            delayed_check.cancel();
            // Also cancel 'global' timers
//            global_check.cancel();
//            clearGlobalAlarm();
        }
        updateState(received_state, getDelay() > 0);
    }

    /** Compute the new alarm state based on current state and new data from
     *  control system.
     *
     *  @param received_state Severity/Status received from control system,
     *                                         or
     *                        the delayed new state from DelayedAlarmCheck
     *  @param with_delay Use delay when raising the alarm severity?
     */
    private void updateState(final AlarmState received_state,
                             final boolean with_delay)
    {
        SeverityLevel raised_level = null;
        final AlarmState current, alarm;
        synchronized (this)
        {
            // Update alarm state. If there is already an update pending,
            // update that delayed state check.
            AlarmState state_to_update = delayed_check.getState();
            if (state_to_update == null)
                state_to_update = alarm_state;
            final AlarmState new_state = latchAlarmState(state_to_update, received_state);
            // Computed a new alarm state? Else: Only current_severity update
            if (new_state != null)
            {
                // Delay if requested and this is indeed triggered by alarm, not OK
                if (with_delay && received_state.severity != SeverityLevel.OK)
                {   // Start or update delayed alarm check
                    delayed_check.schedule_update(new_state, delay.get());
                    // Somewhat in parallel, check for alarm counts
                    if (checkCount(received_state))
                    {   // Exceeded alarm count threshold.
                        // Reset delayed alarms
                        delayed_check.cancel();
                        // Annunciate if going to higher alarm severity
                        if (new_state.hasHigherUpdatePriority(alarm_state))
                            raised_level = new_state.severity;
                        alarm_state = new_state;
                    }
                }
                else
                {   // Annunciate if going to higher alarm severity
                    if (new_state.hasHigherUpdatePriority(alarm_state))
                        raised_level = new_state.severity;
                    alarm_state = new_state;
                }
            }
            // In maint. mode, INVALID & UNDEFINED are automatically ack'ed and not annunciated,
            // except for 'priority' alarms
            if (maintenance_mode &&
                !has_priority &&
                alarm_state.severity.ordinal() >= SeverityLevel.INVALID.ordinal())
            {
                alarm_state = alarm_state.createAcknowledged(alarm_state);
                raised_level = null;
            }
            current = current_state;
            alarm = alarm_state;
        }
        // Update state
        listener.alarmStateChanged(current, alarm);
        // New, higher alarm level?
        if (raised_level != null)
        {
            if (isAnnunciating())
                listener.annunciateAlarm(raised_level);
//            if (global_delay > 0)
//                global_check.schedule_update(global_delay);
        }
    }

    /** Listener to delayed update */
    public void delayedStateUpdate(final AlarmState delayed_state)
    {
        updateState(delayed_state, false);
    }

    /** Check if the new state adds up to 'count' alarms within 'delay'
     *  @param received_state
     *  @return <code>true</code> if alarm count reached/exceeded
     */
    private boolean checkCount(final AlarmState received_state)
    {
        final AlarmStateHistory hist = alarm_history.get();
        if (hist == null)
            return false;
        hist.add(received_state);
        if (! hist.receivedAlarmsWithinTimerange(delay.get()))
            return false;
        // Exceeded the alarm count. Reset the counter for next time.
        hist.reset();
        return true;
    }

    /** Determine new alarm state based on severities and latching behavior
     *  @param state_to_update Alarm state to update
     *  @param received_state Alarm state received from control system
     *  @return New alarm state or <code>null</code> if current state unchanged
     */
    private AlarmState latchAlarmState(final AlarmState state_to_update,
            final AlarmState received_state)
    {
        if (latching.get())
        {   // Latch to maximum severity
            if (received_state.hasHigherUpdatePriority(state_to_update))
            {   // Alarm latched to a higher level
                final AlarmState latched = new AlarmState(received_state.severity, received_state.message, received_state.value, received_state.time, true);
                return latched;
            }
            else if (state_to_update.isLatched())
            {
                // Received an update for a latched state, same or lower severity.
                // Clear the latch indicator
                final AlarmState unlatched = new AlarmState(state_to_update.severity, state_to_update.message, state_to_update.value, state_to_update.time, false);
                return unlatched;
            }
        }
        else
        {   // Not 'latched': Follow _active_ alarms
            if (state_to_update.severity.isActive())
                return received_state;
            else
            {   // We have an acknowledged severity.
                // Keep unless received state is higher
                if (received_state.hasHigherUpdatePriority(state_to_update))
                      return received_state;
            }
        }
        // No change
        return null;
    }

//    /** Send 'global' alarm update.
//     *  {@inheritDoc}
//     *  @see GlobalAlarmListener
//     */
//    public void updateGlobalState()
//    {
//        final AlarmState state;
//        synchronized (this)
//        {
//            in_global_alarm = true;
//            state = alarm_state;
//        }
//        listener.globalStateChanged(state);
//    }

//    /** If there was a 'global' alarm, clear it and notify listener */
//    private void clearGlobalAlarm()
//    {
//        final AlarmState state;
//        synchronized (this)
//        {
//            if (!in_global_alarm)
//                return;
//            in_global_alarm = false;
//            state = alarm_state;
//        }
//        listener.globalStateChanged(state);
//    }

    /** Acknowledge current alarm severity
     *  @param acknowledge Acknowledge or un-acknowledge?
     */
    public void acknowledge(boolean acknowledge)
    {
        final AlarmState current, alarm;
        // Cancel any scheduled 'global' update
//        global_check.cancel();
        boolean clear_global_alarm = false;
        synchronized (this)
        {
            if (acknowledge)
            {   // Does this actually 'clear' an acknowledged severity?
                if (current_state.severity == SeverityLevel.OK)
                {
                    alarm_state = AlarmState.createClearState(current_state.value);
                    clear_global_alarm = true;
                }
                else
                    alarm_state = alarm_state.createAcknowledged(current_state);
            }
            else
                // Un-acknowledge: Use the current state as the new alarm state
                // current state is never 'acked', but just to make sure call createUnack..
                alarm_state = current_state.createUnacknowledged();
            current = current_state;
            alarm = alarm_state;
        }
        // Notify listeners of latest state
//        if (clear_global_alarm)
//            clearGlobalAlarm();
        listener.alarmStateChanged(current, alarm);
    }

    /** Dispose alarm logic
     *
     *  <p>Cancel delayed check.
     */
    public void dispose()
    {
        delayed_check.cancel();
    }

    /** @return String representation for debugging */
    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "Current: " + current_state + ", Alarm: " + alarm_state;
    }
}
