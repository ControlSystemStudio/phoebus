/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.reader.channelarchiver.file;

import java.time.Instant;

import org.phoebus.util.time.TimestampFormats;

/** Helper for dealing with time stamps in Channel Archiver data files
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArchiveFileTime
{
    /** Seconds from Posix epoch (1917) to EPICS epoch (1990) */
    public static long EPICS_OFFSET = 631152000L;

    public static boolean isZeroTime(final Instant timestamp)
    {
        return timestamp.getEpochSecond() == EPICS_OFFSET  &&
               timestamp.getNano() == 0;
    }

    /** @param timestamp Timestamp
     *  @return String representation, recognizing '0' time stamps
     */
    public static String format(final Instant timestamp)
    {
        if (isZeroTime(timestamp))
            return "0";
        return TimestampFormats.FULL_FORMAT.format(timestamp);
    }
}
