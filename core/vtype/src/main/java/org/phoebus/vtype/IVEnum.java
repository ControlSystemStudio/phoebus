/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.util.List;

/**
 *
 * @author carcassi
 */
class IVEnum extends IVMetadata implements VEnum {

    private final int index;
    private final List<String> labels;

    public IVEnum(int index, List<String> labels, Alarm alarm, Time time) {
        super(alarm, time);
        if (index < 0 || index >= labels.size()) {
            throw new IndexOutOfBoundsException("VEnum index must be within the label range");
        }
        this.index = index;
        this.labels = labels;
    }

    @Override
    public String getValue() {
        return labels.get(index);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public List<String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return VTypeToString.toString(this);
    }

}
