/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JUnit tests of the {@link NDStrides}
 *  @author Kay Kasemir
 */
public class NDStrideUnitTest
{
    @Test
    public void testStrides()
    {
        NDShape shape = new NDShape(6);
        NDStrides stride = new NDStrides(shape);
        assertArrayEquals(new int[] { 1 }, stride.getStrides());
        assertTrue(stride.isDefault(shape));

        shape = new NDShape(1, 2, 3);
        stride = new NDStrides(shape);
        assertArrayEquals(new int[] { 6, 3, 1 }, stride.getStrides());
        assertTrue(stride.isDefault(shape));

        stride = new NDStrides(6, 3, 1);
        assertTrue(stride.isDefault(shape));

        stride = new NDStrides(6, 3, 2);
        assertFalse(stride.isDefault(shape));
    }
}
