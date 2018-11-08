/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import java.time.Instant;
import java.util.Objects;

import org.epics.util.array.ListNumber;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

/** {@link VType} helper
 *  @author Kay Kasemir
 */
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
            final ListNumber data = ((VEnumArray) value).getIndexes();
            if (data.size() > 0)
                return data.getDouble(0);
        }
        return Double.NaN;
    }

    /** Decode a {@link VType}'s time stamp
     *  @param value Value to decode
     *  @return {@link Timestamp}
     */
    final public static Instant getTimestamp(final VType value)
    {
        final Time time = Time.timeOf(value);
        if (time != null  &&  time.isValid())
            return time.getTimestamp();
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

    public static String toString(final VType value)
    {
        // XXX Tends to be poorly formatted...
        return Objects.toString(value);
    }
}
