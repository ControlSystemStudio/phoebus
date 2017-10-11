/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListLong;

/**
 * Long array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VLongArray extends VNumberArray {

    @Override
    ListLong getData();
    

    /**
     * Creates a new VLongArray.
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VLongArray create(final ListLong data, final Alarm alarm, final Time time, final Display display) {
        return new IVLongArray(data, null, alarm, time, display);
    }
}
