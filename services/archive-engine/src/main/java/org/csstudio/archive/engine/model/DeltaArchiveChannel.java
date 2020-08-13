/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.engine.model;

import static org.csstudio.archive.Engine.logger;

import java.util.logging.Level;

import org.epics.vtype.VType;
import org.phoebus.util.time.SecondsParser;
import org.phoebus.core.vtypes.VTypeHelper;

/** An ArchiveChannel that stores each incoming value that differs from
 *  the previous sample by some 'delta'.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DeltaArchiveChannel extends ArchiveChannel
{
    /** 'Delta' for value change */
    final private double delta;

    /** Estimated period of change in seconds */
    final private double period_estimate;

    /** @param name Name of the channel (PV)
     *  @param enablement How channel affects its groups
     *  @param buffer_capacity Size of sample buffer
     *  @param last_archived_value Last value from storage, or <code>null</code>.
     *  @param period_estimate Estimated change period [seconds]
     *  @param delta Value changes &ge; this value will be stored
     *  @throws Exception On error in PV setup
     * @deprecated Use {@link #DeltaArchiveChannel(String,String,Enablement,int,VType,double,double)} instead
     */
    @Deprecated
    public DeltaArchiveChannel(final String name,
                               final Enablement enablement,
                               final int buffer_capacity,
                               final VType last_archived_value,
                               final double period_estimate,
                               final double delta) throws Exception
    {
        this(name, null, enablement, buffer_capacity, last_archived_value, period_estimate, delta);
    }

    /** @param name Name of the channel (PV)
     * @param retention Retention policy; may be null for default/not supported
     * @param enablement How channel affects its groups
     * @param buffer_capacity Size of sample buffer
     * @param last_archived_value Last value from storage, or <code>null</code>.
     * @param period_estimate Estimated change period [seconds]
     * @param delta Value changes &ge; this value will be stored
     *  @throws Exception On error in PV setup
     */
    public DeltaArchiveChannel(final String name,
                               final String retention,
                               final Enablement enablement,
                               final int buffer_capacity,
                               final VType last_archived_value,
                               final double period_estimate,
                               final double delta) throws Exception
    {
        super(name, retention, enablement, buffer_capacity, last_archived_value);
        this.delta = delta;
        this.period_estimate = period_estimate;
    }

    public double getPeriodEstimate()
    {
        return period_estimate;
    }

    public double getDelta()
    {
        return delta;
    }

    @Override
    public String getMechanism()
    {
        return "on delta [" + SecondsParser.formatSeconds(period_estimate) +
               ", " + delta + "]";
    }

    /** Attempt to add each new value to the buffer. */
    @Override
    protected boolean handleNewValue(final VType value)
    {
        if (super.handleNewValue(value))
        {
            logger.log(Level.FINE, "Wrote first sample for {0}: {1}", new Object[] { getName(), value });
            return true;
        }
        if (isEnabled()  &&  isBeyondDelta(value))
        {
            logger.log(Level.FINE, "Wrote sample for {0}: {1}", new Object[] { getName(), value });
            addValueToBuffer(value);
            return true;
        }
        return false;
    }

    /** @param value Value to test
     *  @return <code>true</code> if this value is beyond 'delta' from the last value
     */
    private boolean isBeyondDelta(final VType value)
    {
        final double number = VTypeHelper.toDouble(value);
        // Archive NaN, Inf'ty
        if (Double.isNaN(number)  ||  Double.isInfinite(number))
            return true;
        double previous;
        synchronized (this)
        {
            // Anything to compare against?
            if (last_archived_value == null)
                return true;
            previous = VTypeHelper.toDouble(last_archived_value);
        }
        return Math.abs(previous - number) >= delta;
    }
}
