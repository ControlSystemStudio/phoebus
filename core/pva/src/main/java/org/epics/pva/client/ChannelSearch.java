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
import java.util.Collection;
import java.util.LinkedList;
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
import org.epics.pva.data.PVAString;

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

    // SearchedChannels are tracked in two data structures
    //
    // - searched_channels (concurrent)
    //   Fast lookup of channel by ID,
    //   efficient `computeIfAbsent(cid, ..` mechanism for creating
    //   at most one SearchedChannel per CID.
    //   Allows checking if a channel is indeed searched,
    //   and locating the channel for a search reply.
    //
    //  - search_buckets (need to SYNC)
    //   Efficiently schedule the search messages for all channels
    //   up to MAX_SEARCH_PERIOD.

    /**  Map of searched channels by channel ID */
    private ConcurrentHashMap<Integer, SearchedChannel> searched_channels = new ConcurrentHashMap<>();

    /** Search buckets
     *
     *  <p>The {@link #current_search_bucket} selects the list
     *  of channels to be searched by {@link #runSeaches()},
     *  which runs roughly once per second, each time moving to
     *  the next search bucket in a ring buffer fashion.
     *
     *  <p>Each searched channel is removed from the current bucket.
     *  To be searched again, it is inserted into the appropriate
     *  upcoming bucket, allowing for a maximum search
     *  period of <code>MAX_SEARCH_PERIOD == search_buckets.size() - 2</code>.
     *  The search bucket size is <code>MAX_SEARCH_PERIOD + 2</code>
     *  so that a channel in bucket N can be moved to either
     *  <code>N + MAX_SEARCH_PERIOD</code> or
     *  <code>N + MAX_SEARCH_PERIOD + 1</code> to distribute searches
     *  without putting the channel immediately back into bucket N
     *  which would result in an endless loop.
     *
     *  <p>Access to either {@link #search_buckets} or {@link #current_search_bucket}
     *  must SYNC on {@link #search_buckets}.
     */
    private final ArrayList<LinkedList<SearchedChannel>> search_buckets = new ArrayList<>();

    /** Index of current search bucket, i.e. the one about to be searched.
     *
     *  <p>Access must SYNC on {@link #search_buckets}.
     */
    private final AtomicInteger current_search_bucket = new AtomicInteger();

    /** Timer used to periodically check channels and issue search requests */
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(run ->
    {
        final Thread thread = new Thread(run, "ChannelSearch");
        thread.setDaemon(true);
        return thread;
    });

    private final ClientUDPHandler udp;

    private final Function<InetSocketAddress, ClientTCPHandler> tcp_provider;

    /** Buffer for assembling search messages */
    private final ByteBuffer send_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_UNFRAGMENTED_SEND);

    /** After header of about 50 bytes, allow this for the search packet payload (CIDs, names) */
    private final int MAX_SEARCH_PAYLOAD = PVASettings.MAX_UDP_UNFRAGMENTED_SEND - 50;

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

        synchronized (search_buckets)
        {
            for (int i=0; i<MAX_SEARCH_PERIOD+2; ++i)
                search_buckets.add(new LinkedList<>());
        }

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
        logger.log(Level.FINE, () -> "Register search for " + channel);

        final ClientChannelState old = channel.setState(ClientChannelState.SEARCHING);
        if (old == ClientChannelState.SEARCHING)
            logger.log(Level.WARNING, "Registering channel " + channel + " to be searched more than once ");

        final SearchedChannel sc = searched_channels.computeIfAbsent(channel.getCID(), id -> new SearchedChannel(channel));

        synchronized (search_buckets)
        {
            search_buckets.get(current_search_bucket.get()).add(sc);
        }
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
            // NOT removing `searched` from all `search_buckets`.
            // Removal would be a slow, linear operation.
            // `runSearches()` will drop the channel from `search_buckets`
            // because it's no longer listed in `searched_channels`

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
                logger.log(Level.FINE, () -> "Restart search for '" + searched.channel.getName() + "'");
                synchronized (search_buckets)
                {
                    final LinkedList<SearchedChannel> bucket = search_buckets.get(current_search_bucket.get());
                    if (! bucket.contains(searched))
                        bucket.add(searched);
                }
            }
            // Not sending search right now:
            //   search(channel);
            // Instead, scheduling it to be searched again real soon for a few times.
            // We tend to receive multiple copies of the same beacon via various network
            // interfaces, so by scheduling a search real soon it happens once,
            // not for every duplicate of the same beacon
        }
    }

    /** List of channels to search, re-used within runSearches */
    private final ArrayList<PVAChannel> to_search = new ArrayList<>();

    /** Invoked by timer: Check searched channels for the next one to handle */
    @SuppressWarnings("unchecked")
    private void runSearches()
    {
        to_search.clear();
        synchronized (search_buckets)
        {
            // Determine current search bucket
            final int current = current_search_bucket.getAndUpdate(i -> (i + 1) % search_buckets.size());
            final LinkedList<SearchedChannel> bucket = search_buckets.get(current);

            // Remove searched channels from the current bucket
            SearchedChannel sc;
            while ((sc = bucket.poll()) != null)
            {
                if (sc.channel.getState() == ClientChannelState.SEARCHING  &&
                    searched_channels.containsKey(sc.channel.getCID()))
                {
                    // Collect channels in 'to_search' for handling outside of sync. section
                    to_search.add(sc.channel);

                    // Determine next search period
                    final int period = sc.search_period.updateAndGet(sec -> sec < MAX_SEARCH_PERIOD
                                                                     ? sec + 1
                                                                     : MAX_SEARCH_PERIOD);

                    // Add to corresponding search bucket, or delay by one second
                    // in case that search bucket is quite full
                    final int i_n   = (current + period) % search_buckets.size();
                    final int i_n_n = (i_n + 1)          % search_buckets.size();
                    final LinkedList<SearchedChannel> next = search_buckets.get(i_n);
                    final LinkedList<SearchedChannel> next_next = search_buckets.get(i_n_n);
                    if (i_n == current  ||  i_n_n == current)
                        throw new IllegalStateException("Current, next and nextnext search indices for " + sc.channel + " are " +
                                                        current + ", " + i_n + ", " + i_n_n);
                    if (next_next.size() < next.size())
                        next_next.add(sc);
                    else
                        next.add(sc);
                }
                else
                    logger.log(Level.FINE, "Dropping channel from search: " + sc.channel);
            }
        }


        // Search batch..
        // Size of a search request is close to 50 bytes
        // plus { int cid, string name } for each channel.
        // Channel count is unsigned short, but we limit
        // is to a signed short.
        // Similar to PVXS, further limit payload to 1400 bytes
        // to stay well below the ~1500 byte ethernet frame
        int start = 0;
        while (start < to_search.size())
        {
            int payload = 0;
            int count = 0;
            while (start + count < to_search.size()  &&  count < Short.MAX_VALUE-1)
            {
                final PVAChannel channel = to_search.get(start + count);
                int size = 4 + PVAString.getEncodedSize(channel.getName());
                if (payload + size < MAX_SEARCH_PAYLOAD)
                {
                    ++count;
                    payload += size;
                }
                else if (count == 0)
                {   // Can't fit this single name?
                    logger.log(Level.WARNING, "PV name exceeds search buffer size: " + channel);
                    searched_channels.remove(channel.getCID());
                    to_search.remove(start + count);
                }
                else
                {
                    logger.log(Level.FINER, () -> "Reached " + MAX_SEARCH_PAYLOAD + " bytes, splitting");
                    break;
                }
            }
            if (count == 0)
                break;

            final List<PVAChannel> batch = to_search.subList(start, start + count);
            // PVAChannel extends SearchRequest.Channel, so use List<PVAChannel> as Collection<SR.Channel>
            search((Collection<SearchRequest.Channel>) (List<? extends SearchRequest.Channel>)batch);
            start += count;
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
            sendSearch(0, null);
        }
    }

    /** Issue search for channels
     *  @param channels Channels to search, <code>null</code> for 'list'
     */
    private void search(final Collection<SearchRequest.Channel> channels)
    {
        // Search via TCP
        for (AddressInfo name_server : name_server_addresses)
        {
            final ClientTCPHandler tcp = tcp_provider.apply(name_server.getAddress());

            // In case of connection errors (TCP connection blocked by firewall),
            // tcp will be null
            if (tcp != null)
            {
                final RequestEncoder search_request = (version, buffer) ->
                {
                    logger.log(Level.FINE, () -> "Searching for " + channels + " via TCP " + tcp.getRemoteAddress());

                    // Search sequence identifies the potentially repeated UDP.
                    // TCP search is once only, so PVXS always sends 0x66696E64 = "find".
                    // We send "look" ("kool" for little endian).
                    final int seq = 0x6C6F6F6B;

                    // Use 'any' reply address since reply will be via this TCP socket
                    final InetSocketAddress response_address = new InetSocketAddress(0);

                    SearchRequest.encode(true, seq, channels, response_address , buffer);
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
            // For UDP, use bucket index to get a changing number that helps
            // match up duplicate packets and allows debugging bucket usage
            final int seq = current_search_bucket.get();
            logger.log(Level.FINE, () -> "UDP Search Request #" + seq + " for " + channels);
            sendSearch(seq, channels);
        }
    }

    /** Send a 'list' or channel search out via UDP */
    private void sendSearch(final int seq, final Collection<SearchRequest.Channel> channels)
    {
        // Buffer starts out with UNICAST bit set in the search message
        for (AddressInfo addr : unicast_search_addresses)
        {
            send_buffer.clear();
            final InetSocketAddress response = udp.getResponseAddress(addr);
            SearchRequest.encode(true, seq, channels, response, send_buffer);
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
            SearchRequest.encode(false, seq, channels, response, send_buffer);
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
