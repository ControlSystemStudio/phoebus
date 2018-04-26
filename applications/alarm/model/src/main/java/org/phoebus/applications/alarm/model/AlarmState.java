/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.time.Instant;

/** Alarm state held by every alarm tree leaf
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmState extends BasicState
{
    final public String message;
    final public String value;
    final public Instant time;
    final public SeverityLevel current_severity;
    final public String current_message;

    public AlarmState(final SeverityLevel severity, final String message, final String value,
            final Instant time, final SeverityLevel current_severity,
            final String current_message)
    {
        super(severity);
        this.message = message;
        this.value = value;
        this.time = time;
        this.current_severity = current_severity;
        this.current_message = current_message;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(severity).append("/").append(message);
        buf.append("(").append(value).append("), ").append(time);
        buf.append(", current ").append(current_severity).append("/").append(current_message);
        return buf.toString();
    }
}
