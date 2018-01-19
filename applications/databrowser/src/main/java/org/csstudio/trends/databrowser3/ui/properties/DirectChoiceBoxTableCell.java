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
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
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
    private final ChoiceBox<T> choicebox;

    /** Flag to distinguish 'onAction' caused by user
     *  from call when setting value programmatically,
     *  to prevent recursive updates
     */
    private boolean changing = false;

    /** Constructor
     *  @param choices Choices to offer, may be updated at runtime
     */
    public DirectChoiceBoxTableCell(final ObservableList<T> choices)
    {
        choicebox = new ChoiceBox<T>(choices);
    }

    @Override
    protected void updateItem(final T value, final boolean empty)
    {
        super.updateItem(value, empty);

        if (empty)
            setGraphic(null);
        else
        {
            changing = true;
            choicebox.setValue(value);
            changing = false;
            setGraphic(choicebox);

            choicebox.setOnAction(event ->
            {
                // Prevent loop from 'setValue' call above
                if (changing)
                    return;
                // Fire 'onEditCommit'
                final TableView<S> table = getTableView();
                final TableColumn<S, T> col = getTableColumn();
                final TablePosition<S, T> pos = new TablePosition<>(table, getTableRow().getIndex(), col);
                Objects.requireNonNull(col.getOnEditCommit(), "Must define onEditCommit handler")
                       .handle(new CellEditEvent<>(table, pos, TableColumn.editCommitEvent(), choicebox.getValue()));
            });
        }
    }
}