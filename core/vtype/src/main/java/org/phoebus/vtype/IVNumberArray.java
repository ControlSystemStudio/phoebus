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
abstract class IVNumberArray extends IVNumeric implements VNumberArray {

    private final ListInt sizes;
    private final List<ArrayDimensionDisplay> dimensionDisplay;

    public IVNumberArray(ListInt sizes, List<ArrayDimensionDisplay> dimDisplay,
            Alarm alarm, Time time, Display display) {
        super(alarm, time, display);
        this.sizes = sizes;
        if (dimDisplay == null) {
            this.dimensionDisplay = ValueUtil.defaultArrayDisplay(sizes);
        } else {
            this.dimensionDisplay = dimDisplay;
        }
    }

    @Override
    public final ListInt getSizes() {
        return sizes;
    }

    @Override
    public final String toString() {
        return VTypeToString.toString(this);
    }

    @Override
    public final List<ArrayDimensionDisplay> getDimensionDisplay() {
        return dimensionDisplay;
    }

}
