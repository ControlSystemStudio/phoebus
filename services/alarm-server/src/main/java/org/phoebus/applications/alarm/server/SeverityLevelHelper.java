/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.alarm.server;

import java.time.Instant;

import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VEnum;
import org.epics.vtype.VNumber;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.applications.alarm.model.SeverityLevel;

/** Helper for handling {@link VType}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SeverityLevelHelper
{

    /** Decode a {@link VType}'s severity
     *  @param value Value to decode
     *  @return {@link SeverityLevel}
     */
    final public static SeverityLevel decodeSeverity(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm == null) // Can this really happen?
            return SeverityLevel.OK;
        switch (alarm.getSeverity())
        {
        case NONE:
            return SeverityLevel.OK;
        case MINOR:
            return SeverityLevel.MINOR;
        case MAJOR:
            return SeverityLevel.MAJOR;
        case INVALID:
            return SeverityLevel.INVALID;
        default:
            return SeverityLevel.UNDEFINED;
        }
    }

    /** Decode a {@link VType}'s severity
     *  @param value Value to decode
     *  @return Status message
     */
    final public static String getStatusMessage(final VType value)
    {
        final Alarm alarm = Alarm.alarmOf(value);
        if (alarm != null)
            return alarm.getName();
        return SeverityLevel.OK.toString();
    }
}
