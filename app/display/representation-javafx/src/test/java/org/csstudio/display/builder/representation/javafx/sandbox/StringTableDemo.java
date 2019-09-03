/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.builder.representation.javafx.sandbox;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.phoebus.ui.javafx.ApplicationWrapper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;

/** Table of strings that allows editing while a cell updates
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class StringTableDemo extends ApplicationWrapper
{
    private ObservableList<List<StringProperty>> data = FXCollections.observableArrayList();
    private TableView<List<StringProperty>> table = new TableView<>(data);

    public void updateCell(final int row, final int col, final String value)
    {
        data.get(row).get(col).set(value);
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        TableColumn<List<StringProperty>, String> tc = new TableColumn<>("A");
        tc.setCellValueFactory(param ->  param.getValue().get(0));
        tc.setCellFactory(TextFieldTableCell.forTableColumn());

        tc.setOnEditCommit(event ->
        {
            final int col = event.getTablePosition().getColumn();
            event.getRowValue().get(col).set(event.getNewValue());
        });

        tc.setEditable(true);
        table.getColumns().add(tc);

        tc = new TableColumn<>("B");
        tc.setCellValueFactory(param ->  param.getValue().get(1));
        tc.setCellFactory(TextFieldTableCell.forTableColumn());
        tc.setEditable(true);
        table.getColumns().add(tc);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setEditable(true);

        final Scene scene = new Scene(table, 800, 600);
        stage.setScene(scene);
        stage.show();

        List<StringProperty> row = new ArrayList<>();
        row.add(new SimpleStringProperty("One"));
        row.add(new SimpleStringProperty("Another"));
        data.add(row);

        row = new ArrayList<>();
        row.add(new SimpleStringProperty("Two"));
        row.add(new SimpleStringProperty("Something"));
        data.add(row);

        final Thread change_cell = new Thread(() ->
        {
            while (true)
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                Platform.runLater(() ->  updateCell(1, 0, LocalDateTime.now().toString().replace('T', ' ')));
            }
        });
        change_cell.setDaemon(true);
        change_cell.start();
    }

    public static void main(String[] args)
    {
        launch(StringTableDemo.class, args);
    }
}
