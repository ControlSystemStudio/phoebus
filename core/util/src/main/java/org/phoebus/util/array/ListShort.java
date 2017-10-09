/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An ordered collection of {@code short}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class ListShort implements ListNumber, CollectionShort {

    @Override
    public IteratorShort iterator() {
        return new IteratorShort() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public short nextShort() {
                return getShort(index++);
            }
        };
    }

    @Override
    public double getDouble(int index) {
        return (float) getShort(index);
    }

    @Override
    public float getFloat(int index) {
        return (float) getShort(index);
    }

    @Override
    public long getLong(int index) {
        return (long) getShort(index);
    }

    @Override
    public int getInt(int index) {
        return (int) getShort(index);
    }

    @Override
    public byte getByte(int index) {
        return (byte) getShort(index);
    }

    @Override
    public void setDouble(int index, double value) {
        setShort(index, (short) value);
    }

    @Override
    public void setFloat(int index, float value) {
        setShort(index, (short) value);
    }

    @Override
    public void setLong(int index, long value) {
        setShort(index, (short) value);
    }

    @Override
    public void setInt(int index, int value) {
        setShort(index, (short) value);
    }

    @Override
    public void setShort(int index, short value) {
        throw new UnsupportedOperationException("Read only list.");
    }

    @Override
    public void setByte(int index, byte value) {
        setShort(index, (short) value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof ListShort) {
            ListShort other = (ListShort) obj;

            if (size() != other.size())
                return false;

            for (int i = 0; i < size(); i++) {
                if (getShort(i) != other.getShort(i))
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
            result = 31 * result + getShort(i);
        }
        return result;
    }

}
