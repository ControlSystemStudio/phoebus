/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
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
 * structure
 *   int severity
 *   int status
 *   string message
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

    /** No alarm */
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

    public AlarmSeverity alarmSeverity() {
        var values = AlarmSeverity.values();
        var index = this.severity.get();
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
}
