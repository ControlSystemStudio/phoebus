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
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class AnnunciatorMessage implements Comparable<AnnunciatorMessage>
{
    /** Does the message always need to be annunciated? */
    public final boolean      standout;

    /** Severity - allows the messages to be easily sorted */
    public final SeverityLevel severity;

    /** Time when message was received */
    public final Instant       time;

    /** message  - what the annunciator will say */
    public       String        message;

    /** @param standout  Does the message always need to be annunciated?
     *  @param severity Alarm severity
     *  @param time Time when annunciation was received
     *  @param message Annunciation text
     */
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
        String timeStr = (time != null) ? TimestampFormats.MILLI_FORMAT.format(time) : "null";
        return timeStr + " " + severity + " " + message;
    }
}
