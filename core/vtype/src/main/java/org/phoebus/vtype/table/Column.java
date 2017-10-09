/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

/**
 *
 * @author carcassi
 */
public abstract class Column {

    private final String name;
    private final Class<?> type;
    private final boolean generated;

    public Column(String name, Class<?> type, boolean generated) {
        this.name = name;
        this.type = type;
        this.generated = generated;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isGenerated() {
        return generated;
    }

    public abstract Object getData(int size);

}
