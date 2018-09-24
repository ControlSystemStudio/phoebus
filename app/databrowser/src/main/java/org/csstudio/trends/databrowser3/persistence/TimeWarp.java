/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.persistence;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.phoebus.util.time.TimeParser;

/** Convert between old and new start/end time specifications
 *
 *  <p>Original data browser used relative start times of the form
 *  "-3 days -10 minutes -3.124 seconds".
 *  This format is still used for the *.plt files.
 *
 *  <p>Current format is
 *  "3 days 10 minutes 3 seconds 1240 ms".
 *  This format is used within the UI.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeWarp
{
    /** @param amount Relative time span
     *  @return Legacy specification, e.g. "-3 days -3.124 seconds"
     */
    public static String formatAsLegacy(final TemporalAmount amount)
    {
        final StringBuilder buf = new StringBuilder();

        if (amount instanceof Period)
        {
            final Period period = (Period) amount;
            if (period.isZero())
                return "now";
            if (period.getYears() > 1)
                buf.append(-period.getYears()).append(" years ");
            if (period.getMonths() > 0)
                buf.append(-period.getMonths()).append(" months ");
            if (period.getDays() > 0)
                buf.append(-period.getDays()).append(" days");
        }
        else
        {
            long secs = ((Duration) amount).getSeconds();
            if (secs == 0)
                return "now";

            int p = (int) (secs / (24*60*60));
            if (p > 0)
            {
                buf.append(-p).append(" days ");
                secs -= p * (24*60*60);
            }

            p = (int) (secs / (60*60));
            if (p > 0)
            {
                buf.append(-p).append(" hours ");
                secs -= p * (60*60);
            }

            p = (int) (secs / (60));
            if (p > 0)
            {
                buf.append(-p).append(" minutes ");
                secs -= p * (60);
            }

            final long ms = ((Duration) amount).getNano() / 1000000;
            if (ms > 0)
                buf.append(-secs - ms / 1000.0).append(" seconds");
            else
                if (secs > 0)
                    buf.append(-secs).append(" seconds");
        }
        return buf.toString().trim();
    }


    private static final Pattern LEGACY_SECONDS = Pattern.compile("([0-9]+)\\.([0-9]+) sec.*");
    // Actually allows "m", "min", "minuteshours", but also "mintisenu".. Fine.
    private static final Pattern LEGACY_MINUTES = Pattern.compile("([0-9]+\\.[0-9]+) min[utes]*");
    // Actually allows "h", "hour", "hours", but also "horsurrs".. Fine.
    private static final Pattern LEGACY_HOURS = Pattern.compile("([0-9.]+) h[ours]*");

    /** @param legacy_spec Legacy specification, e.g. "-3 days -3.124 seconds"
     *  @return Relative time span
     */
    public static TemporalAmount parseLegacy(final String legacy_spec)
    {
        String spec = legacy_spec.replace("-", "");

        Matcher legacy = LEGACY_HOURS.matcher(spec);
        if (legacy.find())
        {
            // TimeParser can only handle full hours, not floating point
            // Convert to minutes
            final double hours = Double.parseDouble(legacy.group(1));
            final long minutes = Math.round(hours * 60.0);
            // Replace "hh.hh h" with "mmmm minutes"
            spec = spec.substring(0,legacy.start()) + minutes + " minutes" + spec.substring(legacy.end());
        }

        legacy = LEGACY_MINUTES.matcher(spec);
        if (legacy.find())
        {
            // TimeParser can only handle full minutes, not floating point
            // Convert to seconds
            final double minutes = Double.parseDouble(legacy.group(1));
            final long secs = Math.round(minutes * 60.0);
            // Replace "mm.mm m" with "mmmm secconds"
            spec = spec.substring(0,legacy.start()) + secs + " seconds" + spec.substring(legacy.end());
        }

        legacy = LEGACY_SECONDS.matcher(spec);
        if (legacy.find())
        {
            // Pad to nanosecs
            final String padded = (legacy.group(2) + "000000000").substring(0, 9);
            final long nano = Long.parseLong(padded);
            // .. but then only use the ms
            spec = spec.substring(0,legacy.start()) + legacy.group(1) + " seconds " + (nano/1000000) + " ms";
        }
        return TimeParser.parseTemporalAmount(spec);
    }
}
