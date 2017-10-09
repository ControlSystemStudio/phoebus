/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Basic type for statistical information of numeric types. The methods never return
 * null, even if no connection was ever made. One <b>must always look</b>
 * at the alarm severity to be able to correctly interpret the value.
 * <p>
 * This type can be used regardless of the method used to calculate the average
 * (instances: &Sigma;<i>x<sub>i</sub>/N</i>,
 * time: &Sigma;<i>x<sub>i</sub>&Delta;t<sub>i</sub>/&Delta;t</i>,
 * time with linear interpolation, exponential backoff, ...).
 * <p>
 * No integer statistics, since averages are not integer in general.
 *
 * @author carcassi
 */
public interface Statistics {

    /**
     * The average. Never null.
     *
     * @return the average
     */
    Double getAverage();

    /**
     * The standard deviation. Never null.
     *
     * @return the standard deviation
     */
    Double getStdDev();

    /**
     * The minimum value.
     *
     * @return the minimum
     */
    Double getMin();

    /**
     * The maximum value.
     *
     * @return the maximum
     */
    Double getMax();

    /**
     * The number of samples.
     *
     * @return the number of samples
     */
    Integer getNSamples();
}
