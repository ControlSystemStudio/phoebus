/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Scalar double with alarm, timestamp, display and control information.
 * Auto-unboxing makes the extra method for the primitive type
 * unnecessary.
 *
 * @author carcassi
 */
public interface VDouble extends VNumber {

    /**
     * {@inheritDoc }
     */
    @Override
    Double getValue();

    /**
     * Creates a new VDouble.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VDouble create(final Double value, final Alarm alarm, final Time time, final Display display) {
        return new IVDouble(value, alarm, time, display);
    }
}
