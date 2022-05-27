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
         *  @return Does this indicate a "new" beacon?
         */
        boolean updatePeriod()
        {
            final Instant now = Instant.now();
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

        boolean something_changed = info.updatePeriod();
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

        if (detail != null)
            logger.log(Level.SEVERE, "Beacon check\n" + getTable(info.guid, detail));

        // Search or not?
        return something_changed;
    }

    // TODO Delete old beacon info

    /** @param active ID of server that just sent a beacon or <code>null</code> if nothing new
     *  @param detail Detail of what's new or <code>null</code> if nothing new
     *  @return Tabular list of beacons, optionally highlighting the 'active' server with 'detail'
     */
    private String getTable(final Guid active, final String detail)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("GUID                     IP                              Changes Period\n");
        for (BeaconInfo info : beacons.values())
        {
            buf.append(String.format("%s %-35s %3d %4d s",
                                     info.guid.asText(),
                                     info.address,
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
        return getTable(null, null);
    }
}
