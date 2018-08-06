/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive;

import static org.csstudio.archive.Engine.logger;

import java.util.logging.Level;

import org.phoebus.util.time.SecondsParser;

/** Logger that only allows a certain message rate.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ThrottledLogger
{
    /** Log level */
    private final Level level;

    /** Minimum period between events [millis] */
    private final long period;

    /** Time of the last event [millis] */
    private volatile long last = 0;

    /** <code>true</code> when in the 'be quiet' state */
    private volatile boolean throttled = false;

    /** Initialize
     *  @param info Log level to use
     *  @param seconds_between_messages Seconds between allowed messages
     */
    public ThrottledLogger(final Level info, final long seconds_between_messages)
    {
        this.level = info;
        period = seconds_between_messages * 1000;
    }

    /** @param message Message to log (or suppress) */
    public void log(final String message)
    {
        // Is another message permitted?
        final long now = System.currentTimeMillis();
        if ((now - last) < period)
        {
            if (!throttled)
            {
                throttled = true;
                // Last message to be shown for a while
                logger.log(level, message);
                logger.log(level, "More messsages suppressed for " +
                           SecondsParser.formatSeconds(period / 1000.0) +
                           " ....");
            }
            // Don't log this message
            return;
        }
        last = now;

        // OK, show
        logger.log(level, message);
        throttled = false;
    }
}
