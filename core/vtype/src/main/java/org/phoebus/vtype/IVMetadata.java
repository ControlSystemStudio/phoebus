/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.time.Instant;


/**
 * Partial implementation for numeric types.
 *
 * @author carcassi
 */
class IVMetadata implements Alarm, Time {

    private final Alarm alarm;
    private final Time time;

    public IVMetadata(Alarm alarm, Time time) {
        this.alarm = alarm;
        this.time = time;
    }

    @Override
    public AlarmSeverity getAlarmSeverity() {
        return alarm.getAlarmSeverity();
    }

    @Override
    public String getAlarmName() {
        return alarm.getAlarmName();
    }

    @Override
    public Instant getTimestamp() {
        return time.getTimestamp();
    }

    @Override
    public Integer getTimeUserTag() {
        return time.getTimeUserTag();
    }

    @Override
    public boolean isTimeValid() {
        return time.isTimeValid();
    }

}
