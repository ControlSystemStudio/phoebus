/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListInt;

/**
 * Int array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VIntArray extends VNumberArray {

    @Override
    ListInt getData();
    

    /**
     * Creates a new VIntArray.
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VIntArray create(final ListInt data, final Alarm alarm, final Time time, final Display display) {
        return new IVIntArray(data, null, alarm, time, display);
    }
}
