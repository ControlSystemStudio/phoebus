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
    private PVAString message;
    private PVAInt status;
    private PVAInt severity;

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
        this("alarm", 0, 0, message);
    }

    /**
     * Set all parameters in constructor
     * 
     * @param name
     * @param severity
     * @param status
     * @param message
     */
    public PVAAlarm(String name, int severity, int status, String message) {
        super(name, "alarm_t",
                new PVAInt("severity", severity),
                new PVAInt("status", status),
                new PVAString("message", message));
        this.severity = get(1);
        this.status = get(2);
        this.message = get(3);
    }

    /**
     * Set the value of the alarm
     * @param severity
     * @param status
     * @param message
     */
    public void set(int severity, int status, String message) {
        this.severity.set(severity);
        this.status.set(status);
        this.message.set(message);
    }
}
