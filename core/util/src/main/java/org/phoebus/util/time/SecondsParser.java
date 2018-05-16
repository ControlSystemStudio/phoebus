/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.util.time;

import java.util.Locale;

/** Parse seconds from 'HH:MM:SS' text and vice versa.
 *  @author Kay Kasemir
 */
public class SecondsParser
{
    /** Neutral locale */
    private final static Locale LOCALE = Locale.ROOT;

    /** Seconds in a minute */
    final static int SECS_PER_MINUTE=60;

    /** Seconds in an hour */
    final static int SECS_PER_HOUR=60*SECS_PER_MINUTE;

    /** Parse time string, return seconds.
     *  <p>
     *  Allowed formats:
     *  <ul>
     *  <li>"1234" - results in 1234 seconds
     *  <li>"1:30" - results in 1 minute, 30 seconds = 90 seconds
     *  <li>"1:20:30" - results in 1 hour, 20 minutes, 30 seconds = ...
     *  </ul>
     *  Special cases:
     *  One overall '-' is allowed to specify negative seconds.
     *
     *  @return The seconds parsed from the string.
     *  @throws Exception on parse error.
     */
    @SuppressWarnings("nls")
    public static double parseSeconds(String text) throws Exception
    {
        final char sep = ':';
        text = text.trim();
        if (text.length() <= 1)
            throw new Exception("Empty String");
        // Negative time, "-...." ?
        double sign;
        if (text.charAt(0) == '-')
        {
            text = text.substring(1);
            sign = -1.0;
        }
        else
            sign = +1.0;
        // Get the first number out of the string
        int i = text.indexOf(sep);
        if (i < 0) // That's it.
            return sign * Double.parseDouble(text);

        // There's more; these are the hours or minutes
        double secs = 60 * Double.parseDouble(text.substring(0, i));

        int j = text.indexOf(sep, i+1);
        if (j < 0) // That's it: Minutes and hours
            return sign * (secs + Double.parseDouble(text.substring(i+1)));
        // Hours, minutes, and seconds
        secs = 60 * (secs + Double.parseDouble(text.substring(i+1, j)));

        // More than two ':'?
        if (text.indexOf(sep, j+1) >= 0)
            throw new Exception("Invalid format");
        // Get the seconds
        return sign * (secs + Double.parseDouble(text.substring(j+1)));
    }

    /** Convert seconds into "HH:MM:SS" string.
     *  @param seconds Seconds
     *  @return String with "HH:MM:SS"
     */
    public static String formatSeconds(double seconds)
    {
        // Handle sign
        final boolean negative = seconds < 0;
        if (negative)
            seconds = -seconds;
        final String result;
        if (seconds < 1.0)
            result = String.format(LOCALE, "00:00:0%.3f", seconds); //$NON-NLS-1$
        else
        {
            // Convert to hours, minutes, seconds
            final int hours = (int) (seconds / SECS_PER_HOUR);
            seconds -= hours * SECS_PER_HOUR;
            final int minutes = (int) (seconds / SECS_PER_MINUTE);
            seconds -= minutes * SECS_PER_MINUTE;
            // Format as string
            result = String.format("%02d:%02d:%02d", //$NON-NLS-1$
                                   hours, minutes, (int) seconds);
        }
        if (negative)
            return "-" + result; //$NON-NLS-1$
        return result;
    }
}
