/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Alarm information. Represents the severity and name of the highest alarm
 * associated with the channel.
 *
 * @author carcassi
 */
public interface Alarm {

    /**
     * Returns the alarm severity, which describes the quality of the
     * value returned. Never null.
     *
     * @return the alarm severity
     */
    AlarmSeverity getAlarmSeverity();

    /**
     * Returns a brief text representation of the highest currently active alarm.
     * Never null.
     *
     * @return the alarm status
     */
    String getAlarmName();
}
