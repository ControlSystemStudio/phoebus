/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.export;

import static org.csstudio.trends.databrowser3.Activator.logger;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.trends.databrowser3.model.ModelItem;
import org.csstudio.trends.databrowser3.model.PlotSample;
import org.csstudio.trends.databrowser3.model.PlotSamples;
import org.epics.vtype.VType;
import org.phoebus.archive.reader.ValueIterator;
import org.phoebus.core.vtypes.VTypeHelper;

/** Iterator for the samples in a ModelItem, not fetching archived data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ModelSampleIterator implements ValueIterator
{
    /** Samples from which to return values from 'start' to 'end' */
    final private PlotSamples samples;

    /** End time */
    final private Instant end;

    /** The value returned by 'next' or undefined for 'index' < 0 */
    private VType value;

    /** Index of 'value' in 'samples', -1 for end-of-sequence */
    private int index;

    /** Initialize
     *  @param item Item from which to get samples
     *  @param start Start time
     *  @param end End time
     *  @throws Exception on error
     */
    public ModelSampleIterator(final ModelItem item, final Instant start, final Instant end) throws Exception
    {
        this.samples = item.getSamples();
        this.end = end;

        if (! samples.getLock().tryLock(10, TimeUnit.SECONDS))
            throw new TimeoutException("Cannot lock " + samples);
        try
        {
            // Anything?
            if (samples.size() <= 0)
                index = -1;
            // All data after start time?
            else if (samples.get(0).getPosition().compareTo(start) >= 0)
                index = 0;
            else
            {   // There is data before the start time. Find sample just before start time.
                index = findSampleLessOrEqual(start);
            }
            // Is first sample already after end time?
            if (index >= 0)
            {
                final PlotSample sample = samples.get(index);
                value = sample.getVType();
                if (sample.getPosition().compareTo(end) > 0)
                    index = -1;
            }
        }
        finally
        {
            samples.getLock().unlock();
        }
    }

    /** @param start Start time
     *  @return Index sample with time stamp at-or-before start time, or -1.
     */
    private int findSampleLessOrEqual(final Instant start)
    {
        // Would like to use PlotSampleSearch, but that operates on array
        // of PlotSample[]
        int low = 0;
        int high = samples.size()-1;
        int cmp = 0;
        int mid = -1;
        while (low <= high)
        {
            mid = (low + high) / 2;
            // Compare 'mid' sample to goal
            final Instant time = samples.get(mid).getPosition();
            final int compare = time.compareTo(start);
            if (compare > 0)
            {   // 'mid' too big, search lower half
                cmp = 1;
                high = mid - 1;
            }
            else if (compare < 0)
            {   // 'mid' too small, search upper half
                cmp = -1;
                low = mid + 1;
            }
            else
            {
                cmp = 0;
                return mid; // found exact time
            }
        }
        // Didn't find exact match.
        if (cmp < 0) // 'mid' sample is smaller than x, so it's OK
            return mid;
        // cmp > 0, 'mid' sample is greater than x.
        // If there is a sample before, use that
        if (mid > 0)
            return mid-1;
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext()
    {
        return index >= 0;
    }

    /** {@inheritDoc} */
    @Override
    public VType next()
    {
        if (index < 0)
            throw new NoSuchElementException("End of samples");
        // Remember value, prepare the next value
        final VType result = value;
        try
        {
            if (! samples.getLock().tryLock(10, TimeUnit.SECONDS))
                throw new Exception("Cannot lock " + samples);
            try
            {
                ++index;
                if (index >= samples.size())
                    index = -1; // No more samples
                else
                {
                    value = samples.get(index).getVType();
                    if (VTypeHelper.getTimestamp(value).compareTo(end) > 0)
                        index = -1; // Beyond end time
                }
            }
            finally
            {
                samples.getLock().unlock();
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot iterate over samples", ex);
            index = -1;
        }
        return result;
    }
}
