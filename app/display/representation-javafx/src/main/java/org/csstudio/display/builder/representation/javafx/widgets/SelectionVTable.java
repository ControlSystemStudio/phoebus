/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.representation.javafx.Messages;
import org.epics.util.array.ListNumber;

/** VTable that holds current table widget selection
 *
 *  <p>A table with the currently selected rows from
 *  the table, using the table headers,
 *  plus added columns on the left for the row and column index.
 *
 *  <p>For a basic single-click selection, that provides
 *  the cell index as well as the complete row data.
 *  If multiple cells are selected, the row data may
 *  be repeated, as in this example where two cells are
 *  selected in row 1:
 *
 *  <table border="1">
 *  <tr> <th>Row</th> <th>Column</th> <th>Header 1</th> <th>Header 2</th> <th>...</th> </tr>
 *  <tr> <td>1</td>   <td>0</td>      <td>13.15</td>    <td>Fred</td>     <td>...</td> </tr>
 *  <tr> <td>1</td>   <td>1</td>      <td>13.15</td>    <td>Fred</td>     <td>...</td> </tr>
 *  <tr> <td>2</td>   <td>1</td>      <td>3.14</td>     <td>Jane</td>     <td>...</td> </tr>
 *  </table>
 *
 *  @author Kay Kasemir
 */
// Access limited to package
class SelectionVTable implements VTable
{
    private final List<String> headers;
    private final ListInt rows, cols;
    private final List<List<String>> columns;

    public SelectionVTable(final List<String> headers, final int[] rows, final int[] cols, final List<List<String>> columns)
    {
        this.headers = headers;
        this.rows = new ArrayInt(rows);
        this.cols = new ArrayInt(cols);
        this.columns = columns;
    }

    @Override
    public int getRowCount()
    {
        return rows.size();
    }

    @Override
    public int getColumnCount()
    {
        return 2 + headers.size();
    }

    @Override
    public String getColumnName(final int column)
    {
        switch (column)
        {
        case 0:
            return Messages.Row;
        case 1:
            return Messages.Column;
        default:
            return headers.get(column-2);
        }
    }

    @Override
    public Class<?> getColumnType(final int column)
    {
        switch (column)
        {
        case 0:
            return Integer.TYPE;
        case 1:
            return Integer.TYPE;
        default:
            return ListNumber.class;
        }
    }

    @Override
    public Object getColumnData(final int column)
    {
        switch (column)
        {
        case 0:
            return rows;
        case 1:
            return cols;
        default:
            return columns.get(column-2);
        }
    }

    @Override
    public String toString()
    {
        return VTypeUtil.getValueString(this, false);
    }
}
