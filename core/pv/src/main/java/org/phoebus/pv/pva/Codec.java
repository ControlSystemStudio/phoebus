/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVAShortArray;

/** PVA NDArray compression codec
 *  @author Kay Kasemir
 */
abstract public class Codec
{
    /** Decode value
     *
     *  @param value Encoded value bytes
     *  @param orig_data_type Original data type
     *  @param width Width of decoded image
     *  @param height Height of decoded image
     *  @return Array data of decoded image
     *  @throws Exception on error
     */
    public PVAData decode(final PVAByteArray value, final int orig_data_type,
                          final int width, final int height) throws Exception
    {
        final byte[] compressed = ((PVAByteArray)value).get();
        switch (orig_data_type)
        {
        case 1: // byte
        {
            final byte[] expanded = decode(compressed,   width*height);
            System.out.println("-> INT8");
            return new PVAByteArray(value.getName(), false, expanded);
        }
        case 5: // ubyte
        {
            final byte[] expanded = decode(compressed,   width*height);
            System.out.println("-> UINT8");
            return new PVAByteArray(value.getName(), true, expanded);
        }
        case 2: // short
        {
            final byte[] expanded = decode(compressed, 2*width*height);
            final ByteBuffer cvt = ByteBuffer.wrap(expanded);
            cvt.order(ByteOrder.LITTLE_ENDIAN);
            final short[] shorts = new short[width*height];
            for (int i=0; i<shorts.length; ++i)
                shorts[i] = cvt.getShort();
            return new PVAShortArray(value.getName(), false, shorts);
        }
        case 6: // ushort
        {
            final byte[] expanded = decode(compressed, 2*width*height);
            final ByteBuffer cvt = ByteBuffer.wrap(expanded);
            cvt.order(ByteOrder.LITTLE_ENDIAN);
            final short[] shorts = new short[width*height];
            for (int i=0; i<shorts.length; ++i)
                shorts[i] = cvt.getShort();
            return new PVAShortArray(value.getName(), true, shorts);
        }
        case 3: // int
        case 4: // long
        case 7: // uint
        case 8: // ulong
        case 9: // float
        case 10: // double
        default:
            System.out.println("Cannot decode into orig data type  " + orig_data_type);
        }

        return value;
    }


    /** De-compress byte array
     *
     *  @param data Compressed data
     *  @param decompressed_size Expected de-compressed size in bytes
     *  @return Decompressed data
     *  @throws Exception on error
     */
    abstract public byte[] decode(byte[] data, final int decompressed_size) throws Exception;
}
