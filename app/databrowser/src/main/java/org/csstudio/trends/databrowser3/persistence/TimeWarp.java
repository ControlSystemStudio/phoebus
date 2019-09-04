/*******************************************************************************
 * Copyright (c) 2018-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.persistence;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.TemporalAmount;

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

    /** @param legacy_spec Legacy specification, e.g. "-3 days -3.124 seconds"
     *  @return Relative time span
     */
    public static TemporalAmount parseLegacy(final String legacy_spec)
    {
        final String spec = legacy_spec.replace("-", "");
        return TimeParser.parseTemporalAmount(spec);
    }
}
