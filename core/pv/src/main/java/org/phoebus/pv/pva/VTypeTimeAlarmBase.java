/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.pva;

import java.time.Instant;

import org.epics.pvdata.property.AlarmStatus;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVStructure;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.Time;
import org.phoebus.vtype.VType;

/** Base {@link VType} that decodes {@link Time} and {@link Alarm}
 *
 *  <p>Inspired by  org.epics.pvmanager.pva.adapters.AlarmTimeExtractor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class VTypeTimeAlarmBase implements Time, Alarm
{
    final private static Instant NO_TIME = Instant.ofEpochSecond(0, 0);
    final private static Integer NO_USERTAG = Integer.valueOf(0);
    final private Instant timestamp;
    final private Integer usertag;
    final private AlarmSeverity severity;
    final private String message;

    VTypeTimeAlarmBase(final PVStructure struct)
    {
        // Decode time_t timeStamp
        final PVStructure time = struct.getSubField(PVStructure.class, "timeStamp");
        if (time != null)
        {
            final PVLong sec = time.getSubField(PVLong.class, "secondsPastEpoch");
            final PVInt nano = time.getSubField(PVInt.class, "nanoseconds");
            if (sec == null || nano == null)
                timestamp = NO_TIME;
            else
                timestamp = Instant.ofEpochSecond(sec.get(), nano.get());
            final PVInt user = time.getSubField(PVInt.class, "userTag");
            usertag = user == null ? NO_USERTAG : user.get();
        }
        else
        {
            timestamp = NO_TIME;
            usertag = NO_USERTAG;
        }

        // Decode alarm_t alarm
        final PVStructure alarm = struct.getSubField(PVStructure.class, "alarm");
        if (alarm != null)
        {
            PVInt code = alarm.getSubField(PVInt.class, "severity");
            severity = code == null
                ? AlarmSeverity.UNDEFINED
                : AlarmSeverity.values()[code.get()];

            code = alarm.getSubField(PVInt.class, "status");
            message = code == null
                ? AlarmStatus.UNDEFINED.name()
                : AlarmStatus.values()[code.get()].name();
        }
        else
        {
            severity = AlarmSeverity.NONE;
            message = AlarmStatus.NONE.name();
        }
    }

    // Time
    @Override
    public Instant getTimestamp()
    {
        return timestamp;
    }

    // Time
    @Override
    public Integer getTimeUserTag()
    {
        return usertag;
    }

    // Time
    @Override
    public boolean isTimeValid()
    {
        return timestamp != NO_TIME;
    }

    // Alarm
    @Override
    public AlarmSeverity getAlarmSeverity()
    {
        return severity;
    }

    // Alarm
    @Override
    public String getAlarmName()
    {
        return message;
    }
}
