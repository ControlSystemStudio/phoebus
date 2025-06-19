package org.csstudio.trends.databrowser3.ui.smoothview;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Data provider implementation for the plot, with thread-safe access
 */
public class SmoothData implements PlotDataProvider<Instant> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private List<PlotDataItem<Instant>> data = new ArrayList<>();

    @Override
    public java.util.concurrent.locks.Lock getLock() {
        return lock.readLock();
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return data.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public PlotDataItem<Instant> get(final int index) {
        lock.readLock().lock();
        try {
            return data.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Update the data with thread safety
     *
     * @param newData New data items to set
     */
    public void setData(List<PlotDataItem<Instant>> newData) {
        lock.writeLock().lock();
        try {

            this.data = newData;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Update the data with thread safety
     *
     * @param newData New data items to set
     */
    public void addData(PlotDataItem<Instant> newData) {
        lock.writeLock().lock();
        try {
            this.data.add(newData);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
