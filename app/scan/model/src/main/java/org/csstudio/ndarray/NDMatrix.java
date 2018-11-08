/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * GDA is Copyright 2010 Diamond Light Source Ltd.
 ******************************************************************************/
package org.csstudio.ndarray;

import java.util.Arrays;

import org.epics.util.array.ListNumber;

/** Matrix-type operations for {@link NDArray}
 *
 *  <p>Implementation influenced by GDA scisoftpy.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDMatrix
{
    /** Create zero-filled array
     *  @param type Data type {@link Double}, {@link Integer}, ...
     *  @param shape Array shape, e.g. [2, 3]
     *  @return NDArray for data
     */
    public static NDArray zeros(final NDType type, final NDShape shape)
    {
        final ListNumber flat = NDArray.createDataArray(type, shape.getSize());
        // Array is already initialized to zero values, no need to set it:
        // final int size = shape.getSize();
        // for (int i=0; i<size; ++i)
        //      flat.setInt(i, 0);
        return new NDArray(flat, shape, type == NDType.BOOL);
    }

    /** Create one-filled array
     *  @param type Data type {@link Double}, {@link Integer}, ...
     *  @param shape Array shape, e.g. [2, 3]
     *  @return NDArray for data
     */
    public static NDArray ones(final NDType type, final NDShape shape)
    {
        final ListNumber flat = NDArray.createDataArray(type, shape.getSize());
         final int size = shape.getSize();
         for (int i=0; i<size; ++i)
              flat.setInt(i, 1);
        return new NDArray(flat, shape, type == NDType.BOOL);
    }

    /** Create array for range
     *  @param start Initial value
     *  @param stop Next-to-end value, not included in result
     *  @param step Step size
     *  @return Array [start, start+step, ..., end[
     */
    public static NDArray arange(final double start, final double stop, final double step)
    {
        return arange(start, stop, step, NDType.FLOAT64);
    }

    /** Create array for range
     *  @param start Initial value
     *  @param stop Next-to-end value, not included in result
     *  @param step Step size
     *  @return Array [start, start+step, ..., end[
     */
    public static NDArray arange(final double start, final double stop, final double step, final NDType type)
    {
        final int count = NDArray.getCount(start, stop, step);
        final double[] data = new double[count];
        for (int i=0; i<count; ++i)
            data[i] = start + i * step;
        return NDArray.create(data, type);
    }

    /** Create array for linear range
     *  @param start Initial value
     *  @param end Final value
     *  @param count Number of elements
     *  @param type Data type
     *  @return Array [start, ..., end] with count values
     */
    public static NDArray linspace(final double start, final double end, final int count,
            final NDType type)
    {
        if (count < 2)
            return NDArray.create(new double[] { start }, type);
        final double step = (end - start) / (count-1);
        final NDArray data = new NDArray(type, new NDShape(count));
        for (int i=0; i<count; ++i)
            data.setFlatDouble(i, start + i * step);
        return data;
    }

    /** Create view with new shape
     *  @param array Original Array
     *  @param shape Desired shape
     *  @return Array view with new shape
     *  @throws IllegalArgumentException if new shape conflicts with existing size
     *  @see #reshape(NDShape)
     */
    public static NDArray reshape(final NDArray array, final int... shape)
    {
        return reshape(array, new NDShape(shape));
    }

    /** Create view with new shape
     *
     *  <p>Must not change the overall size.
     *  For example, modifying [6] into [2,3] or [3,2] is possible,
     *  but not into [7].
     *  @param array Original Array
     *  @param shape Desired shape
     *  @return Array view with new shape
     *  @throws IllegalArgumentException if new shape conflicts with existing size
     */
    public static NDArray reshape(final NDArray array, final NDShape shape)
    {
        if (array.getShape().getSize() != shape.getSize())
            throw new IllegalArgumentException("Cannot change shape from " +
                    array.getShape() + " to " + shape);
        return new NDArray(array, shape, new NDStrides(shape));
    }

    /** Transpose an array, "swapping" rows and columns for the 2-D case.
     *  For 1D, the original is returned
     *
     *  @param a N-dim array
     *  @param axes Desired sequence of axes, defaulting to reversing all axes
     *  @return View into original array with transposed shape and stride
     *  @throws IllegalArgumentException if axes are not valid
     */
    public static NDArray transpose(final NDArray a, int... axes)
    {
        // Transpose simply swaps the shape, stride elements
        //
        // Example: Raw array 0 1 2 3 4 5
        // Viewed as shape 3,2, strides 2, 1:
        //   0 1
        //   2 3
        //   4 5
        // Iterate indices 0, 1, 2, 3, 4, 5
        //
        // Transposed: shape 2,3,   strides 1, 2
        //   0 2 4
        //   1 3 5
        // Iterate indices 0, 2, 4, 1, 3, 5

        final NDShape shape = a.getShape();
        final int rank = shape.getDimensions();
        if (rank <= 1)
            return a;

        if (axes.length <= 0)
        {    // If no axes specified, use default that reverses them
            axes = new int[rank];
            for (int i = 0; i < rank; ++i)
                axes[i] = rank - 1 - i;
        }
        else
        {    // Check requested axis assignments
            if (axes.length != rank)
                throw new IllegalArgumentException(
                        "Axes " + Arrays.toString(axes) + " don't match array shape " + shape);
            for (int ax : axes)
                if (ax < 0  ||  ax >= rank)
                    throw new IllegalArgumentException(
                            "Invalid axis " + ax + " for array shape " + shape);
        }

        // Determine transposed view of array
        final NDStrides stride = a.getStrides();
        final int[] n_shape = new int[rank];
        final int[] n_stride = new int[rank];
        for (int i=0; i<rank; ++i)
        {
            n_shape[i] = shape.getSize(axes[i]);
            n_stride[i] = stride.getStride(axes[i]);
        }
        return new NDArray(a, new NDShape(n_shape), new NDStrides(n_stride));
    }

    /** Perform matrix multiplication of arrays
     *  @param a N-dim array
     *  @param b N-dim array
     *  @return Result a * b in the matrix sense
     *  @throws IllegalArgumentException array shapes are not supported
     */
    public static NDArray dot(final NDArray a, final NDArray b)
    {
        final NDShape shape_a = a.getShape();
        final NDShape shape_b = b.getShape();

        if (shape_a.getDimensions() == 2  &&  shape_b.getDimensions() == 2)
            return dot2x2(a, b, shape_a, shape_b);
        else if (shape_a.getDimensions() == 2  &&  shape_b.getDimensions() == 1)
            return dot2x1(a, b, shape_a);
        else if (shape_a.getDimensions() == 1  &&  shape_b.getDimensions() == 1)
            return inner(a, b);

        throw new IllegalArgumentException(
            "Matrix multiplication not supported for arrays " +
            "with shapes " + shape_a + " and " + shape_b);
    }

    /** Perform matrix multiplication of arrays 2x2 arrays
     *  @param a 2-dim array
     *  @param b 2-dim array
     *  @param shape_a Shape of a
     *  @param shape_b Shape of b
     *
     *  @return Result a * b in the matrix sense
     */
    private static NDArray dot2x2(final NDArray a, final NDArray b,
            final NDShape shape_a, final NDShape shape_b)
    {
        final NDType type = NDType.determineSuperType(a.getType(), b.getType());
        final int a_rows = shape_a.getSize(0);
        final int a_cols = shape_a.getSize(1);

        if (a_cols != shape_b.getSize(0))
            throw new IllegalArgumentException(
                "For matrix multiplication, number of columns in first array must match number of rows in second array," +
                " but got shapes " + shape_a + " and " + shape_b);

        final int b_cols = shape_b.getSize(1);
        final NDShape shape_r = new NDShape(a_rows, b_cols);
        final NDArray result = zeros(type, shape_r);
        for (int i=0; i<a_rows; ++i)
            for (int j=0; j<b_cols; ++j)
            {
                double sum = 0.0;
                for (int k=0; k<a_cols; ++k)
                    sum += a.getDouble(i, k) * b.getDouble(k, j);
                result.setDouble(sum, i, j);
            }
        return result;
    }

    /** Perform matrix multiplication of arrays 2x1 arrays
     *  @param a 2-dim array
     *  @param b 1-dim array
     *  @param shape_a Shape of a
     *
     *  @return Result a * b in the matrix sense
     */
    private static NDArray dot2x1(final NDArray a, final NDArray b,
            final NDShape shape)
    {
        final NDType type = NDType.determineSuperType(a.getType(), b.getType());
        final int a_rows = shape.getSize(0);
        final int a_cols = shape.getSize(1);

        if (a_cols != b.getSize())
            throw new IllegalArgumentException(
                "For matrix multiplication, number of columns in first array must match number of rows in second array," +
                " but got shapes " + shape + " and " + b.getShape());

        final NDArray result = zeros(type, new NDShape(a_cols));
        for (int i=0; i<a_rows; ++i)
        {
            double sum = 0.0;
            for (int k=0; k<a_cols; ++k)
                sum += a.getDouble(i, k) * b.getFlatDouble(k);
            result.setDouble(sum, i);
        }
        return result;
    }

    /** Perform matrix multiplication of arrays 1x1 arrays
     *  @param a 1-dim array
     *  @param b 1-dim array
     *  @param len_a Length of a
     *  @param len_b Length of b
     *
     *  @return Result a * b in the matrix sense
     */
    public static NDArray inner(final NDArray a, final NDArray b)
    {
        final NDShape shape_a = a.getShape();
        final NDShape shape_b = b.getShape();

        if (shape_a.getDimensions() != shape_b.getDimensions())
            throw new IllegalArgumentException(
                "Inner product only supported for 1-D arrays, " +
                "not for shapes " + shape_a + " and " + shape_b);

        final int len = shape_a.getSize();
        if (len != shape_b.getSize())
            throw new IllegalArgumentException(
                "Inner product arrays must have same size, " +
                "not shapes " + shape_a + " and " + shape_b);

        final NDType type = NDType.determineSuperType(a.getType(), b.getType());
        final NDArray result = zeros(type, new NDShape(1));
        double sum = 0.0;
        for (int i=0; i<len; ++i)
            sum += a.getDouble(i) * b.getDouble(i);
        result.setDouble(sum, 0);
        return result;
    }
}
