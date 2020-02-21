/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.trends.databrowser3.ui.properties;

import java.util.Objects;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

/** Similar to ChoiceBoxTableCell, but always showing the choice box
 *
 *  <p>..so user does not need to click once to activate, then again to pick a value.
 *  Fires the table column's `onEditCommit` event when user selects a value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DirectChoiceBoxTableCell<S, T> extends TableCell<S, T>
{
    private final ComboBox<T> choicebox;

    /** Constructor
     *  @param choices Choices to offer, may be updated at runtime
     */
    public DirectChoiceBoxTableCell(final ObservableList<T> choices)
    {
        // Used to be ChoiceBox, which is like a ComboBox without
        // 'edit' option and without border,
        // but that resulted in high CPU usage deep inside JFX code
        // when choices were added or removed while the choice box is
        // in a table cell.
        // https://github.com/ControlSystemStudio/phoebus/issues/1015
        //
        // ComboBox doesn't have that problem,
        // and with transparent background it looks like the choice box
        choicebox = new ComboBox<>(choices);
        choicebox.setStyle("-fx-background-color: transparent;");
    }

    @Override
    protected void updateItem(final T value, final boolean empty)
    {
        super.updateItem(value, empty);

        if (empty)
            setGraphic(null);
        else
        {
            choicebox.setValue(value);
            setGraphic(choicebox);

            choicebox.setOnAction(event ->
            {
                // 'onAction' is invoked from setValue as called above,
                // but also when table updates its cells.
                // Ignore those.
                // Also ignore dummy updates to null which happen
                // when the list of choices changes
                if (! choicebox.isShowing() ||
                    choicebox.getValue() == null)
                    return;

                final TableRow<S> row = getTableRow();
                if (row == null)
                    return;

                // Fire 'onEditCommit'
                final TableView<S> table = getTableView();
                final TableColumn<S, T> col = getTableColumn();
                final TablePosition<S, T> pos = new TablePosition<>(table, row.getIndex(), col);
                Objects.requireNonNull(col.getOnEditCommit(), "Must define onEditCommit handler")
                       .handle(new CellEditEvent<>(table, pos, TableColumn.editCommitEvent(), choicebox.getValue()));
            });
        }
    }
}
