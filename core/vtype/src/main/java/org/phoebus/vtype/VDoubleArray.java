/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListDouble;

/**
 * Double array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VDoubleArray extends VNumberArray {

    /**
     * {@inheritDoc }
     * @return the data
     */
    @Override
    ListDouble getData();

    /**
     * Creates a new VDoubleArray.
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VDoubleArray create(final ListDouble data, final Alarm alarm, final Time time, final Display display) {
        return new IVDoubleArray(data, null, alarm, time, display);
    }
}
