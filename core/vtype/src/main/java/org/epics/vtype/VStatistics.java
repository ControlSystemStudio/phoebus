/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.epics.vtype;

/** Statistics
 *
 *  <p>Based on similar type in original vtypes removed in EPICS 7.0.2
 *
 *  @author Kay Kasemir
 */
public abstract class VStatistics extends VType implements AlarmProvider, TimeProvider
{
    /** Create immutable {@link VStatistics}
     *  @param average Average
     *  @param stddev Standard deviation
     *  @param min Minimum
     *  @param max Maximum
     *  @param nsamples Number of samples
     *  @param alarm Alarm
     *  @param time Timestamp
     *  @return {@link VStatistics}
     */
    public static VStatistics of(final double average, final double stddev,
                                 final double min, final double max,
                                 final int nsamples,
                                 final Alarm alarm, final Time time)
    {
        return new IVStatistics(average, stddev, min, max, nsamples, alarm, time);
    }

    /** @return The average */
    public abstract double getAverage();

    /** @return The standard deviation */
    public abstract double getStdDev();

    /** @return The minimum */
    public abstract double getMin();

    /** @return The maximum */
    public abstract double getMax();

    /** @return The number of samples */
    public abstract int getNSamples();
}
