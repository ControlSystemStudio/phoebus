/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.client;

import static org.epics.pva.PVASettings.logger;

import java.net.InetAddress;
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
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.epics.pva.PVASettings;
import org.epics.pva.common.Network;
import org.epics.pva.common.PVAHeader;
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
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class ChannelSearch
{
    /** Channel that's being searched */
    private class SearchedChannel
    {
        final AtomicInteger search_counter;
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
    private final List<InetSocketAddress> unicast_search_addresses = new ArrayList<>(), broadcast_search_addresses = new ArrayList<>();

    public ChannelSearch(final ClientUDPHandler udp, final List<InetSocketAddress> search_addresses) throws Exception
    {
        this.udp = udp;

        // Searches sent to broadcast addresses reach every PVA server on that subnet.
        // Searches sent to unicast addresses reach only the PVA server started _last_ on each host.
        // They are thus marked with a UNICAST flag, and the receiving PVA tool then
        // re-broadcasts them locally for other PVA tools on the same host.
        final List<InetAddress> local_bcast = Network.getBroadcastAddresses(0).stream().map(InetSocketAddress::getAddress).collect(Collectors.toList());
        for (InetSocketAddress addr : search_addresses)
        {
            // Trouble is, how do you recognize a unicast address?
            if (addr.getAddress().isMulticastAddress())
            {   // Multicast -> Certainly no unicast!
                broadcast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr.getAddress() + " port " + addr.getPort() + " (multicast)");
            }
            else if (local_bcast.contains(addr.getAddress()))
            {   // One of the local network interface bcasts? -> No unicast
                broadcast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr.getAddress() + " port " + addr.getPort() + " (local broadcast)");
            }
            else
            {   // Else: Assume unicast, but could also be a bcast for another subnet...
                unicast_search_addresses.add(addr);
                logger.log(Level.CONFIG, "Sending searches to " + addr.getAddress() + " port " + addr.getPort() + " (assume unicast)");
            }
        }
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
                logger.log(Level.FINE, () -> "Searching... " + searched.channel);
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
            final int payload_start = send_buffer.position() + PVAHeader.HEADER_SIZE;
            SearchRequest.encode(true, 0, -1, null, udp.getResponseAddress(), send_buffer);
            send_buffer.flip();
            logger.log(Level.FINE, "List Request");
            sendSearch(payload_start);
        }
    }

    /** Issue search for channel
     *  @param channel Channel to search
     */
    private void search(final PVAChannel channel)
    {
        // Search is invoked for new SearchedChannel(channel, now)
        // as well as by regular, timed search.
        // Lock the send buffer to avoid concurrent use.
        synchronized (send_buffer)
        {
            final int payload_start = send_buffer.position() + PVAHeader.HEADER_SIZE;
            final int seq = search_sequence.incrementAndGet();
            SearchRequest.encode(true, seq, channel.getCID(), channel.getName(), udp.getResponseAddress(), send_buffer);
            send_buffer.flip();
            logger.log(Level.FINE, "Search Request #" + seq + " for " + channel);
            sendSearch(payload_start);
        }
    }

    /** Send a 'list' or channel search out via UDP */
    private void sendSearch(final int payload_start)
    {
        for (InetSocketAddress addr : unicast_search_addresses)
        {
            try
            {
                logger.log(Level.FINER, () -> "Sending search to UDP  " + addr + " (unicast)\n" + Hexdump.toHexdump(send_buffer));
                udp.send(send_buffer, addr);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to send search request to " + addr, ex);
            }
            send_buffer.rewind();
        }

        // Clear broadcast flag
        send_buffer.put(payload_start+4, (byte) 0x00);
        for (InetSocketAddress addr : broadcast_search_addresses)
        {
            try
            {
                logger.log(Level.FINER, () -> "Sending search to UDP  " + addr + " (broadcast/multicast)\n" + Hexdump.toHexdump(send_buffer));
                udp.send(send_buffer, addr);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Failed to send search request to " + addr, ex);
            }
            send_buffer.rewind();
        }
    }

    /** Stop searching channels */
    public void close()
    {
        searched_channels.clear();

        timer.shutdown();
    }
}
