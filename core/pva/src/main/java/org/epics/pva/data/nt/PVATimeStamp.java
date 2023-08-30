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
    public static final String SECONDS_PAST_EPOCH = "secondsPastEpoch";
    public static final String NANOSECONDS = "nanoseconds";
    /** Type name for time stamp */
    public static final String TIME_T = "time_t";

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
        this(name,
             new PVALong(SECONDS_PAST_EPOCH, false, time.getEpochSecond()),
             new PVAInt(NANOSECONDS, false, time.getNano()));
    }

    /** 
     * Constructor with PVAData
     * @param secs secondsPastEpoch
     *  @param nanos nanoseconds
     */
    public PVATimeStamp(final String name, final PVALong secs, final PVAInt nanos)
    {
        super(name, TIME_T,
              secs,
              nanos,
              new PVAInt("userTag", 0));
        this.secs = secs;
        this.nano = nanos;
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

    /** 
     * Conversion from structure to PVATimeStamp
     * 
     * @param structure Potential "time_t" structure
     *  @return PVATimeStamp or <code>null</code>
     */
    public static PVATimeStamp fromStructure(PVAStructure structure) {
        if (structure != null && structure.getName().equals(TIMESTAMP_NAME_STRING))
        {
            final PVALong secs = structure.get(SECONDS_PAST_EPOCH);
            final PVAInt nano = structure.get(NANOSECONDS);
            if (secs != null && nano != null) {
                return new PVATimeStamp(TIMESTAMP_NAME_STRING, secs, nano);
            }
        }
        return null;
    }

    /**
     * Get TimeStamp from a PVAStructure
     * 
     * @param structure Structure containing TimeStamp
     * @return PVATimeStamp or <code>null</code>
     */
    public static PVATimeStamp getTimeStamp(PVAStructure structure) {
        PVAStructure timestampStructure = structure.get(TIMESTAMP_NAME_STRING);
        if (timestampStructure != null) {
            return fromStructure(timestampStructure);
        }
        return null;
    }
}
