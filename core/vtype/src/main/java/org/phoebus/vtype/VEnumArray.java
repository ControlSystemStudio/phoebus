/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

import org.phoebus.util.array.ListInt;

/**
 *
 * @author carcassi
 */
public interface VEnumArray extends Array, Enum, Alarm, Time {
    @Override
    List<String> getData();

    /**
     * Returns the indexes instead of the labels.
     *
     * @return an array of indexes
     */
    ListInt getIndexes();
    

    /**
     * Creates a new {@link VEnumArray}
     * 
     * @param indexes
     * @param labels
     * @param sizes
     * @param alarm
     * @param time
     * @return
     */
    public static VEnumArray create(ListInt indexes, List<String> labels, ListInt sizes, Alarm alarm, Time time) {
        return new IVEnumArray(indexes, labels, sizes, alarm, time);
    }
}
