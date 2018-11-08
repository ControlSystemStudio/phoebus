/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

/** JUnit tests of the {@link BroadcastIterator}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DSBroadcastUnitTest
{
    @Test
    public void testBroadcastShape()
    {
        BroadcastIterator b = new BroadcastIterator(new NDShape(6), new NDShape(6));
        assertEquals(new NDShape(6), b.getBroadcastShape());

        b = new BroadcastIterator(new NDShape(6, 1), new NDShape(6, 1));
        assertEquals(new NDShape(6, 1), b.getBroadcastShape());

        b = new BroadcastIterator(new NDShape(6), new NDShape(6, 1));
        assertEquals(new NDShape(6, 6), b.getBroadcastShape());

        b = new BroadcastIterator(new NDShape(8, 1, 6, 1), new NDShape(7, 1, 5));
        assertEquals(new NDShape(8, 7, 6, 5), b.getBroadcastShape());

        try
        {
            b = new BroadcastIterator(new NDShape(6), new NDShape(3));
            fail("Should be incompatible");
        }
        catch (IllegalArgumentException ex)
        {
            assertTrue(ex.getMessage().contains("not compatible"));
        }
    }

    @Test
    public void testBroadcastIterator()
    {
        NDShape a = new NDShape(2, 2);
        NDShape b = new NDShape(2);
        System.out.println("Broadcast " + a + " x " + b);
        BroadcastIterator iter = new BroadcastIterator(a, b);

        for (int i=0; i<2; ++i)
            for (int j=0; j<2; ++j)
            {
                assertTrue(iter.hasNext());

                System.out.println(
                    Arrays.toString(iter.getPosition()) + " <- " +
                    Arrays.toString(iter.getPosA())  +  " x " +
                    Arrays.toString(iter.getPosB()));

                assertArrayEquals(new int[] { i, j }, iter.getPosition());
                assertArrayEquals(new int[] { i, j }, iter.getPosA());
                assertArrayEquals(new int[] { j }, iter.getPosB());
            }

        a = new NDShape(6);
        b = new NDShape(6, 1);
        System.out.println("\nBroadcast " + a + " x " + b);
        iter = new BroadcastIterator(a, b);

        for (int i=0; i<6; ++i)
            for (int j=0; j<6; ++j)
            {
                assertTrue(iter.hasNext());

                System.out.println(
                    Arrays.toString(iter.getPosition()) + " <- " +
                    Arrays.toString(iter.getPosA())  +  " x " +
                    Arrays.toString(iter.getPosB()));

                assertArrayEquals(new int[] { i, j }, iter.getPosition());
                assertArrayEquals(new int[] { j }, iter.getPosA());
                assertArrayEquals(new int[] { i, 0 }, iter.getPosB());
            }

        a = new NDShape(6, 1);
        b = new NDShape(6);
        System.out.println("\nBroadcast " + a + " x " + b);
        iter = new BroadcastIterator(a, b);

        for (int i=0; i<6; ++i)
            for (int j=0; j<6; ++j)
            {
                assertTrue(iter.hasNext());

                System.out.println(
                    Arrays.toString(iter.getPosition()) + " <- " +
                    Arrays.toString(iter.getPosA())  +  " x " +
                    Arrays.toString(iter.getPosB()));

                assertArrayEquals(new int[] { i, j }, iter.getPosition());
                assertArrayEquals(new int[] { i, 0 }, iter.getPosA());
                assertArrayEquals(new int[] { j }, iter.getPosB());
            }
    }

    /** Broadcast iteration is _way_ slower than flat iteration,
     *  but sometimes necessary
     *
     *  Example results:
     *  Broadcast iteration: 3000 ms
     *  Shape iteration:      240 ms
     *  Flat iteration:         4 ms
     */
    @Test
    public void testBroadcastPerformance() throws Exception
    {
        final int size = 10000;
        final NDShape a = new NDShape(size, size);
        final NDShape b = new NDShape(size, size);
        final int flat_size = a.getSize();
        final BroadcastIterator iter = new BroadcastIterator(a, b);

        // In this case broadcasting isn't necessary, but point here is to compare
        assertEquals(a, b);
        assertEquals(a, iter.getBroadcastShape());
        assertEquals(size * size, flat_size);

        // Measure broadcast iteration
        long start = System.currentTimeMillis();
        for (int i=0; i<size; ++i)
            for (int j=0; j<size; ++j)
            {
                if (!iter.hasNext())
                    throw new Exception();
            }
        if (iter.hasNext())
            throw new Exception();
        long end = System.currentTimeMillis();
        System.out.println("Broadcast iteration: " + (end - start) + " ms");

        // Measure shape iterator
        final ShapeIterator shape_iter = new ShapeIterator(a);
        start = System.currentTimeMillis();
        for (int i=0; i<flat_size; ++i)
            if (!shape_iter.hasNext())
                throw new Exception();
        if (shape_iter.hasNext())
            throw new Exception();
        end = System.currentTimeMillis();
        System.out.println("Shape iteration: " + (end - start) + " ms");

        // Measure flat iteration of elements
        start = System.currentTimeMillis();
        for (int i=0; i<flat_size; ++i)
            if (i > flat_size)
                throw new Exception("Trouble at mill");
        end = System.currentTimeMillis();
        System.out.println("Flat iteration: " + (end - start) + " ms");
    }
}
