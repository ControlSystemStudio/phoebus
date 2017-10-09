/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code short[]} into a {@link ListShort}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayShort extends ListShort implements Serializable {

    private static final long serialVersionUID = 7493025761455302921L;

    private final short[] array;
    private final boolean readOnly;

    /**
     * A new {@code ArrayShort} that wraps around the given array.
     *
     * @param array an array
     */

    public ArrayShort(short... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayShort} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */

    public ArrayShort(short[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorShort iterator() {
        return new IteratorShort() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public short nextShort() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public short getShort(int index) {
        return array[index];
    }

    @Override
    public void setShort(int index, short value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayShort) {
            return Arrays.equals(array, ((ArrayShort) obj).array);
        }

        return super.equals(obj);
    }

    short[] wrappedArray() {
        return array;
    }
}
