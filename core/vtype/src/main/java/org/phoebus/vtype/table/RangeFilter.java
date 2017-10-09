/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

import java.util.List;
import org.phoebus.util.array.ListNumber;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VTable;

/**
 *
 * @author carcassi
 */
class RangeFilter {
    private final VTable table;
    private final int columnIndex;
    private final Object min;
    private final Object max;

    public RangeFilter(VTable table, String columnName, Object min, Object max) {
        this.table = table;
        columnIndex = VTableFactory.columnNames(table).indexOf(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Table does not contain column '" + columnName + "'");
        }
        Class<?> columnType = table.getColumnType(columnIndex);
        if (columnType.isPrimitive()) {
            if (!(min instanceof VNumber && max instanceof VNumber)) {
                throw new IllegalArgumentException("Column '" + columnName + "' is a number but not boundaries '" + min + "' and '" + max + "'");
            }
        } else if (columnType.equals(String.class)) {
            if (!(min instanceof VString && max instanceof VString)) {
                throw new IllegalArgumentException("Column '" + columnName + "' is a string but not boundaries '" + min + "' and '" + max + "'");
            }
        } else {
            throw new UnsupportedOperationException("Equal value filter only works on numbers and strings");
        }
        this.min = min;
        this.max = max;
    }

    public boolean filterRow(int rowIndex) {
        if (min instanceof VNumber) {
            double columnValue = ((ListNumber) table.getColumnData(columnIndex)).getDouble(rowIndex);
            double minValue = ((VNumber) min).getValue().doubleValue();
            double maxValue = ((VNumber) max).getValue().doubleValue();
            return columnValue >= minValue && columnValue < maxValue;
        } else if (min instanceof VString) {
            @SuppressWarnings("unchecked")
            List<String> columnData = (List<String>) table.getColumnData(columnIndex);
            String columnValue = columnData.get(rowIndex);
            String minValue = ((VString) min).getValue();
            String maxValue = ((VString) max).getValue();
            return minValue.compareTo(columnValue) <= 0 && maxValue.compareTo(columnValue) > 0;
        }
        throw new IllegalStateException("Unexpected error");
    }

}
