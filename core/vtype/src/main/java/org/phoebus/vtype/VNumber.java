/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Scalar number with alarm, timestamp, display and control information.
 * <p>
 * This class allows to use any scalar number (i.e. {@link VInt} or
 * {@link VDouble}) through the same interface.
 *
 * @author carcassi
 */
public interface VNumber extends Scalar, Alarm, Time, Display {

    /**
     * The numeric value.
     *
     * @return the value
     */
    @Override
    Number getValue();
    

    /**
     * Creates a new VNumber based on the type of the data
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new number
     */
    public static VNumber create(Number value, Alarm alarm, Time time, Display display){
        if (value instanceof Double) {
            return VDouble.create((Double) value, alarm, time, display);
        } else if (value instanceof Float) {
            return VFloat.create((Float) value, alarm, time, display);
        } else if (value instanceof Long) {
            return VLong.create((Long) value, alarm, time, display);
        } else if (value instanceof Integer) {
            return VInt.create((Integer) value, alarm, time, display);
        } else if (value instanceof Short) {
            return VShort.create((Short) value, alarm, time, display);
        } else if (value instanceof Byte) {
            return VByte.create((Byte) value, alarm, time, display);
        }
        throw new IllegalArgumentException("Only standard Java implementations of Number are supported");
    }
}
