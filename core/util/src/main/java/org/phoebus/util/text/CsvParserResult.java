/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.util.text;

import java.util.List;
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListNumber;

/**
 * The result of the CSV parsing.
 * <p>
 * This class contains all the information about the parsing: whether it was
 * successful, an error message, the parsed data, the parsed header and
 * the column type.
 * <p>
 * TODO: the error handling could be extended to include multiple error messages
 * while still giving a best effort result (e.g. skipping the lines that
 * can't be parsed).
 *
 * @author carcassi
 */
public class CsvParserResult {
    private final List<String> columnNames;
    private final List<Object> columnValues;
    private final List<Class<?>> columnTypes;
    private final int rowCount;
    private final boolean parsingSuccessful;
    private final String message;

    CsvParserResult(List<String> columnNames, List<Object> columnValues, List<Class<?>> columnTypes,
            int rowCount, boolean parsingSuccessful, String message) {
        this.columnNames = columnNames;
        this.columnValues = columnValues;
        this.columnTypes = columnTypes;
        this.rowCount = rowCount;
        this.parsingSuccessful = parsingSuccessful;
        this.message = message;
    }

    /**
     * The header of the CSV table.
     *
     * @return the list of column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * The data of each column.
     * <p>
     * Depending on the type, the data will be stored in a {@link List} (for
     * Objects) and in a {@link ListNumber} (for primitive data). For example,
     * if the column type is {@link String}, then one can expect a {@code List<String>}.
     * If it's {@code double}, then one can expect a {@link ListDouble}.
     *
     * @return the list of column data
     */
    public List<Object> getColumnValues() {
        return columnValues;
    }

    /**
     * The type of data found in the column.
     * <p>
     * At present, it can be either {@link String} or {@code double}.
     *
     * @return list of column types
     */
    public List<Class<?>> getColumnTypes() {
        return columnTypes;
    }

    /**
     * The number of rows.
     *
     * @return the number of rows
     */
    public int getRowCount() {
        return rowCount;
    }

    /**
     * True whether the parsing was successful and one can safely read
     * data from the data methods.
     *
     * @return true if data is present and complete
     */
    public boolean isParsingSuccessful() {
        return parsingSuccessful;
    }

    /**
     * An error message.
     *
     * @return an error message; null if no error occurred
     */
    public String getMessage() {
        return message;
    }

}
