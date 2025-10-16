/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
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

    /** Seconds to delay to start a search "soon", not right now */
    private static final int SEARCH_SOON_DELAY = 5;

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

        // Searches are identified by CID because we might have the
        // same channel name in different searches when we try to access
        // the same channel with different field(...) qualifiers.
        // One could try to optimize this by fetching "field()" (everything)
        // and then picking the subelements in the client,
        // but in case there's only a single PV for
        //     pva://GigaBytePV/substruct/double_field
        // we'd want to use "field(substruct.double_field)"
        // and avoid fetching the complete structure.
        // ... unless there is later a PV "pva://GigaBytePV",
        // but we don't know, yet?

        // Hash by CID
        @Override
        public int hashCode()
        {
            return channel.getCID();
        }

        // Compare by CID
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof SearchedChannel other)
                return other.channel.getCID() == channel.getCID();
            return false;
        }
    }

    // SearchedChannels are tracked in two data structures
    //
    // - searched_channels
    //   Fast lookup of channel by ID,
    //   creating at most one SearchedChannel per CID.
    //   Allows checking if a channel is indeed searched,
    //   and locating the channel for a search reply.
    //
    //  - search_buckets
    //   Efficiently schedule the search messages for all channels
    //   up to MAX_SEARCH_PERIOD.
    //
    //  Access to either one needs to be synchronized

    /**  Map of searched channels by channel ID
     *
     *   Access only from synchronized method
     */
    private HashMap<Integer, SearchedChannel> searched_channels = new HashMap<>();

    /** Search buckets
     *
     *  <p>The {@link #current_search_bucket} selects the set
     *  of channels to be searched by {@link #runSearches()},
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
     *  must only occur in a 'synchronized' method.
     */
    private final ArrayList<Set<SearchedChannel>> search_buckets = new ArrayList<>(MAX_SEARCH_PERIOD+2);

    /** Index of current search bucket, i.e. the one about to be searched.
     *
     *  <p>Access must only occur in a 'synchronized' method.
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

    /** Create ClientTCPHandler from IP address and 'tls' flag */
    private final BiFunction<InetSocketAddress, Boolean, ClientTCPHandler> tcp_provider;

    /** Buffer for assembling search messages */
    private final ByteBuffer send_buffer = ByteBuffer.allocate(PVASettings.MAX_UDP_UNFRAGMENTED_SEND);

    /** After header of about 50 bytes, allow this for the search packet payload (CIDs, names) */
    private final int MAX_SEARCH_PAYLOAD = PVASettings.MAX_UDP_UNFRAGMENTED_SEND - 50;

    /** Address list to which search requests are sent */
    private final List<AddressInfo> unicast_search_addresses = new ArrayList<>(),
                                    b_or_mcast_search_addresses = new ArrayList<>(),
                                    name_server_addresses = new ArrayList<>();

    /** Create channel searcher
     *  @param udp UDP handler
     *  @param udp_addresses UDP addresses to search
     *  @param tcp_provider Function that creates ClientTCPHandler for IP address and 'tls' flag
     *  @param name_server_addresses TCP addresses to search
     *  @throws Exception on error
     */
    public ChannelSearch(final ClientUDPHandler udp,
                         final List<AddressInfo> udp_addresses,
                         final BiFunction<InetSocketAddress, Boolean, ClientTCPHandler> tcp_provider,
                         final List<AddressInfo> name_server_addresses) throws Exception
    {
        this.udp = udp;
        this.tcp_provider = tcp_provider;

        // Each bucket holds set of channels to search in that time slot
        for (int i=0; i<MAX_SEARCH_PERIOD+2; ++i)
            search_buckets.add(new HashSet<>());

        // Searches sent to multicast (IPv4, IPv6) or broadcast addresses (IPv4) reach every PVA server
        // on that multicast group or bcast subnet.
        // Searches sent to unicast addresses reach only the PVA server started _last_ on each host.
        // In the original PVA implementation, the receiver would then re-send them via a local multicast group.
        // The original unicast messages were thus marked with a UNICAST flag,
        // while multicast and broadcast messages are not.
        for (AddressInfo addr : udp_addresses)
        {
            if(addr.getAddress().getAddress() == null){ // E.g. address unreachable
                logger.log(Level.CONFIG, "Skipping unreachable address " + addr.getAddress());
                continue;
            }
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
        // 1 second +-jitter to prevent multiple clients from sending concurrent search requests
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
        logger.log(Level.FINE, () -> "Register search for " + channel + (now ? " now" : " soon"));

        synchronized (this)
        {
            final ClientChannelState old = channel.setState(ClientChannelState.SEARCHING);
            if (old == ClientChannelState.SEARCHING)
                logger.log(Level.WARNING, "Registering channel " + channel + " to be searched more than once ");

            final SearchedChannel sc = searched_channels.computeIfAbsent(channel.getCID(), id -> new SearchedChannel(channel));

            int bucket = current_search_bucket.get();
            if (!now)
                bucket = (bucket + SEARCH_SOON_DELAY) % search_buckets.size();
            search_buckets.get(bucket).add(sc);
        }

        // Jumpstart search instead of waiting up to ~1 second for current bucket to be handled
        if (now)
            timer.execute(this::runSearches);
    }

    /** Stop searching for channel
     *  @param channel_id
     *  @return {@link PVAChannel}, <code>null</code> when channel wasn't searched any more
     */
    public synchronized PVAChannel unregister(final int channel_id)
    {
        final SearchedChannel searched = searched_channels.remove(channel_id);
        if (searched != null)
        {
            logger.log(Level.FINE, () -> "Unregister search for " + searched.channel.getName() + " " + channel_id);
            // Remove `searched` from all `search_buckets`.
            for (Set<SearchedChannel> bucket : search_buckets)
                bucket.remove(searched);
            return searched.channel;
        }
        return null;
    }

    /** Boost search for missing channels
     *
     *  <p>Resets their search counter so they're searched "real soon".
     */
    public synchronized void boost()
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

                final Set<SearchedChannel> bucket = search_buckets.get(current_search_bucket.get());
                bucket.add(searched);
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
        // Determine current search bucket
        final int current = current_search_bucket.getAndUpdate(i -> (i + 1) % search_buckets.size());
        // Collect channels to be searched while sync'ed
        final ArrayList<SearchRequest.Channel> to_search = new ArrayList<>();
        synchronized (this)
        {
            final Set<SearchedChannel> bucket = search_buckets.get(current);
            logger.log(Level.FINEST, () -> "Search bucket " + current);

            // Remove searched channels from the current bucket
            for (SearchedChannel sc : bucket)
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
                    final Set<SearchedChannel> next      = search_buckets.get(i_n);
                    final Set<SearchedChannel> next_next = search_buckets.get(i_n_n);
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
            bucket.clear();
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
                final SearchRequest.Channel channel = to_search.get(start + count);
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

            // Submit one batch from 'to_search'
            final List<SearchRequest.Channel> batch = to_search.subList(start, start + count);
            search(batch);
            start += count;
        }
    }

    /** Issue a PVA server list request */
    public void list()
    {
        final boolean tls = !PVASettings.EPICS_PVA_TLS_KEYCHAIN.isBlank();

        // Search is invoked for new SearchedChannel(channel, now)
        // as well as by regular, timed search.
        // Lock the send buffer to avoid concurrent use.
        synchronized (send_buffer)
        {
            logger.log(Level.FINE, "List Request");
            sendSearch(0, null, tls);
        }
    }

    /** Issue search for channels
     *  @param channels Channels to search, <code>null</code> for 'list'
     */
    private void search(final Collection<SearchRequest.Channel> channels)
    {
        // Do we support TLS? This will be encoded in the search requests
        // to tell server if we can support TLS?
        final boolean tls = !PVASettings.EPICS_PVA_TLS_KEYCHAIN.isBlank();

        // Search via TCP
        for (AddressInfo name_server : name_server_addresses)
        {
            // For search via TCP, do we use plain TCP or do we send the search itself via TLS?
            // This is configured in EPICS_PVA_NAME_SERVERS via prefix pvas://
            final ClientTCPHandler tcp = tcp_provider.apply(name_server.getAddress(), name_server.isTLS());

            // In older implementation, tcp was null in case of connection errors (TCP connection blocked by firewall).
            // No longer expected to happen but check anyway
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

                    SearchRequest.encode(true, true, seq, channels, response_address, tls , buffer);
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
            sendSearch(seq, channels, tls);
        }
    }

    /** Send a 'list' or channel search out via UDP
     *  @param seq Search sequence number
     *  @param channels Channels to search, <code>null</code> for listing channels
     *  @param tls Use TLS?
     */
    private void sendSearch(final int seq, final Collection<SearchRequest.Channel> channels, final boolean tls)
    {
        // Buffer starts out with UNICAST bit set in the search message
        for (AddressInfo addr : unicast_search_addresses)
        {
            send_buffer.clear();
            final InetSocketAddress response = udp.getResponseAddress(addr);
            SearchRequest.encode(true, true, seq, channels, response, tls, send_buffer);
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
            SearchRequest.encode(false, true, seq, channels, response, tls, send_buffer);
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
        synchronized (this)
        {
            searched_channels.clear();
        }
        timer.shutdown();
    }
}
