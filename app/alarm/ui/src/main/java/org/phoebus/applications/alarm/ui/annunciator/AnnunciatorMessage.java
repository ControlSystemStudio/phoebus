/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.annunciator;

import java.time.Instant;

import org.phoebus.applications.alarm.model.SeverityLevel;
import org.phoebus.util.time.TimestampFormats;

/**
 * Annunciator Message class.
 * Serves as a container for all the values the annunciator needs to annunciate a message.
 * <p>
 * <b>Contains:</b>
 * <ol>
 * <li> standout - determines if the message should always be annunciated.
 * <li> severity - allows the messages to be easily sorted.
 * <li> message  - what the annunciator will say.
 * </ol>
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorMessage implements Comparable<AnnunciatorMessage>
{
    public final boolean      standout;
    public final SeverityLevel severity;
    public final Instant       time;
    public       String        message;

    public AnnunciatorMessage(final boolean standout, final SeverityLevel severity, final Instant time, final String message)
    {
        this.standout = standout;
        this.severity = severity;
        this.time     = time;
        this.message  = message;
    }

    @Override
    public int compareTo(AnnunciatorMessage other)
    {
        // Compare on severity. Greater severity comes first.
        int result = -1 * this.severity.compareTo(other.severity);
        // If the same severity, compare on time. Newest in time comes first.
        if (result == 0)
            result = this.time.compareTo(other.time);

        return result;
    }

    @Override
    public String toString()
    {
        return TimestampFormats.MILLI_FORMAT.format(time) + " " + severity + " " + message;
    }
}
