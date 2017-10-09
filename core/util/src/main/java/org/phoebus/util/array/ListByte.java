/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An ordered collection of {@code byte}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class ListByte implements ListNumber, CollectionByte {

    @Override
    public IteratorByte iterator() {
        return new IteratorByte() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public byte nextByte() {
                return getByte(index++);
            }
        };
    }

    @Override
    public double getDouble(int index) {
        return (float) getByte(index);
    }

    @Override
    public float getFloat(int index) {
        return (float) getByte(index);
    }

    @Override
    public long getLong(int index) {
        return (long) getByte(index);
    }

    @Override
    public int getInt(int index) {
        return (int) getByte(index);
    }

    @Override
    public short getShort(int index) {
        return (short) getByte(index);
    }

    @Override
    public void setDouble(int index, double value) {
        setByte(index, (byte) value);
    }

    @Override
    public void setFloat(int index, float value) {
        setByte(index, (byte) value);
    }

    @Override
    public void setLong(int index, long value) {
        setByte(index, (byte) value);
    }

    @Override
    public void setInt(int index, int value) {
        setByte(index, (byte) value);
    }

    @Override
    public void setShort(int index, short value) {
        setByte(index, (byte) value);
    }

    @Override
    public void setByte(int index, byte value) {
        throw new UnsupportedOperationException("Read only list.");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        // Should compare to the higher precision if needed
        if (obj instanceof ListDouble || obj instanceof ListFloat ||
                obj instanceof ListLong || obj instanceof ListInt ||
                obj instanceof ListShort) {
            return obj.equals(this);
        }

        if (obj instanceof ListNumber) {
            ListNumber other = (ListNumber) obj;

            if (size() != other.size())
                return false;

            for (int i = 0; i < size(); i++) {
                if (getByte(i) != other.getByte(i))
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
