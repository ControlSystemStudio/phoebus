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

/** 'Full' alarm state that includes severity, message, value, time
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmState extends BasicState
{
    final public String message;
    final public String value;
    final public Instant time;

    public AlarmState(final SeverityLevel severity, final String message,
                      final String value, final Instant time)
    {
        super(severity);
        this.message = message;
        this.value = value;
        this.time = time;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(severity).append("/").append(message);
        buf.append(" (").append(value).append("), ").append(TimestampFormats.MILLI_FORMAT.format(time));
        return buf.toString();
    }
}
