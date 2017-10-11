/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.Objects;
import org.phoebus.util.array.ListNumber;

class IArrayDimensionDisplay implements ArrayDimensionDisplay {

    private final ListNumber cellBoundaries;
    private final boolean reversed;
    private final String units;

    public IArrayDimensionDisplay(ListNumber cellBoundaries, boolean reversed, String units) {
        this.cellBoundaries = cellBoundaries;
        this.reversed = reversed;
        this.units = units;
    }

    @Override
    public ListNumber getCellBoundaries() {
        return cellBoundaries;
    }

    public int getSize() {
        return getCellBoundaries().size() - 1;
    }

    @Override
    public String getUnits() {
        return units;
    }

    @Override
    public boolean isReversed() {
        return reversed;
    }


    @Override
    public int hashCode() {
        return getCellBoundaries().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IArrayDimensionDisplay) {
            IArrayDimensionDisplay info = (IArrayDimensionDisplay) obj;
            return Objects.equals(info.getSize(), getSize()) &&
                    Objects.equals(info.isReversed(), isReversed()) &&
                    Objects.equals(info.getUnits(), getUnits()) &&
                    Objects.equals(info.getCellBoundaries(), getCellBoundaries());

        }

        return false;
    }
}
