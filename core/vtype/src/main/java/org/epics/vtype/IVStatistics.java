/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

/** Immutable {@link VStatistics} implementation
 *
 *  @author Kay Kasemir
 */
final class IVStatistics extends VStatistics
{
    private final double average, stddev, min, max;
    private final int nsamples;
    private final Alarm alarm;
    private final Time time;

    public IVStatistics(final double average, final double stddev,
            final double min, final double max,
            final int nsamples,
            final Alarm alarm, final Time time)
    {
        this.average = average;
        this.stddev = stddev;
        this.min = min;
        this.max = max;
        this.nsamples = nsamples;
        this.alarm = alarm;
        this.time = time;
    }

    @Override
    public Alarm getAlarm()
    {
        return alarm;
    }

    @Override
    public Time getTime()
    {
        return time;
    }

    @Override
    public double getAverage()
    {
        return average;
    }

    @Override
    public double getStdDev()
    {
        return stddev;
    }

    @Override
    public double getMin()
    {
        return min;
    }

    @Override
    public double getMax()
    {
        return max;
    }

    @Override
    public int getNSamples()
    {
        return nsamples;
    }
}
