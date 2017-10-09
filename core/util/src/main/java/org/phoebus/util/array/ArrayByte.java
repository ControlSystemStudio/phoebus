/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code byte[]} into a {@link ListByte}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayByte extends ListByte implements Serializable {

    private static final long serialVersionUID = 7493025761455302916L;

    private final byte[] array;
    private final boolean readOnly;

    /**
     * A new read-only {@code ArrayByte} that wraps around the given array.
     *
     * @param array an array
     */
    public ArrayByte(byte... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayByte} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */
    public ArrayByte(byte[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorByte iterator() {
        return new IteratorByte() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public byte nextByte() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public final byte getByte(int index) {
        return array[index];
    }

    @Override
    public void setByte(int index, byte value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayByte) {
            return Arrays.equals(array, ((ArrayByte) obj).array);
        }

        return super.equals(obj);
    }

    byte[] wrappedArray() {
        return array;
    }

}
