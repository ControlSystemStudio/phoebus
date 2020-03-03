/*******************************************************************************
 * Copyright (c) 2017-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.time.Instant;

import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.pv.TimeHelper;
import org.phoebus.util.time.TimestampFormats;

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
        // Display string PVs at 0
        if (value instanceof VString) {
            return 0.0;
        }
        else{
            return org.phoebus.core.vtypes.VTypeHelper.toDouble(value);
        }
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
        final Time xformed = TimeHelper.fromInstant(time);
        if (value instanceof VNumber)
        {
            final VNumber number = (VNumber) value;
            return VNumber.of(number.getValue(), number.getAlarm(), xformed, number.getDisplay());
        }
        if (value instanceof VString)
        {
            final VString string = (VString) value;
            return VString.of(string.getValue(), string.getAlarm(), xformed);
        }
        if (value instanceof VNumberArray)
        {
            final VNumberArray number = (VNumberArray) value;
            return VNumberArray.of(number.getData(), number.getAlarm(), xformed, number.getDisplay());
        }
        if (value instanceof VEnum)
        {
            final VEnum labelled = (VEnum) value;
            return VEnum.of(labelled.getIndex(), labelled.getDisplay(), labelled.getAlarm(), xformed);
        }
        return null;
    }

    /** @param buf Buffer where value's time stamp is added
     *  @param value {@link VType}
     */
    final public static void addTimestamp(final StringBuilder buf, final VType value)
    {
        final Instant stamp = org.phoebus.core.vtypes.VTypeHelper.getTimestamp(value);
        buf.append(TimestampFormats.FULL_FORMAT.format(stamp));
    }

    /** @param value {@link VType} value
     *  @return Alarm message
     */
    final public static String getMessage(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null)
            return "";
        return alarm.getName();
    }

    /** @param buf Buffer where value's alarm info is added (unless OK)
     *  @param value {@link VType}
     */
    final public static void addAlarm(final StringBuilder buf, final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null  ||  alarm.getSeverity() == AlarmSeverity.NONE)
            return;
        buf.append(alarm.getSeverity().toString())
              .append("/")
              .append(alarm.getName());
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
        final Display display = Display.displayOf(value);
        if (display != null  &&  display.getUnit() != null  &&  !display.getUnit().isEmpty())
            buf.append(" ").append(display.getUnit());
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
