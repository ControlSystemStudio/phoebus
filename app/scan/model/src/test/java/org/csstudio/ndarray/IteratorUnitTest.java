/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.epics.util.array.IteratorNumber;
import org.junit.Test;

/** JUnit tests of the {@link NDArrayIterator}
 *  @author Kay Kasemir
 */
public class IteratorUnitTest
{
    @Test
    public void testIterator()
    {
        final NDArray a = NDMatrix.reshape(NDMatrix.arange(0, 6, 1), 2, 3);
        final NDArray b = NDMatrix.transpose(a);

        System.out.println(a);
        IteratorNumber iterator = a.getIterator();
        for (int i=0; i<6; ++i)
        {
            assertTrue(iterator.hasNext());
            final double value = iterator.nextDouble();
            System.out.println(value);
            if (i==1)
                assertEquals(1, value, 0.001);
        }
        System.out.println(b);
        iterator = b.getIterator();
        for (int i=0; i<6; ++i)
        {
            assertTrue(iterator.hasNext());
            final double value = iterator.nextDouble();
            System.out.println(value);
            if (i==1)
                assertEquals(3, value, 0.001);
        }
 }
}
