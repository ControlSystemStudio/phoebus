/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Immutable VFloat implementation.
 *
 * @author carcassi
 */
class IVFloat extends IVNumeric implements VFloat {

    private final Float value;

    IVFloat(Float value, Alarm alarm, Time time, Display display) {
        super(alarm, time, display);
        this.value = value;
    }

    @Override
    public Float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
