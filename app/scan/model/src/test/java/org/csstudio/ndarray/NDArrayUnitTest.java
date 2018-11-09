/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/** JUnit tests of the {@link NDArray}
 *  @author Kay Kasemir
 */
public class NDArrayUnitTest
{
    @Test
    public void testShape()
    {
        // Create flat array
        NDArray a = NDArray.create(new double[] { 1, 2, 3, 4, 5, 6 });
        assertEquals(6, a.getSize());
        assertEquals(1, a.getRank());
        System.out.println(a);

        // Reshape
        a = NDMatrix.reshape(a, 2, 3);
        assertEquals(6, a.getSize());
        assertEquals(2, a.getRank());
        assertEquals(2, a.getShape().getSize(0));
        assertEquals(3, a.getShape().getSize(1));
        System.out.println(a);

        // Start with shaped array
        a = NDArray.create(new double[][] { { 1, 2}, {3, 4}, {5, 6} });
        assertEquals(6, a.getSize());
        assertEquals(2, a.getRank());
        assertEquals(3, a.getShape().getSize(0));
        assertEquals(2, a.getShape().getSize(1));
        System.out.println(a);

        // Reshape to flat
        a = NDMatrix.reshape(a, 6);
        assertEquals(6, a.getSize());
        assertEquals(1, a.getRank());
        System.out.println(a);
    }

    @Test
    public void testTypes()
    {
        NDArray a = NDArray.create(new double[] { 1, 2, 3, 4, 5, 6 });
        System.out.println(a);
        assertSame(NDType.FLOAT64, a.getType());
        assertSame(NDType.FLOAT64, a.clone().getType());

        a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        System.out.println(a);
        assertSame(NDType.INT64, a.getType());
        assertSame(NDType.INT64, a.clone().getType());
        assertEquals(new NDShape(3, 2), a.getShape());

        a = NDArray.create(new byte[][][] { { {1}, {2}, {3} },
                                            { {4}, {5}, {6}} });
        System.out.println(a);
        assertSame(NDType.INT8, a.getType());
        assertSame(NDType.INT8, a.clone().getType());
        assertEquals(new NDShape(2, 3, 1), a.getShape());
    }

    @Test
    public void testEquals()
    {
        NDArray a = NDArray.create(new double[] { 1, 2, 3, 4 });
        NDArray b = NDArray.create(new double[] { 1, 2, 3, 4 });
        assertEquals(a, b);
        // Differ in value
        a.setDouble(42, 3);
        assertFalse(a.equals(b));

        a = NDArray.create(new int[][] { { 1, 2}, {3, 4} });
        b = NDArray.create(new int[] { 1, 2, 3, 4 });
        // Different shape
        assertFalse(a.equals(b));
        // Now equal in value and shape
        b = NDMatrix.reshape(b, 2, 2);
        assertEquals(a, b);

        a = NDArray.create(new short[] { 1, 2, 3, 4 });
        b = NDArray.create(new short[] { 1, 2, 3, 4 });
        assertNotSame(a, b);
        assertEquals(a, b);

        a = NDArray.create(new int[] { 1, 2, 3, 4 });
        a = NDMatrix.reshape(a, 2, 2);
        a = NDMatrix.transpose(a);
        b = NDArray.create(new int[][] { {1, 3}, {2, 4} });
        assertNotSame(a, b);
        assertEquals(a, b);
    }

    @Test
    public void testAccess()
    {
        final NDArray a = NDArray.create(new double[] { 1, 2, 3, 4, 5, 6 });
        assertEquals(4, a.getFlatDouble(3), 0.1);
        // negative index
        assertEquals(6, a.getDouble(-1), 0.1);

        final NDArray b = NDMatrix.reshape(a, 2, 3);
        assertEquals(4, b.getFlatDouble(3), 0.1);

        assertEquals(5, b.getFlatDouble(4), 0.1);
        a.setFlatDouble(4, 42);
        assertEquals(42, b.getFlatDouble(4), 0.1);
        assertEquals(42, b.getDouble(1, 1), 0.1);
    }

    @Test
    public void testCopy()
    {
        NDArray a = NDArray.create(new double[][] { { 1, 3, 5}, { 2, 4, 6} });
        assertEquals(new NDShape(2, 3), a.getShape());
        a = NDMatrix.transpose(a);

        NDArray b = a.clone();
        assertSame(NDType.FLOAT64, b.getType());

        // Arrays look the same
        assertEquals(a, b);

        // But have different strides
        // The copied version is 'simpler'
        assertEquals(new NDStrides(1, 3), a.getStrides());
        assertEquals(new NDStrides(2, 1), b.getStrides());
    }

