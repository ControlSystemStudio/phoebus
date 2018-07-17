/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.util.concurrent.atomic.AtomicInteger;

import org.csstudio.trends.databrowser3.preferences.Preferences;
import org.phoebus.framework.util.RingBuffer;

/** Ring buffer for 'live' samples.
 *  <p>
 *  New samples are always added to the end of a ring buffer.
 *
 *  @author Kay Kasemir
 *  @author Takashi Nakamoto changed LiveSamples to handle waveform index.
 */
public class LiveSamples extends PlotSamples
{
    // No locking in here, all access is via PVSamples

    private RingBuffer<PlotSample> samples =
        new RingBuffer<PlotSample>(Preferences.buffer_size);

    /** Waveform index */
    final private AtomicInteger waveform_index;

    LiveSamples(final AtomicInteger waveform_index)
    {
        this.waveform_index = waveform_index;
    }

    /** @return Maximum number of samples in ring buffer */
    public int getCapacity()
    {
        return samples.getCapacity();
    }

    /** Set new capacity.
     *  <p>
     *  Tries to preserve the newest samples.
     *  @param new_capacity New sample count capacity
     *  @throws Exception on out-of-memory error
     */
    public void setCapacity(int new_capacity) throws Exception
    {
        if (new_capacity < 10)
            new_capacity = 10;
        samples.setCapacity(new_capacity);
    }

    /** @param sample Sample to add to ring buffer */
    void add(final PlotSample sample)
    {
        sample.setWaveformIndex(waveform_index);
        samples.add(sample);
        have_new_samples.set(true);
    }

    @Override
    public int size()
    {
        return samples.size();
    }

    @Override
    public PlotSample get(final int i)
    {
        return samples.get(i);
    }

    /** Delete all samples */
    public void clear()
    {
        samples.clear();
        have_new_samples.set(true);
    }
}
