/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;

/** Encode/decode PVA size
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVASize
{
    // Spec:
    // Size less than 255 encoded as a single byte containing an unsigned 8-bit integer.
    // Less than 2^31-1 encoded as byte 255, followed by a positive signed 32-bit integer
    // Else encoded as byte 255, positive signed 32-bit integer with value 2^31-1, positive signed 64-bit integer
    //
    // This is the reality from Java and CPP pvData.SerializeHelper, which uses byte -2 == 254

    /** @param size Size to encode
     *  @return Bytes used for the encoded size
     */
    public static int size(final int size)
    {
        if (size == -1)
            return 1;
        else if (size < 254)
            return 1;
        else
            return 1 + 4;
    }

    public static final void encodeSize(final int size, final ByteBuffer buffer)
    {
        if (size == -1)
            buffer.put((byte)-1);
        else if (size < 254)
            buffer.put((byte)size);
        else
            // UByte 254
            buffer.put((byte)-2).putInt(size);
    }

    public static final int decodeSize(final ByteBuffer buffer)
    {
        byte b = buffer.get();
        if (b == -1)
            return -1;
        else if (b == -2)
        {
            int size = buffer.getInt();
            if (size < 0)
                throw new RuntimeException("Negative array size " + size);
            return size;
        }
        else
            return Byte.toUnsignedInt(b);
    }
}