    @Test
    public void testSlice()
    {
        // 0 1 2 4 5   Strides 5, 1
        // 5 6 7 8 9
        NDArray orig = NDMatrix.arange(0, 10, 1);

        // orig[2:9:3] ->  2 5 8
        NDArray slice = orig.getSlice(new int[] { 2 }, new int[] { 9 }, new int[] { 3 });
        assertEquals(new NDShape(3), slice.getShape());
        assertEquals(new NDStrides(3), slice.getStrides());
        System.out.println(slice);

        // Updating the slice changes the original data
        assertEquals(2.0, slice.getDouble(0), 0.01);
        assertEquals(5.0, slice.getDouble(1), 0.01);
        assertEquals(8.0, slice.getDouble(2), 0.01);
        slice.setDouble(42, 1);
        assertEquals(42.0, slice.getDouble(1), 0.01);
        assertEquals(42.0, orig.getDouble(5), 0.01);

        // Slice of re-shaped array
        NDArray shaped = NDMatrix.reshape(orig, 2, 5);
        assertEquals(new NDStrides(5, 1), shaped.getStrides());
        System.out.println(shaped);

        // shaped[:, ::2] = shaped[0:2:1, 0:5:2]
        // 0 2 4    Strides 5, 2
        slice = shaped.getSlice(new int[] { 0, 0 }, new int[] { 2, 5 }, new int[] { 1, 2 });
        assertEquals(new NDShape(2, 3), slice.getShape());
        assertEquals(new NDStrides(5, 2), slice.getStrides());
        System.out.println(slice);

        // Updating the slice changes the original data (via reshape & slice)
        assertEquals(7.0, slice.getDouble(1, 1), 0.01);
        slice.setDouble(666.0, 1, 1);
        System.out.println(orig);
        assertEquals(666.0, slice.getDouble(1, 1), 0.01);
        assertEquals(666.0, orig.getDouble(7), 0.01);

        // Shaped: [ [ [0, 1],
        //             [2, 3]],
        //
        //           [ [4, 5],
        //             [6, 7]]
        //         ]
        // shaped[1,1] = shaped[1, 1, 0:2:1]
        //  6, 7    Strides 1
        orig = NDMatrix.arange(0, 8, 1);
        shaped = NDMatrix.reshape(orig, 2, 2, 2);
        System.out.println(orig);
        slice = shaped.getSlice(new int[] { 1, 1, 0 }, new int[] { 0, 0, 2 }, new int[] { 0, 0, 1 });
        System.out.println(slice);
        assertEquals(new NDShape(2), slice.getShape());
        assertEquals(new NDStrides(1), slice.getStrides());
        assertEquals(7.0, slice.getDouble(1), 0.01);
        slice.setDouble(666.0, 1);
        assertEquals(666.0, slice.getDouble(1), 0.01);
        assertEquals(666.0, orig.getDouble(7), 0.01);

        // shaped[1, :, 1] shaped[1:2:1, 0:2:1, 1:2:1]
        //   [[[5],
        //     [666]]]
        slice = shaped.getSlice(new int[] { 1, 0, 1 }, new int[] { 2, 2, 2 }, new int[] { 1, 1, 1 });
        System.out.println(slice);
        assertEquals(new NDShape(1, 2, 1), slice.getShape());
        assertEquals(new NDStrides(4, 2, 1), slice.getStrides());

        //  Collapses into [ 5, 7 ] when fetched as 1, :, 1
        slice = shaped.getSlice(new int[] { 1, 0, 1 }, new int[] { 0, 2, 0 }, new int[] { 0, 1, 0 });
        System.out.println(slice);
        assertEquals(new NDShape(2), slice.getShape());
        assertEquals(new NDStrides(2), slice.getStrides());
    }

    @Test
    public void testFormatting()
    {
        final NDArray a = NDMatrix.arange(0, 6, 1);
        System.out.println(a);

        final NDArray b = NDMatrix.reshape(a, 3, 2);
        for (int i=0; i<3; ++i)
            for (int j=0; j<2; ++j)
                b.setDouble(i+j*0.1, i, j);
        System.out.println(b);

        final NDArray c = NDMatrix.reshape(NDMatrix.arange(0, 6, 1), 3, 2, 1);
        System.out.println(c);
    }
}
