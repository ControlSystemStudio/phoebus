/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;
import org.csstudio.display.pace.Messages;
import org.csstudio.display.pace.model.Column;
import org.csstudio.display.pace.model.Instance;
import org.csstudio.display.pace.model.Model;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/** GUI for PACE {@link Model}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GUI extends BorderPane
{
    private final Model model;
    private final TableView<Instance> table;

    public GUI(final Model model)
    {
        this.model = model;
        table = createTable();

        setTop(new Label("Optional message..."));
        setCenter(table);

        model.addListener(cell ->
        {
            // System.out.println("Update " + cell);
            Platform.runLater( () -> cell.getValue());
        });
    }

    private TableView<Instance> createTable()
    {
        final TableView<Instance> table = new TableView<>(FXCollections.observableList(model.getInstances()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setEditable(true);

        // By default, selected cells have changed colors that
        // conflict with the PACETableCell coloring.
        // -> Set fixed colors
        table.setStyle("-fx-base: #ddd; -fx-text-background-color: #000;");

        TableColumn<Instance, String> col = new TableColumn<>(Messages.SystemColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        table.getColumns().add(col);

        int col_index = 0;
        for (Column column : model.getColumns())
        {
            final int the_col_index = col_index;
            col = new TableColumn<>(column.getName());
            col.setCellFactory(info -> new PACETableCell());
            col.setCellValueFactory(cell -> cell.getValue().getCell(the_col_index).getValue());
            table.getColumns().add(col);

            if (column.isReadonly())
                col.setEditable(false);
            else
            {
                col.setOnEditCommit(event ->
                {
                    event.getRowValue().getCell(the_col_index).setUserValue(event.getNewValue());
                    final int row = event.getTablePosition().getRow();
                    // Start to edit same column in next row
                    if (row < table.getItems().size() - 1)
                        Platform.runLater(() ->  table.edit(row+1, event.getTableColumn()));
                });
            }

            ++col_index;
        }
        return table;
    }
}
