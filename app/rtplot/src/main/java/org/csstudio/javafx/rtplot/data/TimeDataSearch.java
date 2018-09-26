/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

import java.time.Duration;
import java.time.Instant;

/** Search for samples in a haystack.
 *  @author Kay Kasemir
 */
public class TimeDataSearch extends PlotDataSearch<Instant>
{
    /** Find the sample closest to given value
     *  @param data Data, must already be locked
     *  @param time Time near which to look for sample.
     *  @return Returns index of sample closest to time
     */
    public int findClosestSample(final PlotDataProvider<Instant> data, final Instant time)
    {
        if (search(data, time))
            return mid;
        // Didn't find exact match.
        if (cmp > 0) // 'mid' sample is bigger than x
        {   // [mid-1]  ... time ... [mid]
            if (mid > 0 &&
                    Duration.between(data.get(mid-1).getPosition(), time)
                    .compareTo(Duration.between(time, data.get(mid).getPosition())) < 0)
                return mid-1;
            return mid;
        }
        // cmp < 0, 'mid' sample is smaller than x.
        // [mid] ... time ... [mid+1]
        if (mid+1 < data.size() &&
                Duration.between(data.get(mid).getPosition(), time)
                .compareTo(Duration.between(time, data.get(mid+1).getPosition())) > 0)
            return mid+1;
        return mid;
    }


}
