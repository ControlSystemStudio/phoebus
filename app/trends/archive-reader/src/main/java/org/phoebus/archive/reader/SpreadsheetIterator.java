/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

import org.epics.vtype.VType;
import org.phoebus.archive.vtype.VTypeHelper;

/** Iterates several <code>ValueIterator</code> instances 'in lockstep'
 *  as required to generate spreadsheet-type output.
 *  <p>
 *  It uses staircase interpolation, basically keeping the last value
 *  of a channel until all channels reach a new timestamp.
 *  <p>
 *  While it uses the common Java <code>Iterator</code> idea with
 *  <code>hasNext()/next()</code> for the values, pay attention to
 *  the comments for <code>getTime()</code>!
 *
 *  @author Kay Kasemir
 */
public class SpreadsheetIterator implements Iterator<VType[]>, Closeable
{
    final private static boolean debug = false;

    /** The iterators for the individual channels. */
    final private ValueIterator iters[];

    /** The 'current' values of each <code>iter</code>.
     *  This is usually the 'next' value, stamped after <code>time</code>.
     *  @see #values
     */
    private VType raw_data[];

    /** The timestamp for the current spreadsheet 'line'. */
    private Instant time;

    /** The values of the current spreadsheet 'line', or <code>null</code>. */
    private VType values[];

    /** Constructor.
     *  @param iters The 'base' iterators.
     *  @throws Exception on error in archive access
     */
    @SuppressWarnings("nls")
    public SpreadsheetIterator(final ValueIterator... iters) throws Exception
    {
        this.iters = iters;

        // Get first sample from each base iterator
        raw_data = new VType[iters.length];
        values = new VType[iters.length];
        for (int i=0; i<iters.length; ++i)
        {
            raw_data[i] = iters[i].hasNext()  ?  iters[i].next()  :  null;
            if (debug)
                System.out.println("Initial " + i + ": " + VTypeHelper.toString(raw_data[i]));
        }
        getNextSpreadsheetLine();
    }

    /** @return <code>true</code> if there is more data.
     *  @see #getTime()
     *  @see #next()
     */
    @Override
    public boolean hasNext()
    {
        return values != null;
    }

    /** Get the time of the spreadsheet line.
     *  <p>
     *  While <code>next()</code> returns that line, it also
     *  advances to the following one, invalidating the time stamp!
     *  So the proper usage is this:
     *  <pre>
     *    while (sheet.hasNext())
     *    {
     *        ITimestamp time = sheet.getTime();
     *        IValue line[] = sheet.next();
     *        // time, line[] now contains info
     *        // for the current spreadsheet line
     *        // ...
     *    }
     *  </pre>
     *  @return The time stamp of the current spreadsheet 'line'. */
    public Instant getTime()
    {
        return time;
    }

    /** Get the next set of values, and move iterator to the following line.
     *  <p>
     *  Note that one should neglect the time stamps of those values,
     *  and instead use <code>getTime</code>.
     *  (The values keep their 'real' time stamp, which usually is older than
     *  the time stamp of the spreadsheet 'line')
     *  <p>
     *  It is an error to invoke <code>next</code> unless <code>hasNext</code>
     *  returned <code>true</code>.
     *
     *  @return The next spreadsheed 'line', one sample per channel.
     *          For some channels that might be <code>null</code>.
     *  @see #getTime()
     *  @see #hasNext()
     */
    @Override
    public VType[] next()
    {
        assert hasNext();
        // Keep copy(!) of 'current' spreadsheet line
        final VType[] result = values.clone();
        // Prepare next line
        getNextSpreadsheetLine();
        // return the copy
        return result;
    }

    /** Fill <code>time</code> and <code>values</code> with the next
     *  spreadsheet line.
     *  @throws Exception on error
     */
    private void getNextSpreadsheetLine()
    {
        // Find oldest timestamp
        time = null;
        for (int i=0; i<raw_data.length; ++i)
        {
            if (raw_data[i] == null)
                continue;
            final Instant sample_time = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(raw_data[i]);
            if (time == null  ||  sample_time.compareTo(time) < 0)
                time = sample_time;
        }
        if (time == null)
        {   // No channel left with any data.
            values = null;
            return;
        }

        if (debug)
            System.out.println("Next time stamp: " + time.toString()); //$NON-NLS-1$

        // 'time' now defines the current spreadsheet line.
        for (int i=0; i<raw_data.length; ++i)
        {
            // Channel has no new data? Leave at last known value[i].
            if (raw_data[i] == null)
                continue;
            // Channel has data.
            if (org.phoebus.core.vtypes.VTypeHelper.getTimestamp(raw_data[i]).compareTo(time) <= 0)
            {   // 'raw_data' is still valid, so use it ....
                values[i] = raw_data[i];
                // and get next sample in preparation for next()
                raw_data[i] = iters[i].hasNext()  ?  iters[i].next()  :  null;
            }
            // else: raw_data.time is already > time,
            // so leave values[i] as is until 'time' catches up
            // with raw_data.time.
            // This also covers the initial values[i] == null case.
        }
    }

    /** Must be called to release resources */
    @Override
    public void close() throws IOException
    {
        for (ValueIterator iter : iters)
            iter.close();
    }
}
