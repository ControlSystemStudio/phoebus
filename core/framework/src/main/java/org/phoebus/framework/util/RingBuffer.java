/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.framework.util;

import java.util.Arrays;

/** Generic Ring Buffer.
 *
 *  <p>Elements can be added. When ring buffer is full, oldest elements
 *  are dropped for each newly added item.
 *
 *  @author Kay Kasemir
 *  @param <T> Element type
 */
public class RingBuffer<T>
{
    //  The Circular buffer implementation:
    //
    //  Initial: start = size = 0.
    //
    //  Indices of valid entries:
    //  [start], [start+1], ..., [start+size-1]
    //  with wrap-around at [capacity-1].
    //
    // Could also use start/end indices.
    // This implementation gives more efficient size() call.
    private T ring[];
    private int start, size, capacity;

    /** Initialize
     *  @param capacity Initial capacity
     */
    @SuppressWarnings("unchecked")
    public RingBuffer(final int capacity)
    {
        ring = (T[]) new Object[capacity];
        this.capacity = capacity;
        clear();
    }

    /** @return <code>true</code> if ring buffer is empty */
    public boolean isEmpty()
    {
        return size <= 0;
    }

    /** @return <code>true</code> if ring buffer is full,
     *          i.e. the next addition will override the oldest element
     */
    public boolean isFull()
    {
        return size >= capacity;
    }

    /** @return Number of valid entries in ring buffer */
    public int size()
    {
        return size;
    }

    /** @return Maximum number of entries in ring buffer */
    public int getCapacity()
    {
        return capacity;
    }

    /** Set new capacity.
     *
     *  <p>Tries to preserve the newest samples.
     *  @param new_capacity New sample count capacity
     *  @throws Exception on out-of-memory error
     */
    @SuppressWarnings("unchecked")
    public void setCapacity(int new_capacity) throws Exception
    {
        try
        {
            final T new_ring[] = (T[]) new Object[new_capacity];
            // Copy old items over
            if (ring != null)
            {   // How many can be copied?
                int copy_size = size;
                if (copy_size > new_capacity)
                    copy_size = new_capacity;
                // First 'old' item
                final int copy_start = size - copy_size;
                for (int i=0; i<copy_size; ++i)
                    new_ring[i] = get(copy_start + i);
                size = copy_size;
            }
            else
                size = 0;
            ring = new_ring;
            start = 0;
            capacity = new_capacity;
        }
        catch (OutOfMemoryError err)
        {
            throw new Exception("Out of memory: " + err.getMessage()); //$NON-NLS-1$
        }
    }

    /** Remove all ring buffer elements */
    public void clear()
    {
        Arrays.fill(ring, null);
        start = size = 0;
    }

    /** Add item to ring buffer
     *  @param item Item to add
     */
    public void add(final T item)
    {
        // Obtain index of next element
        if (size >= capacity)
        {
            ++start; // Overwrite oldest element
            if (start >= capacity)
                start = 0;
        }
        else
            ++size; // Add to end of buffer
        final int i = (start + size - 1) % capacity;
        // Update that element
        ring[i] = item;
    }

    /** @param i Ring buffer index 0 .. size()-1
     *  @return Item at given index.
     */
    public T get(int i)
    {
        if (i<0 || i >= size)
            throw new ArrayIndexOutOfBoundsException(i);
        i = (start + i) % capacity;
        return ring[i];
    }

    /** Remove the oldest ring buffer element.
     *  @return Oldest ring buffer element or <code>null</code>
     */
    public T remove()
    {
        if (isEmpty())
            return null;
        final T result = ring[start];
        // Remove reference from buffer
        ring[start] = null;
        --size;
        ++start;
        if (start >= capacity)
            start = 0;
        return result;
    }

    /** @return Array with content of ring buffer */
    public T[] toArray(T[] array)
    {
        if (array.length != size)
            array = Arrays.copyOf(array, size);

        //  [start], [start+1], ..., [start+size-1]
        //  with wrap-around at [capacity-1].
        if (start + size <= capacity)
            System.arraycopy(ring, start, array, 0, size);
        else
        {
            final int part = capacity - start;
            System.arraycopy(ring, start, array, 0, part);
            System.arraycopy(ring, 0, array, part, size-part);
        }
        return array;
    }
}
