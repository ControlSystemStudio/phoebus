/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.ndarray;

import org.epics.util.array.IteratorDouble;

/** Iterate over NDArray elements
 *
 *  <p>Fundamentally a 'flat' iteration, but considering the
 *  shape and strides of the array so that for example a transposed "view"
 *  into an array is iterated per the view, not the base array.
 *
 *  @author Kay Kasemir
 */
public class NDArrayIterator implements IteratorDouble
{
    final private NDArray array;
    final private ShapeIterator shape_iter;

    public NDArrayIterator(final NDArray array)
    {
        this.array = array;
        shape_iter = new ShapeIterator(array.getShape());
    }

    @Override
    public boolean hasNext()
    {
        return shape_iter.hasNext();
    }

    @Override
    public double nextDouble()
    {
        return array.getDouble(shape_iter.getPosition());
    }
}
