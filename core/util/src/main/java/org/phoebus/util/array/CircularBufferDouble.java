/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.array;

/**
 * An implementation of a list on top of a circular buffer. The buffer
 * will start at the initial capacity (default 10) and will continue
 * to grow until the max capacity is reached. At that point, it will
 * start to replace the oldest value with a newer one.
 *
 * @author carcassi
 */
public class CircularBufferDouble extends ListDouble {

    private double[] data;
    private int startOffset;
    private int endOffset;
    private final int maxCapacity;
    private boolean reachedMax;

    /**
     * Creates a new circular buffer with the given maximum capacity.
     *
     * @param maxCapacity maximum capacity
     */
    public CircularBufferDouble(int maxCapacity) {
        this(Math.min(10, maxCapacity), maxCapacity);
    }

    /**
     * Creates a new circular buffer with the given initial and maximum
     * capacity.
     *
     * @param initialCapacity initial capacity
     * @param maxCapacity  maximum capacity
     */
    public CircularBufferDouble(int initialCapacity, int maxCapacity) {
        data = new double[initialCapacity];
        this.maxCapacity = maxCapacity;
    }

    private void resize() {
        int oldSize = data.length;
        int newSize = oldSize * 2;
        if (newSize > maxCapacity) {
            newSize = maxCapacity + 1;
            reachedMax = true;
        }
        double[] newData = new double[newSize];
        System.arraycopy(data, 0, newData, 0, oldSize);
        data = newData;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public double getDouble(int index) {
        index += startOffset;
        if (index >= data.length) {
            index -= data.length;
        }
        return data[index];
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int size() {
        int size = endOffset - startOffset;
        if (size < 0) {
            size += data.length;
        }
        return size;
    }

    /**
     * Adds a new value.
     *
     * @param value new value
     */
    public void addDouble(double value) {
        data[endOffset] = value;
        endOffset++;

        // Grow the buffer if needed
        if (endOffset == data.length && !reachedMax)
            resize();

        // Loop over and advance the start point if needed
        if (endOffset == data.length) {
            endOffset = 0;
        }
        if (endOffset == startOffset)
            startOffset++;
        if (startOffset == data.length)
            startOffset = 0;
    }

    /**
     * Removes all values from the buffer.
     */
    public void clear() {
        startOffset = 0;
        endOffset = 0;
    }

    /**
     * The maximum capacity for this circular buffer.
     *
     * @return maximum capacity
     */
    public int getCurrentCapacity() {
        return reachedMax ? maxCapacity : data.length;
    }
}
