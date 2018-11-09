/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import java.util.logging.Level;

import org.csstudio.archive.Preferences;
import org.csstudio.archive.ThrottledLogger;
import org.epics.vtype.VType;
import org.phoebus.framework.util.RingBuffer;

/** Buffer for the samples of one channel.
 *
 *  <p>Assumes that one thread adds samples, while a different
 *  thread removes them.
 *  When the queue size is reached, older samples get dropped.
 *
 *  @author Kay Kasemir
 */
public class SampleBuffer
{
    /** Name of channel that writes to this buffer.
     *  (we keep only the name, not the full channel,
     *  to decouple stuff).
     */
    final private String channel_name;

    /**
     * Data retention policy. May be null, if default/not supported.
     */
    final private String retention;

    /** The actual samples in a thread-save queue. */
    final private RingBuffer<VType> samples;

    /** Statistics */
    final private BufferStats stats = new BufferStats();

    /** Number of overruns when new string of overruns started, or <code>null</code> */
    private Integer start_of_overruns;

    /** Logger for overrun messages */
    final private static ThrottledLogger overrun_msg = new ThrottledLogger(Level.WARNING, Preferences.log_overrun);

    /** Is the buffer in an error state because of RDB write errors?
     *  Note that this is global for all buffers, not per instance!
     */
    private static volatile boolean error = false;

    /** Create sample buffer of given capacity
     * @deprecated Use {@link #SampleBuffer(String,String,int)} instead*/
    @Deprecated
    SampleBuffer(final String channel_name, final int capacity)
    {
        this(channel_name, null, capacity);
    }

    /** Create sample buffer of given capacity
     * @param retention Sample retention policy (for archive); may be null if default/not supported*/
    SampleBuffer(final String channel_name, String retention, final int capacity)
    {
        this.channel_name = channel_name;
        this.retention = retention;
        samples = new RingBuffer<>(capacity);
    }

    /** @return channel name of this buffer */
    String getChannelName()
    {
        return channel_name;
    }

    /** @return data retention policy (in archive) of this buffer's samples */
    String getArchiveDataRetention()
    {
        return retention;
    }

    /** @return Queue capacity, i.e. maximum queue size. */
    public int getCapacity()
    {
        synchronized (samples)
        {
            return samples.getCapacity();
        }
    }

    /** @return Current queue size, i.e. number of samples in the queue. */
    public int getQueueSize()
    {
        synchronized (samples)
        {
            return samples.size();
        }
    }

    /** @return <code>true</code> if currently experiencing write errors */
    public static boolean isInErrorState()
    {
        return error;
    }

    /** Set the error state. */
    static void setErrorState(final boolean error)
    {
        SampleBuffer.error = error;
    }

    /** Add a sample to the queue, maybe dropping older samples */
    @SuppressWarnings("nls")
    void add(final VType value)
    {
        synchronized (samples)
        {
            if (samples.isFull())
            {   // Note start of overruns, then drop older sample
                if (start_of_overruns == null)
                    start_of_overruns = Integer.valueOf(stats.getOverruns());
                stats.addOverrun();
            }
            else if (start_of_overruns != null)
            {   // Ending a string of overruns. Maybe log it.
                final int overruns = stats.getOverruns() - start_of_overruns;
                overrun_msg.log(channel_name + ": " + overruns + " overruns");
                start_of_overruns = null;
            }
            samples.add(value);
        }
    }

    /** @return latest sample in queue or <code>null</code> if empty */
    VType remove()
    {
        synchronized (samples)
        {
            return samples.remove();
        }
    }

    /** Update stats with current values */
    void updateStats()
    {
        stats.updateSizes(getQueueSize());
    }

    /** @return Buffer statistics. */
    public BufferStats getBufferStats()
    {
        return stats;
    }

    /** Reset statistics */
    public void reset()
    {
        start_of_overruns = null;
        stats.reset();
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return String.format(
        "Sample buffer '%s': %d samples, %d samples max, %.1f samples average, %d overruns",
            channel_name,
            getQueueSize(),
            stats.getMaxSize(),
            stats.getAverageSize(),
            stats.getOverruns());
    }
}
