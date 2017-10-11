/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An implementation of a list on top of buffer. The buffer
 * will start at the initial capacity (default 10) and will continue
 * to grow.
 *
 * @author carcassi
 */
public class BufferInt extends ListInt {

    private int[] data;
    private int endOffset;

    /**
     * Creates a new buffer.
     */
    public BufferInt() {
        this(10);
    }

    /**
     * Creates a new buffer.
     *
     * @param initialCapacity initial capacity
     */
    public BufferInt(int initialCapacity) {
        data = new int[initialCapacity];
    }

    private void resize() {
        int oldSize = data.length;
        int newSize = oldSize * 2;
        int[] newData = new int[newSize];
        System.arraycopy(data, 0, newData, 0, oldSize);
        data = newData;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getInt(int index) {
        if (index >= endOffset) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return data[index];
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int size() {
        int size = endOffset;
        return size;
    }

    /**
     * Adds a new value.
     *
     * @param value new value
     */
    public void addInt(int value) {
        data[endOffset] = value;
        endOffset++;

        // Grow the buffer if needed
        if (endOffset == data.length)
            resize();

    }

    /**
     * Removes all values from the buffer.
     */
    public void clear() {
        endOffset = 0;
    }

    /**
     * The maximum capacity for this buffer.
     *
     * @return maximum capacity
     */
    public int getCurrentCapacity() {
        return data.length;
    }
}
