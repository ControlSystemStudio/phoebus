/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 * Immutable VMultiDouble implementation.
 *
 * @author carcassi
 */
class IVMultiDouble extends IVNumeric implements VMultiDouble {

    private final List<VDouble> values;

    IVMultiDouble(List<VDouble> values, Alarm alarm, Time time, Display display) {
        super(alarm, time, display);
        this.values = values;
    }

    @Override
    public List<VDouble> getValues() {
        return values;
    }

}
