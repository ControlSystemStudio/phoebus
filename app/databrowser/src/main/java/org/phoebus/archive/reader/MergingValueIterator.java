/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.io.IOException;
import java.time.Instant;

import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** Merge values from several <code>ValueIterator</code> based on time stamps
 *  @author Kay Kasemir
 */
public class MergingValueIterator implements ValueIterator
{
    /** The iterators for the individual channels. */
    final private ValueIterator iters[];

    /** The 'current' values of each <code>iter</code>. */
    private VType raw_data[];

    private VType value;

    /** Constructor.
     *  @param iters The 'base' iterators.
     *  @throws Exception on error in archive access
     */
    public MergingValueIterator(final ValueIterator... iters) throws Exception
    {
        this.iters = iters;

        // Get first sample from each base iterator
        raw_data = new VType[iters.length];
        for (int i=0; i<iters.length; ++i)
            raw_data[i] = iters[i].hasNext() ? iters[i].next() :  null;
        fetchNext();
    }

    /** Determine the next value, i.e. the oldest sample from the base iterators
     *  @throws Exception on error
     */
    private void fetchNext()
    {
        // Find oldest time stamp
        Instant time = null;
        int index = -1;
        for (int i=0; i<raw_data.length; ++i)
        {
            if (raw_data[i] == null)
                continue;
            final Instant sample_time = VTypeHelper.getTimestamp(raw_data[i]);
            if (time == null  ||  sample_time.compareTo(time) < 0)
            {
                time = sample_time;
                index = i;
            }
        }
        if (time == null)
        {   // No channel left with any data.
            raw_data = null;
            value = null;
            return;
        }
        value = raw_data[index];
        raw_data[index] = iters[index].hasNext() ? iters[index].next() :  null;
    }

    @Override
    public boolean hasNext()
    {
        return raw_data != null;
    }

    @Override
    public VType next()
    {
        if (! hasNext())
            throw new IllegalStateException();
        final VType result = value;
        fetchNext();
        return result;
    }

    @Override
    public void close() throws IOException
    {
        for (ValueIterator iter : iters)
            iter.close();
    }
}
