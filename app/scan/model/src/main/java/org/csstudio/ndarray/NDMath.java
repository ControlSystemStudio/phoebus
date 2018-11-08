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

/** Math operations for {@link NDArray}
 *
 *  <p>Implementation influenced by GDA scisoftpy
 *  which also has a 'Maths' like this separate from
 *  a '*DataSet' similar to NDArray.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDMath
{
    /** @param array N-dim array
     *  @return Sum over elements. 0 for empty array
     */
    public static double sum(final NDArray array)
    {
        final IteratorNumber iter = array.getIterator();
        double sum = 0.0;
        while (iter.hasNext())
            sum += iter.nextDouble();
        return sum;
    }

    /** @param array N-dim array
     *  @return Maximum array element
     */
    public static double max(final NDArray array)
    {
        final IteratorNumber iter = array.getIterator();
        if (! iter.hasNext())
            throw new IllegalArgumentException("Maximum not defined for empty array");
        double max = iter.nextDouble();
        while (iter.hasNext())
            max = Math.max(max, iter.nextDouble());
        return max;
    }

    /** @param array N-dim array
     *  @return Minimum array element
     */
    public static double min(final NDArray array)
    {
        final IteratorNumber iter = array.getIterator();
        if (! iter.hasNext())
            throw new IllegalArgumentException("Minimum not defined for empty array");
        double min = iter.nextDouble();
        while (iter.hasNext())
            min = Math.min(min, iter.nextDouble());
        return min;
    }

    /** @param array N-dim array where each element is to be turned into its negative */
    public static void negative(final NDArray array)
    {
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            array.setFlatDouble(i, -array.getFlatDouble(i));
    }

    /** @param array N-dim array
     *  @param value Value by which to increment each array element
     */
    public static void increment(final NDArray array, final double value)
    {
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            array.setFlatDouble(i, array.getFlatDouble(i) + value);
    }

    /** @param array N-dim array
     *  @param value Value by which to scale each array element
     */
    public static void scale(final NDArray array, final double value)
    {
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            array.setFlatDouble(i, array.getFlatDouble(i) * value);
    }

    /** @param array N-dim array
     *  @param value Value by which to divide each array element
     */
    public static void divide_elements(final NDArray array, final double value)
    {
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            array.setFlatDouble(i, array.getFlatDouble(i) / value);
    }

    /** @param array N-dim array
     *  @return Array with absolute element values
     */
    public static NDArray abs(final NDArray array)
    {
        final NDArray result = new NDArray(array.getType(), array.getShape());
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            result.setFlatDouble(i, Math.abs(array.getFlatDouble(i)));
        return result;
    }

    /** @param array N-dim array
     *  @return Array with square roots of elements
     */
    public static NDArray sqrt(final NDArray array)
    {
        final NDArray result = new NDArray(NDType.FLOAT64, array.getShape());
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
        {
            final double val = array.getFlatDouble(i);
            if (val >= 0.0)
                result.setFlatDouble(i, Math.sqrt(val));
            else
                result.setFlatDouble(i, Double.NaN);
        }
        return result;
    }

    /** @param array N-dim array
     *  @return Array with exponential values of elements
     */
    public static NDArray exp(final NDArray array)
    {
        final NDArray result = new NDArray(NDType.FLOAT64, array.getShape());
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            result.setFlatDouble(i, Math.exp(array.getFlatDouble(i)));
        return result;
    }

    /** @param array N-dim array
     *  @return Array with log(elements)
     */
    public static NDArray log(final NDArray array)
    {
        final NDArray result = new NDArray(NDType.FLOAT64, array.getShape());
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            result.setFlatDouble(i, Math.log(array.getFlatDouble(i)));
        return result;
    }

    /** @param array N-dim array
     *  @return Array with log10(elements)
     */
    public static NDArray log10(final NDArray array)
    {
        final NDArray result = new NDArray(NDType.FLOAT64, array.getShape());
        final int size = array.getSize();
        for (int i=0; i<size; ++i)
            result.setFlatDouble(i, Math.log10(array.getFlatDouble(i)));
        return result;
    }

    /** Binary operation */
    private interface BinaryOperation
    {
        /** @param a First input
         *  @param b Second input
         *  @return Result of operation
         */
        double calc(double a, double b);
    }

    final private static BinaryOperation op_add = new BinaryOperation()
    {
        @Override
        public double calc(final double a, final double b)     { return a + b;    }
    };

    final private static BinaryOperation op_sub = new BinaryOperation()
    {
        @Override
        public double calc(final double a, final double b)    { return a - b;    }
    };

    final private static BinaryOperation op_mul = new BinaryOperation()
    {
        @Override
        public double calc(final double a, final double b)    { return a * b; }
    };

    final private static BinaryOperation op_div = new BinaryOperation()
    {
        @Override
        public double calc(final double a, final double b)    { return a / b;    }
    };

    final private static BinaryOperation op_pwr = new BinaryOperation()
    {
        @Override
        public double calc(final double a, final double b)    { return Math.pow(a, b); }
    };

    /** Perform in-place operation on array.
     *  Use element-by-element when possible.
     *  @param array N-dim array on which to operate
     *  @param other N-dim array that provides values for operation
     *  @param operation Operation to perform on the elements
     */
    private static void inplace_operation(final NDArray array, final NDArray other,
            final BinaryOperation operation)
    {
        switch (NDCompatibility.forArrays(array, other))
        {
        case FLAT_ITERATION:
            final int size = array.getSize();
            for (int i=0; i<size; ++i)
            {
                final double value = operation.calc(array.getFlatDouble(i),
                        other.getFlatDouble(i));
                array.setFlatDouble(i, value);
            }
            break;
        case SHAPE_ITERATION:
            final ShapeIterator shape = new ShapeIterator(array.getShape());
            while (shape.hasNext())
            {
                final int[] pos = shape.getPosition();
                final double value = operation.calc(array.getDouble(pos),
                        other.getDouble(pos));
                array.setDouble(value, pos);
            }
            break;
        case BROADCAST_ITERATION:
            final BroadcastIterator i = new BroadcastIterator(array.getShape(), other.getShape());
            if (! i.getBroadcastShape().equals(array.getShape()))
                throw new IllegalArgumentException("Cannot operate on array of shape  " + array.getShape() +
                        " in-place with (broadcast) argument of shape " + other.getShape());
            while (i.hasNext())
            {
                final double value = operation.calc(array.getDouble(i.getPosA()),
                        other.getDouble(i.getPosB()));
                array.setDouble(value, i.getPosition());
            }
            break;
        default:
            throw new IllegalArgumentException("Cannot operate on array of shape  " + array.getShape() +
                    " in-place with argument of shape " + other.getShape());
        }
    }

    /** Add arrays element-by-element
     *  @param array N-dim array to increment
     *  @param other N-dim array with values to use as increments
     *  @throws IllegalArgumentException if <code>array + other</code>
     *          would result in a shape different from the original array
     */
    public static void increment(final NDArray array, final NDArray other)
    {
        inplace_operation(array, other, op_add);
    }


    /** Scale arrays element-by-element
     *  @param array N-dim array to scale
     *  @param other N-dim array with values to use as scaling factors
     *  @throws IllegalArgumentException if <code>array * other</code>
     *          would result in a shape different from the original array
     */
    public static void scale(final NDArray array, final NDArray other)
    {
        inplace_operation(array, other, op_mul);
    }

    /** Divide arrays element-by-element
     *  @param array N-dim array to divide
     *  @param other N-dim array with values to use as divisors
     *  @throws IllegalArgumentException if <code>array / other</code>
     *          would result in a shape different from the original array
     */
    public static void divide_elements(final NDArray array, final NDArray other)
    {
        inplace_operation(array, other, op_div);
    }

    /** Perform operation on arrays element-by-element,
     *  using the NumPy broadcast idea but switching
     *  to linear interation if possible
     *  @param a N-dim array
     *  @param b N-dim array
     *  @param operation Operation to perform on the elements
     *  @return Result array
     */
    private static NDArray binary_operation(final NDArray a, final NDArray b,
            final BinaryOperation operation)
    {
        final NDArray result;
        final NDType type = NDType.determineSuperType(a.getType(), b.getType());

        switch (NDCompatibility.forArrays(a, b))
        {
        case FLAT_ITERATION:
            result = new NDArray(type, a.getShape());
            final int size = result.getSize();
            for (int i=0; i<size; ++i)
            {
                final double value = operation.calc(a.getFlatDouble(i),
                        b.getFlatDouble(i));
                result.setFlatDouble(i, value);
            }
            break;
        case SHAPE_ITERATION:
            result = new NDArray(type, a.getShape());
            final ShapeIterator shape = new ShapeIterator(result.getShape());
            while (shape.hasNext())
            {
                final int[] pos = shape.getPosition();
                final double value = operation.calc(a.getDouble(pos),
                        b.getDouble(pos));
                result.setDouble(value, pos);
            }
            break;
        case BROADCAST_ITERATION:
            final BroadcastIterator i = new BroadcastIterator(a.getShape(), b.getShape());
            result = new NDArray(type, i.getBroadcastShape());
            while (i.hasNext())
            {
                final double value = operation.calc(a.getDouble(i.getPosA()),
                        b.getDouble(i.getPosB()));
                result.setDouble(value, i.getPosition());
            }
            break;
        default:
            throw new IllegalArgumentException("Cannot operate on arrays of incompatible shapes " + a +
                    " and " + b);
        }

        return result;
    }

    /** @param a N-dim array
     *  @param b N-dim array
     *  @return result a + b
     */
    public static NDArray add(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_add);
    }

    /** @param a N-dim array
     *  @param b N-dim array
     *  @return result a - b
     */
    public static NDArray subtract(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_sub);
    }

    /** @param a N-dim array
     *  @param b N-dim array
     *  @return result a * b
     */
    public static NDArray multiply(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_mul);
    }

    /** @param a N-dim array
     *  @param b N-dim array
     *  @return result a / b
     */
    public static NDArray divide(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_div);
    }

    /** @param a N-dim array
     *  @param b N-dim array
     *  @return result a ^ b
     */
    public static NDArray power(final NDArray a, final NDArray b)
    {
        return binary_operation(a, b, op_pwr);
    }
}
