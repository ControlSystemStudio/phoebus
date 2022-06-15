/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.epics.pva.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.epics.pva.PVASettings;
import org.epics.pva.server.Guid;

/** Track received beacons
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BeaconTracker
{
    /** Logger for beacon info */
    public static final Logger logger = Logger.getLogger(BeaconTracker.class.getPackage().getName());

    /** Info about one tracked beacon
     *
     *  Server is identified by GUID
     */
    private static class BeaconInfo
    {
        /** Server's unique ID */
        final Guid guid;

        /** Address that sent beacon, likely server's UDP address */
        InetSocketAddress address;

        /** Change increment reported by server */
        int changes;

        /** Time of last beacon */
        Instant last = null;

        /** Period of beacon before the last one */
        long previous_period = 0;

        /** Period (seconds) of last beacon */
        long period = 0;

        BeaconInfo(final Guid guid, final InetSocketAddress address, final int changes)
        {
            this.guid = guid;
            this.address = address;
            this.changes = changes;
        }

        /** Update beacon period because we just received another one
         *  @param now Time for which to update the beacon period
         *  @return Does this indicate a "new" beacon?
         */
        boolean updatePeriod(final Instant now)
        {
            if (last == null)
                period = 0;
            else
                period = Duration.between(last, now).getSeconds();
            last = now;

            // Is this a server we see for the first time (period 0)
            // or one started recently (period ~15 sec)?
            final boolean is_new_server = period >= PVASettings.EPICS_PVA_FAST_BEACON_MIN  &&
                                          period  < PVASettings.EPICS_PVA_FAST_BEACON_MAX;
            // .. and we see it for the first time?
            final boolean was_new_server = previous_period > 0  &&
                                           previous_period < PVASettings.EPICS_PVA_FAST_BEACON_MAX;
            previous_period = period;
            // -> That would be a reason to re-start searches
            //
            // Servers we see for the first time (period 0) may in fact be
            // long running servers that only send beacons every 180 s.
            // They will be reported as "new", but this happens likely
            // when we just started searching for a channel,
            // so its search interval has not settled, yet, and will
            // thus not be 'boosted', avoiding an early burst of searches
            // as we see each server's beacons for the first time.
            return is_new_server  && !was_new_server;
        }
    }

    /** Map of server IDs to beacon info */
    private final ConcurrentHashMap<Guid, BeaconInfo> beacons = new ConcurrentHashMap<>();

    /** Last time the 'beacons' were cleaned of orphaned entries */
    private Instant last_cleanup = Instant.now();

    /** Check if a received beacon indicates a new server landscape
     *  @param guid  Globally unique ID of the server
     *  @param server Server that sent a beacon
     *  @param changes Change count, increments and rolls over as server adds new channels
     *
     *  @return Should we restart searches for unresolved PVs?
     */
    public boolean check(final Guid guid, final InetSocketAddress server, final int changes)
    {
        // Only assemble detail of new beacon if FINE logging is enabled
        String detail = logger.isLoggable(Level.FINE)
                      ? " *"
                      : null;

        final Instant now = Instant.now();

        // Locate or create beacon info for that GUID
        final BeaconInfo info = beacons.computeIfAbsent(guid, s -> new BeaconInfo(guid, server, changes));

        // Does period indicate a new server?
        boolean something_changed = info.updatePeriod(now);
        if (something_changed && detail != null)
            detail += info.period <= 0 ? " (new beacon)" : " (fast period)";
        // Does server report from new address?
        if (! server.equals(info.address))
        {
            if (detail != null)
                detail += " (new address)";
            info.address = server;
            something_changed = true;
        }
        // Does server report that it might have new channels?
        if (changes != info.changes)
        {
            if (detail != null)
                detail += " (new changes)";
            info.changes = changes;
            something_changed = true;
        }

        // Periodically remove old beacon infos
        final long table_age = Duration.between(last_cleanup, now).getSeconds();
        if (table_age > PVASettings.EPICS_PVA_MAX_BEACON_AGE)
        {
            removeOldBeaconInfo(now);
            last_cleanup = now;
        }
        // Log detail, if available
        if (detail != null)
            logger.log(Level.FINE, "Beacon update\n" + getTable(now, info.guid, detail));

        // Search or not?
        return something_changed;
    }

    /** Delete old beacon info */
    private void removeOldBeaconInfo(final Instant now)
    {
        final Iterator<Entry<Guid, BeaconInfo>> infos = beacons.entrySet().iterator();
        while (infos.hasNext())
        {
            final BeaconInfo info = infos.next().getValue();
            final long age = Duration.between(info.last, now).getSeconds();
            if (age > PVASettings.EPICS_PVA_MAX_BEACON_AGE)
            {
                logger.log(Level.FINER,
                           () -> "Removing beacon info " + info.guid + " (" + info.address + "), last seen " + age + " seconds ago");
                infos.remove();
            }
        }
    }

    /** @param now Current time
     *  @param active ID of server that just sent a beacon or <code>null</code> if nothing new
     *  @param detail Detail of what's new or <code>null</code> if nothing new
     *  @return Tabular list of beacons, optionally highlighting the 'active' server with 'detail'
     */
    private String getTable(final Instant now, final Guid active, final String detail)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("GUID                     IP                                    Age Changes Period\n");
        for (BeaconInfo info : beacons.values())
        {
            final long age = Duration.between(info.last, now).getSeconds();
            buf.append(String.format("%s %-35s %3d s  %3d    %4d s",
                                     info.guid.asText(),
                                     info.address,
                                     age,
                                     info.changes,
                                     info.period));
            if (info.guid.equals(active))
                buf.append(" ")
                   .append(detail);
            buf.append("\n");
        }
        return buf.toString();
    }

    /** @return String representation */
    @Override
    public String toString()
    {
        return getTable(Instant.now(), null, null);
    }
}
