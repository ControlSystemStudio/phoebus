/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Scalar short with alarm, timestamp, display and control information.
 * Auto-unboxing makes the extra method for the primitive type
 * unnecessary.
 *
 * @author carcassi
 */
public interface VShort extends VNumber {
    /**
     * {@inheritDoc }
     */
    @Override
    Short getValue();
    

    /**
     * Creates a new VShort.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VShort create(final Short value, final Alarm alarm, final Time time, final Display display) {
        return new IVShort(value, alarm, time, display);
    }
}
