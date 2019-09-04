/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.server;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/** Globally unique ID
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Guid
{
    // Random number generator in holder to defer initialization
    private static class Holder
    {
        static final SecureRandom random = new SecureRandom();
    }

    private final byte[] guid = new byte[12];

    /** Create random Guid */
    public Guid()
    {
        Holder.random.nextBytes(guid);
    }

    /** Read Guid from buffer
     *  @param buffer Buffer with 12-byte Guid
     */
    public Guid(final ByteBuffer buffer)
    {
        buffer.get(guid);
    }

    /** @param buffer Buffer into which to encode Guid */
    public void encode(final ByteBuffer buffer)
    {
        buffer.put(guid);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(guid);
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof Guid))
            return false;
        final Guid other = (Guid) obj;
        return Arrays.equals(guid, other.guid);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder(35);
        buf.append("GUID 0x");
        for (byte b : guid)
        {
            final int i = Byte.toUnsignedInt(b);
            if (i < 16)
                buf.append('0');
            buf.append(Integer.toHexString(i).toUpperCase());
        }
        return buf.toString();
    }
}
