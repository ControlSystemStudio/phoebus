/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 * Immutable VByte implementation.
 *
 * @author carcassi
 */
class IVByte extends IVNumeric implements VByte {

    private final Byte value;

    IVByte(Byte value, Alarm alarm, Time time, Display display) {
        super(alarm, time, display);
        this.value = value;
    }

    @Override
    public Byte getValue() {
        return value;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
