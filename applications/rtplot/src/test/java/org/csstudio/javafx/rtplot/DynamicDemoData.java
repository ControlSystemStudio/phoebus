/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;

/** Demo data ring buffer
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DynamicDemoData implements PlotDataProvider<Instant>
{
    final private ReadWriteLock lock = new InstrumentedReadWriteLock();
    final private PlotDataItem<Instant>[] data;
    final private long start_secs;
    final private double period;
    private int start = 0, size = 0;

    @SuppressWarnings("unchecked")
    public DynamicDemoData(final int capacity, final double period)
    {
        data = new PlotDataItem[capacity];
        start_secs = Instant.now().getEpochSecond();
        this.period = period;
    }

    /** Add a new sample */
    public void add()
    {
        final Instant time = Instant.now();
        final PlotDataItem<Instant> item;
        // 5% dropouts
//        if (Math.random() > 0.95)
//        {
//            data.add(new SimpleDataItem<Instant>(time, Double.NaN));
//        }
//        else
        {
            final double x = (time.getEpochSecond() - start_secs) + time.getNano()*1e-9;
            final double y = 0.1*Math.random()  +  2.0 + Math.sin(2.0*Math.PI * x / period);
            // Some raw samples, rest min/max/average
            if (Math.random() > 0.3)
                item = new SimpleDataItem<Instant>(time, y);
            else
            {
                final double noise = 0.2;
                final double min = y - y*noise*Math.random();
                final double max = y + y*noise*Math.random();
                final double stddev = (max - y)/2;
                item = new SimpleDataItem<Instant>(time, y, stddev, min, max, "Optimized");
            }
        }

        lock.writeLock().lock();
        try
        {
            // Obtain index of next element
            if (size >= data.length)
            {
                ++start; // Overwrite oldest element
                if (start >= data.length)
                    start = 0;
            }
            else
                ++size; // Add to end of buffer
            final int i = (start + size - 1) % data.length;
            // Update that element
            data[i] = item;
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Lock getLock()
    {
        return lock.readLock();
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public PlotDataItem<Instant> get(int index)
    {
        if (index<0 || index >= size)
            throw new ArrayIndexOutOfBoundsException(index);
        index = (start + index) % data.length;
        return data[index];
    }
}
