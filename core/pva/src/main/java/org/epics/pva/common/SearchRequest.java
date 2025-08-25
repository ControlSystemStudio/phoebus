/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
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

    /** Server should reply with its GUID and empty CID list
     *  even if it does not host any of the searched channels
     */
    public static final byte FLAG_SEARCH_MUST_REPLY = 0x01;

    /** Client should ignore the 'port' in the reply and
     *  simply use the port of the 'source', that is the peer port
     *  of the UDP message or TCP connection.
     *
     *  <p>In a forwarded message, the 'client' (reply-to)
     *  address has been updated to reflect the original client.
     *  The 'reply_to_src_port' flag is copied, but server
     *  must NOT reply to the source port because that would
     *  be the port of the forwarder, not the real client.
     *  The presence of an {@link OriginTag} will indicate
     *  that the 'reply_to_src_port' flag is copied and needs
     *  to be ignored.
     *
     *  @since Version 3
     */
    public static final byte FLAG_REPLY_SRC_PORT = 0x02;

    /** Indicates that search message was unicast */
    public static final byte FLAG_SEARCH_UNICAST    = (byte)0x80;

    /** Sequence number */
    public int seq;
    /** Is it a unicast? */
    public boolean unicast;
    /** Is reply required? */
    public boolean reply_required;
    /** Reply to source port instead of port listed in the search request? */
    public boolean reply_to_src_port;
    /** Address of client */
    public InetSocketAddress client;
    /** Use TLS, or plain TCP? */
    public boolean tls;
    /** Names requested in search, <code>null</code> for 'list' */
    public List<Channel> channels;

    /** Check search request
     *  @param origin Optional CMD_ORIGIN_TAG that preceded the search message
     *  @param from Peer address
     *  @param version Message version
     *  @param payload Payload size
     *  @param buffer Buffer positioned on payload
     *  @return Decoded search request or <code>null</code> if not a valid search request
     */
    public static SearchRequest decode(final OriginTag origin, final InetSocketAddress from, final byte version,
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

        final byte flags = buffer.get();
        search.unicast           = (flags & FLAG_SEARCH_UNICAST)    == FLAG_SEARCH_UNICAST;
        search.reply_required    = (flags & FLAG_SEARCH_MUST_REPLY) == FLAG_SEARCH_MUST_REPLY;
        search.reply_to_src_port = (flags & FLAG_REPLY_SRC_PORT)    == FLAG_REPLY_SRC_PORT;

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
        int port = Short.toUnsignedInt(buffer.getShort());
        final InetSocketAddress orig_response_addr = new InetSocketAddress(addr, port);
        // Since version 3, flag can ask us to ignore the reply port in the message
        // and instead use the peer's port.
        // This should help with NAT where we get the message from an intermediate
        // and need to reply via that same intermediate
        if (version >= 3  &&  search.reply_to_src_port  &&  origin == null)
            port = from.getPort();

        // Use address from message unless it's a generic local address
        if (addr.isAnyLocalAddress() || port <= 0)
            search.client = new InetSocketAddress(from.getAddress(), port);
        else
            search.client = new InetSocketAddress(addr, port);

        // Assert that client supports "tcp", ignore rest
        boolean tcp = search.tls = false;
        int count = Byte.toUnsignedInt(buffer.get());
        String unknown_protocol = "<none>";
        for (int i=0; i<count; ++i)
        {
            final String protocol = PVAString.decodeString(buffer);
            if ("tls".equals(protocol))
                search.tls = true;
            else if ("tcp".equals(protocol))
                tcp = true;
            else
                unknown_protocol = protocol;
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
            if (! (tcp || search.tls))
            {
                logger.log(Level.WARNING, "PVA Client " + from + " sent search #" + search.seq + " for protocol '" + unknown_protocol + "', need 'tcp' or 'tls'");
                return null;
            }
            search.channels = new ArrayList<>(count);
            for (int i=0; i<count; ++i)
            {
                final int cid = buffer.getInt();
                final String name = PVAString.decodeString(buffer);
                logger.log(Level.FINER, () -> "PVA Client " + from + " sent search #" + search.seq + " for " + name + " [cid " + cid + "]"
                                            + ", reply addr " + orig_response_addr
                                            + (orig_response_addr.equals(search.client) ? "" : ", using " + search.client)
                                            + (search.tls               ? " (TLS)" : "")
                                            + (search.unicast           ? " (unicast)" : "")
                                            + (search.reply_required    ? " (reply required)" : "")
                                            + (search.reply_to_src_port ? (origin == null ?  " (reply to source port)"  : " (reply to source port ignored because of origin tag)") : ""));
                search.channels.add(new Channel(cid, name));
            }
        }

        return search;
    }

    /** @param unicast Unicast?
     *  @param use_src_port Reply to 'peer port' of message (which will be our port) instead of port in 'address'?
     *  @param seq Sequence number
     *  @param channels Channels to search, <code>null</code> for 'list'
     *  @param address client's address
     *  @param tls Use TLS?
     *  @param buffer Buffer into which to encode
     */
    public static void encode(final boolean unicast, final boolean use_src_port,
                              final int seq, final Collection<Channel> channels,
                              final InetSocketAddress address, final boolean tls,
                              final ByteBuffer buffer)
    {
        final int start = buffer.position();
        // Create with zero payload size, to be patched later
        PVAHeader.encodeMessageHeader(buffer, PVAHeader.FLAG_NONE, PVAHeader.CMD_SEARCH, 0);

        final int payload_start = start + PVAHeader.HEADER_SIZE;

        // SEARCH message sequence
        // PVXS sends "find".getBytes() instead
        // For TCP search via EPICS_PVA_NAME_SERVERS, we send "look" ("kool" for little endian)
        buffer.putInt(seq);

        // If a host has multiple listeners on the UDP search port,
        // only the one started last will see the unicast.
        // Identify unicast search message so that receiver will forward
        // it via local broadcast to other local listeners.
        // If there are no channels, force a "list all servers" reply.
        // Typically use src port unless this is a forwarded message
        // where the original source port is in the 'address'.
        buffer.put((byte) ((unicast ? FLAG_SEARCH_UNICAST : 0x00) |
                           ((channels == null || channels.isEmpty()) ? FLAG_SEARCH_MUST_REPLY : 0x00) |
                           (use_src_port ? FLAG_REPLY_SRC_PORT : 0x00)));

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
            // Support both tls and tcp, or only tcp?
            if (tls)
            {
                buffer.put((byte)2);
                PVAString.encodeString("tls", buffer);
            }
            else
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
        buffer.putInt(start + PVAHeader.HEADER_OFFSET_PAYLOAD_SIZE, buffer.position() - payload_start);
    }
}
