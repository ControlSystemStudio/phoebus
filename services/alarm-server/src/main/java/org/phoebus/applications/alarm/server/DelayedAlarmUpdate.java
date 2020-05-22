/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.AlarmState;

/** Helper for checking alarms after a delay.
 *  It will trigger a transition to a new state only after a delay.
 *
 *  After the delay, it will invoke the listener.
 *
 *  While the timer is running, the state might be updated,
 *  for example to a higher latched state.
 *
 *  The check can also be canceled because the control system sent an 'OK'
 *  value in time.
 */
@SuppressWarnings("nls")
public class DelayedAlarmUpdate
{
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setName("DelayedAlarmUpdate");
        thread.setDaemon(true);
        return thread;
    });

    /** Listener to notify when delay expires */
    private final Consumer<AlarmState> listener;

    /** Alarm state to which we would update after the delay, unless it clears in time */
    private final AtomicReference<AlarmState> state = new AtomicReference<>();

    /** Timer task used to perform the delay */
    private volatile ScheduledFuture<?> scheduled_task = null;

    /** Initialize
     *  @param listener Listener to notify when delay expires
     */
    DelayedAlarmUpdate(final Consumer<AlarmState> listener)
    {
        this.listener = listener;
    }

    /** Schedule a delayed state update, or adjust the update that's already
     *  scheduled with the latest state information.
     *
     *  @param new_state State to which we would go if there was no delay
     *  @param seconds Delay to use if we need to add this to the timer.
     *                 Ignored when adjusting a pending update
     */
    void schedule_update(final AlarmState new_state, final int seconds)
    {
        if (new_state == null)
        {
            logger.log(Level.SEVERE, "DelayedAlarmUpdate with null", new IllegalStateException());
            return;
        }

        state.set(new_state);

        synchronized (this)
        {
            // Already scheduled?
            if (scheduled_task != null)
                return;

            // Schedule in timer
            final Runnable new_task = () ->
            {
                // Indicate that we ran
                scheduled_task = null;

                // Save state for call to listener, reset
                final AlarmState the_state = state.getAndSet(null);
                if (the_state == null)
                {
                    // Don't run because update was cancelled
                    return;
                }
                //  Re-evaluate alarm logic with the delayed state,
                //  not allowing any further delays.
                try
                {
                    listener.accept(the_state);
                }
                catch (Throwable ex)
                {
                    logger.log(Level.SEVERE, "Error in delayed alarm update", ex);
                }
            };
            logger.log(Level.FINE, () -> "Schedule check for " + new_state + " in " + seconds + " secs");
            scheduled_task = timer.schedule(new_task, seconds, TimeUnit.SECONDS);
        }
    }

    /** @return Alarm state to which we'll go after the delay expires */
    AlarmState getState()
    {
        return state.get();
    }

    /** Cancel delayed alarm check because control system PV cleared.
     *  OK to call multiple times, even when nothing was scheduled.
     */
    public void cancel()
    {
        state.set(null);
        final ScheduledFuture<?> task = scheduled_task;
        scheduled_task = null;
        if (task != null)
        {
            task.cancel(false);
            logger.log(Level.FINE, () -> "Schedule check cancelled.");
        }
    }
}
