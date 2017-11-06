/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

/** Listener to a {@link StringTable}
 *
 *  @author Kay Kasemir
 */
public interface StringTableListener
{
    /** Invoked when the headers and data of the table changes
     *
     *  <p>May be the result of user editing a cell,
     *  adding a column etc.
     *
     *  @param table Table that changed
     */
    public void tableChanged(StringTable table);

    /** Invoked when the data of the table changes,
     *
     *  <p>May be the result of user editing a cell,
     *  where the columns otherwise stayed the same
     *
     *  @param table Table that changed
     */
    public void dataChanged(StringTable table);

    /** Invoked when cells in the table are selected
     *
     *  <p>Both arrays will have the same length,
     *  i.e. rows[i] and cols[i] represent the ith selected cell.
     *
     *  @param table Table where cells were selected
     *  @param rows Indices of selected rows
     *  @param cols Indices of selected columns
     */
    public void selectionChanged(StringTable table, int[] rows, int[] cols);
}
