/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.ndarray;

import java.util.Arrays;

/** Iterator over positions within a {@link NDShape}
 *
 *  @author Kay Kasemir
 */
public class ShapeIterator
{
    /** Shape to iterate */
    final private NDShape shape;

    /** Current position within shape */
    final private int[] pos;

    /** Initialize
     *  @param shape Shape over which to iterate
     */
    ShapeIterator(final NDShape shape)
    {
        this.shape = shape;
        // Start at position [ 0, 0, 0, ... ]
        pos = new int[shape.getDimensions()];
        Arrays.fill(pos, 0);
        // Prepare for first iteration step
        pos[pos.length-1] = -1;
    }

    /** Position on the next element.
     *  @return <code>true</code> if there was a next element,
     *          <code>false</code> if the iterator wrapped around
     *          to the start of the iteration.
     */
    public boolean hasNext()
    {
        // Increment position elements starting at the last dimension
        for (int i = pos.length-1; i>=0; --i)
        {
            ++pos[i];
            if (pos[i] < shape.getSize(i))
                return true;
            // Carry over into previous dimension
            pos[i] = 0;
        }
        return false;
    }

    /** @return Current position */
    public int[] getPosition()
    {
        return pos;
    }
}
