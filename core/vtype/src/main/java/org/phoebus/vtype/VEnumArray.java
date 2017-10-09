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
}
