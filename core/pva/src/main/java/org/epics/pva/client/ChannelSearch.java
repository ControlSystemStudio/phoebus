/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
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
 *  Search periods match the design of https://github.com/mdavidsaver/pvxs,
 *  repeating searches for missing channels after 1, 2, 3, ..., 30 seconds
 *  and then continuing every 30 seconds.
 *  The exact period is not a multiple of 1000ms but 1000+-25ms to randomly
 *  distribute searches from different clients.
 *  Once the search plateaus at 30 seconds, which takes about 7.6 to 7.9 minutes,
 *  the search can be "boosted" back to 1, 2, 3, ... seconds.
 *  Since long running servers issue beacons every ~3 minutes,
 *  every existing PVA server on the network will appear "new" when a client
 *  receives its first beacon within ~3 minutes after startup.
 *  Such beacons are ignored for the ~7 minutes where the search period settles
 *  to avoid unnecessary network traffic.
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
    /** Basic search period is one second */
    private static final int SEARCH_PERIOD_MS = 1000;

    /** Search period jitter to avoid multiple clients all searching at the same period */
    private static final int SEARCH_JITTER_MS = 25;

    /** Minimum search for a channel is ASAP, then incrementing by 1 */
    private static final int MIN_SEARCH_PERIOD = 0;

    /** Maximum and eternal search period is every 30 sec */
    private static final int MAX_SEARCH_PERIOD = 30;

    /** Channel that's being searched */
    private class SearchedChannel
    {
        /** Search period in seconds.
         *  Steps up from 0 to MAX_SEARCH_PERIOD and then stays at MAX_SEARCH_PERIOD
         */
        final AtomicInteger search_period = new AtomicInteger(1);

        /** Seconds spent in the current state.
         *  Incremented for every run of the search thread.
         *  If it reaches the current search_period,
         *  a search is performed and search_period updated
         *  to the next one.
         */
        final AtomicInteger seconds_in_state = new AtomicInteger(0);

        final AtomicInteger tcp_search_counter = new AtomicInteger(0);
        final PVAChannel channel;

        SearchedChannel(final PVAChannel channel)
        {
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
            // If search for channel has settled to the long period, restart
            final int period = searched.search_period.updateAndGet(val -> val >= MAX_SEARCH_PERIOD
                                                                        ? MIN_SEARCH_PERIOD
                                                                        : val);
            if (period == MIN_SEARCH_PERIOD)
            {
                searched.seconds_in_state.set(0);
                logger.log(Level.FINE, () -> "Restart search for '" + searched.channel.getName() + "'");
            }
            // Not sending search right now:
            //   search(channel);
            // Instead, scheduling it to be searched again real soon for a few times.
            // We tend to receive multiple copies of the same beacon via various network
            // interfaces, so by scheduling a search real soon it happens once,
            // not for every duplicate of the same beacon
        }
    }

    /** Invoked by timer: Check searched channels for the next one to handle */
    private void runSearches()
    {
        // TODO Collect searched channels, then issue one search message for all of them
        // (several for UDP as we reach max packet size)
        for (SearchedChannel searched : searched_channels.values())
        {
            // Stayed long enough in current search period?
            final int secs = searched.seconds_in_state.incrementAndGet();
            if (secs >= searched.search_period.get())
            {
                logger.log(Level.FINE, () -> "Searching... " + searched.channel);
                search(searched.channel);

                // Move to next search period, plateau at MAX_SEARCH_PERIOD
                searched.seconds_in_state.set(0);
                searched.search_period.updateAndGet(p -> p < MAX_SEARCH_PERIOD
                                                           ? p + 1
                                                           : MAX_SEARCH_PERIOD);
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
            logger.log(Level.FINE, "UDP Search Request #" + seq + " for " + channel);
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
