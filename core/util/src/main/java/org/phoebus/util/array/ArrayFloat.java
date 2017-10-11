/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code float[]} into a {@link ListFloat}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayFloat extends ListFloat implements Serializable {

    private static final long serialVersionUID = 7493025761455302918L;

    private final float[] array;
    private final boolean readOnly;

    /**
     * A new read-only {@code ArrayFloat} that wraps around the given array.
     *
     * @param array an array
     */

    public ArrayFloat(float... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayFloat} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */
    public ArrayFloat(float[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorFloat iterator() {
        return new IteratorFloat() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public float nextFloat() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public float getFloat(int index) {
        return array[index];
    }

    @Override
    public void setFloat(int index, float value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayFloat) {
            return Arrays.equals(array, ((ArrayFloat) obj).array);
        }

        return super.equals(obj);
    }

    float[] wrappedArray() {
        return array;
    }
}
