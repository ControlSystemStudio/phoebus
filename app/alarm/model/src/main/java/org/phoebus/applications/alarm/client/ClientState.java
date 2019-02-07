/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.client;

import java.time.Instant;
import java.util.Objects;

import org.phoebus.applications.alarm.Messages;
import org.phoebus.applications.alarm.model.AlarmState;
import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.util.time.TimestampFormats;

/** A 'full' alarm state with added 'current' severity and message
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ClientState extends AlarmState
{
    public final SeverityLevel current_severity;
    public final String current_message;

    public ClientState(final SeverityLevel severity, final String message, final String value,
                       final Instant time, final SeverityLevel current_severity,
                       final String current_message,
                       final boolean latch)
    {
        super(severity, message, value, time, latch);
        this.current_severity = Objects.requireNonNull(current_severity);
        this.current_message = Objects.requireNonNull(current_message);
    }

    public ClientState(final SeverityLevel severity, final String message, final String value,
                       final Instant time, final SeverityLevel current_severity,
                       final String current_message)
    {
        this(severity, message, value, time, current_severity, current_message, false);
    }

    public ClientState(final AlarmState state,
                       final SeverityLevel current_severity,
                       final String current_message)
    {
        this(state.severity, state.message, state.value, state.time,
             current_severity, current_message, state.latch);
    }

    /** @return <code>true</code> if disabled via filter */
    public boolean isDynamicallyDisabled()
    {
        return severity == SeverityLevel.OK  &&
               message.equals(Messages.Disabled);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + current_message.hashCode();
        result = prime * result + current_severity.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (obj == this)
            return true;
        if (! (obj instanceof ClientState))
            return false;
        final ClientState other = (ClientState) obj;
        return severity == other.severity                  &&
               Objects.equals(message, other.message)      &&
               Objects.equals(value, other.value)          &&
               Objects.equals(time, other.time)            &&
               current_severity == other.current_severity  &&
               Objects.equals(current_message, other.current_message);
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
