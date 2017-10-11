/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Simple implementation for VBoolean.
 *
 * @author carcassi
 */
class IVBoolean extends IVMetadata implements VBoolean {

    private final boolean value;

    public IVBoolean(boolean value, Alarm alarm, Time time) {
        super(alarm, time);
        this.value = value;
    }

    @Override
    public Boolean getValue() {
        return value;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
