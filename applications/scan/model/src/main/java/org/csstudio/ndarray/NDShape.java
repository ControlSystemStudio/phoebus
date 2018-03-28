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

/** Shape of an N-dimensional array
 *
 *  <p>An array of shape [2, 3]
 *  would have rank 2, i.e. 2 dimensions.
 *  The first dimension has size 2,
 *  the second dimension has size 3,
 *  and the overall size is 6.
 *
 *  <p>The behavior of 'broadcast' shape handling
 *  is explained in http://www.scipy.org/EricsBroadcastingDoc
 *
 *  @author Kay Kasemir
 */
final public class NDShape
{
    final private int[] shape;

    /** Initialize
     *  @param shape Array shape, e.g. [2, 3]
     */
    public NDShape(final int... shape)
    {
        this.shape = shape.clone();
    }

    /** Initialize
     *  @param shape Array shape, e.g. [2, 3]
     */
    public NDShape(final List<Integer> shape)
    {
        this.shape = new int[shape.size()];
        for (int i=0; i<this.shape.length; ++i)
            this.shape[i] = shape.get(i);
    }

    /** @return Number of dimensions, e.g. 2 for an array of shape [4, 3] */
    public int getDimensions()
    {
        return shape.length;
    }

    /** Get size of one dimension of the N-dimensional array
     *  @param dimension Selected dimension, <code>0 ... getRank()</code>
     *  @return Number of elements in that dimension
     */
    public int getSize(final int dimension)
    {
        return shape[dimension];
    }

    /** @return Number of elements in flattened array */
    public int getSize()
    {
        int result = 1;
        for (final int s : shape)
            result *= s;
        return result;
    }

    /** @return Sizes of all dimensions */
    public int[] getSizes()
    {
        return shape.clone();
    }

    /** @return Iterator over all positions */
    public ShapeIterator iterator()
    {
        return new ShapeIterator(this);
    }

    /** Adjust negative index to allow access from 'end' of array
     *  @param dim Dimension
     *  @param index Index into array
     *  @return Corrected index
     */
    int adjustIndex(final int dim, final int index)
    {
        if (index >= 0)
            return index;
        return shape[dim] + index;
    }

    /** Check shape compatibility according to NumPy broadcasting rule
     *
     *  <p>Dimensions are checked from the 'end' until one or
     *  both shapes has no more dimension (i.e. ranks may differ).
     *
     *  <p>Size of each dimension must match, or one of them must be equal to 1.
     *
     *  @param other Other shape
     *  @return <code>true</code> if compatible
     */
    public boolean isBroadcastCompatible(final NDShape other)
    {
        // Compare sizes from the 'end'
        int dim = getDimensions();
        int dim_o = other.getDimensions();
        while (--dim >= 0  &&  --dim_o >= 0)
        {
            final int len = getSize(dim);
            final int len_o = other.getSize(dim_o);
            if (len != len_o  &&          // Lengths differ?
                len != 1  &&  len_o != 1) // ... and are not 1?
                return false;
        }
        return true;
    }

    /** Check shape compatibility according to NumPy broadcasting rule
     *
     *  <p>Dimensions are checked from the 'end' until one or
     *  both shapes has no more dimension (i.e. ranks may differ).
     *
     *  <p>Size of each dimension must match,
     *  but one shape may have a smaller rank.
     *
     *  @param a One shape
     *  @param b Other shape
     *  @return <code>true</code> if shapes have the same 'end'
     */
    public static boolean haveEqualEnds(final NDShape a, final NDShape b)
    {
        // Compare sizes from the 'end'
        int dim_a = a.getDimensions();
        int dim_b = b.getDimensions();
        while (--dim_a >= 0  &&  --dim_b >= 0)
        {
            if (a.getSize(dim_a) != b.getSize(dim_b)) // Lengths differ?
                return false;
        }
        return true;
    }

    /** For shapes that have the same end, return the combined
     *  shape, i.e. the one with higher rank.
     *
     *  @param a One shape
     *  @param b Other shape
     *  @return Combined shape. <code>null</code> if shapes don't match at the end.
     *  @see #hasEqualEnd(NDShape)
     */
    public static NDShape combine(final NDShape a, final NDShape b)
    {
        if (haveEqualEnds(a, b))
            return a.getDimensions() > b.getDimensions() ? a : b;
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode()
    {
        return Arrays.hashCode(shape);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other)
    {
        if (! (other instanceof NDShape))
            return false;
        return Arrays.equals(shape, ((NDShape)other).shape);
    }

    /** @return String representation */
    @Override
    public String toString()
    {
        return Arrays.toString(shape);
    }
}
