/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.ndarray;

/** Type of {@link NDArray} compatibility
 *
 *  <p>If two NDArrays have the same layout in shape and stride,
 *  operations can use element-by-element operations
 *  on the raw data (considering the offset).
 *
 *  <p>If their shapes match but strides differ, a
 *  {@link ShapeIterator} is required.
 *
 *  <p>If either shape or stride differ, a
 *  {@link BroadcastIterator} is required.
 *
 *  @author Kay Kasemir
 */
public enum NDCompatibility
{
    /** Arrays have same shape and stride, allowing flat iteration */
    FLAT_ITERATION,
    /** Arrays have same shape but not stride, allowing shape iteration */
    SHAPE_ITERATION,
    /** Arrays have different shape, requiring broadcast iteration */
    BROADCAST_ITERATION,
    /** Arrays are incompatible */
    NONE;

    /** Determine compatibility of arrays
     *  @param a First array
     *  @param b Other array
     *  @return Compatibility
     */
    public static NDCompatibility forArrays(final NDArray a, final NDArray b)
    {
        final NDShape shape_a = a.getShape();
        final NDShape shape_b = b.getShape();
        if (shape_a.equals(shape_b))
        {
            if (a.getStrides().equals(b.getStrides()))
                return FLAT_ITERATION;
            return SHAPE_ITERATION;
        }
        else if (shape_a.isBroadcastCompatible(shape_b))
            return BROADCAST_ITERATION;
        return NONE;
    }
}
