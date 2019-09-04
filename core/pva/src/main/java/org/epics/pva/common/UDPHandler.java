/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;

import org.epics.pva.data.Hexdump;

/** Base for handling UDP traffic
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class UDPHandler
{
    protected volatile boolean running = true;

    /** Read, decode, handle messages
     *  @param udp Socket to use
     *  @param buffer Receive buffer to use
     */
    protected void listen(final DatagramChannel udp, final ByteBuffer buffer)
    {
        logger.log(Level.FINE, "Starting " + Thread.currentThread().getName());
        while (running)
        {
            try
            {
                // Wait for next UDP packet
                buffer.clear();
                final InetSocketAddress from = (InetSocketAddress) udp.receive(buffer);
                buffer.flip();

                // XXX Check against list of ignored addresses?

                logger.log(Level.FINER, () -> "Received UDP from " + from + "\n" + Hexdump.toHexdump(buffer));
                handleMessages(from, buffer);
            }
            catch (Exception ex)
            {
                if (running)
                    logger.log(Level.WARNING, "UDP receive error", ex);
                // else: Ignore, closing
            }
        }
        logger.log(Level.FINE, "Exiting " + Thread.currentThread().getName());
    }

    /** Handle one or more reply messages
     *  @param from
     *  @param buffer
     */
    private void handleMessages(final InetSocketAddress from, final ByteBuffer buffer)
    {
        while (buffer.remaining() >= PVAHeader.HEADER_SIZE)
        {
            byte b = buffer.get();
            if (b != PVAHeader.PVA_MAGIC)
            {
                logger.log(Level.WARNING, "Received UDP packet with invalid magic startbyte from " + from);
                return;
            }

            final byte version = buffer.get();

            final byte flags = buffer.get();
            if ((flags & PVAHeader.FLAG_BIG_ENDIAN) != 0)
                buffer.order(ByteOrder.BIG_ENDIAN);
            else
                buffer.order(ByteOrder.LITTLE_ENDIAN);

            final byte command = buffer.get();
            final int payload = buffer.getInt();
            final int next = buffer.position() + payload;
            if (next > buffer.limit())
            {
                logger.log(Level.WARNING, "Received UDP packet with expected payload of " +
                        payload + " but only " + buffer.remaining() + " bytes of data from " + from);
                return;
            }

            // Skip control messages
            if ((flags & PVAHeader.FLAG_CONTROL) == 0)
            {
                // If message cannot be decoded,
                // this might indicate overall message corruption
                if (! handleMessage(from, version, command, payload, buffer))
                    return;
            }

            // Position on next message in case handleMessage read too much or too little
            buffer.position(next);
        }
    }

    abstract protected boolean handleMessage(final InetSocketAddress from, final byte version,
                                             final byte command, final int payload, final ByteBuffer buffer);

    public void close()
    {
        running = false;
    }
}
