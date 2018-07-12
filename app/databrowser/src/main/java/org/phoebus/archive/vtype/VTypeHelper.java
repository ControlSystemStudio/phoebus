/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.time.Instant;

import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.time.TimestampFormats;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VEnumArray;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VStatistics;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueUtil;

/** {@link VType} helper
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeHelper
{
    /** Read number from a {@link VType}
     *  @param value Value
     *  @return double or NaN
     */
    final public static double toDouble(final VType value)
    {
        if (value instanceof VNumber)
            return ((VNumber)value).getValue().doubleValue();
        if (value instanceof VEnum)
            return ((VEnum)value).getIndex();
        if (value instanceof VStatistics)
            return ((VStatistics)value).getAverage();
        if (value instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray) value).getData();
            if (data.size() > 0)
                return data.getDouble(0);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt data = ((VEnumArray) value).getIndexes();
            if (data.size() > 0)
                return data.getDouble(0);
        }
        return Double.NaN;
    }

    /** Read number from a {@link VType}
     *  @param value Value
     *  @param index Waveform index
     *  @return double or NaN
     */
    final public static double toDouble(final VType value, final int index)
    {
        if (index == 0)
            return toDouble(value);
        if (value instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray) value).getData();
            if (index < data.size())
                return data.getDouble(index);
        }
        if (value instanceof VEnumArray)
        {
            final ListInt data = ((VEnumArray) value).getIndexes();
            if (index < data.size())
                return data.getDouble(index);
        }
        return Double.NaN;
    }


    /** Decode a {@link VType}'s time stamp
     *  @param value Value to decode
     *  @return {@link Timestamp}
     */
    final public static Instant getTimestamp(final VType value)
    {
        if (value instanceof Time)
        {
            final Time time = (Time) value;
            if (time.isTimeValid())
                return time.getTimestamp();
        }
        return Instant.now();
    }

    /** @return Copy of given value with timestamp set to 'now',
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestampToNow(final VType value)
    {
        return transformTimestamp(value, Instant.now());
    }

    /** @return Copy of given value with updated timestamp,
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestamp(final VType value,
                                           final Instant time)
    {
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            return new ArchiveVNumber(time, number.getAlarmSeverity(), number.getAlarmName(), number, number.getValue());
        }
        if (value instanceof VString)
        {
            final VString string = (VString) value;
            return new ArchiveVString(time, string.getAlarmSeverity(), string.getAlarmName(), string.getValue());
        }
        if (value instanceof VNumberArray)
        {
            final VNumberArray number = (VNumberArray) value;
            return new ArchiveVNumberArray(time, number.getAlarmSeverity(), number.getAlarmName(), number, number.getData());
        }
        if (value instanceof VEnum)
        {
            final VEnum labelled = (VEnum) value;
            return new ArchiveVEnum(time, labelled.getAlarmSeverity(), labelled.getAlarmName(), labelled.getLabels(), labelled.getIndex());
        }
        return null;
    }

    /** @param buf Buffer where value's time stamp is added
     *  @param value {@link VType}
     */
    final public static void addTimestamp(final StringBuilder buf, final VType value)
    {
        final Instant stamp = getTimestamp(value);
        buf.append(TimestampFormats.FULL_FORMAT.format(stamp));
    }

    /** @param value {@link VType} value
     *  @return {@link AlarmSeverity}
     */
    final public static AlarmSeverity getSeverity(final VType value)
    {
        final Alarm alarm = ValueUtil.alarmOf(value);
        if (alarm == null)
            return AlarmSeverity.NONE;
        return alarm.getAlarmSeverity();
    }

    /** @param value {@link VType} value
     *  @return Alarm message
     */
    final public static String getMessage(final VType value)
    {
        final Alarm alarm = ValueUtil.alarmOf(value);
        if (alarm == null)
            return "";
        return alarm.getAlarmName();
    }

    /** @param buf Buffer where value's alarm info is added (unless OK)
     *  @param value {@link VType}
     */
    final public static void addAlarm(final StringBuilder buf, final VType value)
    {
        final Alarm alarm = ValueUtil.alarmOf(value);
        if (alarm == null  ||  alarm.getAlarmSeverity() == AlarmSeverity.NONE)
            return;
        buf.append(alarm.getAlarmSeverity().toString())
              .append("/")
              .append(alarm.getAlarmName());
    }

    /** Format value as string
     *  @param value Value
     *  @param format Format to use
     *  @return String representation
     */
    final public static String toString(final VType value, final VTypeFormat format)
    {
        if (value == null)
            return "null";
        final StringBuilder buf = new StringBuilder();
        addTimestamp(buf, value);
        buf.append("\t");
        format.format(value, buf);
        if (value instanceof Display)
        {
            final Display display = (Display) value;
            if (display != null  &&  display.getUnits() != null)
                buf.append(" ").append(display.getUnits());
        }
        buf.append("\t");
        addAlarm(buf, value);
        return buf.toString();
    }

    /** Format value as string
     *  @param value Value
     *  @return String representation
     */
    final public static String toString(final VType value)
    {
        return toString(value, DefaultVTypeFormat.get());
    }
}
