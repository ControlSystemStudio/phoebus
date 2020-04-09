/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.epics.pva.common.PVAHeader;
import org.epics.pva.data.PVABitSet;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAStructure;

/** One client's subscription to "monitor" a PV
 *
 *  <p>Maintains the most recent value sent to client,
 *  sends changes to that client as the value is updated.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class MonitorSubscription
{
    /** ID of monitor request sent by client */
    private final int req;

    /** The PV */
    private final ServerPV pv;

    /** TCP connection to client */
    private final ServerTCPHandler tcp;

    // Clients subscribe at different times,
    // and their TCP connection might be able to handle updates
    // at different rates, so each subscription maintains
    // the per-client state of the data, changes and overruns.

    /** Most recent value, to be sent to clients.
     *  SYNC on data
     */
    private final PVAStructure data;

    /** Most recent changes, yet to be sent to clients
     *  SYNC on data
     */
    private volatile BitSet changes = new BitSet();

    /** Overruns, u.e. updates received between successful transmissions to client
     *  SYNC on data
     */
    private final BitSet overrun = new BitSet();

    /** Is an update pending to be sent out?
     *
     *  <p>Used to prevent scheduling more updates that TCP connection can handle.
     *  Changes from multiple updates are combined, potentially triggering overrun.
     */
    private final AtomicBoolean pending = new AtomicBoolean(true);

    MonitorSubscription(final int req, final ServerPV pv, final ServerTCPHandler tcp)
    {
        this.req = req;
        this.pv = pv;
        this.tcp = tcp;
        data = pv.getData();

        // Initial update: Send all the data
        changes.set(0);
        tcp.submit(this::encodeMonitor);
    }

    boolean isFor(final ServerTCPHandler tcp, final int req)
    {
        return this.tcp == tcp  &&  (req == -1 || this.req == req);
    }

    void update(final PVAStructure new_data) throws Exception
    {
        synchronized (data)
        {
            final BitSet old_changes = changes;

            // Update data, see what's new
            changes = data.update(new_data);

            // Accumulate overrun:
            // See what had changed before, and now changed again
            old_changes.and(changes);
            overrun.or(old_changes);
        }

        // Only submit when there's not already one pending, waiting to be sent out
        if (pending.compareAndSet(false, true))
            tcp.submit(this::encodeMonitor);
        else
            logger.log(Level.WARNING, "Skipping already submitted " + this);
    }

    private void encodeMonitor(final byte version, final ByteBuffer buffer) throws Exception
    {
        pending.set(false);

        logger.log(Level.FINE, () -> "Sending MONITOR value for " + pv + ": changes " + changes + ", overrun " + overrun);

        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_SERVER, PVAHeader.CMD_MONITOR, 0);
        final int payload_start = buffer.position();

        buffer.putInt(req);
        // Subcommand 0 = value update
        buffer.put((byte)0);

        synchronized (data)
        {
            // Encode what changed
            PVABitSet.encodeBitSet(changes, buffer);
            // Encode the changed data
            for (int index = changes.nextSetBit(0);
                    index >= 0;
                    index = changes.nextSetBit(index + 1))
            {
                // final version of index to allow use in logging lambdas
                final int i = index;
                final PVAData element = data.get(i);
                logger.log(Level.FINER, () -> "Encode data for indexed element " + i + ": " + element);
                element.encode(buffer);

                // Javadoc for nextSetBit() suggests checking for MAX_VALUE
                // to avoid index + 1 overflow and thus starting over with first bit
                if (i == Integer.MAX_VALUE)
                    break;
            }
            changes.clear();

            PVABitSet.encodeBitSet(overrun, buffer);
            overrun.clear();
        }

        final int payload_end = buffer.position();
        buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, payload_end - payload_start);
    }

    @Override
    public String toString()
    {
        return "Monitor Subscription(" + pv + ", " + tcp + ")";
    }
}
