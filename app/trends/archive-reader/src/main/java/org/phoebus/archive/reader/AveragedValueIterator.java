/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader;

import static org.phoebus.archive.reader.ArchiveReaders.logger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VType;
import org.phoebus.archive.vtype.StatisticsAccumulator;
import org.phoebus.archive.vtype.TimestampHelper;
import org.phoebus.archive.vtype.VTypeHelper;
import org.phoebus.pv.TimeHelper;
import org.phoebus.util.time.TimestampFormats;

/** Averaging sample iterator.
 *
 *  <p>Reads samples from a given 'base' iterator
 *  and returns averaged samples.
 *
 *  <p>Can be used by readers that cannot perform
 *  server-side optimization to reduce sample
 *  counts in the client.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AveragedValueIterator implements ValueIterator
{
    final private static boolean debug = false;

    /** Iterator for the underlying raw samples over which we average */
    final private ValueIterator base;

    /** Averaging period in seconds */
    final private long seconds;

    /** The most recent value from <code>base</code>, may be <code>null</code> */
    private VType base_value = null;

    /** Display meta data */
    private Display display = null;

    /** The average value that <code>next()</code> will return
     *  or <code>null</code> if there is none
     */
    private VType avg_value = null;

    /** Initialize
     *  @param base Iterator for 'raw' values
     *  @param seconds Averaging period
     *  @throws Exception on error
     */
    public AveragedValueIterator(final ValueIterator base, final double seconds) throws Exception
    {
        this.base = base;
        // Guard against negative secs or values that would map to (long)0.
        if (seconds < 1.0)
            this.seconds = 1;
        else
            this.seconds = (long) seconds;

        // Get initial 'base' sample
        if (base.hasNext())
            base_value = base.next();
        // Get initial average value so that hasNext() and next() will work.
        avg_value = determineNextAverage();
    }

    /** Determine the next average sample.
     *  <p>
     *  <code>base_value</code> has to be on the very first sample
     *  for the current average window; either
     *  <ol>
     *  <li>the very first sample of the base iterator,
     *  <li>or the last sample we got in the previous
     *      call to <code>determineNextAverage</code>,
     *      i.e. the sample that turned out to be just past
     *      the last average window.
     *  </ol>
     *  @return The next average value
     *  @throws Exception on error
     */
    private VType determineNextAverage() throws Exception
    {
        // Anything left to average?
        if (base_value == null)
            return null;
        // Determine next multiple of averaging period
        final Instant average_window_start = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(base_value);
        final Instant average_window_end = TimestampHelper.roundUp(average_window_start, seconds);
        if (debug)
            System.out.println("Average until " + TimestampFormats.FULL_FORMAT.format(average_window_end));
        // Determine average over values within the window
        final StatisticsAccumulator stats = new StatisticsAccumulator();
        VType last_value = null;
        AlarmSeverity severity = org.phoebus.core.vtypes.VTypeHelper.getSeverity(base_value);
        String status = VTypeHelper.getMessage(base_value);
        while (base_value != null &&
               org.phoebus.core.vtypes.VTypeHelper.getTimestamp(base_value).compareTo(average_window_end) < 0)
        {
            final Number num = getNumericValue(base_value);
            if (num != null)
            {   // Has a numeric value
                if (debug)
                    System.out.println("Using " + base_value.toString());
                // Remember the first meta data that we can use
                if (display == null)
                        display = Display.displayOf(base_value);
                // Value average
                stats.add(num.doubleValue());
                // Maximize the severity
                if (isHigherSeverity(severity, org.phoebus.core.vtypes.VTypeHelper.getSeverity(base_value)))
                {
                    severity = org.phoebus.core.vtypes.VTypeHelper.getSeverity(base_value);
                    status = VTypeHelper.getMessage(base_value);
                }
                // Load next base value
                last_value = base_value;
                base_value = base.hasNext()  ?  base.next()  :  null;
            }
            else
            {   // If some average has accumulated, return that;
                // handle non-numeric base_value on the next call.
                if (stats.getNSamples() > 0)
                    break;
                if (debug)
                    System.out.println("Passing through: " + base_value.toString());
                // We have nothing except this non-numeric sample.
                // Return as is after preparing next call.
                last_value = base_value;
                base_value = base.hasNext()  ?  base.next()  :  null;
                return last_value;
            }
        }
        // Only single value? Return as is
        if (stats.getNSamples() <= 1)
            return last_value;
        // Create time stamp in center of averaging window ('bin')
        final Instant bin_time = average_window_end.minus(Duration.ofSeconds(seconds/2));

        // Return the min/max/average
        final VStatistics result = VStatistics.of(stats.getAverage(), stats.getStdDev(), stats.getMin(), stats.getMax(),
                                                  stats.getNSamples(), Alarm.none(), TimeHelper.fromInstant(bin_time), display);
        if (debug)
            System.out.println("Result: " + result.toString());
        return result;
    }

    /** @return <code>true</code> if the <code>new_severity</code> is more
     *          severe than the <code>current</code> severity.
     */
    private boolean isHigherSeverity(final AlarmSeverity current,
                                     final AlarmSeverity new_severity)
    {
        return new_severity.ordinal() > current.ordinal();
    }

    /** Try to get numeric value for interpolation.
     *  <p>
     *  Does <u>not</u> return numbers for enum.
     *  @param value {@link VType}
     *  @return {@link Number} or <code>null</code>
     */
    private static Number getNumericValue(final VType value)
    {
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            if (number.getAlarm().getSeverity() != AlarmSeverity.UNDEFINED)
                return number.getValue();
        }
        if (value instanceof VNumberArray)
        {
            final VNumberArray numbers = (VNumberArray) value;
            if (numbers.getAlarm().getSeverity() != AlarmSeverity.UNDEFINED  &&
                numbers.getData().size() > 0)
                return numbers.getData().getDouble(0);
        }
        // String or Enum, or no Value at all
        // Cannot decode that sample type as a number.
        return null;
    }

    @Override
    public boolean hasNext()
    {
        return avg_value != null;
    }

    @Override
    public VType next()
    {   // Save the value we're about to return, prepare the following avg.
        final VType ret_value = avg_value;
        try
        {
            avg_value = determineNextAverage();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to compute next average", ex);
            avg_value = null;
        }
        return ret_value;
    }

    @Override
    public void close() throws IOException
    {
        base.close();
    }
}
