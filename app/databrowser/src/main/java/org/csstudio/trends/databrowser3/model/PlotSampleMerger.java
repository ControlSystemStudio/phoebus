/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.util.Arrays;

import org.csstudio.javafx.rtplot.data.TimeDataSearch;

/** Helper for merging archived samples.
 *  <p>
 *  New data is to some extend 'merged' with existing data:
 *  Where the time ranges overlap, the data replaces the old data.
 *
 *  @author Kay Kasemir
 */
public class PlotSampleMerger
{
    final private static TimeDataSearch searcher = new TimeDataSearch();

    /** Add newly received samples to existing array of samples.
     *  @param old Existing data
     *  @param add Newly received data
     *  @return Array that combines new and old data
     */
    static public PlotSample[] merge(final PlotSample old[], final PlotSample add[])
    {
        // If one is empty, return the other as is:
        if (old == null  ||  old.length <= 0)
            return add;
        if (add == null  ||  add.length <= 0)
            return old;
        final int No = old.length;
        final int Na = add.length;
        // Both lists have at least one sample.
        // Determine start/end times.
        Instant old_start = old[0].getPosition();
        // ITimestamp old_end = old[No-1].getTime();
        Instant add_start = add[0].getPosition();
        Instant add_end = add[Na-1].getPosition();

        final PlotSampleArray searchable_array = new PlotSampleArray();
        // Not accessed from other threads, but lock to allow lock checks
        searchable_array.lockForWriting();

        //        System.out.print("Have samples " + old_start.toString());
        //        System.out.print(" to " + old_end.toString());
        //        System.out.println(" (" + old.length + ")");
        //        System.out.print("New Samples  " + add_start.toString());
        //        System.out.print(" to " + add_end.toString());
        //        System.out.println(" (" + add.length + ")");

        // Assume old samples are this:        +=============+
        // All new samples are before: +--...+
        if (add_end.compareTo(old_start) < 0)
        {
            final PlotSample result[] = new PlotSample[Na + No];
            System.arraycopy(add, 0, result, 0, Na);
            System.arraycopy(old, 0, result, Na, No);
            return result;
        }
        //                               +=x===========+
        // before, maybe overlap    +---..................+
        if (add_start.compareTo(old_start) <= 0)
        {
            // Result starts with 'new' samples. Then, how many 'old' samples?
            // Determine the first sample to use from the 'old'
            searchable_array.set(Arrays.asList(old));
            int x = searcher.findSampleGreaterThan(searchable_array, add_end);
            if (x < 0)
            {   // Old samples contain nothing beyond end of new samples
                return add;
            }
            final int copy_old = No - x;
            final PlotSample result[] = new PlotSample[Na + copy_old];
            // add new samples
            System.arraycopy(add, 0, result, 0, Na);
            // add old[x, ...]);
            System.arraycopy(old, x, result, Na, copy_old);
            return result;
        }
        // New samples start             +===l=====r===+
        // within old time sample range      +-----+
        // or                                +---............--+
        if (add_start.compareTo(old_start) >= 0)
        {
            // Determine the left/right indices of the section within 'old'.
            searchable_array.set(Arrays.asList(old));
            final int l = searcher.findSampleLessThan(searchable_array, add_start);
            final int r = searcher.findSampleGreaterThan(searchable_array, add_end);
            final int Nl = (l < 0) ? 0 : l + 1;
            final int Nr = (r < 0) ? 0 : No-r;
            final PlotSample result[] = new PlotSample[Nl + Na + Nr];
            // Nl old samples
            if (Nl > 0)
                System.arraycopy(old, 0, result, 0, Nl);
            // add new samples
            System.arraycopy(add, 0, result, Nl, Na);
            // old[r ... N-1]
            if (Nr > 0)
                System.arraycopy(old, r, result, Nl+Na, Nr);
            return result;
        }

        throw new Error("Cannot handle this case");  //$NON-NLS-1$
    }
}
