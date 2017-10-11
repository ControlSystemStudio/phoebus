/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;
import org.phoebus.util.array.ListFloat;
import org.phoebus.util.array.ListInt;

/**
 *
 * @author carcassi
 */
class IVFloatArray extends IVNumberArray implements VFloatArray {

    private final ListFloat data;

    public IVFloatArray(ListFloat data, ListInt sizes,
            Alarm alarm, Time time, Display display) {
        this(data, sizes, null, alarm, time, display);
    }

    public IVFloatArray(ListFloat data, ListInt sizes, List<ArrayDimensionDisplay> dimDisplay,
            Alarm alarm, Time time, Display display) {
        super(sizes, dimDisplay, alarm, time, display);
        this.data = data;
    }

    @Override
    public ListFloat getData() {
        return data;
    }

}
