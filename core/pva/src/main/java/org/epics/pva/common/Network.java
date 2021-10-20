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
    /** @param channel UDP channel
     *  @return Local address or "UNBOUND"
     */
    public static String getLocalAddress(final DatagramChannel channel)
    {
        try
        {
            return channel.getLocalAddress().toString();
        }
        catch (Exception ex)
        {
            return "UNBOUND";
        }
    }

    /** Obtain broadcast addresses for all local network interfaces
     *  @param port UDP port on which to broadcast
     *  @return List of broadcast addresses
     */
    public static List<AddressInfo> getBroadcastAddresses(final int port)
    {
        final List<AddressInfo> addresses = new ArrayList<>();

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
                            final AddressInfo bcast = new AddressInfo(new InetSocketAddress(addr.getBroadcast(), port), 1, iface);
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
            addresses.add(new AddressInfo(new InetSocketAddress("255.255.255.255", port), 1, null));

        return addresses;
    }

    /** Parse a network address setting
     *
     *  Format: IPv4[:port][,TTL][@iface]
     *          '['IPv6']'[:port][,TTL][@iface]
     *
     *  127.0.0.1   -> IPv4 address
     *  [::1]       -> IPv6 address
     *  224.0.2.3,255@192.168.1.1 -> IPv4 224.0.2.3, TTL 255, using interface with address 192.168.1.1
     *  [ff02::42:1]:5076,1@br0   -> IPv6 ff02::42:1, port 5076, TTL 1, interface br0
     *
     *  @param setting Address "IP:port,TTL@iface" to parse
     *  @return {@link AddressInfo}
     *  @throws Exception on error
     */
    public static AddressInfo parseAddress(final String setting) throws Exception
    {
        NetworkInterface iface = null;
        int ttl = -1;
        int port = PVASettings.EPICS_PVA_BROADCAST_PORT;

        String parsed = setting;
        // Optional @interface
        int sep = parsed.lastIndexOf('@');
        if (sep >= 0)
        {
            final String name = parsed.substring(sep+1);
            parsed = parsed.substring(0, sep);
            if (name.matches("[0-9.:]+"))
                iface = NetworkInterface.getByInetAddress(InetAddress.getByName(name));
            else
                iface = NetworkInterface.getByName(name);
            if (iface == null)
                throw new Exception("Unknown network interface '" + name + "'");
        }

        // Optional ,TTL
        sep = parsed.lastIndexOf(',');
        if (sep >= 0)
        {
            ttl = Integer.parseInt(parsed.substring(sep+1));
            parsed = parsed.substring(0, sep);
        }

        // Optional :port, but must be ':' after IPv6 '[...:...:...]'
        final int ipv6 = parsed.lastIndexOf(']');
        sep = parsed.lastIndexOf(':');
        if (sep >= 0  &&  sep > ipv6)
        {
            port = Integer.parseInt(parsed.substring(sep+1));
            parsed = parsed.substring(0, sep);
        }

        // Parse remaining address
        final InetSocketAddress address = new InetSocketAddress(parsed, port);

        return new AddressInfo(address, ttl, iface);
    }

    /** Parse network addresses
     *
     *  @param search_addresses Array of addresses
     *  @return {@link InetSocketAddress} list
     *  @see #parseAddress(String)
     */
    public static List<AddressInfo> parseAddresses(final String... search_addresses)
    {
        final List<AddressInfo> addresses = new ArrayList<>();
        for (String search : search_addresses)
            try
            {
                addresses.add(parseAddress(search));
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot parse search address '" + search + "'", ex);
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


    public static DatagramChannel createUDP(final StandardProtocolFamily family, final int port) throws Exception
    {
        final DatagramChannel udp = DatagramChannel.open(family);
        udp.configureBlocking(true);
        udp.socket().setReuseAddress(true);
        udp.bind(new InetSocketAddress(port));
        return udp;
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
