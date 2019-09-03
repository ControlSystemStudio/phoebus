/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/** Handle throttled updates on UI thread.
 *
 *  <p>First request to schedule an update results in
 *  nearly immediate update. 'Nearly' to allow for a few more
 *  updates to accumulate, since <code>Platform.runLater</code> suggests
 *  "Applications are encouraged to batch up multiple operations
 *   into fewer runLater calls", and the same likely applies to SWT.
 *
 *  <p>After performing updates, a delay prevents consuming 100% of the UI thread.
 *  After the delay, if more updates are found to be scheduled, they are
 *  handled, again followed by a delay.
 *
 *  <p>Once there are no more updates, the thread waits until
 *  woken up again by the next requested update.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RepresentationUpdateThrottle
{
    /** Instance counter to aid in debugging the throttle start/shutdown */
    private static final AtomicInteger instance = new AtomicInteger();

    /** Period in seconds for logging update performance */
    private static final int performance_log_period_secs = Preferences.performance_log_period_secs;

    /** UI thread durations above this threshold are logged */
    private static final int performance_log_threshold_ms = Preferences.performance_log_threshold_ms;

    /** Time waited after a trigger to allow for more updates to accumulate */
    private static final long update_accumulation_time = Preferences.update_accumulation_time;

    /** Pause between updates to prevent flooding the UI thread */
    private static final long update_delay = Preferences.update_delay;

    /** Executor for UI thread */
    private final Executor gui_executor;

    /** Thread that performs the throttling */
    private final Thread throttle_thread;

    /** Flag that informs throttle_thread to enable updates or ignore */
    protected volatile boolean enabled = true;

    /** Flag that informs throttle_thread to run or exit */
    protected volatile boolean run = true;

    /** Representations that requested an update.
     *
     *  <p>Ordered by time when representations requested an update
     *
     *  SYNC on access
     */
    private final Set<WidgetRepresentation<?, ?, ?>> updateable = new LinkedHashSet<>();

    /** @param gui_executor Executor for UI thread */
    public RepresentationUpdateThrottle(final Executor gui_executor)
    {
        final String name = "RepresentationUpdateThrottle" + instance.incrementAndGet();
        logger.log(Level.FINE, "Create " + name);
        this.gui_executor = gui_executor;
        throttle_thread = new Thread(this::doRun);
        throttle_thread.setName(name);
        throttle_thread.setDaemon(true);
        throttle_thread.start();
    }

    /** Called by toolkit representation to request an update.
     *
     *  <p>That representation's <code>updateChanges()</code> will be called
     *
     *  @param representation Toolkit representation that requests update
     */
    public void scheduleUpdate(final WidgetRepresentation<?, ?, ?> representation)
    {
        synchronized (updateable)
        {
            updateable.add(representation);
            updateable.notifyAll();
        }
    }

    /** @param enable Enable updates, or pause? */
    public void enable(final boolean enable)
    {
        enabled = enable;
        synchronized (updateable)
        {
            updateable.notifyAll();
        }
    }

    private void doRun()
    {
        // Running average of update duration, i.e. time spend in UI thread
        long update_ms = -1;

        // Next time we log the update duration
        Instant next_update_log = Instant.now().plusSeconds(6);
        try
        {
            while (run)
            {
                // Wait for requested updates
                synchronized (updateable)
                {
                    while (run  &&  updateable.isEmpty())
                        updateable.wait();
                }
                if (! run)
                    return;
                // Wait a little longer to allow more updates to accumulate
                Thread.sleep(update_accumulation_time);
                if (! enabled)
                    continue;
                // Obtain safe copy, clear what had been accumulated
                final WidgetRepresentation<?, ?, ?>[] representations;
                synchronized (updateable)
                {
                    // Creating a direct copy, i.e. another new LinkedHashSet<>(updateable),
                    // would be expensive, since we only need a _list_ of what's to update.
                    // Could use type-safe
                    //    new ArrayList<WidgetRepresentation<Pane, Node>>(updateable)
                    // but that calls toArray() internally, so doing that directly
                    representations = updateable.toArray(new WidgetRepresentation[updateable.size()]);
                    updateable.clear();
                }

                // Perform requested updates on UI thread
                // Using CountDownLatch because that allows while (await, run).
                // Future would require catching TimeoutException or cancel() on shutdown.
                final CountDownLatch done = new CountDownLatch(1);
                final long update_start = System.currentTimeMillis();
                updateInUI(representations, done);

                // Wait for those updates to finish
                while (! done.await(100, TimeUnit.MILLISECONDS))
                    if (! run)
                        return; // Never mind, shutdown

                // Update performance info
                final long ms = System.currentTimeMillis() - update_start;
                if (update_ms < 0)
                    update_ms = ms;
                else
                    update_ms = (9*update_ms + ms)/10;

                // Wait a little to throttle updates
                Thread.sleep(update_delay);

                final Instant now = Instant.now();
                if (now.isAfter(next_update_log))
                {
                    if (update_ms > performance_log_threshold_ms)
                        logger.log(Level.FINE, "Averange update duration: {0} ms", update_ms);
                    next_update_log = now.plusSeconds(performance_log_period_secs);
                }
            }
        }
        catch (final Exception ex)
        {
            logger.log(Level.SEVERE, "Update throttle failure", ex);
        }
        finally
        {
            logger.log(Level.FINE, "Exiting " + throttle_thread.getName());
        }
    }

    /** Perform updates in UI thread.
     *  @param representations Representations that need to be updated
     *  @param done Must be signaled when representations have been updated
     */
    private void updateInUI(final WidgetRepresentation<?, ?, ?>[] representations,
                            final CountDownLatch done)
    {
        gui_executor.execute(() ->
        {
            for (final WidgetRepresentation<?, ?, ?> representation : representations)
            {
                if (! run)
                    break;
                try
                {
                    // Skip updates when representation has been disposed
                    if (representation.model_widget != null)
                        representation.updateChanges();
                }
                catch (final Throwable ex)
                {
                    logger.log(Level.SEVERE, "Representation update failed", ex);
                }
            }
            done.countDown();
        });
    }

    /** Shutdown the throttle thread and wait for it to exit */
    public void shutdown()
    {
        run = false;
        synchronized (updateable)
        {
            updateable.notifyAll();
        }
        try
        {
            throttle_thread.join(2000);
        }
        catch (final InterruptedException ex)
        {
            // Ignore, closing down anyway
        }
        if (throttle_thread.isAlive())
            logger.log(Level.WARNING, "Representation update throttle fails to terminate within 2 seconds");
    }
}
