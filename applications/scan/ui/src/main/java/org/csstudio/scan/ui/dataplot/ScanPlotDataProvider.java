package org.csstudio.scan.ui.dataplot;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;

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

    private final Lock lock = new ReentrantLock();
    private final String x_device, y_device;

    public ScanPlotDataProvider(final String x_device, final String y_device)
    {
        this.x_device = x_device;
        this.y_device = y_device;
    }

    @Override
    public Lock getLock()
    {
        return lock;
    }

    @Override
    public int size()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public PlotDataItem<Double> get(int index)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void update(final ScanData data)
    {
        final ScanDataIterator iter = new ScanDataIterator(data, x_device, y_device);
        while (iter.hasNext())
        {
            final ScanSample[] samples = iter.getSamples();

            // TODO Collect samples, ...
            new DataItemAdapter(samples[0], samples[1]);
        }
    }

}
