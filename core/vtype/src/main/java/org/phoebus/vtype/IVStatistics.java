/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * VStatistics implementation.
 *
 * @author carcassi
 */
class IVStatistics extends IVNumeric implements VStatistics {

    private Double average;
    private Double stdDev;
    private Double min;
    private Double max;
    private Integer nSamples;

    public IVStatistics(Double average, Double stdDev, Double min, Double max, Integer nSamples,
            Alarm alarm, Time time, Display display) {
        super(alarm, time, display);
        this.average = average;
        this.stdDev = stdDev;
        this.min = min;
        this.max = max;
        this.nSamples = nSamples;
    }



    @Override
    public Double getAverage() {
        return average;
    }

    @Override
    public Double getStdDev() {
        return stdDev;
    }

    @Override
    public Double getMin() {
        return min;
    }

    @Override
    public Double getMax() {
        return max;
    }

    @Override
    public Integer getNSamples() {
        return nSamples;
    }

}
