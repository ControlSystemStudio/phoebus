/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListDouble;

/**
 * Double array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VDoubleArray extends VNumberArray, VType {

    /**
     * {@inheritDoc }
     * @return the data
     */
    @Override
    ListDouble getData();

}
