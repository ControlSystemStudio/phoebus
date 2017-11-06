/*******************************************************************************
 * Copyright (c) 2015-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

/** Helper for TableView
 *  @author Kay Kasemir
 */
public class TableHelper
{
    /** Move currently selected item in table up
     *  @param table {@link TableView}
     *  @param items List with items
     */
    public static <ITEM> void move_item_up(final TableView<ITEM> table, final ObservableList<ITEM> items)
    {
        final int sel = table.getSelectionModel().getSelectedIndex();
        if (sel >= 1)
        {
            ITEM prev = items.set(sel-1, items.get(sel));
            items.set(sel, prev);
        }
    }

    /** Move currently selected item in table down
     *  @param table {@link TableView}
     *  @param items List with items
     */
    public static <ITEM> void move_item_down(final TableView<ITEM> table, final ObservableList<ITEM> items)
    {
        final int sel = table.getSelectionModel().getSelectedIndex();
        if (sel >= 0  &&  (sel+1) < items.size())
        {
            ITEM next = items.set(sel+1, items.get(sel));
            items.set(sel, next);
        }
    }
}
