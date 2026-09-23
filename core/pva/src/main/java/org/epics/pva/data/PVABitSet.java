/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.nio.ByteBuffer;
import java.util.BitSet;

/** Encode/decode {@link BitSet}
 *  @author Kay Kasemir
 */
public class PVABitSet
{
    /** @param bits BitSet
     *  @param buffer Target buffer
     */
    public static void encodeBitSet(final BitSet bits, final ByteBuffer buffer)
    {
        final byte[] bytes = bits.toByteArray();
        PVASize.encodeSize(bytes.length, buffer);
        buffer.put(bytes);
    }

    /** @param buffer Source buffer
     *  @return Decoded bits
     *  @throws Exception on error
     */
    public static BitSet decodeBitSet(final ByteBuffer buffer) throws Exception
    {
        final int size = PVASize.decodeSize(buffer);
        if (size < 0  ||  size > buffer.remaining())
            throw new Exception("Bitset size " + size + " with only " + buffer.remaining() + " bytes in buffer");
        final byte[] bytes = new byte[size];
        buffer.get(bytes);
        return BitSet.valueOf(bytes);
    }
}
