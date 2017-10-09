/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import org.phoebus.util.array.ListBoolean;

/**
 * Byte array with alarm, timestamp, display and control information.
 *
 * @author carcassi
 */
public interface VBooleanArray extends Array, Alarm, Time {

    /**
     * {@inheritDoc }
     * @return the data
     */
    @Override
    ListBoolean getData();
}
