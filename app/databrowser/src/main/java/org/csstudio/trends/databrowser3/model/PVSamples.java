/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.Messages;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Time;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.util.time.TimeInterval;
import org.phoebus.util.time.TimeRelativeInterval;

/** Samples of a {@link PVItem}.
 *  <p>
 *  Made up of two sections,
 *  {@link HistoricSamples} and {@link LiveSamples},
 *  and presenting them as one long stream of samples.
 *
 *  In addition, if the last sample is valid, it's
 *  extended to 'now' assuming no new data means
 *  that the last value is still valid.
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed PVSamples to handle waveform index.
 */
@SuppressWarnings("nls")
public class PVSamples extends PlotSamples
{
    /* history and live are each PlotSamples, i.e. they
     * have a read/write lock, but those are never used.
     *
     * Instead, all read accesses to them goes via
     * PVSamples.size(), PVSamples.get()
     * and caller should take the PVSamples lock.
     *
     * Write access goes via
     * PVSamples.add*(), PVSamples.mergeArchivedData(),
     * which take the write lock.
     */
    /** Historic samples */
    final private HistoricSamples history;

    /** Live samples. Should start after end of historic samples */
    final private LiveSamples live;

    private boolean emptyHistoryOnAdd = false;
    private int samplesAddedSinceLastRefresh = 0;

    PVSamples(final AtomicInteger waveform_index)
    {
        history = new HistoricSamples(waveform_index);
        live = new LiveSamples(waveform_index);
    }

    /** @return Maximum number of live samples in ring buffer */
    public int getLiveCapacity()
    {
        return live.getCapacity();
    }

    /** Set new capacity for live sample ring buffer
     *  <p>
     *  @param new_capacity New sample count capacity
     *  @throws Exception on out-of-memory error
     */
    public void setLiveCapacity(final int new_capacity) throws Exception
    {
        live.setCapacity(new_capacity);
    }

    /** @return Combined count of historic and live samples */
    @Override
    public int size()
    {
        final int raw = getRawSize();
        if (raw <= 0)
            return raw;
        final PlotSample last = get(raw-1);
        if (org.phoebus.core.vtypes.VTypeHelper.getSeverity(last.getVType()) == AlarmSeverity.UNDEFINED)
            return raw;
        // Last sample is valid, so it should still apply 'now'
        return raw+1;
    }

    /** @return Size of the actual historic and live samples
     *          without the continuation to 'now'
     */
    private int getRawSize()
    {
        return history.size() + live.size();
    }

    /** @param index 0... getSize()-1
     *  @return Sample from historic or live sample subsection
     */
    @Override
    public PlotSample get(final int index)
    {
        // For debugging, show stack trace when missing lock
        if (lock.getReadHoldCount() <= 0  && ! lock.isWriteLockedByCurrentThread())
            logger.log(Level.WARNING, "Missing lock", new Exception("Stack Trace"));

        final int raw_count = getRawSize();
        if (index < raw_count)
            return getRawSample(index);
        // Last sample is valid, so it should still apply 'now'
        final PlotSample sample = getRawSample(raw_count-1);
        if (Instant.now().compareTo(sample.getPosition()) < 0)
            return sample;
        else
            return new PlotSample(sample.getSource(), VTypeHelper.transformTimestampToNow(sample.getVType()));
    }

    /** Get 'raw' sample, no continuation until 'now'
     *  @param index 0... getRawSize()-1
     *  @return Sample from historic or live sample subsection
     */
    private PlotSample getRawSample(final int index)
    {
        final int num_old = history.size();
        if (index < num_old)
            return history.get(index);
        return live.get(index - num_old);
    }

    /** Test if samples changed since the last time
     *  <code>testAndClearNewSamplesFlag</code> was called.
     *  @return <code>true</code> if there were new samples
     */
    @Override
    public boolean hasNewSamples()
    {
        return history.hasNewSamples() | live.hasNewSamples();
    }

    /** Test if samples changed since the last time this method was called.
     *  @return <code>true</code> if there were new samples
     */
    @Override
    public boolean testAndClearNewSamplesFlag()
    {
        // Must check & __clear__ both subsections!
        // return hist.test() | live.test() would skip
        // the live.test if hist.test() was already true!
        final boolean hist_change = history.testAndClearNewSamplesFlag();
        final boolean live_change = live.testAndClearNewSamplesFlag();
        return hist_change | live_change;
    }

