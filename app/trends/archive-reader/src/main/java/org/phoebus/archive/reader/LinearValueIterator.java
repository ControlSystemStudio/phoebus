/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.StatisticsAccumulator;
import org.phoebus.archive.vtype.TimestampHelper;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.pv.TimeHelper;
import org.phoebus.util.time.TimeDuration;

/** {@link ValueIterator} that performs linear interpolation
 *
 *  <p>Reads from an underlying value iterator and returns
 *  linearly interpolated values.
 *
 *  <p>Interpolation will 'maximize' the severity of samples
 *  received from the base iterator within an interpolation
 *  period.
 *
 *  <p>Fundamentally, values are interpolated numerically,
 *  allowing <code>NaN</code> to simply result in not-a-number
 *  interpolation values, meaning:
 *  A sample with value 3.0 but INVALID alarm severity will be
 *  treated as 3.0. The INVALID severity will be included in the
 *  maximized severity of the interpolated samples, but it does not
 *  affect the numeric output.
 *
 *  <p>An exception is made for UNDEFINED values.
 *  If the last base sample before an interpolation point
 *  is UNDEFINED, that interpolation point will be undefined
 *  as well.
 *  This way a few UNDEFINED samples within an interpolation period
 *  are mostly ignored as long as there is a valid sample just
 *  before and after the exact interpolation point.
 *  But if the last sample before the interpolation point is UNDEFINED,
 *  so will be the interpolation point.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LinearValueIterator implements ValueIterator
{
    /** Base iterator */
    final private ValueIterator base;

    /** Interpolation interval */
    final private Duration interval;

    /** Last value read from the base iterator */
    private VType base_value;

    /** The next value to return */
    private VType next;

    /** Initialize
     *  @param base Base iterator
     *  @param interval Interpolation interval
     *  @throws Exception on error
     */
    public LinearValueIterator(final ValueIterator base, final Duration interval) throws Exception
    {
        this.base = base;
        this.interval = interval;
        base_value = base.hasNext() ? base.next() : null;
        next = determineNextValue();
    }

    @Override
    public boolean hasNext()
    {
        return next != null;
    }

    @Override
    public VType next()
    {
        final VType result = next;
        next = determineNextValue();
        return result;
    }

    @Override
    public void close() throws IOException
    {
        base.close();
    }

    /** Continue to read base iterator to determine next
     *  interpolated value
     *  @return next value
     *  @throws Exception on error
     */
    private VType determineNextValue()
    {
        if (base_value == null)
            return null;

        // Have one, initial value
        final StatisticsAccumulator accumulator = new StatisticsAccumulator();
        Instant t0, t1 = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(base_value);
        double v0, v1 = VTypeHelper.toDouble(base_value);
        AlarmSeverity severity = org.phoebus.core.vtypes.VTypeHelper.getSeverity(base_value);
        accumulator.add(v1);

        // Most severe alarm
        AlarmSeverity max_severity = severity;
        String max_status = VTypeHelper.getMessage(base_value);

        // Track the last undefined sample
        VType last_undefined = null;

        // Look for values until end of current interpolation bin
        final Instant end_of_bin = TimestampHelper.roundUp(t1, interval);
        do
        {
            // Track previous value
            t0 = t1;
            v0 = v1;

            // Note most recent undefined sample
            if (severity == AlarmSeverity.UNDEFINED)
                last_undefined = base_value;
            else
                last_undefined = null;

            // Reached end of input data?
            if (!base.hasNext())
            {
                final VType last_value = base_value;
                base_value = null;
                return last_value;
            }

            // Get next value
            base_value = base.next();
            t1 = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(base_value);
            v1 = VTypeHelper.toDouble(base_value);
            // 'Maximize' the severity and track the most
            // recent status message for that severity level
            severity = org.phoebus.core.vtypes.VTypeHelper.getSeverity(base_value);
            if (severity.compareTo(max_severity) >= 0)
            {
                max_severity = severity;
                max_status = VTypeHelper.getMessage(base_value);
            }
            accumulator.add(v1);
        }
        while (t1.compareTo(end_of_bin) < 0);

        if (last_undefined != null)
            return VTypeHelper.transformTimestamp(last_undefined, end_of_bin);

        if (accumulator.getNSamples() >= 2)
        {   // Found at least one value in this bin
            // t0, v0 are before, t1, v1 at-or-after end_of_bin
            // Linear interpolation between t0,v0 and t1,v1 onto end_of_bin time
            final double dT = TimeDuration.toSecondsDouble(Duration.between(t0, t1));
            final double interpol;
            if (dT > 0)
            {
                final double secs = TimeDuration.toSecondsDouble(Duration.between(t0, end_of_bin));
                interpol = v0 + (v1 - v0) * (secs / dT);
            }
            else
                interpol = (v0 + v1)/2; // Use average?

            if (! (base_value instanceof VNumber))
            {   // Cannot be packaged as ArchiveVStatistics since there's no Display info,
                // so pass the base value with 'interpolated' time stamp.
                // This typically coincides with interpol == NaN:
                // base_value == String -> v1 == NaN -> interpol == NaN,
                // and ArchiveVStatistics would be useless anyway.
                return VTypeHelper.transformTimestamp(base_value, end_of_bin);
            }
            else
                return VStatistics.of(interpol, accumulator.getStdDev(), accumulator.getMin(), accumulator.getMax(),
                                      accumulator.getNSamples(), Alarm.none(), TimeHelper.fromInstant(end_of_bin), Display.none());
        }

        // Have nothing in this bin
        // TODO Check this case
        throw new IllegalStateException("Not handled");
    }
}
