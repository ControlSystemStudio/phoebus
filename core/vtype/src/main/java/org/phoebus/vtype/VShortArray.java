/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListShort;

/**
 * Short array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VShortArray extends VNumberArray {

    @Override
    ListShort getData();
    

    /**
     * Creates a new VShortArray.
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VShortArray create(final ListShort data, final Alarm alarm, final Time time, final Display display) {
        return new IVShortArray(data, null, alarm, time, display);
    }
}
