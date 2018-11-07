/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.javafx.rtplot.data.TimeDataSearch;
import org.epics.vtype.VType;

/** Holder for 'historic' samples.
 *  <p>
 *  In addition to holding 'all' historic samples, this class
 *  allows for a 'border' time beyond which no samples will
 *  be provided.
 *  When setting this border to the start of the 'live' samples,
 *  this class will thus assert that the live samples have
 *  precedence because no 'historic' sample is provided
 *  for the 'live' time range.
 *  When the start of the 'live' time range moves because
 *  the live data ring buffer rolls around, the 'border' time adjustments
 *  might then uncover historic samples that were previously
 *  hidden below the 'live' time range.
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed HistoricSamples to handle waveform index.
 */
public class HistoricSamples extends PlotSamples
{
    private class HistoricSampleAccess extends PlotSamples
    {
        @Override
        public int size()
        {
            return samples.length;
        }

        @Override
        public PlotSample get(int index)
        {
            return samples[index];
        }
    };

    // No locking in here, all access is via PVSamples

    /** "All" historic samples */
    private PlotSample samples[] = new PlotSample[0];

    /** If set, samples beyond this time are hidden from access */
    private Optional<Instant> border_time = Optional.empty();

    /** Subset of samples.length that's below border_time
     *  @see #computeVisibleSize()
     */
    private int visible_size = 0;

    /** Waveform index */
    final private AtomicInteger waveform_index;

    final private TimeDataSearch searcher = new TimeDataSearch();

    HistoricSamples(final AtomicInteger waveform_index)
    {
        this.waveform_index = waveform_index;
    }

    /** Define a new 'border' time beyond which no samples
     *  are returned from the history
     *  @param border_time New time or <code>empty</code> to access all samples
     */
    public void setBorderTime(final Optional<Instant> border_time)
    {   // Anything new?
        if (this.border_time.equals(border_time))
            return;
        // New border, recompute, mark as 'new data'
        this.border_time = border_time;
        computeVisibleSize();
    }

    /** Update visible size */
    private void computeVisibleSize()
    {
        if (border_time.isPresent())
        {
            final int last_index = searcher.findSampleLessThan(new HistoricSampleAccess(), border_time.get());
            visible_size = (last_index < 0)   ?   0   :   last_index + 1;
        }
        else
            visible_size = samples.length;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("nls")
    @Override
    public PlotSample get(final int i)
    {
        if (i >= visible_size)
            throw new IndexOutOfBoundsException("Index " + i + " exceeds visible size " + visible_size);
        return samples[i];
    }

    /** {@inheritDoc} */
    @Override
    public int size()
    {
        return visible_size;
    }

    /**
     * @return the number of samples, ignoring the border time
     */
    public int getRawSize() {
        return samples.length;
    }

    /**
     * Returns the sample at the specified index, ignoring the border time.
     *
     * @param i the index of the requested sample
     * @return the plot sample
     */
    public PlotSample getRawSample(int i) {
        return samples[i];
    }

    /** Merge newly received archive data into historic samples
     *  @param source Info about data source
     *  @param result Samples to add/merge
     */
    public void mergeArchivedData(final String source, final List<VType> result)
    {
        // Anything new at all?
        if (result.size() <= 0)
            return;
        // Turn IValues into PlotSamples
        final PlotSample new_samples[] = new PlotSample[result.size()];
        for (int i=0; i<new_samples.length; ++i)
            new_samples[i] = new PlotSample(waveform_index, source, result.get(i));
        // Merge with existing samples
        final PlotSample merged[] = PlotSampleMerger.merge(samples, new_samples);
        if (merged == samples)
            return;
        samples = merged;
        have_new_samples.set(true);
        computeVisibleSize();
    }

    /** Delete all samples */
    public void clear()
    {
        visible_size = 0;
        samples = new PlotSample[0];
    }
}
