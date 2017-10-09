/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListLong;

/**
 * Long array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VLongArray extends VNumberArray {

    @Override
    ListLong getData();
}
