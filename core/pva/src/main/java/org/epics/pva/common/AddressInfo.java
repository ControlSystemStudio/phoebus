/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Objects;

/** Address info
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AddressInfo
{
    private final InetSocketAddress address;
    private final int ttl;
    private final NetworkInterface iface;

    /** @param address Network address and port
     *  @param ttl Time-to-live for UDP packets
     *  @param iface Interface via which to send/receive
     */
    public AddressInfo(final InetSocketAddress address, final int ttl, final NetworkInterface iface)
    {
        this.address = address;
        this.ttl = ttl;
        this.iface = iface;
    }

    /** @return Network address */
    public InetSocketAddress getAddress()
    {
        return address;
    }

    /** @return <code>true</code> if address appears to be an IPv4 broadcast address */
    public boolean isBroadcast()
    {
        if (! (address.getAddress() instanceof Inet4Address))
            return false;

        // How to determine bcast address?
        // Would depend on the netmask within the _target_ network,
        // which might not be our local subnet.
        // In most cases, however, a bcast address ends in 255,
        // so go by that
        final byte[] addr = address.getAddress().getAddress();
        return Byte.toUnsignedInt(addr[addr.length-1]) == 255;
    }

    /** @return Time-to-live for UDP packets */
    public int getTTL()
    {
        return ttl;
    }

    /** @return Interface via which to send/receive */
    public NetworkInterface getInterface()
    {
        return iface;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + address.hashCode();
        result = prime * result + ttl;
        if (iface != null)
            result = prime * result + iface.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (obj instanceof AddressInfo)
            return false;
        final AddressInfo other = (AddressInfo) obj;
        return address.equals(other.address)  &&
               ttl == other.ttl  &&
               Objects.equals(iface, other.iface);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("Address ").append(address.getHostString());
        if (address.getAddress().isLoopbackAddress())
            buf.append(" (LOOPBACK)");
        if (address.getAddress().isMulticastAddress())
            buf.append(" (MULTICAST)");
        if (isBroadcast())
            buf.append(" (BROADCAST)");
        buf.append(" port ").append(address.getPort());
        if (ttl > 0)
            buf.append(", TTL ").append(ttl);
        if (iface != null)
            buf.append(", interface ").append(iface.getName());

        return buf.toString();
    }
}
