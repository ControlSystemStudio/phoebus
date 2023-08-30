/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data;

import java.time.Instant;

import org.epics.pva.data.nt.PVAAlarm;
import org.epics.pva.data.nt.PVAEnum;
import org.epics.pva.data.nt.PVATimeStamp;
import org.epics.pva.data.nt.PVAAlarm.AlarmSeverity;

/** Helper to decode certain structures
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVAStructures
{

    /** @param structure Potential "time_t" structure
     *  @return Instant or <code>null</code>
     */
    public static Instant getTime(final PVAStructure structure)
    {
        PVATimeStamp timeStamp = PVATimeStamp.fromStructure(structure);
        if (timeStamp != null) 
        {
            return timeStamp.instant();
        }
        return null;
    }


    /** @param structure Potential "enum_t" structure
     *  @return Selected option or <code>null</code>
     */
    public static String getEnum(final PVAStructure structure)
    {
        PVAEnum pvaEnum = PVAEnum.fromStructure(structure);
        if (pvaEnum != null)
        {
            return pvaEnum.enumString();
        }
        return null;
    }


    /** @param structure Potential "alarm_t" structure
     *  @return Alarm info or <code>null</code>
     */
    public static String getAlarm(final PVAStructure structure)
    {
        PVAAlarm alarm = PVAAlarm.fromStructure(structure);

        if (alarm != null)
        {
            AlarmSeverity severity = alarm.alarmSeverity();
            if (severity != null) {
                return severity.toString();
            } else {
                return "Invalid severity <" + alarm.get("severity") + ">";
            }
        }
        return null;
    }
}
