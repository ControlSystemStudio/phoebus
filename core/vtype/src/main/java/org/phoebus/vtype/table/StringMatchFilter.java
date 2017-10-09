/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

import java.util.List;
import org.phoebus.vtype.VTable;

/**
 *
 * @author carcassi
 */
class StringMatchFilter {
    private final VTable table;
    private final int columnIndex;
    private final String substring;

    public StringMatchFilter(VTable table, String columnName, String substring) {
        this.table = table;
        columnIndex = VTableFactory.columnNames(table).indexOf(columnName);
        if (columnIndex == -1) {
            throw new IllegalArgumentException("Table does not contain column '" + columnName + "'");
        }
        Class<?> columnType = table.getColumnType(columnIndex);
        if (!columnType.equals(String.class)) {
            throw new IllegalArgumentException("Column '" + columnName + "' is not a string");
        }
        this.substring = substring;
    }

    public boolean filterRow(int rowIndex) {
        @SuppressWarnings("unchecked")
        List<String> columnData = (List<String>) table.getColumnData(columnIndex);
        return columnData.get(rowIndex).contains(substring);
    }

}
