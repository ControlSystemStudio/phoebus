/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code int[]} into a {@link ListInt}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayInt extends ListInt implements Serializable {

    private static final long serialVersionUID = 7493025761455302919L;

    private final int[] array;
    private final boolean readOnly;

    /**
     * A new {@code ArrayInt} that wraps around the given array.
     *
     * @param array an array
     */
    public ArrayInt(int... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayInt} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */
    public ArrayInt(int[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorInt iterator() {
        return new IteratorInt() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public int nextInt() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public int getInt(int index) {
        return array[index];
    }

    @Override
    public void setInt(int index, int value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayInt) {
            return Arrays.equals(array, ((ArrayInt) obj).array);
        }

        return super.equals(obj);
    }

    int[] wrappedArray() {
        return array;
    }
}
