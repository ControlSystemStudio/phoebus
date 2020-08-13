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

/** Normative timestamp type
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVATimeStamp extends PVAStructure
{
    private final PVALong secs;
    private final PVAInt nano;

    public PVATimeStamp()
    {
        this(Instant.now());
    }

    public PVATimeStamp(final Instant time)
    {
        this("timeStamp", time);
    }

    public PVATimeStamp(final String name)
    {
        this(name, Instant.now());
    }

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
        final PVAStructure ts = value.get("timeStamp");
        if (ts == null)
            throw new IllegalArgumentException("Cannot locate timeStamp in " + value);
        // Assume a structure "timeStamp" starts with seconds, nano
        PVALong secs = ts.get(1);
        PVAInt nano = ts.get(2);
        secs.set(time.getEpochSecond());
        nano.set(time.getNano());
    }
}
