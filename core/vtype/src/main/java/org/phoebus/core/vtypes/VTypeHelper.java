/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.core.vtypes;

import org.epics.util.array.ListInteger;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;

import java.time.Instant;

public class VTypeHelper
{
    /** Read number from a {@link VType}
     *  @param value Value
     *  @return double or NaN
     */
    final public static double toDouble(final VType value)
    {
        if (value instanceof VNumber)
        {
            return ((VNumber) value).getValue().doubleValue();
        }
        if (value instanceof VString){
            try
            {
                return Double.parseDouble(((VString) value).getValue());
            }
            catch (NumberFormatException ex)
            {
                // Ignore
                return Double.NaN;
            }
        }
        if (value instanceof VBoolean) {
            return ((VBoolean) value).getValue() ? 1.0 : 0.0;
        }
        if (value instanceof VEnum) {
            return ((VEnum) value).getIndex();
        }
        if (value instanceof VStatistics) {
            return ((VStatistics) value).getAverage();
        }
        if (value instanceof VNumberArray)
        {
            return toDouble(value, 0);
        }
        if (value instanceof VEnumArray)
        {
           return toDouble(value, 0);
        }
        return Double.NaN;
    }


    /** Get VType as double[]; empty array if not possible
     *  @param value {@link VType}
     *  @return double[]
     */
    public static double[] toDoubles(final VType value)
    {
        final double[] array;
        if (value instanceof VNumberArray)
        {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new double[list.size()];
            for (int i=0; i<array.length; ++i) {
                array[i] = list.getDouble(i);
            }
        }
        else
            array = new double[0];
        return array;
    }

    /** @param value {@link VType}
     *  @return Value as String
     */
    public static String toString(final VType value)
    {
        if (value == null) {
            return "null";
        }
        if (isDisconnected(value)) {
            return null;
        }
        if (value instanceof VNumber) {
            return ((VNumber) value).getValue().toString();
        }
        if (value instanceof VEnum) {
            return ((VEnum) value).getValue();
        }
        if (value instanceof VString) {
            return ((VString) value).getValue();
        }
        // Else: Hope that value somehow represents itself
        return value.toString();
    }

    /** Read number by array index from array {@link VType}
     *
     *  @param value Value
     *  @param index Array index
     *  @return The double value at the specified index, or NaN if the index is invalid.
     */
    public static double toDouble(final VType value, final int index)
    {
        if(index < 0){
            return Double.NaN;
        }
        if (value instanceof VNumberArray)
        {
            final ListNumber data = ((VNumberArray) value).getData();
            if (index < data.size()) {
                return data.getDouble(index);
            }
        }
        if (value instanceof VEnumArray)
        {
            final ListNumber data = ((VEnumArray) value).getIndexes();
            if (index < data.size()) {
                return data.getDouble(index);
            }
        }
        return Double.NaN;
    }

    /** @param value {@link VType}
     *  @return <code>true</code> if value is a numeric array
     */
    public static boolean isNumericArray(final VType value)
    {
        return value instanceof VNumberArray  ||
               value instanceof VEnumArray;
    }

    /** @param value {@link VType}
     *  @return Array size. 0 for scalar.
     */
    public static int getArraySize(final VType value)
    {
        final ListInteger sizes;
        if (value instanceof VNumberArray)
        {
            sizes = ((VNumberArray) value).getSizes();
        }
        else if (value instanceof VEnumArray)
        {
            sizes = ((VEnumArray) value).getSizes();
        }
        else if (value instanceof VStringArray)
        {
            sizes = ((VStringArray) value).getSizes();
        }
        else
        {
            return 0;
        }
        return sizes.size() > 0 ? sizes.getInt(0) : 0;
    }

    /** @param a {@link VType}
     *  @param b {@link VType}
     *  @return Highest alarm of the two values
     */
    public static Alarm highestAlarmOf(final VType a, VType b)
    {
        return Alarm.highestAlarmOf(java.util.List.of(a, b), false);
    }

    /** @param a {@link VType}
     *  @param b {@link VType}
     *  @return Latest time stamp of the two values
     */
    public static Time lastestTimeOf(final VType a, final VType b)
    {
        final Time ta = Time.timeOf(a);
        final Time tb = Time.timeOf(b);
        if (ta.getTimestamp().isAfter(tb.getTimestamp()))
        {
            return ta;
        }
        return tb;
    }

    /** Decode a {@link VType}'s time stamp
     *  @param value Value to decode
     *  @return {@link Instant}
     */
    final public static Instant getTimestamp(final VType value)
    {
        final Time time = Time.timeOf(value);
        if (time != null  &&  time.isValid())
        {
            return time.getTimestamp();
        }
        return Instant.now();
    }

    /** @return Copy of given value with updated timestamp,
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestamp(final VType value,
                                           final Instant time)
    {
        if (value instanceof VDouble)
        {
            final VDouble number = (VDouble) value;
            return VDouble.of(number.getValue().doubleValue(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            return VInt.of(number.getValue().intValue(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VString)
        {
            final VString string = (VString) value;
            return VString.of(string.getValue(), string.getAlarm(), Time.of(time));
        }
        if (value instanceof VDoubleArray)
        {
            final VDoubleArray number = (VDoubleArray) value;
            return VDoubleArray.of(number.getData(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VEnum)
        {
            final VEnum labelled = (VEnum) value;
            return VEnum.of(labelled.getIndex(), labelled.getDisplay(), labelled.getAlarm(), Time.of(time));
        }
        return null;
    }

    /** @return Copy of given value with timestamp set to 'now',
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestampToNow(final VType value)
    {
        return transformTimestamp(value, Instant.now());
    }

    public static boolean isDisconnected(final VType value)
    {
        if (value == null)
        {
            return true;
        }

        // VTable does not implement alarm,
        // but receiving a table means we're not disconnected
        if (value instanceof VTable)
        {
            return false;
        }
        final Alarm alarm = Alarm.alarmOf(value);
        return Alarm.disconnected().equals(alarm);
    }

    public static AlarmSeverity getSeverity(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null)
            return AlarmSeverity.UNDEFINED;
        return alarm.getSeverity();
    }
}
