package org.csstudio.trends.databrowser3.ui.waveformoverlapview;

import org.csstudio.javafx.rtplot.data.InstrumentedReadWriteLock;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.SimpleDataItem;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;


/**
 * Data provider for the plot that shows the overlap of waveform elements of a VNumberArray
 * @author Mingtao Li
 * @since 5.0
 * China Spallation Neutron Sources
 */
@SuppressWarnings("nls")
public class WaveformOverlapValueDataProvider implements PlotDataProvider<Double> {

    final private ReadWriteLock lock = new InstrumentedReadWriteLock();

    private double[] numbers = null;
    private double[] offset;

    public WaveformOverlapValueDataProvider() {
    }

    public WaveformOverlapValueDataProvider(double[] offset) {
        this.offset = offset;
    }

    @Override
    public Lock getLock() {
        return lock.readLock();
    }

    /**
     * Update the waveform value.
     *
     * @param data New value Fires event to listeners (plot)
     */
    public void setValue(double[] data) {


        lock.writeLock().lock();
        try {
            numbers = data;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        return numbers == null ? 0 : numbers.length;
    }

    @Override
    public PlotDataItem<Double> get(final int index) {
        return new SimpleDataItem<>((double) index, offset == null ? numbers[index] : numbers[index] - offset[index]);
    }

    @Override
    public String toString() {
        return "WaveformOverlapValueDataProvider, lock: " + lock;
    }
}
