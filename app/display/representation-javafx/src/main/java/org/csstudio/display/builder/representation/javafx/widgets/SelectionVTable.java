/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.display.builder.representation.javafx.Messages;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListNumber;
import org.epics.vtype.VTable;

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
class SelectionVTable
{
    public static VTable create(final List<String> headers, final int[] rows, final int[] cols, final List<List<String>> columns)
    {
        final List<String> names = new ArrayList<>(2 + headers.size());
        names.add(Messages.Row);
        names.add(Messages.Column);
        names.addAll(headers);

        final List<Class<?>> types = new ArrayList<>(names.size());
        types.add(Integer.TYPE);
        types.add(Integer.TYPE);
        for (int i=2; i<names.size(); ++i)
            types.add(ListNumber.class);

        final List<Object> values = new ArrayList<>(names.size());
        values.add(ArrayInteger.of(rows));
        values.add(ArrayInteger.of(cols));
        for (Object col : columns)
            values.add(col);

        return VTable.of(types, names, values);
    }
}
