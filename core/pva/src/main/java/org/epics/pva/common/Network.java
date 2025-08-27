/*******************************************************************************
 * Copyright (c) 2019-2025 Oak Ridge National Laboratory.
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
                            final AddressInfo bcast = new AddressInfo(false, new InetSocketAddress(addr.getBroadcast(), port), 1, iface);
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
            addresses.add(new AddressInfo(false, new InetSocketAddress("255.255.255.255", port), 1, null));

        return addresses;
    }

    /** Parse a network address setting
     *
     *  Format: IPv4[:port][,TTL][@iface]
     *          '['IPv6']'[:port][,TTL][@iface]
     *
     *  <pre>
     *  127.0.0.1                  IPv4 address
     *  127.0.0.1:9876             IPv4 address with port
     *  ::1                        IPv6 address
     *  [::1]                      IPv6 address
     *  [::1]:9876                 IPv6 address with port
     *  224.0.2.3,255@192.168.1.1  IPv4 224.0.2.3, TTL 255, using interface with address 192.168.1.1
     *  [ff02::42:1]:5076,1@br0    IPv6 ff02::42:1, port 5076, TTL 1, interface br0
     *  pvas://....                Request TLS, i.e. secure TCP, and default to EPICS_PVAS_TLS_PORT
     *  </pre>
     *
     *  @param setting Address "IP:port,TTL@iface" to parse
     *  @param default_port Port to use when 'setting' does not specify one
     *  @return {@link AddressInfo}
     *  @throws Exception on error
     */
    public static AddressInfo parseAddress(final String setting, final int default_port) throws Exception
    {
        NetworkInterface iface = null;
        int ttl = 1;
        int port = default_port;

        String parsed = setting;

        final boolean tls = parsed.startsWith("pvas://");
        if (tls)
        {
            parsed = parsed.substring(7);
            port = PVASettings.EPICS_PVAS_TLS_PORT;
        }

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

        // An IPv6 address contains at least two ':'.
        // It may be enclosed in '[...]', which is mandatory
        // when a ':port' follows
        boolean isv6 = false;
        int colons = 0;
        for (int i=0; i<parsed.length(); ++i)
        {
            if (parsed.charAt(i) == ':')
                ++colons;
            if (colons >= 2  ||  parsed.charAt(i) == '[')
            {
                isv6 = true;
                break;
            }
        }

        sep = parsed.lastIndexOf(':');
        if (isv6)
        {
            // Optional :port must be after the ':' inside IPv6 '[...:...:...]'
            final int ipv6 = parsed.lastIndexOf(']');
            if (sep >= 0  &&  ipv6 > 0  &&  sep > ipv6)
            {
                port = Integer.parseInt(parsed.substring(sep+1));
                parsed = parsed.substring(0, sep);
            }
        }
        else if (sep >= 0)
        {
            port = Integer.parseInt(parsed.substring(sep+1));
            parsed = parsed.substring(0, sep);
        }

        // Parse remaining address
        final InetSocketAddress address = new InetSocketAddress(parsed, port);

        return new AddressInfo(tls, address, ttl, iface);
    }

    /** Parse network addresses
     *
     *  @param search_list String with space-separated list of addresses
     *  @param default_port Port to use when address 'search_list' entry does not specify one
     *  @return {@link InetSocketAddress} list
     *  @see #parseAddress(String, int)
     */
    public static List<AddressInfo> parseAddresses(final String search_list, final int default_port)
    {
        // If search_list is empty, search_addresses will be [ "" ] with one empty entry
        final String[] search_addresses = search_list.split("\\s+");
        final List<AddressInfo> addresses = new ArrayList<>();
        for (String search : search_addresses)
            try
            {
                if (! search.isEmpty())
                    addresses.add(parseAddress(search, default_port));
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

    /** Create UDP channel
     *
     *  @param family {@link StandardProtocolFamily}
     *  @param address {@link Inet4Address}, {@link Inet6Address} or <code>null</code>
     *  @param port Port to use or 0 to auto-assign
     *  @return UDP channel
     *  @throws Exception on error
     */
    public static DatagramChannel createUDP(final StandardProtocolFamily family, final InetAddress address, final int port) throws Exception
    {
        final DatagramChannel udp = DatagramChannel.open(family);
        udp.configureBlocking(true);
        udp.socket().setReuseAddress(true);

        final InetSocketAddress addr;
        // If an address is provided, use it
        if (address != null)
            addr = new InetSocketAddress(address, port);
        // Otherwise, `new InetSocketAddress(port)` will unpredictably
        // use either a cached "0.0.0.0" or "::0".
        // Trying to then bind an INET udp channel to "::0" will fail,
        // so create "0..." address for the specific family
        else if (family == StandardProtocolFamily.INET)
            addr = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);
        else
            addr = new InetSocketAddress(InetAddress.getByName("::0"), port);
        try
        {
            udp.bind(addr);
        }
        catch (Exception ex)
        {
            // Add family and effective addr to stack trace
            throw new Exception("Cannot create UDP channel for " + family + " bound to " + addr, ex);
        }
        return udp;
    }

    /** Get a local multicast group address for IPv4 socket
     *
     *  IPv4 unicasts are re-sent as local multicast,
     *  and this configures a socket to receive them
     *
     *  @param udp UDP channel from which we plan to send multicast messages
     *  @param port Port to use
     *  @return Local multicast address, or <code>null</code> if no multicast support
     */
    public static AddressInfo getLocalMulticastGroup(final DatagramChannel udp, final int port)
    {
        try
        {
            if (! (udp.getLocalAddress() instanceof InetSocketAddress))
                throw new Exception("UDP socket is not bound");
            if (((InetSocketAddress)udp.getLocalAddress()).getAddress() instanceof Inet6Address)
                throw new Exception("Re-sending of unicast only used for legacy IPv4");

            final NetworkInterface loopback = getLoopback();
            if (loopback != null)
            {
                final InetAddress group = InetAddress.getByName(PVASettings.EPICS_PVA_MULTICAST_GROUP);
                final InetSocketAddress local_multicast = new InetSocketAddress(group, port);
                logger.log(Level.CONFIG, "Local multicast of IPv4 unicast using group " + local_multicast + " using network interface " + loopback.getDisplayName());

                udp.join(group, loopback);
                // Default is TRUE anyway?
                udp.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
                udp.setOption(StandardSocketOptions.IP_MULTICAST_IF, loopback);
                return new AddressInfo(false, local_multicast, 1, loopback);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot configure local IPv4 multicast support", ex);
        }
        return null;
    }
}
