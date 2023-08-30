/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
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
    /** Empty Guid */
    public static final Guid EMPTY = new Guid(new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 });

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

    /** Set Guid from value
     *  @param value 12-byte Guid value
     */
    private Guid(final byte[] value)
    {
        if (value.length != guid.length)
            throw new IllegalArgumentException("Need 12, got " + guid.length + " bytes");
        System.arraycopy(value, 0, guid, 0, guid.length);
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

    /** @return GUID as "FE1A.." type text */
    public String asText()
    {
        final StringBuilder buf = new StringBuilder(35);
        for (byte b : guid)
        {
            final int i = Byte.toUnsignedInt(b);
            if (i < 16)
                buf.append('0');
            buf.append(Integer.toHexString(i).toUpperCase());
        }
        return buf.toString();
    }

    /** @return String representation "GUID 0xFE1A.." */
    @Override
    public String toString()
    {
        return "GUID 0x" + asText();
    }
}
