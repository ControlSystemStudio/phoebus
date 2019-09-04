/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;

/** Encode/decode IP addresses
 *
 *  <p>Stored in 16 bytes
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAAddress
{
    public static void encode(final InetAddress address, final ByteBuffer buffer)
    {
        final int start = buffer.position();
        if (address instanceof Inet6Address)
            buffer.put(address.getAddress());   // always network byte order
        else if (address instanceof Inet4Address)
        {
            // IPv4 compatible IPv6 address
            // first 80-bit are 0
            buffer.putLong(0);
            buffer.putShort((short)0);
            // next 16-bits are 1
            buffer.putShort((short)0xFFFF);
            // following IPv4 address
            buffer.put(address.getAddress());   // always network byte order
        }
        else
            throw new RuntimeException("Cannot serialize network addresss " + address);
        final int len = buffer.position() - start;
        if (len != 16)
            throw new RuntimeException("Network address serialized as " + len + " bytes: " + address);
    }

    public static InetAddress decode(final ByteBuffer buffer) throws Exception
    {
        // 128-bit IPv6 address
        byte[] byteAddress = new byte[16];
        buffer.get(byteAddress);
        return InetAddress.getByAddress(byteAddress);
    }
}
