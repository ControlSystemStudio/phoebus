/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 *
 * @author carcassi
 */
public interface VStringArray extends Array, Alarm, Time {
    @Override
    List<String> getData();

    /**
     * Creates a new VStringArray.
     *
     * @param data the value
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VStringArray create(final List<String> data, final Alarm alarm, final Time time) {
        return new IVStringArray(data, null, alarm, time);
    }
}
