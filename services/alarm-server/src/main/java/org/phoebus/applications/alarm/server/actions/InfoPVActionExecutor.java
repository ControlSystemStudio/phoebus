/*******************************************************************************
 * Copyright (c) 2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.server.actions;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.phoebus.applications.alarm.model.AlarmTreeItem;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** Executor for 'infopv:' actions
 *
 *  <p>Handles automated actions with the following detail:
 *
 *  <p>"infopv:SomePVName"<br>
 *  Writes alarm detail as string to PV.
 *
 *  @author Kay Kasemir
 */
public class InfoPVActionExecutor
{
    /** Map of PV name to PV.
     *  PVs are created/added on first use and then kept in here until the server shuts down.
     */
    private static final ConcurrentHashMap<String, PV> pvs = new ConcurrentHashMap<>();

    /** Info to write to a PV and time it was requested */
    private static record TimedInfo(Instant time, String info) {};

    /** Map of PV name to most recent info.
     *  Catches updates to a PV.
     *  If several updates arrive for the same PV, they are not queued
     *  because older messages tend to be obsolete.
     *  They are written with a slight delay to reduce traffic, then removed.
     */
    private static final ConcurrentHashMap<String, TimedInfo> updates = new ConcurrentHashMap<>();

    /** Flag for thread to exit */
    private static CountDownLatch done = new CountDownLatch(1);

    /** Thread that performs updates */
    private static final Thread thread = new Thread(InfoPVActionExecutor::run, "InfoPVActionExecutor");


    /** Initialize, start InfoPVActionExecutor thread */
    public static void initialize()
    {
        thread.setDaemon(true);
        thread.start();
    }

    /** Request writing alarm info text to PV
     *  @param item Alarm item from which to get alarm info
     *  @param pv_name Name of PV to update
     */
    public static void writeInfo(final AlarmTreeItem<?> item, final String pv_name)
    {
        final String info = EmailActionExecutor.createTitle(item) + System.lineSeparator() +
                            EmailActionExecutor.createBody(item);

        // Register PV or find existing one
        PV pv = pvs.computeIfAbsent(pv_name, name ->
        {
            try
            {
                return PVPool.getPV(pv_name);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create PV '" + pv_name + "'", ex);
            }
            return null;
        });
        // On success, register update
        if (pv != null)
            updates.put(pv_name, new TimedInfo(Instant.now(), info));
    }


    /** Executed by InfoPVActionExecutor:
     *  Waits for requested updates and performs them.
     */
    private static void run()
    {
        try
        {
            // Delay to throttle the rate of writes and re-tries
            while (! done.await(1, TimeUnit.SECONDS))
            {
                // Keep trying to write an update until it's old
                final Instant old = Instant.now().minus(Duration.ofSeconds(30));
                for (String pv_name : updates.keySet())
                {
                    final TimedInfo update = updates.get(pv_name);
                    boolean success = false;
                    try
                    {
                        final PV pv = pvs.get(pv_name);
                        pv.write(update.info);
                        success = true;
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Failed to write alarm info to " + pv_name, ex);
                    }
                    // If update was handled, or failed several times and is now old, remove it
                    if (success  ||   update.time.isBefore(old))
                    {   // Only remove the one we're dealing with!
                        if (updates.remove(pv_name, update))
                        {
                            if (success)
                                logger.log(Level.INFO, "Wrote alarm info to " + pv_name);
                            else
                                logger.log(Level.WARNING, "Give up writing alarm info to " + pv_name);
                        }
                        // else: There's already a new update, keep that
                    }
                    // else: Update failed, not old, try again
                }

            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, Thread.currentThread().getName() + " error", ex);
        }
    }


    /** Release all PVs */
    public static void stop()
    {
        // Stop thread
        done.countDown();
        try
        {
            thread.join(5000);
        }
        catch (InterruptedException ex)
        {
            // Ignore, closing down anyway
        }
        // Release all PVs
        for (PV pv : pvs.values())
            PVPool.releasePV(pv);
        pvs.clear();
    }
}
