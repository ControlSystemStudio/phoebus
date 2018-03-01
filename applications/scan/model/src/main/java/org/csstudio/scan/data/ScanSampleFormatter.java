/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.data;

import java.time.Instant;
import java.util.Arrays;

import org.phoebus.util.time.TimestampFormats;

/** Helper for formatting {@link ScanSample}s
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanSampleFormatter
{
    /** Extract double from sample
     *  @param sample ScanSample
     *  @return Double or NaN if there is no number to extract
     */
    public static double asDouble(final ScanSample sample)
    {
        if (sample instanceof NumberScanSample)
            return ((NumberScanSample) sample).getNumber(0).doubleValue();
        return Double.NaN;
    }

    /** Extract String from sample
     *  @param sample ScanSample
     *  @return String for scan sample
     */
    public static String asString(final ScanSample sample)
    {
        if (sample == null)
            return "";
        final Object[] values = sample.getValues();
        if (values == null)
            return "";
        if (values.length == 1)
            return values[0].toString();
        return Arrays.toString(values);
    }

    /** Format {@link Instant} to the fullest detail: Date, time, seconds, ...
     *  @param timestamp {@link Instant}
     *  @return Date in preferred text format
     */
    public static String format(final Instant timestamp)
    {
        if (timestamp == null)
            return "?";
        return TimestampFormats.MILLI_FORMAT.format(timestamp);
    }
}
