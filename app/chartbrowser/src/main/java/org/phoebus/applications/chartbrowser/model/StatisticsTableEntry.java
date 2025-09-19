/*******************************************************************************
 * Copyright (C) 2025 Thales.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.chartbrowser.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Represents an entry in the statistics table for the Chart Browser application.
 * Holds the process variable name and various statistical measures such as sample count,
 * mean, median, standard deviation, minimm, maximum, and sum for the PV data.
 */
public class StatisticsTableEntry {
    private final StringProperty pvName;
    private final IntegerProperty sampleCount;
    private final DoubleProperty mean;
    private final DoubleProperty median;
    private final DoubleProperty stdDev;
    private final DoubleProperty minValue;
    private final DoubleProperty maxValue;
    private final DoubleProperty sum;

    /**
     * Creates a new StatisticsTableEntry with the given process variable name.
     * Initializes sample count to 0, and all statistical values (mean, median, stdDev, minValue, maxValue, sum) to 0.0.
     *
     * @param pvName the name of the process variable
     */
    public StatisticsTableEntry(String pvName) {
        this.pvName = new SimpleStringProperty(pvName);
        this.sampleCount = new SimpleIntegerProperty(0);
        this.mean = new SimpleDoubleProperty(0.0);
        this.median = new SimpleDoubleProperty(0.0);
        this.stdDev = new SimpleDoubleProperty(0.0);
        this.minValue = new SimpleDoubleProperty(0.0);
        this.maxValue = new SimpleDoubleProperty(0.0);
        this.sum = new SimpleDoubleProperty(0.0);
    }

    /**
     * Gets the PV name property.
     *
     * @return the PV name property
     */
    public StringProperty pvNameProperty() {
        return pvName;
    }

    /**
     * Gets the process variable name.
     *
     * @return the PV name value
     */
    public String getPvName() {
        return pvName.get();
    }

    /**
     * Sets the process variable name.
     *
     * @param name the new PV name
     */
    public void setPvName(String name) {
        pvName.set(name);
    }

    /**
     * Gets the sample count property.
     *
     * @return the sample count property
     */
    public IntegerProperty sampleCountProperty() {
        return sampleCount;
    }

    /**
     * Gets the number of samples in the statistical calculation.
     *
     * @return the sample count value
     */
    public int getSampleCount() {
        return sampleCount.get();
    }

    /**
     * Sets the number of samples for the statistical calculation.
     *
     * @param count the new sample count value
     */
    public void setSampleCount(int count) {
        sampleCount.set(count);
    }

    /**
     * Gets the mean property.
     *
     * @return the mean property
     */
    public DoubleProperty meanProperty() {
        return mean;
    }

    /**
     * Gets the mean value of the samples.
     *
     * @return the mean value
     */
    public double getMean() {
        return mean.get();
    }

    /**
     * Sets the mean value for the samples.
     *
     * @param value the new mean value
     */
    public void setMean(double value) {
        mean.set(value);
    }

    /**
     * Gets the median property.
     *
     * @return the median property
     */
    public DoubleProperty medianProperty() {
        return median;
    }

    /**
     * Gets the median value of the samples.
     *
     * @return the median value
     */
    public double getMedian() {
        return median.get();
    }

    /**
     * Sets the median value for the samples.
     *
     * @param value the new median value
     */
    public void setMedian(double value) {
        median.set(value);
    }

    /**
     * Gets the standard deviation property.
     *
     * @return the stdDev property
     */
    public DoubleProperty stdDevProperty() {
        return stdDev;
    }

    /**
     * Gets the standard deviation of the samples.
     *
     * @return the standard deviation value
     */
    public double getStdDev() {
        return stdDev.get();
    }

    /**
     * Sets the standard deviation for the samples.
     *
     * @param value the new standard deviation value
     */
    public void setStdDev(double value) {
        stdDev.set(value);
    }

    /**
     * Gets the minimum value property.
     *
     * @return the minValue property
     */
    public DoubleProperty minValueProperty() {
        return minValue;
    }

    /**
     * Gets the minimum sample value.
     *
     * @return the minimum value
     */
    public double getMinValue() {
        return minValue.get();
    }

    /**
     * Sets the minimum sample value.
     *
     * @param value the new minimum value
     */
    public void setMinValue(double value) {
        minValue.set(value);
    }

    /**
     * Gets the maximum value property.
     *
     * @return the maxValue property
     */
    public DoubleProperty maxValueProperty() {
        return maxValue;
    }

    /**
     * Gets the maximum sample value.
     *
     * @return the maximum value
     */
    public double getMaxValue() {
        return maxValue.get();
    }

    /**
     * Sets the maximum sample value.
     *
     * @param value the new maximum value
     */
    public void setMaxValue(double value) {
        maxValue.set(value);
    }

    /**
     * Gets the sum property.
     *
     * @return the sum property
     */
    public DoubleProperty sumProperty() {
        return sum;
    }

    /**
     * Gets the sum of the sample values.
     *
     * @return the sum value
     */
    public double getSum() {
        return sum.get();
    }

    /**
     * Sets the sum of the sample values.
     *
     * @param value the new sum value
     */
    public void setSum(double value) {
        sum.set(value);
    }
}
