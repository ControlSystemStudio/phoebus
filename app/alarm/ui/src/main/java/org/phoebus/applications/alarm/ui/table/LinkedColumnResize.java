/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.alarm.ui.table;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/** When column in 'master' table is resized, update column in 'other' table.
 *  @author Kay Kasemir
 */
@SuppressWarnings("rawtypes")
class LinkedColumnResize implements Callback<ResizeFeatures, Boolean>
{
    private static boolean updating = false;

    private final TableView<AlarmInfoRow> master, other;

    public LinkedColumnResize(final TableView<AlarmInfoRow> master, final TableView<AlarmInfoRow> other)
    {
        this.master = master;
        this.other = other;

        // call() is invoked when user moves the table column separator,
        // but not when double-clicking it.
        // => Force update on double-click anywhere in the table
        master.addEventHandler(MouseEvent.MOUSE_CLICKED, event ->
        {
            if (event.getClickCount() >= 2)
                updateOther();
        });
    }

    @Override
    public Boolean call(final ResizeFeatures param)
    {
        final Boolean result = TableView.UNCONSTRAINED_RESIZE_POLICY.call(param);
        updateOther();
        return result;
    }

    private void updateOther()
    {
        if (! updating)
        {
            updating = true;
            int i = 0;
            for (TableColumn col : master.getColumns())
                other.getColumns().get(i++).setPrefWidth(col.getWidth());
            updating = false;
        }
    }
}