/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

/**
 *
 * @author carcassi
 */
class IVString extends IVMetadata implements VString {

    private final String value;

    public IVString(String value, Alarm alarm, Time time) {
        super(alarm, time);
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
