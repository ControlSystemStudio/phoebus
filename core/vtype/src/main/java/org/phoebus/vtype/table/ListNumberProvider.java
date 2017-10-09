/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

import org.phoebus.util.array.ListNumber;

/**
 *
 * @author carcassi
 */
public abstract class ListNumberProvider {

    private final Class<?> type;

    public ListNumberProvider(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    public abstract ListNumber createListNumber(int size);
}
