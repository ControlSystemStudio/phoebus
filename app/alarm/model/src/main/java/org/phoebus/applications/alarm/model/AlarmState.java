/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.phoebus.util.time.SecondsParser;
import org.phoebus.util.time.TimeDuration;
import org.phoebus.util.time.TimestampFormats;

/** 'Full' alarm state that includes severity, message, value, time
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmState extends BasicState
{
    public final String message;
    public final String value;
    public final Instant time;
    public final boolean latch;

    public AlarmState(final SeverityLevel severity, final String message,
                      final String value, final Instant time)
    {
        this(severity, message,value, time, false);
    }

    public AlarmState(final SeverityLevel severity, final String message,
                      final String value, final Instant time,
                      final boolean latch)
    {
        super(severity);
        this.message = Objects.requireNonNull(message);
        this.value = value; // May be null
        this.time = Objects.requireNonNull(time);
        this.latch = latch;
    }

    /** @return <code>true</code> if this state update is the one when the severity 'latched',
     *          i.e. when the alarm first reached this severity level.
     */
    public boolean isLatched()
    {
        return latch;
    }

    /** @return Alarm message */
    public String getMessage()
    {
        return message;
    }

    /** @return Value that triggered the alarm state */
    public String getValue()
    {
        return value;
    }

    /** @return Time stamp */
    public Instant getTime()
    {
        return time;
    }

    /** @return Duration of current alarm state or empty text */
    public String getDuration()
    {
        final Duration duration = Duration.between(time, Instant.now());
        if (duration.isNegative())
            return "";
        return SecondsParser.formatSeconds(TimeDuration.toSecondsDouble(duration));
    }

    /** @return <code>true</code> if this state has higher alarm update
     *          priority than other state
     *  @param other State to compare
     *  @see SeverityLevel#getAlarmUpdatePriority()
     */
    public boolean hasHigherUpdatePriority(final AlarmState other)
    {
        return severity.getAlarmUpdatePriority()  >  other.severity.getAlarmUpdatePriority();
    }

    /** Create alarm state that's all OK
     *  @param value Value to use for the 'clear' state
     *  @param time Time to use for the 'OK' state
     *  @return AlarmState
     */
    public static AlarmState createClearState(final String value, final Instant time)
    {
        return new AlarmState(SeverityLevel.OK,
                SeverityLevel.OK.name(),
                value, time);
    }

    /** Create alarm state that's all OK with the current time stamp
     *  @return AlarmState
     */
    public static AlarmState createClearState(final String value)
    {
        return createClearState(value, Instant.now());
    }

    /** Create an alarm state similar to current one but with updated severity
     *  @param new_severity Severity to use for created alarm state
     *  @return AlarmState
     */
    public AlarmState createUpdatedState(final SeverityLevel new_severity)
    {
        return new AlarmState(new_severity, message, value, time, latch);
    }

    /** Change 'active' alarm severity into 'acknowledged' type, relaxing
     *  to the severity of the current state in case that's already lower
     *  than the original alarm state.
     *  @param current_state
     */
    public AlarmState createAcknowledged(final AlarmState current_state)
    {
        if (current_state != null  &&
            current_state.severity.ordinal() < severity.ordinal())
        {
            switch (current_state.severity)
            {
            case UNDEFINED:
                return new AlarmState(SeverityLevel.UNDEFINED_ACK, current_state.message, current_state.value, current_state.time, current_state.latch);
            case INVALID:
                return new AlarmState(SeverityLevel.INVALID_ACK, current_state.message, current_state.value, current_state.time, current_state.latch);
            case MAJOR:
                return new AlarmState(SeverityLevel.MAJOR_ACK, current_state.message, current_state.value, current_state.time, current_state.latch);
            case MINOR:
                return new AlarmState(SeverityLevel.MINOR_ACK, current_state.message, current_state.value, current_state.time, current_state.latch);
            default:
                // other severities stay as they are
                return current_state;
            }
        }
        // Else: Use the alarm severity as the one to ack'
        switch (severity)
        {
        case UNDEFINED:
            return createUpdatedState(SeverityLevel.UNDEFINED_ACK);
        case INVALID:
            return createUpdatedState(SeverityLevel.INVALID_ACK);
        case MAJOR:
            return createUpdatedState(SeverityLevel.MAJOR_ACK);
        case MINOR:
            return createUpdatedState(SeverityLevel.MINOR_ACK);
        default:
            // other severities stay as they are
            return this;
        }
    }

    /** Change acknowledged alarm severity into active type */
    public AlarmState createUnacknowledged()
    {
        switch (severity)
        {
        case UNDEFINED_ACK:
            return createUpdatedState(SeverityLevel.UNDEFINED);
        case INVALID_ACK:
            return createUpdatedState(SeverityLevel.INVALID);
        case MAJOR_ACK:
            return createUpdatedState(SeverityLevel.MAJOR);
        case MINOR_ACK:
            return createUpdatedState(SeverityLevel.MINOR);
        default:
            // other severities stay as they are
            return this;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj == this)
            return true;
        if (! (obj instanceof AlarmState))
            return false;
        final AlarmState other = (AlarmState) obj;
        return other.severity == severity    &&
               Objects.equals(other.message, message) &&
               Objects.equals(other.value, value)     &&
               Objects.equals(other.time, time)       &&
               other.latch == latch;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = message.hashCode();
        result = prime * result + severity.hashCode();
        result = prime * result + time.hashCode();
        result = prime * result + (value == null ? 0 : value.hashCode());
        result = prime * result + Boolean.hashCode(latch);
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        if (latch)
            buf.append("latch ");
        buf.append(severity).append("/").append(message);
        buf.append(" (").append(value).append("), ").append(TimestampFormats.MILLI_FORMAT.format(time));
        return buf.toString();
    }
}
