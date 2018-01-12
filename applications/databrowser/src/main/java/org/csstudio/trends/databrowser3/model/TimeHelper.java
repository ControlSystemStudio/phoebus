/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/** Helper for <code>java.time</code>
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeHelper
{
    // Java 8 DateTimeFormatter is thread safe!
    final private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS xx");
    final public static ZoneId zone = ZoneId.systemDefault();

    /** Format instant as local time for persisting as string or debug printouts
     *  @param time Instant
     *  @return String that can be used to persist the time
     */
    public static String format(final Instant time)
    {
        final ZonedDateTime local = ZonedDateTime.ofInstant(time, zone);
        return formatter.format(local);
    }

    /** Parse time from text created by <code>format()</code>
     *  @param time Text
     *  @return Instant parsed from the text
     */
    public static Instant parse(final String time)
    {
        final ZonedDateTime local = ZonedDateTime.parse(time, formatter);
        return local.toInstant();
    }
}
