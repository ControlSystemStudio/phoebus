/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import static org.phoebus.pv.PV.logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;

import org.epics.pva.data.PVAByteArray;
import org.epics.pva.data.PVAData;
import org.epics.pva.data.PVADoubleArray;
import org.epics.pva.data.PVAFloatArray;
import org.epics.pva.data.PVAIntArray;
import org.epics.pva.data.PVALongArray;
import org.epics.pva.data.PVAShortArray;

/** PVA NDArray compression codec
 *
 *  <p>Base for all compression codecs.
 *
 *  @author Kay Kasemir
 */
abstract public class Codec
{
    /** Bytes per sample for the various `orig_data_type` */
    private static final int[] BYTES_PER_SAMPLE =
    {
        0,
        Byte.BYTES,     //  1 byte
        Short.BYTES,    //  2 int16
        Integer.BYTES,  //  3 int32
        Long.BYTES,     //  4 int64
        Byte.BYTES,     //  5 ubyte
        Short.BYTES,    //  6 uint16
        Integer.BYTES,  //  7 uint32
        Long.BYTES,     //  8 uint64
        Float.BYTES,    //  9 float
        Double.BYTES    // 10 double
    };

    /** De-compress value
     *
     *  @param value Value with compressed value
     *  @param orig_data_type Original data type ID
     *  @param value_count Value count of decoded data, i.e. number of 'int' or 'long', not byte size
     *  @return Array data of decoded image
     *  @throws Exception on error
     */
    public PVAData decompress(final PVAByteArray value, final int orig_data_type,
                              final int value_count) throws Exception
    {
        final boolean unsigned = orig_data_type >= 5  &&  orig_data_type <= 8;
        final byte[] compressed = ((PVAByteArray)value).get();
        final byte[] expanded = decompress(compressed, BYTES_PER_SAMPLE[orig_data_type] * value_count);
        logger.log(Level.FINE, () -> "Decompressed " + compressed.length + " into " + expanded.length + " bytes");

        // byte, ubyte: Done!
        if (orig_data_type == 1  ||  orig_data_type == 5)
            return new PVAByteArray(unsigned ? "ubyteValue" : "byteValue", unsigned, expanded);

        // Need to 'cast' the expanded data from byte[] to orig_data_type[].
        // In C/C++, that's easy without copying the data:
        //   return new PVAIntArray("intValue", unsigned, (int *) expanded);
        // For Java, these similar looking constructs compile, but result in runtime errors:
        final ByteBuffer cvt = ByteBuffer.wrap(expanded);
        //   int[] ints = (short []) (Object) expanded;
        //   int[] ints = cvt.asIntBuffer().array();
        //
        // cvt.getInt() as used below eventually calls Unsafe.getInt(..).
        // There are per-element access routines for each data type in Unsafe,
        // but couldn't find a general array cast operation.
        // There are Unsafe.copyMemoryXX routines that might save a little time over
        // a per-element copy, but they still result in a full copy of the data,
        // not saving any memory.
        // Besides, the array elements might need to be swapped to the correct endian,
        // so going with the ByteBuffer.getXXX() calls which handle byte order.

        // Unclear what byte order the data will be.
        // This worked in tests with X86_64 on Linux,
        // but byte order depends on server which populates the data
        // and NTNDArray doesn't include any hint
        cvt.order(ByteOrder.LITTLE_ENDIAN);

        switch (orig_data_type)
        {
        case 2: // short
        case 6: // ushort
            final short[] shorts = new short[value_count];
            for (int i=0; i<shorts.length; ++i)
                shorts[i] = cvt.getShort();
            return new PVAShortArray(unsigned ? "ushortValue" : "shortValue", unsigned, shorts);

        case 3: // int
        case 7: // uint
            final int[] ints = new int[value_count];
            for (int i=0; i<ints.length; ++i)
                ints[i] = cvt.getInt();
            return new PVAIntArray(unsigned ? "uintValue" : "intValue", unsigned, ints);

        case 4: // long
        case 8: // ulong
            final long[] longs = new long[value_count];
            for (int i=0; i<longs.length; ++i)
                longs[i] = cvt.getLong();
            return new PVALongArray(unsigned ? "ulongValue" : "longValue", unsigned, longs);

        case 9: // float
            final float[] floats = new float[value_count];
            for (int i=0; i<floats.length; ++i)
                floats[i] = cvt.getFloat();
            return new PVAFloatArray("floatValue", floats);

        case 10: // double
            final double[] doubles = new double[value_count];
            for (int i=0; i<doubles.length; ++i)
                doubles[i] = cvt.getDouble();
            return new PVADoubleArray("doubleValue", doubles);

        default:
            throw new Exception("Cannot decode compressed data for orig data type  " + orig_data_type);
        }
    }

    /** De-compress byte array
     *
     *  @param data Compressed data
     *  @param decompressed_size Expected de-compressed size in bytes
     *  @return De-compressed data
     *  @throws Exception on error
     */
    abstract public byte[] decompress(byte[] data, final int decompressed_size) throws Exception;
}
