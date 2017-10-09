/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Scalar number with alarm, timestamp, display and control information.
 * <p>
 * This class allows to use any scalar number (i.e. {@link VInt} or
 * {@link VDouble}) through the same interface.
 *
 * @author carcassi
 */
public interface VNumber extends Scalar, Alarm, Time, Display, VType {

    /**
     * The numeric value.
     *
     * @return the value
     */
    @Override
    Number getValue();
}
