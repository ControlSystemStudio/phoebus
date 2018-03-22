/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.ndarray;

import java.util.Arrays;
import java.util.List;

/** Strides into an N-dimensional array
 *
 *  <p>An array of shape [2, 3]
 *  would default to strides [1, 2]
 *  because indices [i, j] convert into
 *  the flat array index as i*1 + j*2.
 *
 *  <p>Strides differ from the Python/NumPy strides
 *  because these address Java array elements, not raw
 *  byte offsets into memory.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
final public class NDStrides
{
    final private int[] strides;

    /** Initialize
     *  @param strides Array strides, e.g. [1, 2]
     */
    public NDStrides(final int... strides)
    {
        this.strides = strides.clone();
    }

    /** Initialize
     *  @param shape Array strides, e.g. [1, 2]
     */
    public NDStrides(final List<Integer> strides)
    {
        this.strides = new int[strides.size()];
        for (int i=0; i<this.strides.length; ++i)
            this.strides[i] = strides.get(i);
    }

    /** Initialize with default strides for a shape
     *  @param shape Shape
     */
    public NDStrides(final NDShape shape)
    {
        this.strides = defaultStrides(shape);
    }

    /** Compute default strides for a shape addressing a complete flat array
     *  @param shape Shape that describes the array
     *  @return strides
     */
    private static int[] defaultStrides(final NDShape shape)
    {
        final int[] strides = new int[shape.getDimensions()];
        strides[strides.length-1] = 1;
        for (int i=strides.length-2; i>=0; --i)
            strides[i] = strides[i+1] * shape.getSize(i+1);
        return strides;
    }

    /** Check if strides match the default for a shape
     *  @param shape Shape
     *  @return <code>true</code> if strides are the default, not 'skipping' anything
     */
    public boolean isDefault(final NDShape shape)
    {
        // Simpler but slower:
        // return Arrays.equals(strides, defaultStrides(shape));
        final int dim = strides.length;
        if (shape.getDimensions() != dim)
            return false;
        int step = 1;
        for (int i=dim-1; i>=0; --i)
        {
            if (step != strides[i])
                return false;
            step *= shape.getSize(i);
        }
        return true;
    }

    /** @return Number of strides */
    public int getSize()
    {
        return strides.length;
    }

    /** Get stride for one dimension
     *  @param dimension Selected dimension, <code>0 ... getSize()</code>
     *  @return Stride for that dimension
     */
    public int getStride(final int dimension)
    {
        return strides[dimension];
    }

    /** @return Stride elements */
    public int[] getStrides()
    {
        return strides.clone();
    }

    /** Compute index in flattened array
     *  @param shape Associated shape, used to correct indices
     *  @param position Position in N-dimensional shape, e.g. [1, 0] for shape [2, 3]
     *                  Negative elements counts from the end of associated dimension.
     *  @return Flattened array index, e.g. 3 for [1,0]
     *  @throws IllegalArgumentException when number of indices does not match shape
     */
    public int getIndex(final NDShape shape, final int... position)
    {
        if (position.length > strides.length)
            throw new IllegalArgumentException("Need at most " + strides.length + " indices, got " + position.length);
        int i = 0;
        // Correct negative index via modulo. -1 -> last element
        for (int dim=0; dim<position.length; ++dim)
            i += shape.adjustIndex(dim, position[dim]) * strides[dim];
        return i;
    }

    /** Compute index in flattened array. No negative indices, less error checking)
     *  @param position Position in N-dimensional shape, e.g. [1, 0]
     *  @return Flattened array index, e.g. 3 for [1,0]
     */
    public int getIndex(final int... position)
    {
        int i = 0;
        for (int dim=0; dim<position.length; ++dim)
            i += position[dim] * strides[dim];
        return i;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(strides);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other)
    {
        if (! (other instanceof NDStrides))
            return false;
        return Arrays.equals(strides, ((NDStrides)other).strides);
    }

    /** @return String representation */
    @Override
    public String toString()
    {
        return Arrays.toString(strides);
    }
}
