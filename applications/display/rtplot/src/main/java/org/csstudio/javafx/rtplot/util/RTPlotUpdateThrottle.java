/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.util;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/** Throttle for updates.
 *
 *  <p>Initial trigger will result in update.
 *  Further triggers are suppressed during a 'dormant' period.
 *  At end, accumulated triggers received while dormant result
 *  in another update.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RTPlotUpdateThrottle
{
    final private ScheduledExecutorService timer =
            Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("RTPlotUpdateThrottle"));

    /** How long to stay dormant after an update */
    private volatile long dormant_ms;

    /** The update to perform */
    final private Runnable update_then_wake;

    /** Are updates currently suppressed? */
    final private AtomicBoolean dormant = new AtomicBoolean();

    /** Scheduled wakeUp call when no longer dormant */
    private ScheduledFuture<?> scheduled_wakeup;

    /** Any pending triggers while dormant */
    final private AtomicBoolean pending_trigger = new AtomicBoolean();


    /** Initialize
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     *  @param update {@link Runnable} to invoke for triggers
     */
    public RTPlotUpdateThrottle(final long dormant_time, final TimeUnit unit, final Runnable update)
    {
        setDormantTime(dormant_time, unit);
        this.update_then_wake = () ->
        {   // Perform the update
            try
            {
                // Wait a little to allow more updates to accumulate
                Thread.sleep(20);
                pending_trigger.set(false);
                update.run();
            }
            catch (InterruptedException ex)
            {
                // Shutdown, don't schedule another wakeup
                return;
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "Update failed", ex);
            }
            // Schedule wakeup
            scheduled_wakeup = timer.schedule(this::wakeUp, dormant_ms, TimeUnit.MILLISECONDS);
        };
    }

    /** Update the dormant time
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setDormantTime(final long dormant_time, final TimeUnit unit)
    {
        dormant_ms = unit.toMillis(dormant_time);
    }

    /** Call to request an update.
     *
     *  <p>First call will cause an update.
     *  Follow-up calls are noted but delayed until end of dormant period,
     *  resulting in one(!) follow-up update.
     */
    public void trigger()
    {
        if (dormant.getAndSet(true))
        {   // In dormant period, note additional triggers but don't act
            pending_trigger.set(true);
        }
        else
        {
            // In idle period, react to trigger, but on timer thread
            try
            {
                timer.execute(update_then_wake);
            }
            catch (RejectedExecutionException ex)
            {
                if (timer.isShutdown())
                    logger.log(Level.FINE, "Update throttle thread already closed", ex);
                else
                    throw ex;
            }
        }
    }

    private void wakeUp()
    {   // End dormant period
        dormant.set(false);
        // React on (one or more) pending triggers
        // received while dormant.
        if (pending_trigger.getAndSet(false))
            trigger();
    }

    /** Call to cancel scheduled updates */
    public void dispose()
    {
        // In case an update is running right now, terminate()
        timer.shutdownNow();
        // Waiting for updates to complete via
        //  timer.shutdown();
        //  timer.awaitTermination(2, TimeUnit.SECONDS)
        // doesn't help because threads may be in BufferUtil.getBufferedImage(),
        // waiting on UI thread, i.e. the current thread, and would need some time to time out.
        // terminate() is faster.

        pending_trigger.set(false);
        if (scheduled_wakeup != null)
            scheduled_wakeup.cancel(false);
    }
}
