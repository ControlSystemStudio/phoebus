/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** JUnit test of {@link NDCompatibility}
 *  @author Kay Kasemir
 */
public class NDCompatibilityUnitTest
{
    @Test
    public void testCompatibility()
    {
        NDArray a, b, c;
        // Same layout
        a = NDMatrix.zeros(NDType.INT8, new NDShape(10));
        b = NDMatrix.zeros(NDType.INT8, new NDShape(10));
        assertEquals(NDCompatibility.FLAT_ITERATION, NDCompatibility.forArrays(a, b));

        a = NDMatrix.zeros(NDType.INT8, new NDShape(2, 3));
        b = NDMatrix.zeros(NDType.INT8, new NDShape(2, 3));
        assertEquals(NDCompatibility.FLAT_ITERATION, NDCompatibility.forArrays(a, b));

        // View into other shape. Same shape, but different strides
        a = NDMatrix.arange(0, 10, 1);
        b = new NDArray(a, new NDShape(2, 2), 0, new NDStrides(2, 1));
        c = new NDArray(a, new NDShape(2, 2), 0, new NDStrides(4, 2));
        System.out.println(b);
        System.out.println(c);
        assertEquals(NDCompatibility.SHAPE_ITERATION, NDCompatibility.forArrays(b, c));

        // Different subsections of a but same stride
        c = new NDArray(a, new NDShape(2, 2), 2, new NDStrides(2, 1));
        System.out.println(b);
        System.out.println(c);
        assertEquals(NDCompatibility.FLAT_ITERATION, NDCompatibility.forArrays(b, c));

        // Require broadcasting
        c = new NDArray(a, new NDShape(2, 1), 0, new NDStrides(2, 1));
        System.out.println(c);
        assertEquals(NDCompatibility.BROADCAST_ITERATION, NDCompatibility.forArrays(b, c));

        // Incompatible
        c = new NDArray(a, new NDShape(2, 3), 0, new NDStrides(2, 1));
        System.out.println(c);
        assertEquals(NDCompatibility.NONE, NDCompatibility.forArrays(b, c));
    }
}
