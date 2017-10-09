/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Wraps a {@code double[]} into a {@link ListDouble}.
 *
 * @author Gabriele Carcassi
 */
public final class ArrayDouble extends ListDouble implements Serializable {

    private static final long serialVersionUID = 7493025761455302917L;

    private final double[] array;
    private final boolean readOnly;

    /**
     * A new read-only {@code ArrayDouble} that wraps around the given array.
     *
     * @param array an array
     */
    public ArrayDouble(double... array) {
        this(array, true);
    }

    /**
     * A new {@code ArrayDouble} that wraps around the given array.
     *
     * @param array an array
     * @param readOnly if false the wrapper allows writes to the array
     */
    public ArrayDouble(double[] array, boolean readOnly) {
        this.array = array;
        this.readOnly = readOnly;
    }

    @Override
    public final IteratorDouble iterator() {
        return new IteratorDouble() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < array.length;
            }

            @Override
            public double nextDouble() {
                return array[index++];
            }
        };
    }

    @Override
    public final int size() {
        return array.length;
    }

    @Override
    public double getDouble(int index) {
        return array[index];
    }

    @Override
    public void setDouble(int index, double value) {
        if (!readOnly) {
            array[index] = value;
        } else {
            throw new UnsupportedOperationException("Read only list.");
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof ArrayDouble) {
            return Arrays.equals(array, ((ArrayDouble) obj).array);
        }

        return super.equals(obj);
    }

    double[] wrappedArray() {
        return array;
    }
}
