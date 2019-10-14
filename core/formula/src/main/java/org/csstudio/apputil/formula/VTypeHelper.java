/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import java.util.List;

import org.epics.util.array.ListInteger;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;

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

    public static double getDouble(final VType value, final int i)
    {
        if (value instanceof VNumberArray)
            return ((VNumberArray)value).getData().getDouble(i);
        if (value instanceof VEnumArray)
            return ((VNumberArray)value).getData().getDouble(i);
        return Double.NaN;
    }

    public static boolean isNumericArray(final VType value)
    {
        return value instanceof VNumberArray  ||
               value instanceof VEnumArray;
    }

    public static int getArraySize(final VType value)
    {
        final ListInteger sizes;
        if (value instanceof VNumberArray)
            sizes = ((VNumberArray)value).getSizes();
        else if (value instanceof VEnumArray)
            sizes = ((VEnumArray)value).getSizes();
        else if (value instanceof VStringArray)
            sizes = ((VStringArray)value).getSizes();
        else
            return 0;
        return sizes.size() > 0 ? sizes.getInt(0) : 0;
    }

    public static Alarm highestAlarmOf(final VType a, VType b)
    {
        return Alarm.highestAlarmOf(List.of(Alarm.alarmOf(a),
                                            Alarm.alarmOf(b)),
                                    false);
    }

    public static Time lastestTimeOf(final VType a, final VType b)
    {
        final Time ta = Time.timeOf(a);
        final Time tb = Time.timeOf(b);
        if (ta.getTimestamp().isAfter(tb.getTimestamp()))
            return ta;
        return tb;
    }
}
