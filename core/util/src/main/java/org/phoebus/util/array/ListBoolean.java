/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An ordered collection of {@code boolean}s. Since in Java Boolean does
 * not inherit from Number, ListBoolean does not inherit from ListNumber.
 *
 * @author Gabriele Carcassi
 */
public abstract class ListBoolean {

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index position of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public abstract boolean getBoolean(int index);

    /**
     * Changes the element at the specified position.
     *
     * @param index position of the element to change
     * @param value the new value
     * @throws IndexOutOfBoundsException if the index is out of range
     *         (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public abstract void setBoolean(int index, boolean value);

    /**
     * Returns the number of elements in the collection.
     *
     * @return the number of elements in the collection
     */
    public abstract int size();

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj instanceof ListBoolean) {
            ListBoolean other = (ListBoolean) obj;

            if (size() != other.size())
                return false;

            for (int i = 0; i < size(); i++) {
                if (getBoolean(i) != other.getBoolean(i))
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
            result = 31 * result + (getBoolean(i) ? 1 : 0);
        }
        return result;
    }

}
