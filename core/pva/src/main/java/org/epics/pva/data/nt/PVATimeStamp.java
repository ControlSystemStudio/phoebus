/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
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
        super("timeStamp", "time_t",
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
}
