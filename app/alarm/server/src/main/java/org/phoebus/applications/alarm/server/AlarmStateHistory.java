/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import java.time.Duration;
import java.time.Instant;

import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.framework.util.RingBuffer;
import org.phoebus.util.time.TimeDuration;

/** Ring buffer of AlarmState entries with time stamp that
 *  can check for N errors within some time
 *  @author Kay Kasemir
 */
public class AlarmStateHistory
{
    /** History of the last "count" alarms with time stamp */
    final private RingBuffer<AlarmState> history;

    /** Initialize
     *  @param count Number of recent alarms to track in history
     */
    public AlarmStateHistory(final int count)
    {
        history = new RingBuffer<>(count);
    }

    /** Release memory */
    public void dispose()
    {
        history.clear();
    }

    /** @return Maximum count of alarms that this history will track */
    public int getCount()
    {
        return history.getCapacity();
    }

    /** Add alarm state to history
     *  @param state New state
     *  @param timestamp Time stamp for that state
     */
    public void add(final AlarmState state)
    {
        history.add(state);
    }

    /** Check if we received 'count' alarms within the given number of seconds
     *  @param seconds Time range to check
     *  @return <code>true</code> if there were 'count' alarms received and
     *          they are time stamped within the seconds
     */
    public boolean receivedAlarmsWithinTimerange(final double seconds)
    {
        final int capacity = history.getCapacity();
        if (history.size() < capacity)
            return false;
        final Instant oldest = history.get(0).time;
        final Instant newest = history.get(capacity-1).time;
        final double span = TimeDuration.toSecondsDouble(Duration.between(oldest, newest));
        return span <= seconds;
    }

    /** Reset alarm history, forget what we accumulated so far */
    public void reset()
    {
        history.clear();
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        final int capacity = history.getCapacity();
        final int count = history.size();
        final double span;
        if (count < capacity)
            span = 0.0;
        else
        {
            final Instant oldest = history.get(0).time;
            final Instant newest = history.get(capacity-1).time;
            span = TimeDuration.toSecondsDouble(Duration.between(oldest, newest));
        }
        return String.format("AlarmStateHistory: %d/%d entries, span %.1f secs", //$NON-NLS-1$
                             count, capacity, span);
    }
}