    /** Add data retrieved from an archive to the 'historic' section
     *  @param source Source of the samples
     *  @param result Historic data
     */
    public void mergeArchivedData(final String source,
            final List<VType> result)
    {
        if (! lockForWriting())
            return;
        try
        {
            if (emptyHistoryOnAdd)
            {
                emptyHistoryOnAdd = false;
                history.clear();
            }
            history.mergeArchivedData(source, result);
        }
        finally
        {
            unlockForWriting();
        }
    }

    /** Add another 'live' sample
     *  @param value 'Live' sample
     */
    public void addLiveSample(VType value)
    {
        if (! Time.timeOf(value).isValid())
            value = VTypeHelper.transformTimestampToNow(value);
        addLiveSample(new PlotSample(Messages.LiveData, value));
    }

    /** Add another 'live' sample
     *  @param sample 'Live' sample
     */
    public void addLiveSample(final PlotSample sample)
    {
        if (! lockForWriting())
            return;
        try
        {
            // Skip the initial UNDEFINED/Disconnected sample sent by PVManager
            if (live.size() == 0  &&
                org.phoebus.core.vtypes.VTypeHelper.getSeverity(sample.getVType()) == AlarmSeverity.UNDEFINED)
                return;
            live.add(sample);
            // History ends before the start of 'live' samples.
            // Adding a live sample might have moved the ring buffer,
            // so need to update whenever live data is extended.
            history.setBorderTime(Optional.of(live.get(0).getPosition()));
        }
        finally
        {
            unlockForWriting();
        }
        samplesAddedSinceLastRefresh++;
    }

    /** Delete all samples */
    public void clear()
    {
        if (! lockForWriting())
            return;
        try
        {
            history.clear();
            live.clear();
        }
        finally
        {
            unlockForWriting();
        }
    }

    /**
     * Check if the current data matches the criteria to refresh the history data.
     * History data is refreshed when the live buffer is full and there exists a gap
     * between the last history data and the current live data. This method will
     * also trigger purge of all currently cached history data of this sample.
     *
     * This method can only ever be called from the PVItem.
     *
     * @param interval Time range of the current visible window on the chart
     * @return true if the history data needs to be refreshed or false otherwise
     */
    boolean isHistoryRefreshNeeded(final TimeRelativeInterval interval)
    {
        try
        {
            if (! getLock().tryLock(10, TimeUnit.SECONDS))
                throw new TimeoutException();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot lock " + this, ex);
            return false;
        }

        final TimeInterval abs = interval.toAbsoluteInterval();
        try
        {
            //if already waiting for history to be loaded, wait on
            if (emptyHistoryOnAdd) return false;
            //if the live samples have more than 15% of capacity left before old data is erased,
            //refresh is not yet needed
            if (samplesAddedSinceLastRefresh < live.getCapacity()*0.85) return false;
            //if live data hasn't reached capacity, do not refresh anything
            if (live.size() < live.getCapacity() || live.size() == 0) return false;
            //if there is no history data, there is nothing to refresh anyway
            if (history.getRawSize() == 0) return false;
            PlotSample first = live.get(0);
            //if the first time in the live data is smaller than the visible start time,
            //the buffer is large enough to contain all the "currently" visible data
            if (first.getPosition().compareTo(abs.getStart()) <= 0) return false;
            PlotSample last = live.get(live.size()-1);
            //if the las sample is greater than the current end time than we are not
            //looking at the live data
            if (last.getPosition().compareTo(abs.getEnd()) > 0) return false;
            PlotSample historyLast = history.getRawSample(history.getRawSize()-1);
            //if the last raw history data is smaller than the first live sample, do refresh
            if (historyLast.getPosition().compareTo(first.getPosition()) < 0) {
                samplesAddedSinceLastRefresh = 0;
                emptyHistoryOnAdd = true;
                return true;
            }
            //maybe we are looking at live data with a window extending into the future:
            //in such case check the number of samples that arrived since the previous refresh
            if (samplesAddedSinceLastRefresh > live.getCapacity()) {
                samplesAddedSinceLastRefresh = 0;
                emptyHistoryOnAdd = true;
                return true;
            }
        }
        finally
        {
            getLock().unlock();
        }
        return false;
    }

    /** @return (Long) string representation for debugging */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("PV Samples, lock state: ").append(lock);

        buf.append("\nHistory: ");
        buf.append(history.toString());

        buf.append("\nLive Buffer: ");
        buf.append(live.toString());

        if (getLock().tryLock())
        {
            try
            {
                final int count = size();
                if (count != getRawSize())
                {
                    buf.append("\nContinuation to 'now':\n");
                    buf.append("     " + get(count-1));
                }
            }
            finally
            {
                getLock().unlock();
            }
        }
        return buf.toString();
    }
}
