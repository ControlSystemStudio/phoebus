/*
 * Copyright (C) 2023 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.epics.pva.data.nt;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVAString;
import org.epics.pva.data.PVAStructure;

/**
 * Normative alarm type
 * 
 * An alarm_t describes a diagnostic of the value of a control system process
 * variable.
 * 
 * <ul>
 * <li>structure
 * <ul>
 * <li>int severity
 * <li>int status
 * <li>string message
 * 
 */
public class PVAAlarm extends PVAStructure {
    public static final String ALARM_NAME_STRING = "alarm";
    /** Type name for alarm info */
    public static final String ALARM_T = "alarm_t";
    private PVAString message;
    private PVAInt status;
    private PVAInt severity;

    public enum AlarmSeverity {
        NO_ALARM,
        MINOR,
        MAJOR,
        INVALID,
        UNDEFINED,
    }

    /** Default no alarm */
    public PVAAlarm() {
        this("");
    }

    /**
     * Setting only the message, status and severity are set to 0.
     * 
     * @param message String message
     */
    public PVAAlarm(String message) {
        this(0, 0, message);
    }

    /**
     * Set all parameters in constructor
     * 
     * @param severity
     * @param status
     * @param message
     */
    public PVAAlarm(int severity, int status, String message) {
        this(new PVAInt("severity", severity),
                new PVAInt("status", status),
                new PVAString("message", message));
    }

    /**
     * Set all parameters in constructor
     * 
     * @param severity
     * @param status
     * @param message
     */
    public PVAAlarm(PVAInt severity, PVAInt status, PVAString message) {
        super(ALARM_NAME_STRING, ALARM_T,
                severity,
                status,
                message);
        this.severity = severity;
        this.status = status;
        this.message = message;
    }

    /**
     * Set the value of the alarm
     * 
     * @param severity
     * @param status
     * @param message
     */
    public void set(int severity, int status, String message) {
        this.severity.set(severity);
        this.status.set(status);
        this.message.set(message);
    }

    /**
     * Returns the enum representing the severity
     * 
     * @return
     */
    public AlarmSeverity alarmSeverity() {
        AlarmSeverity[] values = AlarmSeverity.values();
        int index = this.severity.get();
        if (index > values.length) {
            return null;
        }
        return values[index];
    }

    /**
     * Conversion from structure to PVAAlarm
     * 
     * @param structure Potential "alarm_t" structure
     * @return PVAAlarm or <code>null</code>
     */
    public static PVAAlarm fromStructure(PVAStructure structure) {
        if (structure.getStructureName().equals(ALARM_T)) {
            final PVAInt severity = structure.get("severity");
            final PVAInt status = structure.get("status");
            final PVAString message = structure.get("message");
            return new PVAAlarm(severity, status, message);
        }
        return null;
    }

    /**
     * Get Alarm from a PVAStructure
     * 
     * @param structure Structure containing alarm
     * @return PVAAlarm or <code>null</code>
     */
    public static PVAAlarm getAlarm(PVAStructure structure) {
        PVAStructure alarmStructure = structure.get(ALARM_NAME_STRING);
        if (alarmStructure != null) {
            return fromStructure(alarmStructure);
        }
        return null;
    }

}
