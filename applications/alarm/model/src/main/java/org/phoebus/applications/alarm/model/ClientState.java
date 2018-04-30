/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.model;

import java.time.Instant;

import org.phoebus.util.time.TimestampFormats;

/** A 'full' alarm state with added 'current' severity and message
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ClientState extends AlarmState
{
    final public SeverityLevel current_severity;
    final public String current_message;

    public ClientState(final SeverityLevel severity, final String message, final String value,
            final Instant time, final SeverityLevel current_severity,
            final String current_message)
    {
        super(severity, message, value, time);
        this.current_severity = current_severity;
        this.current_message = current_message;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(severity).append("/").append(message);
        buf.append(" (").append(value).append("), ").append(TimestampFormats.MILLI_FORMAT.format(time));
        buf.append(", current ").append(current_severity).append("/").append(current_message);
        return buf.toString();
    }
}
