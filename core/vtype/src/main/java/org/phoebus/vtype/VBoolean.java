/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Scalar boolean with alarm and timestamp.
 *
 * @author carcassi
 */
public interface VBoolean extends Scalar, Alarm, Time {

    /**
     * {@inheritDoc }
     */
    @Override
    Boolean getValue();
}
