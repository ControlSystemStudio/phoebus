/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListLong;

/**
 *
 * @author carcassi
 */
class IVLongArray extends IVNumberArray implements VLongArray {

    private final ListLong data;


    public IVLongArray(ListLong data, ListInt sizes,
            Alarm alarm, Time time, Display display) {
        super(sizes, null, alarm, time, display);
        this.data = data;
    }
    
    public IVLongArray(ListLong data, ListInt sizes, List<ArrayDimensionDisplay> dimDisplay,
            Alarm alarm, Time time, Display display) {
        super(sizes, dimDisplay, alarm, time, display);
        this.data = data;
    }

    @Override
    public ListLong getData() {
        return data;
    }

}
