/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import java.time.Instant;

import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListNumber;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VDouble;
import org.phoebus.vtype.VDoubleArray;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VEnumArray;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VStatistics;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

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

    /** @return Copy of given value with updated timestamp,
     *          or <code>null</code> if value is not handled
     */
    public static VType transformTimestamp(final VType value,
                                           final Instant time)
    {
        if (value instanceof VDouble)
        {
            final VDouble number = (VDouble) value;
            return ValueFactory.newVDouble(number.getValue().doubleValue(), number, ValueFactory.newTime(time), number);
        }
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            return ValueFactory.newVInt(number.getValue().intValue(), number, ValueFactory.newTime(time), number);
        }
        if (value instanceof VString)
        {
            final VString string = (VString) value;
            return ValueFactory.newVString(string.getValue(), string, ValueFactory.newTime(time));
        }
        if (value instanceof VDoubleArray)
        {
            final VDoubleArray number = (VDoubleArray) value;
            return ValueFactory.newVDoubleArray(number.getData(), number, ValueFactory.newTime(time), number);
        }
        if (value instanceof VEnum)
        {
            final VEnum labelled = (VEnum) value;
            return ValueFactory.newVEnum(labelled.getIndex(), labelled.getLabels(), labelled, ValueFactory.newTime(time));
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
        return value.toString();
    }
}
