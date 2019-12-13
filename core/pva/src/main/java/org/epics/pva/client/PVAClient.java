/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.common.Network;
import org.epics.pva.server.Guid;

/** PV Access Client
 *
 *  <p>The {@link PVAClient} is the central client API.
 *  Basic usage:
 *  <pre>
 *  PVAClient client = new PVAClient();
 *  PVAChannel channel = client.getChannel("SomePVName");
 *  channel.connect().get(5, TimeUnit.SECONDS);
 *  PVAStructure value = channel.read("").get(5, TimeUnit.SECONDS);
 *  System.out.println(channel.getName() + " = " + value);
 *  channel.close();
 *  client.close();
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAClient implements AutoCloseable
{
    /** Default channel listener logs state changes */
    private static final ClientChannelListener DEFAULT_CHANNEL_LISTENER = (ch, state) ->  logger.log(Level.INFO, ch.toString());

    private final ClientUDPHandler udp;

    final ChannelSearch search;

    private final ConcurrentHashMap<Guid, ServerInfo> list_replies = new ConcurrentHashMap<>();

    /** Channels by client ID */
    private final ConcurrentHashMap<Integer, PVAChannel> channels_by_id = new ConcurrentHashMap<>();

    /** TCP handlers by server address */
    private final ConcurrentHashMap<InetSocketAddress, ClientTCPHandler> tcp_handlers = new ConcurrentHashMap<>();

    private final AtomicInteger request_ids = new AtomicInteger();

    /** Create a new PVAClient
     *
     *  <p>The {@link PVAClient} maintain PVs and coordinates the necessary search requests.
     *
     *  <p>It does not pool PVs by name. A caller requesting
     *  channels for the same name more than once will receive
     *  separate channels, with different internal channel IDs,
     *  which will result in separate channels on the PVA server.
     *
     *  <p>The PVAClient API thus provides full control over the number of channels.
     *  A higher-level PV layer is suggested to perform channel pooling.
     *
     * @throws Exception on error
     */
    public PVAClient() throws Exception
    {
        List<InetSocketAddress> search_addresses = Network.parseAddresses(PVASettings.EPICS_PVA_ADDR_LIST.split("\\s+"));
        if (PVASettings.EPICS_PVA_AUTO_ADDR_LIST)
            search_addresses.addAll(Network.getBroadcastAddresses(PVASettings.EPICS_PVA_BROADCAST_PORT));

        udp = new ClientUDPHandler(this::handleBeacon, this::handleSearchResponse);
        search = new ChannelSearch(udp, search_addresses);

        udp.start();
        search.start();
    }

    /** List PVA servers
     *  @param unit How long...
     *  @param duration ... to await replies
     *  @return List of servers that have replied
     *  @throws Exception on error
     */
    public Collection<ServerInfo> list(final TimeUnit unit, final long duration) throws Exception
    {
        list_replies.clear();
        search.list();
        unit.sleep(duration);
        return list_replies.values();
    }

    private void handleListResponse(final InetSocketAddress server, final int version, final Guid guid)
    {
        logger.log(Level.FINE, () -> guid + " version " + version + ": tcp@" + server);
        final ServerInfo info = list_replies.computeIfAbsent(guid, g -> new ServerInfo(g));
        info.version = version;
        info.addresses.add(server);
    }

    /** @return New request ID unique to this client and all its connections */
    int allocateRequestID()
    {
        return request_ids.incrementAndGet();
    }

    /** Create channel by name
    *
    *  <p>Starts search.
    *
    *  @param channel_name PVA channel name
    *  @return {@link PVAChannel}
    */
    public PVAChannel getChannel(final String channel_name)
    {
        return getChannel(channel_name, DEFAULT_CHANNEL_LISTENER);
    }

    /** Create channel by name
     *
     *  <p>Starts search.
     *
     *  @param channel_name PVA channel name
     *  @param listener {@link ClientChannelListener} that will be invoked with connection state updates
     *  @return {@link PVAChannel}
     */
    public PVAChannel getChannel(final String channel_name, final ClientChannelListener listener)
    {
        final PVAChannel channel = new PVAChannel(this, channel_name, listener);
        channels_by_id.putIfAbsent(channel.getCID(), channel);
        search.register(channel, true);
        return channel;
    }

    /** Get channel by client ID
     *  @param cid Channel ID, using client's ID
     *  @return {@link PVAChannel}, may be <code>null</code>
     */
    PVAChannel getChannel(final int cid)
    {
        return channels_by_id.get(cid);
    }

    /** Forget about a channel
     *
     *  <p>Called when server confirmed that channel has been destroyed.
     *  Removes channel from ID map, so it will no longer be
     *  recognized.
     *
     *  @param channel Channel to forget
     */
    void forgetChannel(final PVAChannel channel)
    {
        channels_by_id.remove(channel.getCID());

        // Did channel have a connection?
        final ClientTCPHandler tcp = channel.tcp.get();
        if (tcp == null)
            return;

        tcp.removeChannel(channel);
    }

    private void handleBeacon(final InetSocketAddress server, final Guid guid, final int changes)
    {
        final ClientTCPHandler tcp = tcp_handlers.get(server);
        if (tcp == null)
            logger.log(Level.FINER, () -> "Beacon from new server " + server);
        else
        {
            if (tcp.checkBeaconChanges(changes))
                logger.log(Level.FINER, () -> "Beacon from " + server + " indicates changes");
            else if (! tcp.getGuid().equals(guid))
                logger.log(Level.FINER, () -> "Beacon from " + server +
                                              " has new GUID " + guid +
                                              " (was " + tcp.getGuid() + ")");
            else
                return;
        }
        // Beacon indicates changes or new GUID, so re-search missing channels
        search.boost();
    }

    private void handleSearchResponse(final int channel_id, final InetSocketAddress server, final int version, final Guid guid)
    {
        // Generic server 'list' response?
        if (channel_id < 0)
        {
            handleListResponse(server, version, guid);
            return;
        }

        // Reply for specific channel
        final PVAChannel channel = search.unregister(channel_id);
        // Late reply for search that was already satisfied?
        if (channel == null)
        {
            // Since searches are sent out multiple times until there's a response,
            // and searches are forwarded via multicast,
            // so it's common to receive multiple replies for the same channel
            // from the same server.
            // Check GUID for unexpected reply from another(!) server.
            final PVAChannel check = channels_by_id.get(channel_id);
            if (check == null)
                logger.log(Level.WARNING, "Received search reply for unknown channel ID " + channel_id + " from " + server + " " + guid);
            else
            {
                final ClientTCPHandler tcp = check.tcp.get();
                // Warn about duplicate PVs on network
                if (tcp != null  &&  !tcp.getGuid().equals(guid))
                    logger.log(Level.WARNING, "More than one channel with name '" + check.getName() + "' detected, connected to " + tcp.getRemoteAddress() + " " + tcp.getGuid() + ", ignored " + server + " " + guid);
            }
            return;
        }
        channel.setState(ClientChannelState.FOUND);
        logger.log(Level.FINE, () -> "Reply for " + channel + " from " + server + " " + guid);

        final ClientTCPHandler tcp = tcp_handlers.computeIfAbsent(server, addr ->
        {
            try
            {
                return new ClientTCPHandler(this, addr, guid);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot connect to TCP " + addr, ex);
            }
            return null;
        });
        // In case of connection errors (TCP connection blocked by firewall),
        // tcp will be null
        if (tcp != null)
            channel.registerWithServer(tcp);
    }

    /** Called by {@link ClientTCPHandler} when connection is lost or closed because unused
     *
     *  <p>Client should detach all channels that are still on this connection
     *  and re-search them, forget the connection and close it.
     *
     *  @param tcp TCP handler that needs to be closed
     */
    void shutdownConnection(final ClientTCPHandler tcp)
    {
        // Forget this connection
        final ClientTCPHandler removed = tcp_handlers.remove(tcp.getRemoteAddress());
        if (removed != tcp)
            logger.log(Level.WARNING, "Closed unknown " + tcp, new Exception("Call stack"));

        // Reset all channels that used the connection
        for (PVAChannel channel : tcp.getChannels())
        {
            try
            {
                // Reset channel, detach from TCP.
                // If the channel was active, search again soon
                if (channel.resetConnection())
                    search.register(channel, false);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error resetting channel " + channel, ex);
            }
        }

        tcp.close(false);
    }

    /** Allow in-package test code to check for TCP connections
     *  @return <code>true</code> if there are still TCP connections
     */
    boolean haveTCPConnections()
    {
        return ! tcp_handlers.isEmpty();
    }

    /** Close all channels and network connections
     *
     *  <p>Waits a little for all channels to be closed.
     */
    @Override
    public void close()
    {
        // Stop searching for missing channels
        search.close();

        // Assume caller has closed channels, wait 2 seconds for that to be confirmed
        int wait = 20;
        while (! channels_by_id.isEmpty())
        {
            if (--wait > 0)
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            else
            {
                // Channels_by_id might still contain channels for which
                // the server is not able to confirm channel deletion.
                logger.log(Level.WARNING, "PVA Client closed with remaining channels: " + channels_by_id.values());
                break;
            }
        }

        // Stop TCP and UDP threads
        for (ClientTCPHandler handler : tcp_handlers.values())
            handler.close(true);

        udp.close();
    }
}
