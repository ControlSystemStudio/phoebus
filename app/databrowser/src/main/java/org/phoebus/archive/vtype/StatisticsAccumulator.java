/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VStatistics;

/** Accumulator that tracks the {@link Statistics} of received values
 *  @author Kay Kasemir
 */
public class StatisticsAccumulator extends VStatistics
{
    private double sum = 0.0;
    private double square = 0.0;
    private double min = Double.MAX_VALUE;
    private double max = -Double.MAX_VALUE;
    private int count = 0;
    private Time time = Time.now();

    /** Initialize empty statistics */
    public StatisticsAccumulator()
    {
    }

    @Override
    public Display getDisplay()
    {
        return Display.none();
    }

    /** Initialize
     *  @param initial_values Initial values to add to the stats
     */
    public StatisticsAccumulator(final double... initial_values)
    {
        for (double value : initial_values)
            add(value);
    }

    /** @param value Value to be added to the accumulator */
    public void add(final double value)
    {
        sum += value;
        square += value * value;
        if (value < min)
            min = value;
        if (value > max)
            max = value;
        ++count;
        time = Time.now();
    }

    @Override
    public Double getAverage()
    {
        return sum / count;
    }

    @Override
    public Double getStdDev()
    {
        return Math.sqrt(count * square - sum * sum) / count;
    }

    @Override
    public Double getMin()
    {
        return min;
    }

    @Override
    public Double getMax()
    {
        return max;
    }

    @Override
    public Integer getNSamples()
    {
        return count;
    }

    @Override
    public Alarm getAlarm()
    {
        return Alarm.none();
    }

    @Override
    public Time getTime()
    {
        return time;
    }
}
