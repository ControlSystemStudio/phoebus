/*******************************************************************************
 * Copyright (c) 2018-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.phoebus.applications.alarm.AlarmSystem;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

/** Handle the update of "severity PVs"
 *
 *  <p>Maintains the PV connections,
 *  updates the PVs in a background thread.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SeverityPVHandler
{
    // When alarm server calls update(severity_pv_name, severity),
    // this value is not written to the PV right away for two reasons:
    //
    // 1) This could result in many updates. Channel Access/PV Access might
    //    suppress intermediate values and displays also tend to throttle updates,
    //    but we want to coalesce updates at the source to reduce overall traffic.
    // 2) For alarms with few changes, imagine that the IOC hosting the PV is down so PV cannot be written.
    //    Once the IOC reappears on the network, severity PV would have wrong value
    //    until the alarm state changes and the PV is then written.
    //
    // To avoid both issues, 'updates' tracks the most recent value for each PV,
    // and a separate thread 'updates' to PVs and - on success - removes from 'updates'.
    // The map coalesces multiple updates to a PV between writes,
    // and writes to missing PVs are repeated until they succeed.

    /** PVs that are currently handled */
    private static final Set<String> handled_pvs = ConcurrentHashMap.newKeySet();

    /** Pending updates
     *
     *  Updates are added, removed to be handled, re-added on errors to try again.
     *
     *  Entry in the map means PV has a severity to be written.
     *  PV not in map means either PV isn't in use, or there is no need to write a new value.
     */
    private static final ConcurrentHashMap<String, SeverityLevel> updates = new ConcurrentHashMap<>();

    /** Map of PVs by name */
    private static final ConcurrentHashMap<String, PV> pvs = new ConcurrentHashMap<>();

    /** Used to 'wait'. Write 'true' to abort ASAP. */
    private static final SynchronousQueue<Boolean> abort = new SynchronousQueue<>();

    /** Initialize, start SeverityPVUpdater thread */
    public static void initialize()
    {
        final Thread thread = new Thread(SeverityPVHandler::run, "SeverityPVUpdater");
        thread.setDaemon(true);
        thread.start();
    }

    /** Executed by SeverityPVUpdater:
     *  Waits for requested updates and performs them.
     */
    private static void run()
    {
        long next_heartbeat;

        if (AlarmSystem.heartbeat_pv.isEmpty())
        {
            logger.info("Not using any heartbeat PV");
            next_heartbeat = -1;
        }
        else
        {
            logger.info("Setting heartbeat PV '" + AlarmSystem.heartbeat_pv + "' every " + AlarmSystem.heartbeat_ms/1000 + " sec");
            next_heartbeat = 0;
        }

        while (true)
        {
            if (next_heartbeat >= 0)
            {
                // Trigger optional 'heartbeat' PV
                final long now = System.currentTimeMillis();
                if (now >= next_heartbeat)
                {
                    try
                    {
                        getConnectedPV(AlarmSystem.heartbeat_pv).write(1);
                    }
                    catch (Exception ex)
                    {
                        logger.log(Level.WARNING, "Error sending heartbeat to " + AlarmSystem.heartbeat_pv, ex);
                    }
                    next_heartbeat = now + AlarmSystem.heartbeat_ms;
                }
            }
            try
            {
                // Sleep if nothing else to do.
                // This sleep determines the heartbeat accuracy.
                if (updates.isEmpty())
                    Thread.sleep(500);
                else
                    performUpdates();
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, "SeverityPVUpdater error", ex);
            }
        }
    }

    /** Perform all requested updates, clearing the 'updates' map */
    private static void performUpdates()
    {
        for (String pv_name : updates.keySet())
        {
            // Atomically remove severity for this PV from accumulated updates
            final SeverityLevel severity = updates.remove(pv_name);
            logger.log(Level.FINE, "Should update PV '" + pv_name + "' to " + severity.name());
            try
            {
                final PV pv = getConnectedPV(pv_name);
                // null? Cannot create PV, forget about it. Else write...
                if (pv != null)
                    pv.write(severity.ordinal());
            }
            catch (Exception ex)
            {
                // Cannot connect, put back into `updates` for another attempt.
                // put-if-absent because update() could by now have registered a _new_ severity that we must preserve
                if (handled_pvs.contains(pv_name))
                {
                    logger.log(Level.WARNING, "Cannot set severity PV '" + pv_name + "' to " + severity.ordinal(), ex);
                    updates.putIfAbsent(pv_name, severity);
                }
            }
        }
    }

    private static PV getConnectedPV(final String pv_name) throws Exception
    {
        // Get or create PV
        final PV pv = Objects.requireNonNull(pvs.computeIfAbsent(pv_name, name ->
        {
            try
            {
                return PVPool.getPV(name);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create severity PV '" + name + "'", ex);
                return null;
            }
        }));

        // Assert connection
        int timeout = AlarmSystem.severity_pv_timeout;
        while (PV.isDisconnected(pv.read()))
        {
            if (abort.poll(1, TimeUnit.SECONDS) == Boolean.TRUE)
                // Abort waiting for PV
                return null;
            if (--timeout < 0)
                throw new Exception("No connection to " + pv_name);
        }
        return pv;
    }

    /** Request an update
     *  @param severity_pv_name Name of severity PV
     *  @param severity Severity to write
     */
    public static void update(final String severity_pv_name, final SeverityLevel severity)
    {
        // Write to map, handle in SeverityPVUpdater thread
        // If SeverityPVUpdater is slow, and several updates arrive for the same PV,
        // this will place the most recent severity for that PV in the map.
        handled_pvs.add(severity_pv_name);
        updates.put(severity_pv_name, severity);
    }

    /** Clear all entries for a PV
     *  @param severity_pv_name PV that should no longer be updated
     */
    public static void clear(final String severity_pv_name)
    {
        if (severity_pv_name != null)
        {
            // Mark to be cleared, and remove from 'updates' in case one is pending..
            handled_pvs.remove(severity_pv_name);
            updates.remove(severity_pv_name);
        }
    }

    /** Release all PVs */
    public static void stop()
    {
        // Delete all queued updates
        updates.clear();
        // Abort potential wait for a PV
        abort.offer(Boolean.TRUE);
        // Release all PVs
        final Iterator<PV> pv_iter = pvs.values().iterator();
        while (pv_iter.hasNext())
        {
            final PV pv = pv_iter.next();
            PVPool.releasePV(pv);
            pv_iter.remove();
        }
    }
}
