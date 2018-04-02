/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.ndarray;

/** Broadcast 'iterator'
 *
 *  <p>If the shapes of two arrays to add, multiply, .. match,
 *  it is most effecitve to operate element-by-element on the
 *  underlying flat arrays.
 *
 *  <p>But NumPy assumes a special "broadcast" behavior
 *  for arrays of different sizes as explained
 *  on http://www.scipy.org/EricsBroadcastingDoc
 *
 *  <p>This iterator takes two input shapes,
 *  computes the shape of the result,
 *  and provides iterator positions for each of the inputs
 *  as well as the result array.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class BroadcastIterator
{
    /** The 2 shapes on which to iterate */
    final private NDShape shape_a, shape_b;

    /** The broadcast shape */
    final private NDShape broadcast;

    /** Iterator for broadcast shape */
    final private ShapeIterator broadcast_iter;

    /** Current position within the shapes */
    final private int[] pos_a, pos_b;

    /** Initialize
     *  @param a First shape over which to iterate
     *  @param b Second shape over which to iterate
     *  @throws IllegalArgumentException if shapes are not compatible
     */
    public BroadcastIterator(final NDShape a, final NDShape b)
    {
        shape_a = a;
        shape_b = b;

        // Create broadcast shape with iterator
        broadcast = computeBroadcastShape(a, b);
        broadcast_iter = broadcast.iterator();

        // Position holders for input shapes
        pos_a = new int[a.getDimensions()];
        pos_b = new int[b.getDimensions()];
    }

    /** @return Shape of the result from broadcasting
     *          the two input arrays against each other
     */
    public NDShape getBroadcastShape()
    {
        return broadcast;
    }

    /** Determine 'broadcast' shape
     *
     *  <p>Shape of the result from an element-wise operation between
     *  two shapes.
     *
     *  @param a One shape
     *  @param b Other shape
     *  @return Broadcast shape
     *  @throws IllegalArgumentException if other shape is not compatible
     */
    private NDShape computeBroadcastShape(final NDShape a, final NDShape b)
    {
        // Compare sizes from the 'end'
        int dim_a = a.getDimensions();
        int dim_b = b.getDimensions();
        // Resulting shape has maximum rank
        int i = Math.max(dim_a, dim_b);
        final int[] result = new int[i];
        // Each dimension uses the maximum length of the original shapes
        while (--i >= 0)
        {
            // Original shapes may have smaller rank, so check dim < 0
            --dim_a;
            --dim_b;
            final int len_a = dim_a < 0 ? 1 : a.getSize(dim_a);
            final int len_b = dim_b < 0 ? 1 : b.getSize(dim_b);
            if (len_a != len_b  &&          // Lengths differ?
                len_a != 1  &&  len_b != 1) // ... and are not 1?
               throw new IllegalArgumentException("Array of shape " + a +
                       " is not compatible with array of shape " + b);
           result[i] = Math.max(len_a,  len_b);
        }
        return new NDShape(result);
    }

    /** Position on the next element for both inputs as well as the result
     *  @return <code>true</code> if there was a next element,
     *          <code>false</code> if the iterator wrapped around
     *          to the start of the iteration.
     */
    public boolean hasNext()
    {
        // Iterate over broadcast shape
        if (! broadcast_iter.hasNext())
            return false;

        // Determine matching indices for input shapes
        final int[] position = broadcast_iter.getPosition();
        broadcastPosition(position, pos_a, shape_a);
        broadcastPosition(position, pos_b, shape_b);
        return true;
    }

    /** Broadcast a position into a shape
     *  @param broadcast Broadcast position
     *  @param pos Position to set
     *  @param shape Shape where to set the position
     */
    private void broadcastPosition(final int[] broadcast, final int[] pos, final NDShape shape)
    {
        // Start with the 'rightmost' dimension
        int dim_b = broadcast.length - 1;
        int dim = pos.length - 1;
        while (dim >= 0)
        {
            if (shape.getSize(dim) > 1)
                pos[dim] = broadcast[dim_b];
            else // 'stretch' if shape is only 1 element long in this dimension
                pos[dim] = 0;
            --dim_b;
            --dim;
        }
    }

    /** @return Current position within first input shape */
    public int[] getPosA()
    {
        return pos_a;
    }

    /** @return Current position within second input shape */
    public int[] getPosB()
    {
        return pos_b;
    }

    /** @return Current position in broadcast i.e. result shape */
    public int[] getPosition()
    {
        return broadcast_iter.getPosition();
    }
}
