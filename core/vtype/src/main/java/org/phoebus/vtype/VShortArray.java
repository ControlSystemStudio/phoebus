/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListShort;

/**
 * Short array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VShortArray extends VNumberArray, VType {

    @Override
    ListShort getData();
}
