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

/** An ArchiveChannel that stores each incoming value.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MonitoredArchiveChannel extends ArchiveChannel
{
    /** Estimated period of change in seconds */
    final private double period_estimate;

    /** @see ArchiveChannel#ArchiveChannel(String, int, IValue)
     * @deprecated Use {@link #MonitoredArchiveChannel(String,String,Enablement,int,VType,double)} instead*/
    @Deprecated
    public MonitoredArchiveChannel(final String name,
                                   final Enablement enablement,
                                   final int buffer_capacity,
                                   final VType last_archived_value,
                                   final double period_estimate) throws Exception
    {
        this(name, null, enablement, buffer_capacity, last_archived_value, period_estimate);
    }

    /** @see ArchiveChannel#ArchiveChannel(String, String, int, IValue) */
    public MonitoredArchiveChannel(final String name,
                                   final String retention,
                                   final Enablement enablement,
                                   final int buffer_capacity,
                                   final VType last_archived_value,
                                   final double period_estimate) throws Exception
    {
        super(name, retention, enablement, buffer_capacity, last_archived_value);
        this.period_estimate = period_estimate;
    }

    public double getPeriodEstimate()
    {
        return period_estimate;
    }

    @Override
    public String getMechanism()
    {
        return "on change [" + SecondsParser.formatSeconds(period_estimate) + "]";
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
        if (isEnabled())
        {
            logger.log(Level.FINE, "Wrote sample for {0}: {1}", new Object[] { getName(), value });
            addValueToBuffer(value);
            return true;
        }
        return false;
    }
}
