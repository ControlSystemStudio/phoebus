/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

/** Array converters
 *  @author Kay Kasemir
 */
public class Convert
{
    /** @param array Original array
     *  @return Converted array
     */
    public static float[] toFloat(final double[] array)
    {
        final float[] cvt = new float[array.length];
        for (int i=0; i<cvt.length; ++i)
               cvt[i] = (float) array[i];
        return cvt;
    }

    /** @param array Original array
     *  @return Converted array
     */
    public static long[] toLong(final double[] array)
    {
        final long[] cvt = new long[array.length];
        for (int i=0; i<cvt.length; ++i)
               cvt[i] = (long) array[i];
        return cvt;
    }

    /** @param array Original array
     *  @return Converted array
     */
    public static int[] toInt(final double[] array)
    {
        final int[] cvt = new int[array.length];
        for (int i=0; i<cvt.length; ++i)
               cvt[i] = (int) array[i];
        return cvt;
    }

    /** @param array Original array
     *  @return Converted array
     */
    public static short[] toShort(final double[] array)
    {
        final short[] cvt = new short[array.length];
        for (int i=0; i<cvt.length; ++i)
               cvt[i] = (short) array[i];
        return cvt;
    }

    /** @param array Original array
     *  @return Converted array
     */
    public static byte[] toByte(final double[] array)
    {
        final byte[] cvt = new byte[array.length];
        for (int i=0; i<cvt.length; ++i)
               cvt[i] = (byte) array[i];
        return cvt;
    }
}
