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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.CollectionNumbers;
import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListNumber;

/** N-dimensional array
 *
 *  <p>Inspired by the Python/NumPy ndarray.
 *  Data is internally held in a flat array.
 *  For a 1-D array, the internal array is as expected.
 *  For a 2-D array of shape 2x3 the data is stored as follows:
 *  <pre>
 *  2-D index:  [0,0] [0,1] [0,2] [1,0], [1,1], [1,2]
 *  Flat index:   0     1     2     3      4      5
 *  </pre>
 *  In this example the stride would be 3,1 because a 2D index
 *  [x,y] converts into a flat index of 3*x+1*y.
 *
 *  <p>The layout of an NDArray layout is immutable, but it is
 *  possible to create additional NDArrays that view the same
 *  data with a different shape, stride or offset to the 1st element.
 *  Those NDArrays will then have this array as their 'base'.
 *  Changes to the view will update the base data.
 *
 *  <p>Implementation influenced by GDA scisoftpy,
 *  but different in key points:
 *  <ul>
 *  <li>Using {@link ListNumber} for underlying array</li>
 *  <li>Non-copy views for slices, 'transpose()', ...</li>
 *  </ul>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class NDArray
{
    /** Base array.
     *  If <code>null</code>, this array owns the data.
     *  If not <code>null</code>, this array is a view into
     *  the data owned by the base array.
     */
    final private transient NDArray base;

    /** Flat array */
    final private ListNumber data;

    /** Is byte array data used as boolean?
     *
     *  <p>For most data types there's a corresponding
     *  {@link ListNumber} array.
     *  But there's no ArrayBool, and {@link ArrayByte} is probably
     *  better anyway because it can be used in computations
     *  with 0/1 as False/True.
     *  This indicator is for example used to print the data
     *  when we meant to use a BOOL type.
     */
    final private boolean is_boolean;

    /** Shape of the data */
    final private NDShape shape;

    /** Offset into <code>data</code> to access first element */
    final private int offset;

    /** How to access the data */
    final private NDStrides stride;

    /** Initialize with existing data
     *  @param data {@link ListNumber}
     *  @param shape Array shape, e.g. [2, 3]
     */
    protected NDArray(final ListNumber data, final NDShape shape, final boolean is_boolean)
    {
        this.base = null;
        this.data = data;
        this.shape = shape;
        this.offset = 0;
        this.stride = new NDStrides(shape);
        this.is_boolean = is_boolean;
    }

    /** Initialize as view into existing array
     *  @param base Base array
     *  @param shape Array shape, e.g. [2, 3]
     *  @param offset Offset to first item in base array
     *  @param stride Array stride
     *  @throws IllegalArgumentException if shape, stride are incompatible
     */
    protected NDArray(final NDArray base, final NDShape shape, final NDStrides stride)
    {
        this(base, shape, base.offset, stride);
    }

    /** Initialize as view into existing array
     *  @param base Base array
     *  @param shape Array shape, e.g. [2, 3]
     *  @param offset Offset to first item in base array
     *  @param stride Array stride
     *  @throws IllegalArgumentException if shape, stride are incompatible
     */
    protected NDArray(final NDArray base, final NDShape shape, final int offset, final NDStrides stride)
    {
        if (shape.getSize() > base.getShape().getSize())
            throw new IllegalArgumentException("Attempting to access array with shape " +
                    base.getShape() + " as bigger shape " + shape);
        // Better way to check stride?
        if (stride.getSize() != shape.getDimensions())
            throw new IllegalArgumentException("Stride " + stride +
                    " not compatible with array shape " + base.getShape());
        this.base = base;
        this.data = base.data;
        this.shape = shape;
        this.offset = offset;
        this.stride = stride;
        this.is_boolean = base.is_boolean;
    }

    /** Initialize empty array (values will be undefined)
     *  @param type Data type {@link Double}, {@link Integer}, ...
     *  @param shape Array shape, e.g. [2, 3]
     */
    protected NDArray(final NDType type, final NDShape shape)
    {
        this(createDataArray(type, shape.getSize()), shape, type == NDType.BOOL);
    }

    /** Create from plain Java array data, determine type from data
     *  @param data Data, for example <code>double[]</code> or <code>int[][][]</code>
     *  @return NDArray for given data
     *  @throws IllegalArgumentException for unhandled data type
     */
    public static NDArray create(final Object data)
    {
        return create(data, determineType(data));
    }

    /** Create from plain Java array data
     *  @param data Data, for example <code>double[]</code> or <code>int[][][]</code>
     *  @param type Data type
     *  @return NDArray for given data
     *  @throws IllegalArgumentException for unhandled data type
     */
    public static NDArray create(final Object data, final NDType type)
    {
        final NDShape shape = determineShape(data);
        final ListNumber flat = createDataArray(type, shape.getSize());
        fillFlatArray(flat, 0, data);
        return new NDArray(flat, shape, type == NDType.BOOL);
    }

    /** @param start Start of a range
     *  @param stop Stop value of a range
     *  @param step Step size
     *  @return Number of elements in range start:stop:step, or 0
     */
    static int getCount(final double start, final double stop, final double step)
    {
        final int count = (int) Math.ceil((stop - start) / step);
        if (count > 0)
            return count;
        return 0;
    }

    /** @return Copy of this array (flattened) */
    @Override
    public NDArray clone()
    {
        final int size = data.size();
        final ListNumber copy = createDataArray(getType(), size);
        final IteratorNumber iterator = getIterator();
        int i=0;
        while (iterator.hasNext())
            copy.setDouble(i++, iterator.nextDouble());
        return new NDArray(copy, getShape(), is_boolean);
    }

    /** Create {@link ListNumber} for requested data type
     *  @param type Data type {@link Double}, {@link Integer}, ...
     *  @param size Array size
     *  @return {@link ListNumber}
     *  @throws IllegalArgumentException for unhandled data type
     */
    static ListNumber createDataArray(final NDType type, final int size)
    {


        switch (type)
        {
        case FLOAT64: return CollectionNumbers.toListDouble(new double[size]);
        case FLOAT32: return CollectionNumbers.toListFloat(new float[size]);
        case INT64:   return CollectionNumbers.toListLong(new long[size]);
        case INT32:   return CollectionNumbers.toListInt(new int[size]);
        case INT16:   return CollectionNumbers.toListShort(new short[size]);
        case INT8:    return CollectionNumbers.toListByte(new byte[size]);
        case BOOL:    return CollectionNumbers.toListByte(new byte[size]);
        default:
            throw new IllegalArgumentException("Unsupported data type " + type);
        }
    }

    /** Determine the data type of an (array) object
     *  @param data Data that must be an array, flat or nested, of {@link Double}, ..., {@link Byte}
     *  @return Type of the array data. Defaults to double.class for empty array
     *  @throws IllegalArgumentException for unhandled data type
     */
    private static NDType determineType(final Object data)
    {
        if (data.getClass().isArray())
        {
            if (Array.getLength(data) <= 0)
                return NDType.FLOAT64;
            final Object element = Array.get(data, 0);
            return determineType(element);
        }
        else if (data instanceof List)
        {
            final List<?> list = (List<?>)data;
            if (list.size() <= 0)
                return NDType.FLOAT64;
            final Object element = list.get(0);
            return determineType(element);
        }
        else
            return NDType.forJavaObject(data);
    }

    /** Determine shape of an (array) object
     *  @param data Data that must be an array, flat or nested
     *  @return Shape of the data, i.e. size of each dimension
     *  @throws IllegalArgumentException for unhandled data type
     */
    private static NDShape determineShape(final Object data)
    {
        final List<Integer> shape = new ArrayList<>();
        doDetermineShape(shape, data);
        // Convert to int[]
        final int[] result = new int[shape.size()];
        for (int i=0; i<result.length; ++i)
            result[i] = shape.get(i);
        return new NDShape(result);
    }

    /** Recursively determine shape of an (array) object
     *  @param shape Size of each dimension is added to this list
     *  @param data Data that must be an array
     *  @throws IllegalArgumentException for unhandled data type
     */
    private static void doDetermineShape(final List<Integer> shape, final Object data)
    {
        if (data instanceof List)
        {
            final List<?> list = (List<?>)data;
            final int length = list.size();
            shape.add(length);
            if (length > 0)
            {
                final Object element = list.get(0);
                if (element != null  &&  element instanceof List)
                    doDetermineShape(shape, element);
            }
        }
        else if (data.getClass().isArray())
        {
            final int length = Array.getLength(data);
            shape.add(length);
            if (length > 0)
            {
                final Object element = Array.get(data, 0);
                if (element != null  &&  element.getClass().isArray())
                    doDetermineShape(shape, element);
            }
        }
        else
            throw new IllegalArgumentException("Expect array, got " + data.getClass().getName());
    }

    /** Recursively fill a flat array from shaped data
     *  @param flat Flat array to be filled
     *  @param index Index for next flat array element
     *  @param data N-dimensional array data
     *  @return Index for next array element
     */
    private static int fillFlatArray(final ListNumber flat, int index, final Object data)
    {
        if (data instanceof List)
        {
            final List<?> list = (List<?>)data;
            for (int i=0; i<list.size(); ++i)
                index = fillFlatArray(flat, index, list.get(i));
            return index;
        }
        else if (data.getClass().isArray())
        {
            final int dim = Array.getLength(data);
            for (int i=0; i<dim; ++i)
                index = fillFlatArray(flat, index, Array.get(data, i));
            return index;
        }
        else if (data instanceof Number)
        {
            flat.setDouble(index, ((Number)data).doubleValue());
            return index + 1;
        }
        else if (data instanceof Boolean)
        {
            flat.setByte(index, ((Boolean)data).booleanValue() ? (byte)1 : 0);
            return index + 1;
        }
        else
            throw new IllegalArgumentException("Cannot handle data of type " + data.getClass().getName());
    }

    /** @return Data type of array elements: {@link Double}, ..., {@link Byte} */
    public NDType getType()
    {
        if (data instanceof ArrayDouble)
            return NDType.FLOAT64;
        else if (data instanceof ArrayFloat)
            return NDType.FLOAT32;
        if (data instanceof ArrayLong)
            return NDType.INT64;
        else if (data instanceof ArrayInteger)
            return NDType.INT32;
        if (data instanceof ArrayShort)
            return NDType.INT16;
        else if (data instanceof ArrayByte)
        {
            if (is_boolean)
                return NDType.BOOL;
            return NDType.INT8;
        }
        throw new IllegalStateException("Unhandled data type " + data.getClass().getName());
    }

    /** @return NDArray that holds the data, or <code>null</code> if this array owns the data */
    public NDArray getBase()
    {
        return base;
    }

    /** @return Shape of the array */
    public NDShape getShape()
    {
        return shape;
    }

    /** @return Number of dimensions, e.g. 2 for an array of shape [4, 3] */
    public int getRank()
    {
        return shape.getDimensions();
    }

    /** @return Number of elements in flattened array */
    public int getSize()
    {
        // Cannot use data.getSize() because this may be
        // a view of a sub-section of the raw data
        return shape.getSize();
    }

    /** @return Stride */
    public NDStrides getStrides()
    {
        return stride;
    }

    /** @return Iterator for the flat data */
    public IteratorNumber getIterator()
    {   // Can iterate over the raw data?
        if (offset == 0  &&  stride.isDefault(shape))
            return data.iterator();
        // Use array iterator that accounts for offset, strides, ...
        return new NDArrayIterator(this);
    }

    /** Read a flat array element
     *  @param index Flat index, 0 ... <code>getSize()-1</code>
     *  @return Number at that index
     */
    public double getFlatDouble(final int index)
    {
        return data.getDouble(offset + index);
    }

    /** Write a flat array element
     *  @param index Flat index, 0 ... <code>getSize()-1</code>
     *  @param value Desired value for that element
     */
    public void setFlatDouble(final int index, final double value)
    {
        data.setDouble(offset + index, value);
    }

    /** Access array element as double
     *  @param position Indices into array. Number of indices must match rank
     *  @return Array element as double
     *  @see #getRank()
     */
    public double getDouble(int... position)
    {
        return getFlatDouble(stride.getIndex(shape, position));
    }

    /** Set array element as double
     *  @param value Desired value
     *  @param position Indices into array. Number of indices must match rank
     *  @see #getRank()
     */
    public void setDouble(final double value, int... position)
    {
        setFlatDouble(stride.getIndex(shape, position), value);
    }

    /** Set array elements from other array
     *  @param other Array that provides values
     */
    public void set(final NDArray other)
    {
        switch (NDCompatibility.forArrays(this, other))
        {
        case FLAT_ITERATION:
            for (int i=getSize()-1; i>=0; --i)
                setFlatDouble(i, other.getFlatDouble(i));
            break;
        case SHAPE_ITERATION:
            final ShapeIterator iter = new ShapeIterator(shape);
            while (iter.hasNext())
            {
                final int[] position = iter.getPosition();
                setDouble(other.getDouble(position), position);
            }
            break;
        case BROADCAST_ITERATION:
            final BroadcastIterator i = new BroadcastIterator(shape, other.getShape());
            if (! shape.equals(i.getBroadcastShape()))
                throw new IllegalArgumentException("Cannot assign to array with shape " +
                        shape + " from incompatible shape " + other.getShape());
            while (i.hasNext())
            {
                final double value = other.getDouble(i.getPosB());
                setDouble(value, i.getPosition());
            }
            break;
        default:
            throw new IllegalArgumentException("Cannot assign to array with shape " +
                    shape + " from incompatible shape " + other.getShape());
        }
    }

    /** Create slice, i.e. view of subsection of the array
     *
     *  <p>Requires a start:stop:step slice specification for each
     *  dimension of the array.
     *
     *  <p>When the stop and step values for a dimension are 0,
     *  just the start value is used as an index for a single element
     *  on that axis, collapsing the output by removing that axis.

     *  <p>For example, assume a 2x2 array
     *  <pre>
     *  [ [ 41, 42 ], [ 51, 52 ]
     *  </pre>
     *
     *  When fetching the slice [1:2:1, 0:2:1] the result will
     *  be another 2D array of shape (1, 2)
     *  <pre>
     *  [ [ 51, 52 ] ]
     *  </pre>
     *
     *  When instead specifying stop and step values of 0,
     *  the result will be a 1D array of shape (2,)
     *  because the axis with just one element has been removed:
     *  <pre>
     *  [ 51, 52 ]
     *  </pre>
     *
     *  @param start Start indices
     *  @param stop Stop indices
     *  @param step Step sizes
     *  @return View to slice of original array
     */
    public NDArray getSlice(final int[] start, final int[] stop, final int[] step)
    {
        // Check input
        if (start.length != getRank())
            throw new IllegalArgumentException("Need " + getRank() + " indices, got " + start.length);
        if (start.length != stop.length)
            throw new IllegalArgumentException("Length of start and stop indices differ");
        if (start.length != step.length)
            throw new IllegalArgumentException("Length of start and step indices differ");

        // Determine index of first element
        final int offset = this.offset + stride.getIndex(shape, start);

        // Determine new shape and stride
        final List<Integer> n_shape = new ArrayList<>();
        final List<Integer> n_stride = new ArrayList<>();
        for (int i=0; i<start.length; ++i)
        {
            // System.out.println("Requesting " + start[i] + ":" + stop[i] + ":" + step[i]
            //        + " from 0:" + shape.getSize(i) + ":1");
            final int dim;
            final int hop;
            if (step[i] <= 0)
            {
                dim = 1;
                hop = stride.getStride(i);
            }
            else
            {
                dim = getCount(shape.adjustIndex(i, start[i]),
                               shape.adjustIndex(i,  stop[i]), step[i]);
                hop = stride.getStride(i) * step[i];
                n_shape.add(dim);
                n_stride.add(hop);
            }
        }

        return new NDArray(this, new NDShape(n_shape), offset, new NDStrides(n_stride));
    }

    /** Compare arrays by shape and element values
     *  {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof NDArray))
            return false;
        final NDArray other = (NDArray) obj;

        if (! shape.equals(other.shape))
            return false;
        // Compare as double
        if (stride.equals(other.stride))
        {    // If strides match, perform flat comparison
            final int len = getSize();
            for (int i=0; i<len; ++i)
                if (getFlatDouble(i) != other.getFlatDouble(i))
                    return false;
        }
        else
        {   // Iterate to compare
            final IteratorNumber i1 = getIterator();
            final IteratorNumber i2 = other.getIterator();
            while (i1.hasNext() && i2.hasNext())
                if (i1.nextDouble() != i2.nextDouble())
                    return false;
        }
        return true;
    }

    /** @return Array formatted as text "[[1,2],[3,4]]" */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        final int[] indices = new int[shape.getDimensions()];
        printToBuf(buf, 0, indices);
        return buf.toString();
    }

    /** Recursively print array content
     *  @param buf Target buffer
     *  @param dim Current dimension to handle within the recursion
     *  @param position Array indices
     */
    private void printToBuf(final StringBuilder buf, final int dim, final int[] position)
    {
        if (dim == getRank())
        {    // Print actual values on inner level
            if (is_boolean)
                buf.append(getDouble(position) > 0 ? "True" : "False");
            else
                buf.append(getDouble(position));
            return;
        }
        // Recurse towards inner level
        final int len = shape.getSize(dim);
        // If shape has more than one dimension,
        // print one line per outermost dimension
        final boolean outermost = dim == 0  && getRank() > 1;
        buf.append("[ ");
        for (int i=0; i<len; ++i)
        {
            position[dim] = i;
            if (i > 0)
                buf.append(", ");
            if (outermost)
                buf.append("\n  ");
            printToBuf(buf, dim+1, position);
        }
        if (outermost)
            buf.append("\n]");
        else
            buf.append(" ]");
    }
}
