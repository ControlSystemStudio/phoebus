/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.archive.vtype;

import java.time.Instant;

import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.Statistics;
import org.phoebus.vtype.VStatistics;

/** Archive-derived {@link VStatistics} implementation
 *  @author Kay Kasemir
 */
public class ArchiveVStatistics extends ArchiveVDisplayType implements VStatistics
{
    final private double average;
    final private double min;
    final private double max;
    final private double stddev;
    final private int count;

    public ArchiveVStatistics(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final Display display,
            final double average, final double min, final double max, final double stddev, final int count)
    {
        super(timestamp, severity, status, display);
        this.average = average;
        this.min = min;
        this.max = max;
        this.stddev = stddev;
        this.count = count;
    }

    public ArchiveVStatistics(final Instant timestamp,
            final AlarmSeverity severity, final String status,
            final Display display,
            final Statistics stats)
    {
        this(timestamp, severity, status, display,
             stats.getAverage(),
             stats.getMin(), stats.getMax(),
             stats.getStdDev(), stats.getNSamples());
    }

    @Override
    public Double getAverage()
    {
        return average;
    }

    @Override
    public Double getStdDev()
    {
        return stddev;
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
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(average);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + count;
        temp = Double.doubleToLongBits(max);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(min);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(stddev);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (! (obj instanceof VStatistics))
            return false;
        final VStatistics other = (VStatistics) obj;
        return count == other.getNSamples()
            && min == other.getMin()
            && max == other.getMax()
            && average == other.getAverage()
            && stddev == other.getStdDev();
    }

    @Override
    public String toString()
    {
        return VTypeHelper.toString(this);
    }
}
