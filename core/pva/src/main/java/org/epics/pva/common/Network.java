/*******************************************************************************
 * Copyright (c) 2019-2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.epics.pva.PVASettings.logger;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;

import org.epics.pva.PVASettings;

/** Network helpers
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Network
{
    /** Obtain broadcast addresses for all local network interfaces
     *  @param port UDP port on which to broadcast
     *  @return List of broadcast addresses
     */
    public static List<InetSocketAddress> getBroadcastAddresses(final int port)
    {
        final List<InetSocketAddress> addresses = new ArrayList<>();

        try
        {
            final Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements())
            {
                final NetworkInterface iface = ifs.nextElement();
                try
                {
                    // Only consider operational interfaces
                    if (!iface.isUp())
                        continue;
                    for (InterfaceAddress addr : iface.getInterfaceAddresses())
                        if (addr.getBroadcast() != null)
                        {
                            final InetSocketAddress bcast = new InetSocketAddress(addr.getBroadcast(), port);
                            if (! addresses.contains(bcast))
                                addresses.add(bcast);
                        }
                }
                catch (Throwable ex)
                {
                    logger.log(Level.WARNING, "Cannot inspect network interface " + iface, ex);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot list network interfaces", ex);
        }

        if (addresses.isEmpty())
            addresses.add(new InetSocketAddress("255.255.255.255", port));

        return addresses;
    }

    /** Parse network addresses
     *
     *  Supported formats:
     *  <ul>
     *  <li>IPv4       - "127.0.0.1"
     *  <li>hostname   -  "my_ioc.site.org"
     *  <li>IPv4:port  - "127.0.0.1:5076"
     *  <li>[IPv6]     - "[::1]"
     *  <li>[IPv6]:port - "[::1]:5076"
     *  </ul>
     *  @param search_addresses Array of "IP:port" or just "IP", defaulting to {@link PVASettings#EPICS_PVA_BROADCAST_PORT}
     *  @return {@link InetSocketAddress} list
     */
    public static List<InetSocketAddress> parseAddresses(final String... search_addresses)
    {
        final List<InetSocketAddress> addresses = new ArrayList<>();
        for (String search : search_addresses)
        {
            // Don't confuse the last nibble of "[::1]" with a trailing ":1" port.
            final int ip6 = search.lastIndexOf(']');
            final int sep = search.lastIndexOf(':');
            // Use ':' only if it was after the ']',
            // or if there is no ']' (ip6 == -1)
            if (sep > 0  &&  sep > ip6)
            {
                final String hostname = search.substring(0, sep);
                final int port = Integer.parseInt(search.substring(sep+1));
                addresses.add(new InetSocketAddress(hostname, port));
            }
            else
                addresses.add(new InetSocketAddress(search, PVASettings.EPICS_PVA_BROADCAST_PORT));
        }
        return addresses;
    }

    /** @return Loopback network interface or <code>null</code> */
    public static NetworkInterface getLoopback()
    {
        try
        {
            final Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements())
            {
                final NetworkInterface iface = nets.nextElement();
                try
                {
                    if (iface.isUp() && iface.isLoopback())
                        return iface;
                }
                catch (Throwable ex)
                {
                    logger.log(Level.WARNING, "Cannot inspect network interface " + iface, ex);
                }
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot list network interfaces", ex);
        }

        return null;
    }

    /** Create UDP channel
     *
     *  @param broadcast Support broadcast?
     *  @param address IP address of interface, use empty string for wildcard address
     *  @param port Port to use or 0 to auto-assign
     *  @return UDP channel
     *  @throws Exception on error
     */
    public static DatagramChannel createUDP(final boolean broadcast, final String address,  final int port) throws Exception
    {
        final InetAddress inet = address.isEmpty()
                               ? null
                               : InetAddress.getByName(address);

        // TODO Current use of v4 multicast addresses works only with INET, not INET6
        // TODO For address.isEmpty(), we always assume INET6...
        final DatagramChannel udp = inet instanceof Inet4Address
                                  ? DatagramChannel.open(StandardProtocolFamily.INET)
                                  : DatagramChannel.open();
        udp.configureBlocking(true);
        if (broadcast)
            udp.socket().setBroadcast(true);
        udp.socket().setReuseAddress(true);
        if (inet == null)
            udp.bind(new InetSocketAddress(port));
        else
            udp.bind(new InetSocketAddress(inet, port));
        return udp;
    }

    /** Try to listen to multicast messages
     *  @param udp UDP channel that should listen to multicast messages
     *  @param port Port to use
     *  @return Local multicast address, or <code>null</code> if no multicast support
     */
    public static InetSocketAddress configureMulticast(final DatagramChannel udp, final int port)
    {
        try
        {
            if (! (udp.getLocalAddress() instanceof InetSocketAddress))
                throw new Exception("UDP socket is not bound");
            // TODO What to use for IPv6?
            if (((InetSocketAddress)udp.getLocalAddress()).getAddress() instanceof Inet6Address)
            {
                logger.log(Level.CONFIG, "For now not using any Multicast group while operating with IPv6");
                return null;
            }

            final NetworkInterface loopback = getLoopback();
            if (loopback != null)
            {
                final InetAddress group = InetAddress.getByName(PVASettings.EPICS_PVA_MULTICAST_GROUP);
                final InetSocketAddress local_broadcast = new InetSocketAddress(group, port);
                udp.join(group, loopback);

                logger.log(Level.CONFIG, "Multicast group " + local_broadcast + " using network interface " + loopback.getDisplayName());
                udp.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
                udp.setOption(StandardSocketOptions.IP_MULTICAST_IF, loopback);

                return local_broadcast;
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot configure multicast support", ex);
        }
        return null;
    }
}
