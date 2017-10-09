/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype.table;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.array.ListNumbers;
import org.phoebus.vtype.VTable;

/**
 *
 * @author carcassi
 */
public class VColumn {

    private final String name;
    private final Class<?> type;
    private final Object data;

    public VColumn(String name, Class<?> type, Object data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public static VColumn from(VTable table, int index) {
        return new VColumn(table.getColumnName(index), table.getColumnType(index), table.getColumnData(index));
    }

    public static VColumn from(VTable table, String column) {
        if (column == null || table == null) {
            return null;
        }
        for (int index = 0; index < table.getColumnCount(); index++) {
            if (column.equals(table.getColumnName(index))) {
                return new VColumn(table.getColumnName(index), table.getColumnType(index), table.getColumnData(index));
            }
        }
        return null;
    }

    public static Map<String, VColumn> columnMap(VTable table) {
        Map<String, VColumn> columns = new HashMap<>();
        for (int index = 0; index < table.getColumnCount(); index++) {
            columns.put(table.getColumnName(index), from(table, index));
        }
        return columns;
    }

    public static Object combineData(Class<?> type, int size, ListInt offsets, List<VColumn> columns) {
        if (String.class.equals(type)) {
            return combineStringData(size, offsets, columns);
        } else if (Double.TYPE.equals(type)) {
            return combineDoubleData(size, offsets, columns);
        }
        throw new UnsupportedOperationException("Type " + type + " not supported for column combineData");
    }

    private static Object combineStringData(final int size, final ListInt offsets, final List<VColumn> columns) {
        return new AbstractList<String>() {

            @Override
            public String get(int index) {
                int tableIndex = ListNumbers.binarySearchValueOrLower(offsets, index);
                if (columns.get(tableIndex) == null) {
                    return null;
                }

                int rowIndex = index - offsets.getInt(tableIndex);
                // TODO: mismatched type should be handled better
                if (columns.get(tableIndex).getType() != String.class) {
                    return null;
                }
                @SuppressWarnings("unchecked")
                List<String> values = (List<String>) columns.get(tableIndex).getData();
                if (rowIndex < values.size()) {
                    return values.get(rowIndex);
                } else {
                    return null;
                }
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    private static Object combineDoubleData(final int size, final ListInt offsets, final List<VColumn> columns) {
        return new ListDouble() {

            @Override
            public double getDouble(int index) {
                int tableIndex = ListNumbers.binarySearchValueOrLower(offsets, index);
                if (columns.get(tableIndex) == null) {
                    return Double.NaN;
                }

                int rowIndex = index - offsets.getInt(tableIndex);
                // TODO: mismatched type should be handled better
                if (!ListNumber.class.isInstance(columns.get(tableIndex).getData())) {
                    return Double.NaN;
                }
                @SuppressWarnings("unchecked")
                ListNumber values = (ListNumber) columns.get(tableIndex).getData();
                if (rowIndex < values.size()) {
                    return values.getDouble(rowIndex);
                } else {
                    return Double.NaN;
                }
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

}
