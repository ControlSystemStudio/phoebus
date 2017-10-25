/*******************************************************************************
 * Copyright (c) 2014-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

import java.util.Arrays;

/** List of <code>int</code>
 *
 *  <p>Fundamentally like <code>List&lt;Integer></code>,
 *  but avoids boxing operations.
 *
 *  @author Kay Kasemir
 */
public class IntList
{
    private int[] data;
    private int size;

    /** @param capacity Initial capacity */
    public IntList(final int capacity)
    {
        data = new int[capacity];
        size = 0;
    }

    /** @return Size */
    final public int size()
    {
        return size;
    }

    /** @param value Value to add */
    final public void add(final int value)
    {
        if (size == data.length)
            data = Arrays.copyOf(data, 2*data.length);
        data[size++] = value;
    }

    /** @param index Valid index 0 .. size()-1
     *  @param value Value for that array element
     */
    public void set(final int index, final int value)
    {
        data[index] = value;
    }

    /** @param index Index of value to get
     *  @return Value at that index
     */
    final public int get(final int index)
    {
        return data[index];
    }

    /** Clear array elements */
    final public void clear()
    {
        size = 0;
    }

    /** @return Plain <code>int</code> array of data */
    final public int[] getArray()
    {
        return data;
    }

    /** @return Plain <code>int</code> array, sized to exact data bounds */
    final public int[] toArray()
    {
        final int[] copy = new int[size];
        System.arraycopy(data, 0, copy, 0, size);
        return copy;
    }
}
