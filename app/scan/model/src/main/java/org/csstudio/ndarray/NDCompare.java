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

import org.epics.util.array.IteratorNumber;

/** Compare operations for {@link NDArray}
 *
 *  <p>Implementation influenced by GDA scisoftpy
 *  which also has a 'Comparisons' like this separate from
 *  a '*DataSet' similar to NDArray.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDCompare
{
    /** @param a N-dim array
     *  @return True if any element is true
     */
    public static boolean any(final NDArray a)
    {
        final IteratorNumber iterator = a.getIterator();
        while (iterator.hasNext())
            if (iterator.nextByte() != 0)
                return true;
        return false;
    }

    /** @param a N-dim array
     *  @return True if all elements are true
     */
    public static boolean all(final NDArray a)
    {
        final IteratorNumber iterator = a.getIterator();
        while (iterator.hasNext())
            if (iterator.nextByte() == 0)
                return false;
        return true;
    }


    /** @param a N-dim array
     *  @return Array, length matching the rank of input, where element [d] has indices of non-zero elements for dimension d
     */
    public static NDArray nonzero(final NDArray a)
    {
        // Iterate twice (count, then store actual indices)
        // to avoid having to grow a memory structure
        int count = 0;
        final IteratorNumber count_iter = a.getIterator();
        while (count_iter.hasNext())
            if (count_iter.nextByte() != 0)
                ++count;

        final NDArray result = NDMatrix.zeros(NDType.INT32, new NDShape(a.getRank(), count));
        int next_result = 0;
        final ShapeIterator iter = new ShapeIterator(a.getShape());
        while (iter.hasNext())
        {
            final int[] pos = iter.getPosition();
            if (a.getDouble(pos) != 0.0)
            {
                for (int d=0; d<pos.length; ++d)
                    result.setDouble(pos[d], d, next_result);
                ++next_result;
            }
        }
        return result;
    }


    /** Binary operation */
    private interface BinaryOperation
    {
        /** @param a First input
         *  @param b Second input
         *  @return Result of operation
         */
        boolean compare(double a, double b);
    }

    final private static BinaryOperation op_eq = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a == b;    }
    };

    final private static BinaryOperation op_ne = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a != b;    }
    };

    final private static BinaryOperation op_lt = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a < b;    }
    };

    final private static BinaryOperation op_le = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a <= b;    }
    };

    final private static BinaryOperation op_gt = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a > b;    }
    };

    final private static BinaryOperation op_ge = new BinaryOperation()
    {
        @Override
        public boolean compare(final double a, final double b)     { return a >= b;    }
    };

    /** Perform operation on arrays element-by-element,
     *  using the NumPy broadcast idea but switching
     *  to linear interation if possible
     *  @param array N-dim array
     *  @param other N-dim array
     *  @param operation Operation to perform on the elements
     *  @return Result array
     *  @throws IllegalArgumentException if shapes are not compatible
     */
    private static NDArray binary_operation(final NDArray a, final NDArray b,
            final BinaryOperation operation)
    {
        final NDArray result;

        switch (NDCompatibility.forArrays(a, b))
        {
        case FLAT_ITERATION:
            result = new NDArray(NDType.BOOL, a.getShape());
            final int size = result.getSize();
            for (int i=0; i<size; ++i)
            {
                final boolean value = operation.compare(a.getFlatDouble(i),
                                                        b.getFlatDouble(i));
                result.setFlatDouble(i, value ? 1.0 : 0.0);
            }
            break;
        case SHAPE_ITERATION:
            result = new NDArray(NDType.BOOL, a.getShape());
            final ShapeIterator shape = new ShapeIterator(result.getShape());
            while (shape.hasNext())
            {
                final int[] pos = shape.getPosition();
                final boolean value = operation.compare(a.getDouble(pos),
                        b.getDouble(pos));
                result.setDouble(value ? 1.0 : 0.0, pos);
            }
            break;
        case BROADCAST_ITERATION:
            final BroadcastIterator bcst = new BroadcastIterator(a.getShape(), b.getShape());
            result = new NDArray(NDType.BOOL, bcst.getBroadcastShape());
            while (bcst.hasNext())
            {
                final boolean value = operation.compare(
                        a.getDouble(bcst.getPosA()), b.getDouble(bcst.getPosB()));
                result.setDouble(value ? 1.0 : 0.0, bcst.getPosition());
            }
            break;
        default:
            throw new IllegalArgumentException("Cannot compare array of shape " + a +
                    " with incompatible array of shape " + b);
        }

        return result;
    }

    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray equal_to(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_eq);
    }


    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray not_equal_to(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_ne);
    }

    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray less_than(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_lt);
    }

    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray less_equal(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_le);
    }

    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray greater_than(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_gt);
    }

    /** Element-by-element comparison
     *  @param array N-dim array
     *  @param other N-dim array
     *  @return Bool array
     */
    public static NDArray greater_equal(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_ge);
    }
}
