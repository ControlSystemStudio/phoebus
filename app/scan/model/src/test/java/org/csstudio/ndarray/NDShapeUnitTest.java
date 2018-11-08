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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JUnit tests of the {@link NDShape}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDShapeUnitTest
{
    @Test
    public void testComparisons()
    {
        NDShape a = new NDShape(6);
        NDShape b = new NDShape(6);
        System.out.println(a + " compatible with " + b + " ? " + a.isBroadcastCompatible(b));
        System.out.println(a + " has same end as " + b + " ? " + NDShape.haveEqualEnds(a, b));
        System.out.println(a + " is equal to     " + b + " ? " + a.equals(b));
        assertTrue(a.isBroadcastCompatible(b));
        assertTrue(NDShape.haveEqualEnds(a, b));
        assertEquals(a, b);
        assertEquals(a, NDShape.combine(a, b));

        b = new NDShape(6, 1);
        System.out.println(a + " compatible with " + b + " ? " + a.isBroadcastCompatible(b));
        System.out.println(a + " has same end as " + b + " ? " + NDShape.haveEqualEnds(a, b));
        System.out.println(a + " is equal to     " + b + " ? " + a.equals(b));
        assertTrue(a.isBroadcastCompatible(b));
        assertFalse(NDShape.haveEqualEnds(a, b));
        assertFalse(a.equals(b));
        assertNull(NDShape.combine(a, b));

        b = new NDShape(3, 2);
        System.out.println(a + " compatible with " + b + " ? " + a.isBroadcastCompatible(b));
        System.out.println(a + " has same end as " + b + " ? " + NDShape.haveEqualEnds(a, b));
        System.out.println(a + " is equal to     " + b + " ? " + a.equals(b));
        assertFalse(a.isBroadcastCompatible(b));
        assertFalse(NDShape.haveEqualEnds(a, b));
        assertFalse(a.equals(b));
        assertNull(NDShape.combine(a, b));

        a = new NDShape(6, 6, 3, 2);
        System.out.println(a + " compatible with " + b + " ? " + a.isBroadcastCompatible(b));
        System.out.println(a + " has same end as " + b + " ? " + NDShape.haveEqualEnds(a, b));
        System.out.println(a + " is equal to     " + b + " ? " + a.equals(b));
        assertTrue(a.isBroadcastCompatible(b));
        assertTrue(NDShape.haveEqualEnds(a, b));
        assertFalse(a.equals(b));
        assertEquals(a, NDShape.combine(a, b));

        b = new NDShape(1, 1);
        System.out.println(a + " compatible with " + b + " ? " + a.isBroadcastCompatible(b));
        System.out.println(a + " has same end as " + b + " ? " + NDShape.haveEqualEnds(a, b));
        System.out.println(a + " is equal to     " + b + " ? " + a.equals(b));
        assertTrue(a.isBroadcastCompatible(b));
        assertFalse(NDShape.haveEqualEnds(a, b));
        assertFalse(a.equals(b));
        assertNull(NDShape.combine(a, b));
    }

    @Test
    public void testIterator()
    {
        final NDShape s = new NDShape(3, 2);
        final ShapeIterator iter = s.iterator();
        for (int i=0; i<3; ++i)
            for (int j=0; j<2; ++j)
            {
                assertTrue(iter.hasNext());
                final int[] pos = iter.getPosition();
                assertEquals(2, pos.length);
                assertEquals(i, pos[0]);
                assertEquals(j, pos[1]);
            }
        // Iteration ends
        assertFalse(iter.hasNext());
        // .. and wraps around
        assertTrue(iter.hasNext());
        final int[] pos = iter.getPosition();
        assertEquals(0, pos[0]);
        assertEquals(1, pos[1]);
    }
}
