/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.text.NumberFormat;
import java.util.stream.Collectors;

import org.epics.util.array.IteratorNumber;
import org.epics.util.array.ListByte;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.applications.pvtable.Settings;

/** Helper for handling {@link VType} data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypeFormatter
{
    /** @param value {@link VType}
     *  @return Alarm text or ""
     */
    final public static String formatAlarm(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null || alarm.getSeverity() == AlarmSeverity.NONE)
            return "";
        return alarm.getSeverity().toString() + "/" + alarm.getName();
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
            final NumberFormat format = number.getDisplay().getFormat();
            final String data;
            if (format != null)
                data = format.format(number.getValue().doubleValue());
            else
                data = number.getValue().toString();
            if (Settings.show_units)
            {
                final String units = number.getDisplay().getUnit();
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
        if(value instanceof VBoolean) {
            //Add Boolean type to get true or false instead of VBoolean.toString()
            //TODO Manage ONAM ZNAM for CA
            return String.valueOf(((VBoolean)value).getValue());
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
        if (value instanceof VStringArray)
        {
            return ((VStringArray) value).getData()
                                         .stream()
                                         .collect(Collectors.joining(", "));
        }
        if (value instanceof VEnumArray)
        {
            final StringBuilder buf = new StringBuilder();
            IteratorNumber indices = ((VEnumArray) value).getIndexes().iterator();
            if (indices.hasNext())
                buf.append(indices.nextInt());
            while (indices.hasNext())
                buf.append(", ").append(indices.nextInt());
            return buf.toString();
        }
        if (value == null)
            return "";
        return value.toString();
    }
}
