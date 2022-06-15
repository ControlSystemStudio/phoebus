/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.epics.pva.data.PVAAddress;
import org.epics.pva.data.PVAString;

/** Helper for search requests
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SearchRequest
{
    /** Channel with CID to be searched */
    public static class Channel
    {
        /** Client ID */
        protected final int cid;

        /** Channel name */
        protected final String name;

        /** @param cid Client ID
         *  @param name Channel name
         */
        public Channel(final int cid, final String name)
        {
            this.cid = cid;
            this.name = name;
        }

        /** @return Client channel ID */
        public int getCID()
        {
            return cid;
        }

        /** @return Channel name */
        public String getName()
        {
            return name;
        }

        @Override
        public String toString()
        {
            return "'" + name + "' [CID " + cid + "]";
        }
    };

    /** Sequence number */
    public int seq;
    /** Is it a unicast? */
    public boolean unicast;
    /** Is reply required? */
    public boolean reply_required;
    /** Address of client */
    public InetSocketAddress client;
    /** Names requested in search, <code>null</code> for 'list' */
    public List<Channel> channels;

    /** Check search request
     *
     *  @param from Peer address
     *  @param version Message version
     *  @param payload Payload size
     *  @param buffer Buffer positioned on payload
     *  @return Decoded search request or <code>null</code> if not a valid search request
     */
    public static SearchRequest decode(final InetSocketAddress from, final byte version,
                                       final int payload, final ByteBuffer buffer)
    {
        // pvinfo sends 0x1D=29 bytes:
        // Header and flags
        // 0000 - CA 02 00 03 1D 00 00 00 00 00 00 00 81 00 00 00 - ................
        // Return address
        // 0010 - 00 00 00 00 00 00 00 00 00 00 FF FF 00 00 00 00 - ................
        // Return port uint16 0xB7C8, byte 0 protocols, uint16 0 channels
        // 0020 - C8 B7 00 00 00
        // Searches add 4 bytes for protocol (length 3) "tcp",
        // plus the list of names.
        if (payload < 4+1+3+16+2+1+2)
        {
            logger.log(Level.WARNING, "PVA client " + from + " sent only " + payload + " bytes for search request");
            return null;
        }
        final SearchRequest search = new SearchRequest();

        // Search Sequence ID
        search.seq = buffer.getInt();

        // 0-bit for replyRequired, 7-th bit for "sent as unicast" (1)/"sent as broadcast/multicast" (0)
        final byte flags = buffer.get();
        search.unicast = (flags & 0x80) == 0x80;
        search.reply_required = (flags & 0x01) == 0x01;

        // reserved
        buffer.get();
        buffer.get();
        buffer.get();

        // responseAddress, IPv6 address in case of IP based transport, UDP
        final InetAddress addr;
        try
        {
            addr = PVAAddress.decode(buffer);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA Client " + from + " sent search #" + search.seq + " with invalid address");
            return null;
        }
        final int port = Short.toUnsignedInt(buffer.getShort());

        // Use address from message unless it's a generic local address
        if (addr.isAnyLocalAddress() || port <= 0)
            search.client = from;
        else
            search.client = new InetSocketAddress(addr, port);

        // Assert that client supports "tcp", ignore rest
        boolean tcp = false;
        int count = Byte.toUnsignedInt(buffer.get());
        String protocol = "<none>";
        for (int i=0; i<count; ++i)
        {
            protocol = PVAString.decodeString(buffer);
            if ("tcp".equals(protocol))
            {
                tcp = true;
                break;
            }
        }

        // Loop over searched channels
        count = Short.toUnsignedInt(buffer.getShort());

        if (count == 0)
        {   // pvlist request
            search.channels = null;
            logger.log(Level.FINER, () -> "PVA Client " + from + " sent search #" + search.seq + " to list servers");
        }
        else
        {   // Channel search request
            if (! tcp)
            {
                logger.log(Level.WARNING, "PVA Client " + from + " sent search #" + search.seq + " for protocol '" + protocol + "', need 'tcp'");
                return null;
            }
            search.channels = new ArrayList<>(count);
            for (int i=0; i<count; ++i)
            {
                final int cid = buffer.getInt();
                final String name = PVAString.decodeString(buffer);
                logger.log(Level.FINER, () -> "PVA Client " + from + " sent search #" + search.seq + " for " + name + " [cid " + cid + "]");
                search.channels.add(new Channel(cid, name));
            }
        }

        return search;
    }

    /** @param unicast Unicast?
     *  @param seq Sequence number
     *  @param channels Channels to search, <code>null</code> for 'list'
     *  @param address client's address
     *  @param buffer Buffer into which to encode
     */
    public static void encode(final boolean unicast, final int seq, final Collection<Channel> channels, final InetSocketAddress address, final ByteBuffer buffer)
    {
        // Create with zero payload size, to be patched later
        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_SEARCH, 0);

        final int payload_start = buffer.position();

        // SEARCH message sequence
        buffer.putInt(seq);

        // If a host has multiple listeners on the UDP search port,
        // only the one started last will see the unicast.
        // Mark search message as unicast so that receiver will forward
        // it via local broadcast to other local listeners.
        // 0-bit for replyRequired, 7-th bit for "sent as unicast" (1)/"sent as broadcast/multicast" (0)
        buffer.put((byte) ((unicast ? 0x80 : 0x00) | (channels == null ? 0x01 : 0x00)));

        // reserved
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        // responseAddress, IPv6 address in case of IP based transport, UDP
        PVAAddress.encode(address.getAddress(), buffer);

        // responsePort
        buffer.putShort((short)address.getPort());

        // string[] protocols with count as byte since < 254
        // struct { int searchInstanceID, string channelName } channels[] with count as short
        // No protocol and empty channels[] for 'list' aka 'discover' request
        if (channels == null)
        {
            buffer.put((byte)0);
            buffer.putShort((short)0);
        }
        else
        {
            buffer.put((byte)1);
            PVAString.encodeString("tcp", buffer);

            buffer.putShort((short)channels.size());
            for (Channel channel : channels)
            {
                buffer.putInt(channel.cid);
                PVAString.encodeString(channel.name, buffer);
            }
        }

        // Update payload size
        buffer.putInt(PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, buffer.position() - payload_start);
    }
}
