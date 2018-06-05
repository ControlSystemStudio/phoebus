/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/** When column in 'master' table is sorted, do the same in 'other' table.
 *  @author Kay Kasemir
 */
class LinkedColumnSorter implements InvalidationListener
{
    private static boolean updating = false;

    private final TableView<AlarmInfoRow> master, other;

    LinkedColumnSorter(final TableView<AlarmInfoRow> master, final TableView<AlarmInfoRow> other)
    {
        this.master = master;
        this.other = other;
        // Listening master.getSortOrder() only detects changes
        // in the column that's used to sort,
        // missing change in up/down sort direction of that same column.
        // Listening to the comparator detects any change in sorting.
        master.comparatorProperty().addListener(this);
    }

    @Override
    public void invalidated(final Observable observable)
    {
        if (updating)
            return;

        updating = true;

        final List<TableColumn<AlarmInfoRow, ?>> mcols = master.getColumns();
        final List<TableColumn<AlarmInfoRow, ?>> ocols = other.getColumns();

        final List<TableColumn<AlarmInfoRow, ?>> sorted = master.getSortOrder();
        final int index;
        if (sorted.isEmpty())
            index = -1;
        else
            index = mcols.indexOf(sorted.get(0));

        if (index < 0)
            other.getSortOrder().clear();
        else
        {
            final TableColumn<AlarmInfoRow, ?> col = ocols.get(index);
            other.getSortOrder().setAll(List.of(col));
            col.setSortType(mcols.get(index).getSortType());
        }

        updating = false;
    }
}
