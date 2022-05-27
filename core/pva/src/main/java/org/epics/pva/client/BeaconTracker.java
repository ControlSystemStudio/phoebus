/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.epics.pva.server.Guid;

/** Track received beacons
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BeaconTracker
{
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

            // Is this a newly started server..
            final boolean is_new_server = period > 0  &&  period < 30;
            // .. and we see it for the first time?
            final boolean was_new_server = previous_period > 0  &&  previous_period < 30;
            previous_period = period;
            return is_new_server  && !was_new_server;
        }
    }

    private final ConcurrentHashMap<Guid, BeaconInfo> beacons = new ConcurrentHashMap<>();
    private Instant last_cleanup = Instant.now();

    /** Check if a received beacon indicates a new server landscape
     *  @param guid  Globally unique ID of the server
     *  @param server Server that sent a beacon
     *  @param changes Change count, increments & rolls over as server has different channels
     *
     *  @return Should we restart searches for unresolved PVs?
     */
    public boolean check(final Guid guid, final InetSocketAddress server, final int changes)
    {
        String detail = logger.isLoggable(Level.SEVERE)
                      ? " *"
                      : null;

        final BeaconInfo info = beacons.computeIfAbsent(guid, s -> new BeaconInfo(guid, server, changes));

        final Instant now = Instant.now();

        boolean something_changed = info.updatePeriod(now);
        if (something_changed && detail != null)
            detail += " (new fast period)";
        if (! server.equals(info.address))
        {
            if (detail != null)
                detail += " (new address)";
            info.address = server;
            something_changed = true;
        }
        if (changes != info.changes)
        {
            if (detail != null)
                detail += " (new changes)";
            info.changes = changes;
            something_changed = true;
        }

        final long table_age = Duration.between(last_cleanup, now).getSeconds();
        if (table_age > 100) // TODO beacon_cleanup_period
        {
            removeOldBeaconInfo(now);
            last_cleanup = now;
        }
        if (detail != null)
            logger.log(Level.SEVERE, "Beacon check\n" + getTable(now, info.guid, detail));

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
            if (age > 180) // TODO beacon_cleanup_period
            {
                logger.log(Level.SEVERE,
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

    @Override
    public String toString()
    {
        return getTable(Instant.now(), null, null);
    }
}
