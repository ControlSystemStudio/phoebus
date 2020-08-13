/*******************************************************************************
 * Copyright (c) 2012-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtree.model;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VType;
import org.phoebus.ui.vtype.FormatOption;
import org.phoebus.ui.vtype.FormatOptionHandler;

/** Helper for {@link VType} gymnastics
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmFormatter
{

    public static void appendAlarm(final StringBuilder buf, final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null)
            return;
        if (alarm.getSeverity() == AlarmSeverity.NONE)
            return;
        buf.append(" [").append(alarm.getSeverity());
        buf.append(",").append(alarm.getName()).append("]");
    }

    public static String formatValue(final VType value)
    {
        if (value instanceof VByteArray)
            return FormatOptionHandler.format(value, FormatOption.STRING, -1, true);
        else
            return FormatOptionHandler.format(value, FormatOption.DEFAULT, -1, true);
    }

    public static String format(final VType value)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(formatValue(value));

        // If there is no value, suppress the alarm.
        // TODO Check note in PVTreeItem#updateLinks():
        // If a link is empty, the record could still be in alarm.
        // So in here we must NOT return "'' [MINOR/Whatever]" for
        // an empty value ('') with alarm, because updateLinks()
        // would consider that overall a non-empty string,
        // and the tree item would appear.
        // So in here we suppress the alarm for empty values,
        // but in other cases the empty value could well be
        // a valid alarm to display...
        if (buf.length() > 0)
            appendAlarm(buf, value);
        return buf.toString();
    }
}
