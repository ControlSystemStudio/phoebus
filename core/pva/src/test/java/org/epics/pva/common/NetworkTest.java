/*******************************************************************************
 * Copyright (c) 2021-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.util.List;
import java.util.stream.Collectors;

import org.epics.pva.PVASettings;
import org.junit.Test;

/** Unit test of Network helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NetworkTest
{
    // If running on a host that does not support IPv6,
    // ignore the checks that require a local "::1" IPv6 address
    private static boolean ignore_local_ipv6 = Boolean.parseBoolean(System.getProperty("ignore_local_ipv6"));

    @Test
    public void testBroadcastAddresses() throws Exception
    {
        for (AddressInfo info : Network.getBroadcastAddresses(9876))
            System.out.println("Broadcast " + info);
    }

    @Test
    public void testIPv4() throws Exception
    {
        AddressInfo addr = Network.parseAddress("127.0.0.1", PVASettings.EPICS_PVA_BROADCAST_PORT);
        System.out.println(addr);
        assertEquals("127.0.0.1", addr.getAddress().getHostString());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());

        addr = Network.parseAddress("127.0.0.1:5086", PVASettings.EPICS_PVA_BROADCAST_PORT);
        System.out.println(addr);
        assertEquals("127.0.0.1", addr.getAddress().getHostString());
        assertEquals(5086, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());


        addr = Network.parseAddress("224.0.2.3@127.0.0.1", PVASettings.EPICS_PVA_BROADCAST_PORT);
        System.out.println(addr);
        assertEquals("224.0.2.3", addr.getAddress().getHostString());
        assertTrue(addr.getAddress().getAddress().isMulticastAddress());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT, addr.getAddress().getPort());
        final String iface_addr = addr.getInterface()
                                      .inetAddresses()
                                      .map(iface -> iface.getHostAddress())
                                      .collect(Collectors.joining(", "));
        System.out.println("Interface addresses: " + iface_addr);
        assertTrue(iface_addr.contains("127.0.0.1"));
        assertFalse(addr.isBroadcast());

        addr = Network.parseAddress("127.0.0.255", PVASettings.EPICS_PVA_BROADCAST_PORT);
        System.out.println(addr);
        assertEquals("127.0.0.255", addr.getAddress().getHostString());
        assertTrue(addr.isBroadcast());
    }

    @Test
    public void testIPv6() throws Exception
    {
        AddressInfo addr = Network.parseAddress("[::1]", PVASettings.EPICS_PVA_BROADCAST_PORT);
        System.out.println(addr);
        assertEquals("0:0:0:0:0:0:0:1", addr.getAddress().getHostString());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());

        addr = Network.parseAddress("::1", 9876);
        System.out.println(addr);
        assertEquals("0:0:0:0:0:0:0:1", addr.getAddress().getHostString());
        assertEquals(9876, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());

        addr = Network.parseAddress("[::1]:5086", PVASettings.EPICS_PVA_BROADCAST_PORT+2);
        System.out.println(addr);
        assertEquals("0:0:0:0:0:0:0:1", addr.getAddress().getHostString());
        assertEquals(5086, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());

        addr = Network.parseAddress("ff02::42:1", PVASettings.EPICS_PVA_BROADCAST_PORT+3);
        System.out.println(addr);
        assertEquals("ff02:0:0:0:0:0:42:1", addr.getAddress().getHostString());
        assertTrue(addr.getAddress().getAddress().isMulticastAddress());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT+3, addr.getAddress().getPort());
        assertFalse(addr.isBroadcast());

        final NetworkInterface lo = Network.getLoopback();
        if (lo == null)
            System.out.println("Skipping loopback test");
        else
        {
            addr = Network.parseAddress("[ff02::42:1]:5099@" + lo.getDisplayName(), PVASettings.EPICS_PVA_BROADCAST_PORT);
            System.out.println(addr);
            assertEquals("ff02:0:0:0:0:0:42:1", addr.getAddress().getHostString());
            assertTrue(addr.getAddress().getAddress().isMulticastAddress());
            assertEquals(5099, addr.getAddress().getPort());
            assertEquals(lo, addr.getInterface());
            final String iface_addr = addr.getInterface()
                                          .inetAddresses()
                                          .map(iface -> iface.getHostAddress())
                                          .collect(Collectors.joining(", "));
            System.out.println("Interface addresses: " + iface_addr);
            if (! ignore_local_ipv6)
                assertTrue(iface_addr.contains("0:0:0:0:0:0:0:1"));
            assertFalse(addr.isBroadcast());
        }
    }

    @Test
    public void testAddressList() throws Exception
    {
        final String spec = "0.0.0.0 :: 224.0.1.1,1@127.0.0.1 [ff02::42:1],1@::1";
        System.out.println("Result of parsing '" + spec + "':");
        final List<AddressInfo> infos = Network.parseAddresses(spec, PVASettings.EPICS_PVA_BROADCAST_PORT);
        for (AddressInfo info : infos)
            System.out.println(info);
        if (! ignore_local_ipv6)
            assertEquals(4, infos.size());

        assertTrue(infos.get(0).getAddress().getAddress() instanceof Inet4Address);
        assertTrue(infos.get(1).getAddress().getAddress() instanceof Inet6Address);
        assertTrue(infos.get(2).getAddress().getAddress() instanceof Inet4Address);

        assertTrue(((Inet4Address)infos.get(2).getAddress().getAddress()).isMulticastAddress());

        assertTrue(infos.get(2).getInterface() != null);

        if (! ignore_local_ipv6)
        {
             assertTrue(infos.get(3).getAddress().getAddress() instanceof Inet6Address);
             assertTrue(((Inet6Address)infos.get(3).getAddress().getAddress()).isMulticastAddress());
             assertTrue(infos.get(3).getInterface() != null);
        }
    }
}
