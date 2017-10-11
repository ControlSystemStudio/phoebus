/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

import org.phoebus.util.array.ListByte;
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListFloat;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListLong;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.array.ListShort;

/**
 * Numeric array with alarm, timestamp, display and control information.
 * <p>
 * This class allows to use any numeric array (i.e. {@link VIntArray} or
 * {@link VDoubleArray}) through the same interface.
 *
 * @author carcassi
 */
public interface VNumberArray extends Array, Alarm, Time, Display {
    @Override
    ListNumber getData();

    /**
     * Returns the boundaries of each cell.
     *
     * @return the dimension display; can't be null
     */
    List<ArrayDimensionDisplay> getDimensionDisplay();
    

    /**
     * Creates a new VNumber based on the type of the data
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new number
     */
    public static VNumberArray create(ListNumber data, Alarm alarm, Time time, Display display){
        if (data instanceof ListDouble) {
            return VDoubleArray.create((ListDouble) data, alarm, time, display);
        } else if (data instanceof ListFloat) {
            return VFloatArray.create((ListFloat) data, alarm, time, display);
        } else if (data instanceof ListLong) {
            return VLongArray.create((ListLong) data, alarm, time, display);
        } else if (data instanceof ListInt) {
            return VIntArray.create((ListInt) data, alarm, time, display);
        } else if (data instanceof ListShort) {
            return VShortArray.create((ListShort) data, alarm, time, display);
        } else if (data instanceof ListByte) {
            return VByteArray.create((ListByte) data, alarm, time, display);
        }
        throw new UnsupportedOperationException();
    }
}
