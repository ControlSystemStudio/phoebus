/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.pva.data.nt;

import java.time.Instant;

import org.epics.pva.data.PVAInt;
import org.epics.pva.data.PVALong;
import org.epics.pva.data.PVAStructure;

/**
 * Normative timestamp type
 * 
 * structure
 *   long secondsPastEpoch
 *   int nanoseconds
 *   int userTag
 * 
 * @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVATimeStamp extends PVAStructure
{
    public static final Instant NO_TIME = Instant.ofEpochSecond(0, 0);
    public static final String TIMESTAMP_NAME_STRING = "timeStamp";

    private final PVALong secs;
    private final PVAInt nano;

    /** 'now' */
    public PVATimeStamp()
    {
        this(Instant.now());
    }

    /** @param time Instant */
    public PVATimeStamp(final Instant time)
    {
        this(TIMESTAMP_NAME_STRING, time);
    }

    /** @param name Name for 'now' */
    public PVATimeStamp(final String name)
    {
        this(name, Instant.now());
    }

    /** @param name Name
     *  @param time Time
     */
    public PVATimeStamp(final String name, final Instant time)
    {
        super(name, "time_t",
              new PVALong("secondsPastEpoch", false, time.getEpochSecond()),
              new PVAInt("nanoseconds", false, time.getNano()),
              new PVAInt("userTag", 0));
        secs = get(1);
        nano = get(2);
    }

    /** @param time Desired time (seconds, nanoseconds) */
    public void set(final Instant time)
    {
        secs.set(time.getEpochSecond());
        nano.set(time.getNano());
    }

    /** Update "timeStamp" in value
     *  @param value Value
     *  @param time Desired time (seconds, nanoseconds)
     */
    public static void set(final PVAStructure value, final Instant time)
    {
        final PVAStructure ts = value.get(TIMESTAMP_NAME_STRING);
        if (ts == null)
            throw new IllegalArgumentException("Cannot locate timeStamp in " + value);
        // Assume a structure "timeStamp" starts with seconds, nano
        PVALong secs = ts.get(1);
        PVAInt nano = ts.get(2);
        secs.set(time.getEpochSecond());
        nano.set(time.getNano());
    }

    public Instant instant() {
        if (secs == null || nano == null)
                return NO_TIME;
        else
            return Instant.ofEpochSecond(secs.get(), nano.get());
    }
}
