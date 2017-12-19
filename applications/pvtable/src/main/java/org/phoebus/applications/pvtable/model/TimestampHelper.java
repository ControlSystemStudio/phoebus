/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.model;

import java.time.Instant;

import org.phoebus.util.time.TimestampFormats;


/** Time stamp gymnastics
 *
 *  <p>Shortcut for {@link TimestampFormats}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimestampHelper
{
    /** @param timestamp {@link Instant}, may be <code>null</code>
     *  @return Time stamp formatted as string
     */
    public static String format(final Instant timestamp)
    {
        if (timestamp == null)
            return "null";
        return TimestampFormats.FULL_FORMAT.format(timestamp);
    }

    /** Take a String and return a time stamp
     *
     *  @param sTimestamp
     *  @return
     *  @throws Exception on error
     *  @author A.PHILIPPE, L.PHILIPPE GANIL/FRANCE
     */
    public static Instant parse(final String sTimestamp) throws Exception
    {
        if (sTimestamp == "" || sTimestamp == null)
            return null;

        Exception error = null;
        try
        {
            return Instant.from(TimestampFormats.FULL_FORMAT.parse(sTimestamp));
        }
        catch (Exception ex)
        {
            error = ex;
        }

        try
        {
            return Instant.from(TimestampFormats.SECONDS_FORMAT.parse(sTimestamp));
        }
        catch (Exception ex)
        {
            error = ex;
        }
        // Reports the last error. Could also use the first one...
        throw error;
    }
}
