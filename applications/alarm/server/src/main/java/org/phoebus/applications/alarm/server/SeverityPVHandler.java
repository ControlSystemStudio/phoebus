/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import static org.phoebus.applications.alarm.AlarmSystem.logger;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
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
    /** Pending updates */
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
        final Iterator<Entry<String, SeverityLevel>> entries = updates.entrySet().iterator();
        while (entries.hasNext())
        {
            final Entry<String, SeverityLevel> entry = entries.next();
            final String pv_name = entry.getKey();
            final SeverityLevel severity = entry.getValue();

            logger.log(Level.FINE, "Should update PV '" + pv_name + "' to " + severity.name());
            try
            {
                final PV pv = getConnectedPV(pv_name);
                if (pv != null)
                    pv.write(severity.ordinal());

                // Remove request on success. Otherwise will try again
                entries.remove();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot set severity PV '" + pv_name + "' to " + severity.ordinal(), ex);
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
        int timeout = AlarmSystem.connection_timeout;
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
        updates.put(severity_pv_name, severity);
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
