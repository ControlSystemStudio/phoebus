/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import static org.epics.pva.PVASettings.logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.common.AddressInfo;
import org.epics.pva.common.Network;
import org.epics.pva.common.OriginTag;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.SearchRequest;
import org.epics.pva.common.SearchResponse;
import org.epics.pva.common.UDPHandler;
import org.epics.pva.data.Hexdump;

/** Listen to search requests, send beacons
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ServerUDPHandler extends UDPHandler
{
    private final PVAServer server;

    /** UDP channels on which we listen to name searches,
     *  reply to them,
     *  and on which we send beacons.
     *
     *  To reply/send to both IPv4 and IPv6, we need one
     *  channel per protocol family.
     */
    private volatile DatagramChannel udp4, udp6;

    /** Local multicast used to re-send IPv4 unicasts */
    private volatile AddressInfo local_multicast = null;

    private final ByteBuffer send_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);

    private volatile Thread listen_thread4 = null;
    private volatile Thread listen_thread6 = null;


    /** Start handling UDP search requests
     *  @param server PVA Server
     *  @throws Exception on error
     */
    public ServerUDPHandler(final PVAServer server) throws Exception
    {
        this.server = server;

        for (AddressInfo info : Network.parseAddresses(PVASettings.EPICS_PVAS_INTF_ADDR_LIST, PVASettings.EPICS_PVAS_BROADCAST_PORT))
        {
            // First should be non-multicast addresses that create the IPv4 and/or IPv6 channel
            if (! info.getAddress().getAddress().isMulticastAddress())
            {
                if (info.isIPv4())
                {
                    if (udp4 != null)
                        throw new Exception("EPICS_PVAS_INTF_ADDR_LIST has more than one IPv4 address");
                    udp4 = Network.createUDP(StandardProtocolFamily.INET, info.getAddress().getAddress(), PVASettings.EPICS_PVAS_BROADCAST_PORT);
                    logger.log(Level.FINE, "Awaiting searches and sending beacons on UDP " + info);
                }

                if (info.isIPv6())
                {
                    if (!PVASettings.EPICS_PVA_ENABLE_IPV6)
                        throw new Exception("Must have IPv6 enabled for IPv6 address!");
                    if (udp6 != null)
                        throw new Exception("EPICS_PVAS_INTF_ADDR_LIST has more than one IPv6 address");
                    udp6 = Network.createUDP(StandardProtocolFamily.INET6, info.getAddress().getAddress(), PVASettings.EPICS_PVAS_BROADCAST_PORT);
                    logger.log(Level.FINE, "Awaiting searches and sending beacons on UDP " + info);
                }
            }
            else
            {
                // Have socket channel (which must already exist) join multicast group
                if (info.getInterface() == null)
                    throw new Exception("EPICS_PVAS_INTF_ADDR_LIST contains multicast group without interface");
                if (info.isIPv4())
                {
                    if (udp4 == null)
                        throw new Exception("EPICS_PVAS_INTF_ADDR_LIST lacks IPv4 address, cannot add multicast");
                    // Configure interface to send multicasts out via this interface
                    udp4.setOption(StandardSocketOptions.IP_MULTICAST_IF, info.getInterface());
                    // Configure socket channel to receive from the multicast group
                    udp4.join(info.getAddress().getAddress(), info.getInterface());
                    logger.log(Level.FINE, "Listening to UDP multicast " + info);
                    local_multicast = info;
                }
                if (info.isIPv6())
                {
                    if (udp6 == null)
                        throw new Exception("EPICS_PVAS_INTF_ADDR_LIST lacks IPv6 address, cannot add multicast");
                    udp6.join(info.getAddress().getAddress(), info.getInterface());
                    logger.log(Level.FINE, "Listening to UDP multicast " + info);
                }
            }
        }

        if (local_multicast != null)
            logger.log(Level.FINE, "IPv4 unicasts are re-transmitted via local multicast " + local_multicast);

        if (udp4 != null)
        {
            final ByteBuffer receive_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);
            listen_thread4 = new Thread(() -> listen(udp4, receive_buffer),
                                        "UDP4-receiver " + Network.getLocalAddress(udp4));
            listen_thread4.setDaemon(true);
            listen_thread4.start();
        }
        if (udp6 != null)
        {
            final ByteBuffer receive_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);
            listen_thread6 = new Thread(() -> listen(udp6, receive_buffer),
                                        "UDP6-receiver " + Network.getLocalAddress(udp6));
            listen_thread6.setDaemon(true);
            listen_thread6.start();
        }
    }

    @Override
    protected boolean handleMessage(final InetSocketAddress from, final byte version,
                                    final byte command, final int payload, final ByteBuffer buffer)
    {
        switch (command)
        {
        case PVAHeader.CMD_ORIGIN_TAG:
            // Will be decoded with CMD_SEARCH
            break;
        case PVAHeader.CMD_SEARCH:
            return handleSearch(from, version, payload, buffer);
        case PVAHeader.CMD_BEACON:
            // Clients may send or forward beacons, server ignores them
            break;
        default:
            logger.log(Level.WARNING, "PVA Client " + from + " sent UDP packet with unknown command 0x" + Integer.toHexString(command));
        }
        return true;
    }

    /** @param from Sender of search request
     *  @param version Client's version
     *  @param payload Size of payload
     *  @param buffer Buffer with search request
     *  @return Valid request?
     */
    private boolean handleSearch(final InetSocketAddress from, final byte version,
                                 final int payload, final ByteBuffer buffer)
    {
        // Check for optional origin tag.
        final OriginTag origin = OriginTag.testForOriginOfSearch(from, buffer);
        final SearchRequest search = SearchRequest.decode(origin, from, version, payload, buffer);
        if (search == null)
            return false;

        if (search.channels == null)
        {
            if (search.reply_required)
            {   // pvlist request
                final boolean handled = server.handleSearchRequest(0, -1, null, search.client, search.tls, null);
                if (! handled  &&  search.unicast)
                    PVAServer.POOL.submit(() -> forwardSearchRequest(0, null, search.client, search.reply_to_src_port, search.tls));
            }
        }
        else
        {   // Channel search request
            List<SearchRequest.Channel> forward = null;
            for (SearchRequest.Channel channel : search.channels)
            {
                final boolean handled = server.handleSearchRequest(search.seq, channel.getCID(), channel.getName(), search.client, search.tls, null);
                if (! handled && search.unicast)
                {
                    if (forward == null)
                        forward = new ArrayList<>();
                    forward.add(channel);
                }
            }

            if (forward != null)
            {
                final List<SearchRequest.Channel> to_forward = forward;
                PVAServer.POOL.submit(() -> forwardSearchRequest(search.seq, to_forward, search.client, search.reply_to_src_port, search.tls));
            }
        }

        return true;
    }

    /** Forward a search request that we received as unicast to the local multicast group
     *
     *  <p>For example, when two servers run on the same host,
     *  only the one started last will see UDP search requests unicast from a remote client.
     *
     *  <p>This method forwards them to the local multicast group,
     *  allowing all servers on this host to reply.
     *
     *  @param seq Search sequence or 0
     *  @param channels Channel CIDs and names or <code>null</code> for 'list'
     *  @param address Client's address and port
     *  @param reply_to_src_port Set flag to use the 'peer' port, ignoring the reply port?
     *  @param tls Use TLS or plain TCP?
     */
    private void forwardSearchRequest(final int seq, final Collection<SearchRequest.Channel> channels, final InetSocketAddress address, final boolean reply_to_src_port, final boolean tls)
    {
        // TODO Remove the local IPv4 multicast re-send from the protocol, just use multicast from the start as with IPv6
        if (local_multicast == null)
            return;
        synchronized (send_buffer)
        {
            send_buffer.clear();
            InetAddress origin = OriginTag.encode(udp4, send_buffer);
            SearchRequest.encode(false, reply_to_src_port, seq, channels, address, tls, send_buffer);
            send_buffer.flip();
            logger.log(Level.FINER, () -> "Forward search from " + origin + " to " + local_multicast + "\n" + Hexdump.toHexdump(send_buffer));
            try
            {
                udp4.send(send_buffer, local_multicast.getAddress());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot forward search", ex);
            }
        }
    }

    /** Send a "channel found" reply to a client's search
     *  @param guid This server's GUID
     *  @param seq Client search request sequence number
     *  @param cid Client's channel ID or -1
     *  @param server_address TCP address where client can connect to server
     *  @param tls Should client use tls?
     *  @param client Address of client's UDP port
     */
    public void sendSearchReply(final Guid guid, final int seq, final int cid, final InetSocketAddress server_address, final boolean tls, final InetSocketAddress client)
    {
        synchronized (send_buffer)
        {
            send_buffer.clear();
            SearchResponse.encode(guid, seq, cid, server_address.getAddress(), server_address.getPort(), tls, send_buffer);
            send_buffer.flip();
            logger.log(Level.FINER, () -> "Sending UDP search reply to " + client + "\n" + Hexdump.toHexdump(send_buffer));

            try
            {
                if (client.getAddress() instanceof Inet4Address)
                    udp4.send(send_buffer, client);
                else
                    udp6.send(send_buffer, client);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot send search reply", ex);
            }
        }
    }

    @Override
    public void close()
    {
        super.close();
        // Close sockets, wait a little for threads to exit
        try
        {
            if (udp4 != null)
                udp4.close();
            if (udp6 != null)
                udp6.close();

            if (listen_thread4 != null)
                listen_thread4.join(5000);
            if (listen_thread6 != null)
                listen_thread6.join(5000);
        }
        catch (Exception ex)
        {
            // Ignore
        }
    }
}