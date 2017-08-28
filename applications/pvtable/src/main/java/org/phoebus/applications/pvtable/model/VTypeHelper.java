/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.text.NumberFormat;
import java.time.Instant;

import org.diirt.util.array.IteratorInt;
import org.diirt.util.array.IteratorNumber;
import org.diirt.util.array.ListByte;
import org.diirt.vtype.Alarm;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.Time;
import org.diirt.vtype.VByteArray;
import org.diirt.vtype.VDoubleArray;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VEnumArray;
import org.diirt.vtype.VFloatArray;
import org.diirt.vtype.VNumber;
import org.diirt.vtype.VNumberArray;
import org.diirt.vtype.VString;
import org.diirt.vtype.VType;
import org.diirt.vtype.ValueUtil;
import org.phoebus.applications.pvtable.Settings;

/** Helper for handling {@link VType} data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeHelper
{
    // TODO Move into core/pv

    /** Decode a {@link VType}'s time stamp
     *
     *  @param value Value to decode
     *  @return {@link Instant}
     */
    final public static Instant getTimestamp(final VType value)
    {
        if (value instanceof Time)
        {
            // TODO Update to new VType which is already providing Instant
            final Time time = (Time) value;
            if (time.isTimeValid())
                return Instant.ofEpochSecond(time.getTimestamp().getSec(), time.getTimestamp().getNanoSec());
        }
        return Instant.now();
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

    /** @param value {@link VType}
     *  @return Alarm text or ""
     */
    final public static String formatAlarm(final VType value)
    {
        final Alarm alarm = ValueUtil.alarmOf(value);
        if (alarm == null || alarm.getAlarmSeverity() == AlarmSeverity.NONE)
            return "";
        return alarm.getAlarmSeverity().toString() + "/" + alarm.getAlarmName();
    }

    /** Format value as string for display
     *
     *  @param value {@link VType}
     *  @return String representation
     */
    final public static String toString(final VType value)
    {
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            final NumberFormat format = number.getFormat();
            final String data;
            if (format != null)
                data = format.format(number.getValue().doubleValue());
            else
                data = number.getValue().toString();
            if (Settings.show_units)
            {
                final String units = number.getUnits();
                if (units.length() > 0)
                    return data + " " + units;
            }
            return data;
        }
        if (value instanceof VEnum)
        {
            final VEnum ev = (VEnum) value;
            try
            {
                return ev.getIndex() + " = " + ev.getValue();
            }
            catch (ArrayIndexOutOfBoundsException ex)
            {
                return ev.getIndex() + " = ?";
            }
        }
        if (value instanceof VString)
            return ((VString) value).getValue();
        if (value instanceof VByteArray && Settings.treat_byte_array_as_string)
        {
            // Check if byte array can be displayed as ASCII text
            final ListByte data = ((VByteArray) value).getData();
            byte[] bytes = new byte[data.size()];
            // Copy bytes until end or '\0'
            int len = 0;
            while (len < bytes.length)
            {
                final byte b = data.getByte(len);
                if (b == 0)
                    break;
                else if (b >= 32 && b < 127)
                    bytes[len++] = b;
                else
                { // Not ASCII
                    bytes = null;
                    break;
                }
            }
            if (bytes != null)
                return new String(bytes, 0, len);
            // else: Treat as array of numbers
        }
        if (value instanceof VDoubleArray || value instanceof VFloatArray)
        {
            // Show double arrays as floating point
            final StringBuilder buf = new StringBuilder();
            final IteratorNumber numbers = ((VNumberArray) value).getData()
                    .iterator();
            if (numbers.hasNext())
                buf.append(numbers.nextDouble());
            while (numbers.hasNext())
                buf.append(", ").append(numbers.nextDouble());
            return buf.toString();
        }
        if (value instanceof VNumberArray)
        {
            // Show other number arrays as integer
            final StringBuilder buf = new StringBuilder();
            final IteratorNumber numbers = ((VNumberArray) value).getData()
                    .iterator();
            if (numbers.hasNext())
                buf.append(numbers.nextLong());
            while (numbers.hasNext())
                buf.append(", ").append(numbers.nextLong());
            return buf.toString();
        }
        if (value instanceof VEnumArray)
        {
            final StringBuilder buf = new StringBuilder();
            IteratorInt indices = ((VEnumArray) value).getIndexes().iterator();
            if (indices.hasNext())
                buf.append(indices.nextInt());
            while (indices.hasNext())
                buf.append(", ").append(indices.nextInt());
            return buf.toString();
        }
        if (value == null)
            return "null";
        return value.toString();
    }
}
