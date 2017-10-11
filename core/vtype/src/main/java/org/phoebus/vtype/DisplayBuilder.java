/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.text.NumberFormat;
import org.phoebus.util.text.NumberFormats;

/**
 * Allows to build a Display object which can then be re-used to generate
 * values with the same metadata.
 *
 * @author carcassi
 */
public class DisplayBuilder {

    private Double lowerDisplayLimit = Double.NaN;
    private Double lowerCtrlLimit = Double.NaN;
    private Double lowerAlarmLimit = Double.NaN;
    private Double lowerWarningLimit = Double.NaN;
    private String units = "";
    private NumberFormat format = NumberFormats.toStringFormat();
    private Double upperWarningLimit = Double.NaN;
    private Double upperAlarmLimit = Double.NaN;
    private Double upperCtrlLimit = Double.NaN;
    private Double upperDisplayLimit = Double.NaN;

    DisplayBuilder(Double lowerDisplayLimit,
            Double lowerCtrlLimit, Double lowerAlarmLimit, Double lowerWarningLimit,
            String units, NumberFormat format, Double upperWarningLimit, Double upperAlarmLimit,
            Double upperCtrlLimit, Double upperDisplayLimit) {
        this.lowerDisplayLimit = lowerDisplayLimit;
        this.lowerCtrlLimit = lowerCtrlLimit;
        this.lowerAlarmLimit = lowerAlarmLimit;
        this.lowerWarningLimit = lowerWarningLimit;
        this.units = units;
        this.format = format;
        this.upperWarningLimit = upperWarningLimit;
        this.upperAlarmLimit = upperAlarmLimit;
        this.upperCtrlLimit = upperCtrlLimit;
        this.upperDisplayLimit = upperDisplayLimit;
    }

    /**
     * Creates a new display builder.
     */
    public DisplayBuilder() {
    }


    /**
     * Changes the lower display limit.
     *
     * @param lowerDisplayLimit the lower display limit
     * @return this
     */
    public DisplayBuilder lowerDisplayLimit(Double lowerDisplayLimit) {
        this.lowerDisplayLimit = lowerDisplayLimit;
        return this;
    }

    /**
     * Changes the lower control limit.
     *
     * @param lowerCtrlLimit the lower control limit
     * @return this
     */
    public DisplayBuilder lowerCtrlLimit(Double lowerCtrlLimit) {
        this.lowerCtrlLimit = lowerCtrlLimit;
        return this;
    }

    /**
     * Changes the lower alarm limit.
     *
     * @param lowerAlarmLimit the lower alarm limit
     * @return this
     */
    public DisplayBuilder lowerAlarmLimit(Double lowerAlarmLimit) {
        this.lowerAlarmLimit = lowerAlarmLimit;
        return this;
    }

    /**
     * Changes the lower warning limit.
     *
     * @param lowerWarningLimit the lower warning limit
     * @return this
     */
    public DisplayBuilder lowerWarningLimit(Double lowerWarningLimit) {
        this.lowerWarningLimit = lowerWarningLimit;
        return this;
    }

    /**
     * Changes the upper warning limit.
     *
     * @param upperWarningLimit the upper warning limit
     * @return this
     */
    public DisplayBuilder upperWarningLimit(Double upperWarningLimit) {
        this.upperWarningLimit = upperWarningLimit;
        return this;
    }

    /**
     * Changes the upper alarm limit.
     *
     * @param upperAlarmLimit the upper alarm limit
     * @return this
     */
    public DisplayBuilder upperAlarmLimit(Double upperAlarmLimit) {
        this.upperAlarmLimit = upperAlarmLimit;
        return this;
    }

    /**
     * Changes the upper control limit.
     *
     * @param upperCtrlLimit the upper control limit
     * @return this
     */
    public DisplayBuilder upperCtrlLimit(Double upperCtrlLimit) {
        this.upperCtrlLimit = upperCtrlLimit;
        return this;
    }

    /**
     * Changes the upper display limit.
     *
     * @param upperDisplayLimit the upper display limit
     * @return this
     */
    public DisplayBuilder upperDisplayLimit(Double upperDisplayLimit) {
        this.upperDisplayLimit = upperDisplayLimit;
        return this;
    }

    /**
     * Changes the unit.
     *
     * @param units the unit
     * @return this
     */
    public DisplayBuilder units(String units) {
        this.units = units;
        return this;
    }

    /**
     * Changes the number format.
     *
     * @param format the number format
     * @return this
     */
    public DisplayBuilder format(NumberFormat format) {
        this.format = format;
        return this;
    }

    /**
     * Creates a new Display.
     *
     * @return a new display
     */
    public Display build() {
        return ValueFactory.newDisplay(lowerDisplayLimit,
                lowerAlarmLimit, lowerWarningLimit, units, format, upperWarningLimit,
                upperAlarmLimit, upperDisplayLimit, lowerCtrlLimit, upperCtrlLimit);
    }

}
