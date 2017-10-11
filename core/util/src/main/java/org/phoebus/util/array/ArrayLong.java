/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code long[]} into a {@link ListLong}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayLong extends ListLong implements Serializable {

    private static final long serialVersionUID = 7493025761455302920L;

    private final long[] array;
    private final boolean readOnly;

    /**
     * A new {@code ArrayLong} that wraps around the given array.
     *
     * @param array an array
     */
    public ArrayLong(long... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayLong} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */
    public ArrayLong(long[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorLong iterator() {
        return new IteratorLong() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public long nextLong() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public long getLong(int index) {
        return array[index];
    }

    @Override
    public void setLong(int index, long value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayLong) {
            return Arrays.equals(array, ((ArrayLong) obj).array);
        }

        return super.equals(obj);
    }

    long[] wrappedArray() {
        return array;
    }
}
