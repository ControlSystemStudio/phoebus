/*******************************************************************************
 * Copyright (c) 2021 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.common;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.List;

import org.epics.pva.PVASettings;
import org.junit.Test;

/** Unit test of Network
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NetworkTest
{
    @Test
    public void testIPv4()
    {
        List<InetSocketAddress> addr = Network.parseAddresses("127.0.0.1");
        assertEquals("127.0.0.1", addr.get(0).getAddress().getHostAddress());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT, addr.get(0).getPort());

        addr = Network.parseAddresses("127.0.0.1:5086");
        assertEquals("127.0.0.1", addr.get(0).getAddress().getHostAddress());
        assertEquals(5086, addr.get(0).getPort());
    }

    @Test
    public void testIPv5()
    {
        List<InetSocketAddress> addr = Network.parseAddresses("[::1]");
        assertEquals("0:0:0:0:0:0:0:1", addr.get(0).getAddress().getHostAddress());
        assertEquals(PVASettings.EPICS_PVA_BROADCAST_PORT, addr.get(0).getPort());

        addr = Network.parseAddresses("[::1]:5086");
        assertEquals("0:0:0:0:0:0:0:1", addr.get(0).getAddress().getHostAddress());
        assertEquals(5086, addr.get(0).getPort());
    }
}
