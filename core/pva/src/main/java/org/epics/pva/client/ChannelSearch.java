/*******************************************************************************
 * Copyright (c) 2019-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;

import org.epics.pva.PVASettings;
import org.epics.pva.common.AddressInfo;
import org.epics.pva.common.RequestEncoder;
import org.epics.pva.common.SearchRequest;
import org.epics.pva.data.Hexdump;

/** Handler for search requests
 *
 *  <p>Maintains thread that periodically issues search requests
 *  for registered channels.
 *
 *  <p>Details of search timing are based on
 *  https://github.com/epics-base/epicsCoreJava/blob/master/pvAccessJava/src/org/epics/pvaccess/client/impl/remote/search/SimpleChannelSearchManagerImpl.java
 *
 *  <p>Can send search requests to unicast (IPv4 and IPv6), multicast (4 & 6), broadcast (IPv4 only).
 *  Since only StandardProtocolFamily.INET sockets support IPv4 multicast,
 *  but then won't handle IPv6 traffic, need to maintain one socket per protocol family.
 *
 *  <p>Can also search via TCP. While TCP is fundamentally reliable, we don't expect lost
 *  search requests as with UDP, search requests are still repeated because the PVA gateway
 *  is stateless with respect to searches.
 *  The gateway does not remember past searches.
 *  When the gateway receives the first search request for an unknown channel,
 *  the gateway itself will start to search for that channel on its client side.
 *  Once it connects to the channel, the gateway then depends on another search request for the now
 *  known channel to return a positive reply.
 *  TCP searches thus need to be repeated, but compared to the UDP searches they are sent less frequently.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ChannelSearch
{
    /** Channel that's being searched */
    private class SearchedChannel
    {
        final AtomicInteger search_counter;
        final AtomicInteger tcp_search_counter = new AtomicInteger(0);
        final PVAChannel channel;

        SearchedChannel(final PVAChannel channel)
        {
            // Counter of 0 means the next regular search will increment
            // to 1 (no search), then 2 (power of two -> search).
            // So it'll "soon" perform a regular search.
            this.search_counter = new AtomicInteger(0);
            this.channel = channel;
            // Not starting an _immediate_ search in here because
            // this needs to be added to searched_channels first.
            // Otherwise run risk of getting reply without being able
            // to handle it
        }
    }

    /** Search request sequence number */
    private static final AtomicInteger search_sequence = new AtomicInteger();

    private final ClientUDPHandler udp;

    private final Function<InetSocketAddress, ClientTCPHandler> tcp_provider;

    /** Basic search period */
    private static final int SEARCH_PERIOD_MS = 225;

    /** Search period jitter to avoid multiple clients all searching at the same period */
    private static final int SEARCH_JITTER_MS = 25;

    /** Exponential search intervals
     *
     *  <p>Search counter for a channel is incremented each SEARCH_PERIOD_MS.
     *  When counter is a power of 2, search request is sent.
     *  Counter starts at 1, and first search period increments to 2:
     *     0 ms increments to 2 -> Search!
     *   225 ms increments to 3 -> No search
     *   450 ms increments to 4 -> Search (~0.5 sec after last)
     *   675 ms increments to 5 -> No search
     *   900 ms increments to 6 -> No search
     *  1125 ms increments to 7 -> No search
     *  1350 ms increments to 8 -> Search (~ 1 sec after last)
     *  ...
     *
     *  <p>So the time between searches is roughly 0.5 seconds,
     *  1 second, 2, 4, 8, 15, 30 seconds.
     *
     *  <p>Once the search count reaches 256, it's reset to 129.
     *  This means it then takes 128 periods to again reach 256
     *  for the next search, so searches end up being issued
     *  roughly every 128*0.225 = 30 seconds.
     */
    private static final int BOOST_SEARCH_COUNT = 1,
                             MAX_SEARCH_COUNT = 256,
                             MAX_SEARCH_RESET = 129;

    /** Map of searched channels by channel ID */
    private ConcurrentHashMap<Integer, SearchedChannel> searched_channels = new ConcurrentHashMap<>();

    /** Timer used to periodically check channels and issue search requests */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(run ->
    {
        final Thread thread = new Thread(run, "ChannelSearch");
        thread.setDaemon(true);
        return thread;
    });

    /** Buffer for assembling search messages */
    private final ByteBuffer send_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_UNFRAGMENTED_SEND);

    /** Address list to which search requests are sent */
    private final List<AddressInfo> unicast_search_addresses = new ArrayList<>(),
                                    b_or_mcast_search_addresses = new ArrayList<>(),
                                    name_server_addresses = new ArrayList<>();

    public ChannelSearch(final ClientUDPHandler udp,
                         final List<AddressInfo> udp_addresses,
                         final Function<InetSocketAddress, ClientTCPHandler> tcp_provider,
                         final List<AddressInfo> name_server_addresses) throws Exception
    {
        this.udp = udp;
        this.tcp_provider = tcp_provider;

        // Searches sent to multicast (IPv4, IPv6) or broadcast addresses (IPv4) reach every PVA server
        // on that multicast group or bcast subnet.
        // Searches sent to unicast addresses reach only the PVA server started _last_ on each host.
        // In the original PVA implementation, the receiver would then re-send them via a local multicast group.
        // The original unicast messages were thus marked with a UNICAST flag,
        // while multicast and broadcast messages are not.
        for (AddressInfo addr : udp_addresses)
        {
            // Trouble is, how do you recognize a unicast address?
            if (addr.getAddress().getAddress().isMulticastAddress())
            {   // Multicast -> Certainly no unicast!
                b_or_mcast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr);
            }
            else if (addr.isBroadcast())
            {   // Looks like broadcast?
                b_or_mcast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr);
            }
            else
            {
                // Assume unicast, but could also be a bcast for another subnet that has a netmask which doesn't end in 255
                unicast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr + " (assume unicast)");
            }
        }

        this.name_server_addresses.addAll(name_server_addresses);
    }

    public void start()
    {
        // +-jitter to prevent multiple clients from sending concurrent search requests
        final long period = SEARCH_PERIOD_MS + (new Random().nextInt(2*SEARCH_JITTER_MS+1) - SEARCH_JITTER_MS);

        logger.log(Level.FINER,
                   () -> String.format("Search intervals: %.2f s, %.2f s, %.2f s, ..., %.2f s",
                                 2*period/1000.0,
                                 4*period/1000.0,
                                 8*period/1000.0,
                                 128*period/1000.0));
        timer.scheduleAtFixedRate(this::runSearches, period, period, TimeUnit.MILLISECONDS);
    }

    /** @param channel Channel that should be searched
     *  @param now Start searching as soon as possible, or delay?
     */
    public void register(final PVAChannel channel, final boolean now)
    {
        logger.log(Level.FINE, () -> "Register search for " + channel.getName() + " " + channel.getCID());
        channel.setState(ClientChannelState.SEARCHING);
        searched_channels.computeIfAbsent(channel.getCID(), id -> new SearchedChannel(channel));
        // Issue immediate search request?
        if (now)
            search(channel);
    }

    /** Stop searching for channel
     *  @param channel_id
     *  @return {@link PVAChannel}, <code>null</code> when channel wasn't searched any more
     */
    public PVAChannel unregister(final int channel_id)
    {
        final SearchedChannel searched = searched_channels.remove(channel_id);
        if (searched != null)
        {
            logger.log(Level.FINE, () -> "Unregister search for " + searched.channel.getName() + " " + channel_id);
            return searched.channel;
        }
        return null;
    }

    /** Boost search for missing channels
     *
     *  <p>Resets their search counter so they're searched "real soon".
     */
    public void boost()
    {
        for (SearchedChannel searched : searched_channels.values())
        {
            logger.log(Level.FINE, () -> "Restart search for " + searched.channel.getName());
            searched.search_counter.set(BOOST_SEARCH_COUNT);
            // Not sending search right now:
            //   search(channel);
            // Instead, scheduling it to be searched again real soon for a few times.
            // We tend to receive multiple copies of the same beacon via various network
            // interfaces, so by scheduling a search real soon it happens once,
            // not for every duplicate of the same beacon
        }
    }

    private static boolean isPowerOfTwo(final int x)
    {
        return x > 0  &&  (x & (x - 1)) == 0;
    }

    /** Invoked by timer: Check searched channels for the next one to handle */
    private void runSearches()
    {
        for (SearchedChannel searched : searched_channels.values())
        {
            final int counter = searched.search_counter.updateAndGet(val -> val >= MAX_SEARCH_COUNT ? MAX_SEARCH_RESET : val+1);
            if (isPowerOfTwo(counter))
            {
                logger.log(Level.FINER, () -> "Searching... " + searched.channel);
                search(searched.channel);
            }
        }
    }

    /** Issue a PVA server list request */
    public void list()
    {
        // Search is invoked for new SearchedChannel(channel, now)
        // as well as by regular, timed search.
        // Lock the send buffer to avoid concurrent use.
        synchronized (send_buffer)
        {
            logger.log(Level.FINE, "List Request");
            sendSearch(0, -1, null);
        }
    }

    /** Issue search for channel
     *  @param channel Channel to search
     */
    private void search(final PVAChannel channel)
    {
        // Search via TCP
        for (AddressInfo name_server : name_server_addresses)
        {
            final SearchedChannel searched = searched_channels.get(channel.getCID());
            if (searched == null)
                continue;
            // Compared to UDP, search only every other cycle
            int count = searched.tcp_search_counter.incrementAndGet();
            if (count % 2 != 0)
                continue;

            final ClientTCPHandler tcp = tcp_provider.apply(name_server.getAddress());

            // In case of connection errors (TCP connection blocked by firewall),
            // tcp will be null
            if (tcp != null)
            {
                final RequestEncoder search_request = (version, buffer) ->
                {
                    logger.log(Level.FINE, () -> "Searching for " + channel + " via TCP " + tcp.getRemoteAddress() + ", take " + count/2);

                    // Search sequence identifies the potentially repeated UDP.
                    // TCP search is once only, so PVXS always sends 0x66696E64 = "find".
                    // We send "look".
                    final int seq = 0x6C6F6F6B;

                    // Use 'any' reply address since reply will be via this TCP socket
                    final InetSocketAddress response_address = new InetSocketAddress(0);

                    SearchRequest.encode(true, seq, channel.getCID(), channel.getName(), response_address , buffer);
                };
                tcp.submit(search_request);
            }
        }

        // Shortcut UDP search, avoid log messages when lists are empty
        if (unicast_search_addresses.isEmpty()  &&  b_or_mcast_search_addresses.isEmpty())
            return;

        // Search is invoked for new SearchedChannel(channel, now)
        // as well as by regular, timed search.
        // Lock the send buffer to avoid concurrent use.
        synchronized (send_buffer)
        {
            final int seq = search_sequence.incrementAndGet();
            logger.log(Level.FINE, "Search Request #" + seq + " for " + channel);
            sendSearch(seq, channel.getCID(), channel.getName());
        }
    }

    /** Send a 'list' or channel search out via UDP */
    private void sendSearch(final int seq, final int cid, final String name)
    {
        // Buffer starts out with UNICAST bit set in the search message
        for (AddressInfo addr : unicast_search_addresses)
        {
            send_buffer.clear();
            final InetSocketAddress response = udp.getResponseAddress(addr);
            SearchRequest.encode(true, seq, cid, name, response, send_buffer);
            send_buffer.flip();
            try
            {
                logger.log(Level.FINER, () -> "Sending search to UDP  " + addr + " (unicast), " +
                                              "response addr " + response + "\n" + Hexdump.toHexdump(send_buffer));
                udp.send(send_buffer, addr);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to send search request to " + addr, ex);
            }
        }

        for (AddressInfo addr : b_or_mcast_search_addresses)
        {
            send_buffer.clear();
            final InetSocketAddress response = udp.getResponseAddress(addr);
            SearchRequest.encode(false, seq, cid, name, response, send_buffer);
            send_buffer.flip();
            try
            {
                logger.log(Level.FINER, () -> "Sending search to UDP  " + addr + " (broadcast/multicast), " +
                                              "response addr " + response + "\n" + Hexdump.toHexdump(send_buffer));
                udp.send(send_buffer, addr);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to send search request to " + addr, ex);
            }
        }
    }

    /** Stop searching channels */
    public void close()
    {
        searched_channels.clear();

        timer.shutdown();
    }
}
