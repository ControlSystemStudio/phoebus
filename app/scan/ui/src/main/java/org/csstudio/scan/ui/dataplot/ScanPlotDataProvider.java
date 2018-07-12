/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.dataplot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;

/** Data provider for RTPlot based on scan data
 *  @author Kay Kasemir
 */
public class ScanPlotDataProvider implements PlotDataProvider<Double>
{
    /** Adapt an X/Y {@link ScanSample} pair into a {@link PlotDataItem} */
    private static class DataItemAdapter implements PlotDataItem<Double>
    {
        private final ScanSample x, y;

        DataItemAdapter(final ScanSample x, final ScanSample y)
        {
            this.x = x;
            this.y = y;
        }

        @Override
        public Double getPosition()
        {
            return ScanSampleFormatter.asDouble(x);
        }

        @Override
        public double getValue()
        {
            return ScanSampleFormatter.asDouble(y);
        }
    };

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String x_device, y_device;
    private volatile List<PlotDataItem<Double>> samples = new ArrayList<>();

    public ScanPlotDataProvider(final String x_device, final String y_device)
    {
        this.x_device = x_device;
        this.y_device = y_device;
    }

    @Override
    public Lock getLock()
    {
        return lock.readLock();
    }

    @Override
    public int size()
    {
        return samples.size();
    }

    @Override
    public PlotDataItem<Double> get(final int index)
    {
        return samples.get(index);
    }

    public void update(final ScanData data)
    {
        final ScanDataIterator iter = new ScanDataIterator(data, x_device, y_device);

        lock.writeLock().lock();
        try
        {
            samples.clear();
            while (iter.hasNext())
            {
                final ScanSample[] s = iter.getSamples();
                samples.add(new DataItemAdapter(s[0], s[1]));
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}
