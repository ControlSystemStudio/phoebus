/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.monitoring;

import static org.phoebus.ui.application.PhoebusApplication.logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import javafx.application.Platform;

/** Responsiveness Monitor
 *
 *  <p>Checks if the UI thread is processing events,
 *  logs stack traces when the UI thread is suspected
 *  to be blocked.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ResponsivenessMonitor
{
    // Compare org.eclipse.ui.monitoring
    // https://github.com/eclipse/eclipse.platform.ui/tree/master/bundles/org.eclipse.ui.monitoring .
    // It hooks into the SWT event loop to time the period between events.
    //
    // Didn't see a way to do that for JavaFX.
    // Instead, periodically submitting runnable to Platform.Platform.runLater()
    // and testing if it was invoked.

    /** Timer for scheduling periodic test */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
        final Thread thread = new Thread(runnable);
        thread.setName("ResponsivenessMonitor");
        thread.setDaemon(true);
        return thread;
    });

    /** Set by UI 'ping' runnable */
    private final AtomicBoolean ui_thread_responded = new AtomicBoolean(true);

    /** Is the UI thread frozen at this time? */
    private final AtomicBoolean frozen = new AtomicBoolean(false);

    /** ID of UI thread */
    private final long ui_thread_id;

    /** Beam for dumping thread infos */
    private final ThreadMXBean thread_bean;

    /** Functionalities of ThreadMXBean */
    private final boolean dumpLockedMonitors, dumpLockedSynchronizers;


    /** Create responsiveness monitor
     *
     *  @param initial_delay Initial delay, time to wait until first check
     *  @param period Period between tests, i.e. the minimum detected UI freeze duration
     *  @param unit Units for the period
     */
    public ResponsivenessMonitor(final long initial_delay, final long period, final TimeUnit unit)
    {
        if (! Platform.isFxApplicationThread())
            throw new IllegalStateException("Must create on UI thread");
        ui_thread_id = Thread.currentThread().getId();
        thread_bean = ManagementFactory.getThreadMXBean();
        dumpLockedMonitors = thread_bean.isObjectMonitorUsageSupported();
        dumpLockedSynchronizers = thread_bean.isSynchronizerUsageSupported();
        timer.scheduleWithFixedDelay(this::check, initial_delay, period, unit);
    }

    /** Called periodically to check if UI thread responds */
    private void check()
    {
        // Did the UI thread respond to the last 'ping'?
        if (ui_thread_responded.getAndSet(false))
        {
            // Yes.
            final boolean was_frozen = frozen.getAndSet(false);
            if (was_frozen)
                logger.log(Level.SEVERE, "UI Updates resume");
            // Ping UI thread gain, see if it remains responsive
            Platform.runLater(this::pingUI);
        }
        else
        {
            // UI did not respond
            final boolean was_frozen = frozen.getAndSet(true);
            if (! was_frozen)
                reportUIFreeze();
            // else: Log only once, then note when UI resumes
        }
    }

    /** Log detail about UI freezeup */
    private void reportUIFreeze()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("UI Freezeup\n\n");
        final ThreadInfo[] thread_infos = thread_bean.dumpAllThreads(dumpLockedMonitors, dumpLockedSynchronizers);
        for (ThreadInfo info : thread_infos)
        {
            if (info.getThreadId() == Thread.currentThread().getId())
            {
                // Exclude the ResponsivenessMonitor thread
                continue;
            }
            if (info.getThreadId() == ui_thread_id)
            {
                buf.append("\n");
                buf.append("*********************************\n");
                buf.append("*** JavaFX Application Thread ***\n");
                buf.append("*********************************\n");
            }
            buf.append(info);
        }

        logger.log(Level.SEVERE, buf.toString());
    }

    /** Called by UI thread */
    private void pingUI()
    {
        // Indicate that UI thread executed
        ui_thread_responded.set(true);
    }

    /** Stop the monitor */
    public void close()
    {
        timer.shutdown();
    }
}
