/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JUnit tests of the {@link NDMath}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDMathUnitTest
{
    @Test
    public void testTypes()
    {
        NDArray a = NDArray.create(new double[] { 1 });
        NDArray b = NDArray.create(new double[] { 1 });
        NDArray r = NDMath.add(a, b);
        assertSame(NDType.FLOAT64, r.getType());

        a = NDArray.create(new double[] { 1 });
        b = NDArray.create(new float[] { 1 });
        r = NDMath.add(a, b);
        assertSame(NDType.FLOAT64, r.getType());

        a = NDArray.create(new float[] { 1 });
        b = NDArray.create(new float[] { 1 });
        r = NDMath.add(a, b);
        assertSame(NDType.FLOAT32, r.getType());

        a = NDArray.create(new int[] { 1 });
        b = NDArray.create(new byte[] { 1 });
        r = NDMath.add(a, b);
        assertSame(NDType.INT64, r.getType());

        a = NDArray.create(new byte[] { 1 });
        b = NDArray.create(new byte[] { 1 });
        r = NDMath.add(a, b);
        assertSame(NDType.INT8, r.getType());
    }

    @Test
    public void testSum()
    {
        final NDArray a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        assertEquals(21, NDMath.sum(a), 0.001);
    }

    @Test
    public void testNegative()
    {
        final NDArray a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        NDMath.negative(a);
        assertEquals(-21, NDMath.sum(a), 0.001);
    }

    @Test
    public void testIncrement()
    {
        // Increment by number
        NDArray a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        NDMath.increment(a, 1);
        for (int i=0; i<a.getSize(); ++i)
            assertEquals(i+2, a.getFlatDouble(i), 0.001);

        // Increment by array of same shape
        a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        NDMath.increment(a, a);
        for (int i=0; i<a.getSize(); ++i)
            assertEquals(2*(i+1), a.getFlatDouble(i), 0.001);

        // Increment by broadcasting { -1, -2 }
        a = NDArray.create(new int[][] { { 1, 2 }, { 3, 4 }, { 5, 6 } });
        final NDArray b = NDArray.create(new int[] { -1, -2 });
        System.out.println(a + " incremented by " + b);
        NDMath.increment(a, b);
        System.out.println("= " + a);
        assertEquals(NDArray.create(new int[][] { { 0, 0 }, { 2, 2 }, { 4, 4 } }), a);
    }

    /** If the shapes have equal ends, allowing simple iteration
     *  over the flat array (with wrap-around for the smaller array),
     *  that's at least 3 times faster:
     *  Broadcasting: 431 ms per run
     *  Flat, wrap-around: 109 ms per run
     */
    @Test
    public void compareAccess()
    {
        final int runs = 10;
        // size x size matrix where each element itself is [2]
        final int size = 2000;
        NDArray a = NDMatrix.zeros(NDType.INT32, new NDShape(size, size, 2));
        for (int row=0; row<size; ++row)
            for (int col=0; col<size; ++col)
                for (int i=0; i<2; ++i)
                    a.setDouble(100*(row+1) + 10*(col+1), row, col, i);
        NDArray b = NDArray.create(new short[] { 1, 2 });
        NDArray r = null;
        final NDType type = NDType.determineSuperType(a.getType(), b.getType());

        // Use broadcasting
        assertTrue(a.getShape().isBroadcastCompatible(b.getShape()));
        long start = System.currentTimeMillis();
        for (int run=0; run<runs; ++run)
        {
            final BroadcastIterator i = new BroadcastIterator(a.getShape(), b.getShape());
            r = new NDArray(type, i.getBroadcastShape());
            while (i.hasNext())
            {
                final double sum = a.getDouble(i.getPosA()) + b.getDouble(i.getPosB());
                r.setDouble(sum, i.getPosition());
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("Broadcasting: " + (end - start)/runs + " ms per run");

        for (int row=0; row<size; ++row)
            for (int col=0; col<size; ++col)
                for (int e=0; e<2; ++e)
                    assertEquals(100*(row+1) + 10*(col+1) + (e+1), r.getDouble(row, col, e), 0.001);


        // Use flat, wrap-around iteration
        assertTrue(NDShape.haveEqualEnds(a.getShape(), b.getShape()));
        start = System.currentTimeMillis();
        for (int run=0; run<runs; ++run)
        {
            r = new NDArray(type, NDShape.combine(a.getShape(), b.getShape()));
            final int size_r = r.getSize();
            final int size_a = a.getSize();
            final int size_b = b.getSize();
            for (int i=0; i<size_r; ++i)
            {
                final double sum = a.getFlatDouble(i % size_a) + b.getFlatDouble(i % size_b);
                r.setFlatDouble(i, sum);
            }
        }
        end = System.currentTimeMillis();
        System.out.println("Flat, wrap-around: " + (end - start)/runs + " ms per run");

        for (int row=0; row<size; ++row)
            for (int col=0; col<size; ++col)
                for (int e=0; e<2; ++e)
                    assertEquals(100*(row+1) + 10*(col+1) + (e+1), r.getDouble(row, col, e), 0.001);
    }

    @Test
    public void testAddSubMulDiv()
    {
        // Two line vectors
        NDArray a = NDMatrix.arange(0, 6, 1, NDType.FLOAT64);
        NDArray b = NDMatrix.arange(0, 6, 1);
        NDArray r = NDMath.add(a, b);
        System.out.println("Linear " + a + " + " + b + " = " + r);
        for (int i=0; i<6; ++i)
            assertEquals(2*i, r.getDouble(i), 0.001);

        r = NDMath.subtract(a, b);
        System.out.println("Linear " + a + " - " + b + " = " + r);
        for (int i=0; i<6; ++i)
            assertEquals(0, r.getDouble(i), 0.001);

        r = NDMath.multiply(a, b);
        System.out.println("Linear " + a + " * " + b + " = " + r);
        for (int i=0; i<6; ++i)
            assertEquals(i*i, r.getDouble(i), 0.001);

        r = NDMath.divide(a, b);
        System.out.println("Linear " + a + " / " + b + " = " + r);
        for (int i=0; i<6; ++i)
            assertEquals(i == 0 ? Double.NaN : 1.0, r.getDouble(i), 0.001);


        // Row and column, requiring numpy-type broadcasting
        b = NDMatrix.reshape(b, 6, 1);
        r = NDMath.add(a, b);
        System.out.println("\nBroadcast \n" + a + " +\n" + b + " =\n" + r);
        for (int row=0; row<6; ++row)
            for (int col=0; col<6; ++col)
                assertEquals(row + col, r.getDouble(row, col), 0.001);

        r = NDMath.subtract(a, b);
        System.out.println("\nBroadcast \n" + a + " -\n" + b + " =\n" + r);
        for (int row=0; row<6; ++row)
            for (int col=0; col<6; ++col)
                assertEquals(col - row, r.getDouble(row, col), 0.001);

        r = NDMath.multiply(a, b);
        System.out.println("\nBroadcast \n" + a + " *\n" + b + " =\n" + r);
        for (int row=0; row<6; ++row)
            for (int col=0; col<6; ++col)
                assertEquals(col * row, r.getDouble(row, col), 0.001);

        r = NDMath.divide(a, b);
        System.out.println("\nBroadcast \n" + a + " /\n" + b + " =\n" + r);
        for (int row=0; row<6; ++row)
            for (int col=0; col<6; ++col)
                assertEquals((double)col / row, r.getDouble(row, col), 0.001);


        // 3x3 matrix where each element itself is [2]
        a = NDMatrix.zeros(NDType.INT32, new NDShape(3, 3, 2));
        for (int row=0; row<3; ++row)
            for (int col=0; col<3; ++col)
                for (int i=0; i<2; ++i)
                    a.setDouble(100*(row+1) + 10*(col+1), row, col, i);
        b = NDArray.create(new int[] { 1, 2 });
        r = NDMath.add(a, b);
        System.out.println("\nBroadcast sum of\n" + a + " +\n" + b + " =\n" + r);
        for (int row=0; row<3; ++row)
            for (int col=0; col<3; ++col)
                for (int i=0; i<2; ++i)
                    assertEquals(100*(row+1) + 10*(col+1) + (i+1), r.getDouble(row, col, i), 0.001);

        r = NDMath.subtract(a, b);
        System.out.println("\nBroadcast diff\n" + a + " -\n" + b + " =\n" + r);
        for (int row=0; row<3; ++row)
            for (int col=0; col<3; ++col)
                for (int i=0; i<2; ++i)
                    assertEquals(100*(row+1) + 10*(col+1) - (i+1), r.getDouble(row, col, i), 0.001);

        r = NDMath.multiply(a, b);
        System.out.println("\nBroadcast mul\n" + a + " *\n" + b + " =\n" + r);
        for (int row=0; row<3; ++row)
            for (int col=0; col<3; ++col)
                for (int i=0; i<2; ++i)
                    assertEquals((i+1) * (100*(row+1) + 10*(col+1)), r.getDouble(row, col, i), 0.001);

        r = NDMath.divide(a, b);
        System.out.println("\nBroadcast div\n" + a + " /\n" + b + " =\n" + r);
        for (int row=0; row<3; ++row)
            for (int col=0; col<3; ++col)
                for (int i=0; i<2; ++i)
                    assertEquals((100*(row+1) + 10*(col+1)) / (i+1), r.getDouble(row, col, i), 0.001);
    }

    @Test
    public void testDivision()
    {
        NDArray r = NDMath.divide(NDArray.create(new int[] { 1 }), NDArray.create(new int[] { 1 }));
        assertEquals(1.0, r.getDouble(0), 0.001);

        r = NDMath.divide(NDArray.create(new int[] { 0 }), NDArray.create(new int[] { 1 }));
        assertEquals(0.0, r.getDouble(0), 0.001);

        r = NDMath.divide(NDArray.create(new int[] { 0 }), NDArray.create(new int[] { 0 }));
        assertEquals(0.0, r.getDouble(0), 0.001);

        // 1/0 and -1/0 would also return 0 for integer numbers in NumPy...
        r = NDMath.divide(NDArray.create(new int[] { 1 }), NDArray.create(new int[] { 0 }));
        assertEquals(Long.MAX_VALUE, r.getDouble(0), 1);

        r = NDMath.divide(NDArray.create(new int[] { -1 }), NDArray.create(new int[] { 0 }));
        assertEquals(-Long.MAX_VALUE, r.getDouble(0), 1);

        // Floating point behavior matches NumPy
        r = NDMath.divide(NDArray.create(new double[] { 0 }), NDArray.create(new int[] { 0 }));
        assertTrue(Double.isNaN(r.getDouble(0)));

        r = NDMath.divide(NDArray.create(new double[] { 1 }), NDArray.create(new int[] { 0 }));
        assertTrue(r.getDouble(0) == Double.POSITIVE_INFINITY);

        r = NDMath.divide(NDArray.create(new double[] { -1 }), NDArray.create(new int[] { 0 }));
        assertTrue(r.getDouble(0) == Double.NEGATIVE_INFINITY);
    }
}
