/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

import java.util.List;
import java.util.Objects;
import org.phoebus.util.array.ListNumber;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VTable;

/**
 *
 * @author carcassi
 */
class ValueFilter {
    private final VTable table;
    private final int columnIndex;
    private final Object value;

    public ValueFilter(VTable table, String columnName, Object value) {
        this.table = table;
        columnIndex = VTableFactory.columnNames(table).indexOf(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Table does not contain column '" + columnName + "'");
        }
        Class<?> columnType = table.getColumnType(columnIndex);
        if (columnType.isPrimitive()) {
            if (!(value instanceof VNumber)) {
                throw new IllegalArgumentException("Column '" + columnName + "' is a number but not value '" + value + "'");
            }
        } else if (columnType.equals(String.class)) {
            if (!(value instanceof VString)) {
                throw new IllegalArgumentException("Column '" + columnName + "' is a string but not value '" + value + "'");
            }
        } else {
            throw new UnsupportedOperationException("Equal value filter only works on numbers and strings");
        }
        this.value = value;
    }

    public boolean filterRow(int rowIndex) {
        if (value instanceof VNumber) {
            double columnValue = ((ListNumber) table.getColumnData(columnIndex)).getDouble(rowIndex);
            return columnValue == ((VNumber) value).getValue().doubleValue();
        } else if (value instanceof VString) {
            @SuppressWarnings("unchecked")
            List<String> columnData = (List<String>) table.getColumnData(columnIndex);
            return Objects.equals(columnData.get(rowIndex), ((VString) value).getValue());
        }
        throw new IllegalStateException("Unexpected error");
    }

}
