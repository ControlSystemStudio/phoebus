/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An ordered collection of {@code float}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class ListFloat implements ListNumber, CollectionFloat {

    @Override
    public IteratorFloat iterator() {
        return new IteratorFloat() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public float nextFloat() {
                return getFloat(index++);
            }
        };
    }

    @Override
    public double getDouble(int index) {
        return (double) getFloat(index);
    }

    @Override
    public long getLong(int index) {
        return (long) getFloat(index);
    }

    @Override
    public int getInt(int index) {
        return (int) getFloat(index);
    }

    @Override
    public short getShort(int index) {
        return (short) getFloat(index);
    }

    @Override
    public byte getByte(int index) {
        return (byte) getFloat(index);
    }

    @Override
    public void setDouble(int index, double value) {
        setFloat(index, (float) value);
    }

    @Override
    public void setFloat(int index, float value) {
        throw new UnsupportedOperationException("Read only list.");
    }

    @Override
    public void setLong(int index, long value) {
        setFloat(index, (float) value);
    }

    @Override
    public void setInt(int index, int value) {
        setFloat(index, (float) value);
    }

    @Override
    public void setShort(int index, short value) {
        setFloat(index, (float) value);
    }

    @Override
    public void setByte(int index, byte value) {
        setFloat(index, (float) value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof ListFloat) {
            ListFloat other = (ListFloat) obj;

            if (size() != other.size())
                return false;

            for (int i = 0; i < size(); i++) {
                if (Float.floatToIntBits(getFloat(i)) != Float.floatToIntBits(other.getFloat(i)))
                    return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size(); i++) {
            result = 31 * result + Float.floatToIntBits(getFloat(i));
        }
        return result;
    }
}
