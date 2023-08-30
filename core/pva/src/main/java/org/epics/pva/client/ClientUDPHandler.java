/*******************************************************************************
 * Copyright (c) 2019-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.common.AddressInfo;
import org.epics.pva.common.Network;
import org.epics.pva.common.PVAHeader;
import org.epics.pva.common.SearchRequest;
import org.epics.pva.common.SearchResponse;
import org.epics.pva.common.UDPHandler;
import org.epics.pva.data.Hexdump;
import org.epics.pva.data.PVAAddress;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVATypeRegistry;
import org.epics.pva.server.Guid;

/** Sends and receives search replies, monitors beacons
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ClientUDPHandler extends UDPHandler
{
    @FunctionalInterface
    public interface BeaconHandler
    {
        /** @param server Server that sent a beacon
         *  @param guid  Globally unique ID of the server
         *  @param changes Change count, increments & rolls over as server has different channels
         */
        void handleBeacon(InetSocketAddress server, Guid guid, int changes);
    }

    /** Invoked when receiving a search reply
     *
     *  <p>Indicates both a plain server 'list' reply
     *  or a reply for a specific channel
     */
    @FunctionalInterface
    public interface SearchResponseHandler
    {
        /** @param channel_id Channel for which server replied, -1 if this is a server 'list' reply
         *  @param server Server that replied to a search request
         *  @param version Server version
         *  @param guid  Globally unique ID of the server
         *  @param tcp Does server require TLS?
         */
        void handleSearchResponse(int channel_id, InetSocketAddress server, int version, Guid guid, boolean tls);
    }

    private final BeaconHandler beacon_handler;
    private final SearchResponseHandler search_response;

    // When multiple UDP sockets bind to the same port,
    // IPv4 broadcast traffic reaches all of them.
    // Direct traffic is only received by the socket bound last.
    //
    // When sending search requests, we need separate IPv4 and IPv6 sockets.
    // They're each bound to a free, unique port.
    // Since the replies arrive via the port that sent the request,
    // we use one receiver thread per 'search' socket.
    private final DatagramChannel udp_search4, udp_search6;
    private final InetSocketAddress udp_localaddr4, udp_localaddr6;

    private final AddressInfo local_multicast;
    private final ByteBuffer forward_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);

    // Listen for UDP beacons on a separate socket, bound to the EPICS_PVA_BROADCAST_PORT,
    // with the understanding that it will only receive broadcasts;
    // since they are often blocked by firewall, may receive nothing, ever.
    private final DatagramChannel udp_beacon;
    private final ByteBuffer beacon_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);

    private volatile Thread search_thread4, search_thread6, beacon_thread;

    public ClientUDPHandler(final BeaconHandler beacon_handler,
                            final SearchResponseHandler search_response) throws Exception
    {
        this.beacon_handler = beacon_handler;
        this.search_response = search_response;

        // IPv4 socket, also used to send broadcasts and for the local re-sending
        udp_search4 = Network.createUDP(StandardProtocolFamily.INET, null, 0);
        udp_search4.socket().setBroadcast(true);
        local_multicast = Network.configureLocalIPv4Multicast(udp_search4, PVASettings.EPICS_PVA_BROADCAST_PORT);

        // IPv6 socket
        udp_search6 = Network.createUDP(StandardProtocolFamily.INET6, null, 0);

        // Beacon socket only receives, does not send broadcasts
        udp_beacon = Network.createUDP(StandardProtocolFamily.INET6, null, PVASettings.EPICS_PVA_BROADCAST_PORT);

        udp_localaddr4 = (InetSocketAddress) udp_search4.getLocalAddress();
        udp_localaddr6 = (InetSocketAddress) udp_search6.getLocalAddress();

        logger.log(Level.FINE, "Awaiting search replies on UDP " + udp_localaddr4 +
                               " and " + udp_localaddr6 +
                               ", and beacons on " + Network.getLocalAddress(udp_beacon));
    }

    /** @param target Address to which message will be sent
     *  @return Suitable local return address where server should sent reply
     */
    InetSocketAddress getResponseAddress(final AddressInfo target)
    {
        return target.isIPv4() ? udp_localaddr4 : udp_localaddr6;
    }

    public void send(final ByteBuffer buffer, final AddressInfo info) throws Exception
    {
        // synchronized (udp_search)?
        // Not necessary based on Javadoc for send(),
        // but in case we set the multicast IF & TTL
        if (info.getAddress().getAddress() instanceof Inet4Address)
        {
            synchronized (udp_search4)
            {
                if (info.getAddress().getAddress().isMulticastAddress())
                {
                    udp_search4.setOption(StandardSocketOptions.IP_MULTICAST_IF, info.getInterface());
                    udp_search4.setOption(StandardSocketOptions.IP_MULTICAST_TTL, info.getTTL());
                }
                udp_search4.send(buffer, info.getAddress());
            }
        }
        else
        {
            synchronized (udp_search6)
            {
                if (info.getAddress().getAddress().isMulticastAddress())
                {
                    udp_search6.setOption(StandardSocketOptions.IP_MULTICAST_IF, info.getInterface());
                    udp_search6.setOption(StandardSocketOptions.IP_MULTICAST_TTL, info.getTTL());
                }
                udp_search6.send(buffer, info.getAddress());
            }
        }
    }

    public void start()
    {
        // Same code for messages from the 'search' and 'beacon' socket,
        // though each socket is likely to see only one type of message.
        final ByteBuffer receive_buffer4 = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);
        search_thread4 = new Thread(() -> listen(udp_search4, receive_buffer4), "UDP4-receiver " + Network.getLocalAddress(udp_search4));
        search_thread4.setDaemon(true);
        search_thread4.start();

        final ByteBuffer receive_buffer6 = ByteBuffer.allocate(PVASettings.MAX_UDP_PACKET);
        search_thread6 = new Thread(() -> listen(udp_search6, receive_buffer6), "UDP6-receiver " + Network.getLocalAddress(udp_search6));
        search_thread6.setDaemon(true);
        search_thread6.start();

        beacon_thread = new Thread(() -> listen(udp_beacon, beacon_buffer), "UDP-beacon-receiver " + Network.getLocalAddress(udp_beacon));
        beacon_thread.setDaemon(true);
        beacon_thread.start();
    }

    @Override
    protected boolean handleMessage(final InetSocketAddress from, final byte version,
                                    final byte command, final int payload, final ByteBuffer buffer)
    {
        switch (command)
        {
        case PVAHeader.CMD_BEACON:
            return handleBeacon(from, version, payload, buffer);
        case PVAHeader.CMD_SEARCH:
            return handleSearchRequest(from, version, payload, buffer);
        case PVAHeader.CMD_SEARCH_RESPONSE:
            return handleSearchReply(from, version, payload, buffer);
        default:
            logger.log(Level.WARNING, "PVA Server " + from + " sent UDP packet with unknown command 0x" + Integer.toHexString(command));
        }
        return false;
    }

    /** Decode beacon info: Changes? New GUID? */
    private boolean handleBeacon(final InetSocketAddress from, final byte version,
                                 final int payload, final ByteBuffer buffer)
    {
        if (payload < 12 + 1 + 1 + 2 + 16 + 2 + 4 + 1)
        {
            logger.log(Level.WARNING, "PVA Server " + from + " sent only " + payload + " bytes for beacon");
            return false;
        }

        // Server GUID
        final Guid guid = new Guid(buffer);

        // Flags
        buffer.get();

        final int sequence = Byte.toUnsignedInt(buffer.get());
        final short changes = buffer.getShort();

        // Server's address and port
        final InetAddress addr;
        try
        {
            addr = PVAAddress.decode(buffer);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA Server " + from + " sent beacon with invalid address");
            return false;
        }
        final int port = Short.toUnsignedInt(buffer.getShort());

        // Use address from reply unless it's a generic local address
        final InetSocketAddress server;
        if (addr.isAnyLocalAddress())
            server = new InetSocketAddress(from.getAddress(), port);
        else
            server = new InetSocketAddress(addr, port);

        final String protocol = PVAString.decodeString(buffer);
        if (! "tcp".equals(protocol))
        {
            logger.log(Level.WARNING, "PVA Server " + from + " sent beacon for protocol '" + protocol + "'");
            return false;
        }

        try
        {
            // Decode optional server status (likely null)
            final PVATypeRegistry types = new PVATypeRegistry();
            final PVAData server_status = types.decodeType("", buffer);
            if (server_status != null)
                server_status.decode(types, buffer);
            logger.log(Level.FINE, () ->
            {
                return "Received Beacon #" + sequence + " from " + server + " " +
                        guid +
                        ", version " + version + ", " +
                        changes + " changes" +
                        (server_status == null ? "" : ", status " + server_status);
            });
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA Server " + from + " sent beacon with broken server status", ex);
        }
        beacon_handler.handleBeacon(server, guid, changes);

        return true;
    }

    /** Check if search request needs to be forwarded
     *
     *  <p>For example, assume one or more servers on this host.
     *  In addition, this PVA client is running, and it has been started last.
     *  When remote clients send a unicast search request to this host,
     *  only this PVA client will see the search request.
     *  By forwarding it to the local multicast group,
     *  all PVA servers on this host can see the search request.
     */
    private boolean handleSearchRequest(final InetSocketAddress from, final byte version,
                                        final int payload, final ByteBuffer buffer)
    {
        final SearchRequest search = SearchRequest.decode(from, version, payload, buffer);
        try
        {
            if (local_multicast != null  &&  search != null  &&  search.unicast)
            {
                if (search.channels == null)
                {
                    if (search.reply_required)
                    {
                        forward_buffer.clear();
                        SearchRequest.encode(false, 0, null, search.client, search.tls, forward_buffer);
                        forward_buffer.flip();
                        logger.log(Level.FINER, () -> "Forward search to list servers to " + local_multicast + "\n" + Hexdump.toHexdump(forward_buffer));
                        send(forward_buffer, local_multicast);
                    }
                }
                else
                {
                    forward_buffer.clear();
                    SearchRequest.encode(false, search.seq, search.channels, search.client, search.tls, forward_buffer);
                    forward_buffer.flip();
                    logger.log(Level.FINER, () -> "Forward search to " + local_multicast + "\n" + Hexdump.toHexdump(forward_buffer));
                    send(forward_buffer, local_multicast);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot forward search request", ex);
        }
        return true;
    }

    /** Handle search reply: Server has one of our desired channel names */
    private boolean handleSearchReply(final InetSocketAddress from, final byte version,
                                      final int payload, final ByteBuffer buffer)
    {
        try
        {
            final SearchResponse response = SearchResponse.decode(payload, buffer);

            // Did server sent specific address? Otherwise use remote address
            InetSocketAddress server = response.server;
            if (server.getAddress().isAnyLocalAddress())
                server = new InetSocketAddress(from.getAddress(), server.getPort());

            // Server may reply with list of PVs that it does _not_ have...
            if (! response.found)
                search_response.handleSearchResponse(-1, server, version, response.guid, response.tls);
            else
                for (int cid : response.cid)
                    search_response.handleSearchResponse(cid, server, version, response.guid, response.tls);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "PVA Server " + from + " sent invalid search reply", ex);
            return false;
        }

        return true;
    }

    @Override
    public void close()
    {
        super.close();
        // Close sockets, wait a little for threads to exit
        try
        {
            udp_search6.close();
            udp_search4.close();
            udp_beacon.close();

            if (search_thread6 != null)
                search_thread6.join(5000);
            if (search_thread4 != null)
                search_thread4.join(5000);
            if (beacon_thread != null)
                beacon_thread.join(5000);
        }
        catch (Exception ex)
        {
            // Ignore
        }
    }
}
