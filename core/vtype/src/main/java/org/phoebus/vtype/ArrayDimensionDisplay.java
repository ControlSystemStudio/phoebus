/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListNumber;

/**
 * Cell boundaries and unit information needed for array display.
 * <p>
 * For a given numeric dimension, it provides the cell boundaries and the unit
 * in terms of which the boundaries are expressed.
 *
 * @author carcassi
 */
public interface ArrayDimensionDisplay {

    /**
     * Returns the boundaries of the cell in the given unit.
     *
     * @return the boundaries
     */
    ListNumber getCellBoundaries();

    /**
     * String representation of the units using for all values.
     * Never null. If not available, returns the empty String.
     *
     * @return units
     */
    String getUnits();

    /**
     * Whether the values for this dimension are organized in the
     * opposite order.
     *
     * @return true if values are stored in the array in the reverse order
     */
    boolean isReversed();

}
