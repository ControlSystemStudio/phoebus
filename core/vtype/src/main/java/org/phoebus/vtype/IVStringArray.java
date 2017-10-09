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
class IVStringArray extends IVMetadata implements VStringArray {

    private final ListInt sizes;
    private final List<String> data;

    public IVStringArray(List<String> data, ListInt sizes, Alarm alarm, Time time) {
        super(alarm, time);
        this.data = data;
        this.sizes = sizes;
    }

    @Override
    public List<String> getData() {
        return data;
    }

    @Override
    public ListInt getSizes() {
        return sizes;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
