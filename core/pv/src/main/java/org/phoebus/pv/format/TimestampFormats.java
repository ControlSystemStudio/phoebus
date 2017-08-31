/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.format;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Time stamp formats
 *
 *  <p>PV time stamps use the standard Java {@link Instant}.
 *  The formatters defined here are suggested because they can show the full detail of control system time stamps
 *  in a portable way, independent from locale.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimestampFormats
{
    final private static ZoneId zone = ZoneId.systemDefault();

    /** Time stamp format for 'full' time stamp */
    final public static DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn").withZone(zone);

    /** Time stamp format for time stamp up to seconds, but not nanoseconds */
    final public static DateTimeFormatter SECONDS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zone);

    /** Time stamp format for time date and time, no seconds */
    final public static DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);

    /** Time stamp format for date, no time */
    final public static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zone);

    /** Time stamp format for time stamp up to seconds, but no date */
    final public static DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(zone);
}
