/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.ca;

import org.diirt.util.time.Timestamp;
import org.diirt.vtype.Alarm;
import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.Time;

import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.TIME;
import gov.aps.jca.dbr.TimeStamp;

/** Implement VType Alarm and Time for a DBR
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DBRAlarmTimeWrapper<T_DBR extends TIME> implements Alarm, Time
{
    final protected T_DBR dbr;

    public DBRAlarmTimeWrapper(final T_DBR dbr)
    {
        this.dbr = dbr;
    }

    @Override
    public AlarmSeverity getAlarmSeverity()
    {
        if (dbr == null)
            return AlarmSeverity.NONE;

        if (dbr.getSeverity() == Severity.NO_ALARM)
            return AlarmSeverity.NONE;
        if (dbr.getSeverity() == Severity.MINOR_ALARM)
            return AlarmSeverity.MINOR;
        if (dbr.getSeverity() == Severity.MAJOR_ALARM)
            return AlarmSeverity.MAJOR;
        if (dbr.getSeverity() == Severity.INVALID_ALARM)
            return AlarmSeverity.INVALID;
        return AlarmSeverity.UNDEFINED;
    }

    @Override
    public String getAlarmName()
    {
        if (dbr == null)
            return "";
        return dbr.getStatus().getName();
    }

    @Override
    public Timestamp getTimestamp()
    {
        // TODO Use Instant
//        if (dbr == null)
//            return Instant.now();
//        final TimeStamp epics_time = dbr.getTimeStamp();
//        if (epics_time == null)
//            return Instant.now();
//        return Instant.ofEpochSecond(epics_time.secPastEpoch() + 631152000L,  (int) epics_time.nsec());


        if (dbr == null)
            return Timestamp.now();
        final TimeStamp epics_time = dbr.getTimeStamp();
        if (epics_time == null)
            return Timestamp.now();
        return Timestamp.of(epics_time.secPastEpoch() + 631152000L,  (int) epics_time.nsec());
    }

    @Override
    public Integer getTimeUserTag()
    {
        return null;
    }

    @Override
    public boolean isTimeValid()
    {
        if (dbr == null)
            return false;
        final TimeStamp epics_time = dbr.getTimeStamp();
        return epics_time != null  &&  epics_time.secPastEpoch() > 0;
    }
}
