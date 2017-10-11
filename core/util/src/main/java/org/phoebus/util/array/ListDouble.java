/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An ordered collection of {@code double}s.
 *
 * @author Gabriele Carcassi
 */
public abstract class ListDouble implements ListNumber, CollectionDouble {

    @Override
    public IteratorDouble iterator() {
        return new IteratorDouble() {

            private int index;

            @Override
            public boolean hasNext() {
                return index < size();
            }

            @Override
            public double nextDouble() {
                return getDouble(index++);
            }
        };
    }

    @Override
    public float getFloat(int index) {
        return (float) getDouble(index);
    }

    @Override
    public long getLong(int index) {
        return (long) getDouble(index);
    }

    @Override
    public int getInt(int index) {
        return (int) getDouble(index);
    }

    @Override
    public short getShort(int index) {
        return (short) getDouble(index);
    }

    @Override
    public byte getByte(int index) {
        return (byte) getDouble(index);
    }

    @Override
    public void setDouble(int index, double value) {
        throw new UnsupportedOperationException("Read only list.");
    }

    @Override
    public void setFloat(int index, float value) {
        setDouble(index, (double) value);
    }

    @Override
    public void setLong(int index, long value) {
        setDouble(index, (double) value);
    }

    @Override
    public void setInt(int index, int value) {
        setDouble(index, (double) value);
    }

    @Override
    public void setShort(int index, short value) {
        setDouble(index, (double) value);
    }

    @Override
    public void setByte(int index, byte value) {
        setDouble(index, (double) value);
    }

    /**
     * Concatenates a several lists of numbers into a single list
     *
     * @param lists the lists to concatenate
     * @return the given lists concatenated together
     * @author mjchao
     */
    public static ListDouble concatenate( final ListNumber... lists ) {

        //since these lists are read-only, we precompute the size
        int size = 0;
        for ( ListNumber l : lists ) {
            size += l.size();
        }
        final int sizeCopy = size;

        return new ListDouble() {

            @Override
            public int size() {
                return sizeCopy;
            }

            @Override
            public double getDouble( int index ) {
                if ( index < 0 || index >= size() ) {
                    throw new IndexOutOfBoundsException( "Index out of bounds: " + index + ", size: " + size() );
                }
                //treat the lists we concatenated as a whole set - that is
                //we never start back at index 0 after traversing through one
                //of the concatenated lists

                //for example, {a, b, c} {d, e, f} used to be indexed as
                //             {0, 1, 2} {0, 1, 2} and they are now indexed as
                //             {0, 1, 2} {3, 4, 5}
                int startIdx = 0;
                for ( ListNumber l : lists ) {
                    int endIdx = startIdx + l.size()-1;
                    if ( startIdx <= index && index <= endIdx ) {
                        return l.getDouble( index - startIdx );
                    }
                    startIdx += l.size();
                }

                //should never happpen
                return 0;
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof ListDouble) {
            ListDouble other = (ListDouble) obj;

            if (size() != other.size())
                return false;

            for (int i = 0; i < size(); i++) {
                if (Double.doubleToLongBits(getDouble(i)) != Double.doubleToLongBits(other.getDouble(i)))
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
            long bits = Double.doubleToLongBits(getDouble(i));
            result = 31 * result + (int)(bits ^ (bits >>> 32));
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (; i < size() - 1; i++) {
            builder.append(getDouble(i)).append(", ");
        }
        builder.append(getDouble(i)).append("]");
        return builder.toString();
    }

}
