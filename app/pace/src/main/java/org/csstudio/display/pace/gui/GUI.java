/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;
import static org.csstudio.display.pace.PACEApp.logger;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.pace.Messages;
import org.csstudio.display.pace.model.Cell;
import org.csstudio.display.pace.model.Column;
import org.csstudio.display.pace.model.Instance;
import org.csstudio.display.pace.model.Model;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

/** GUI for PACE {@link Model}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class GUI extends BorderPane
{
    private Model model = null;
    private TableView<Instance> table;
    private final Consumer<Cell> model_listener = this::handleModelChanges;

    public GUI()
    {
        setTop(new Label("Optional message..."));
        setCenter(new Label("Loading ..."));
    }

    /** Set model
     *  May be called off the UI thread, will wait until UI thread update completes
     *  @param model
     */
    public void setModel(final Model model)
    {
        if (Platform.isFxApplicationThread())
            doSetModel(model);
        else
        {
            final CountDownLatch done = new CountDownLatch(1);
            Platform.runLater(() ->
            {
                doSetModel(model);
                done.countDown();
            });
            try
            {
                done.await();
            }
            catch (InterruptedException ex)
            {
                logger.log(Level.WARNING, "Cannot set model", ex);
            }
        }
    }

    private void doSetModel(final Model model)
    {
        if (this.model != null)
            this.model.removeListener(model_listener);
        this.model = model;

        table = createTable();
        setCenter(table);

        this.model.addListener(model_listener);

        createContextMenu();
    }

    private TableView<Instance> createTable()
    {
        final TableView<Instance> table = new TableView<>(FXCollections.observableList(model.getInstances()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.getSelectionModel().setCellSelectionEnabled(true);

        table.setEditable(true);

        TableColumn<Instance, String> col = new TableColumn<>(Messages.SystemColumn);
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        table.getColumns().add(col);

        int col_index = 0;
        for (Column column : model.getColumns())
        {
            final int the_col_index = col_index;
            col = new TableColumn<>(column.getName());
            col.setCellFactory(info -> new PACETableCell());
            col.setCellValueFactory(cell -> cell.getValue().getCell(the_col_index).getObservable());
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

    private void handleModelChanges(final Cell cell)
    {
        // System.out.println("Update " + cell);
        Platform.runLater( () -> cell.getObservable());
    }

    private void createContextMenu()
    {
        final MenuItem restore = new MenuItem(Messages.RestoreCell);
        final MenuItem setvalue = new MenuItem(Messages.SetValue);
        final ContextMenu menu = new ContextMenu(restore, setvalue);
        menu.setOnShowing(event ->
        {
            @SuppressWarnings("rawtypes")
            final ObservableList<TablePosition> selection = table.getSelectionModel().getSelectedCells();
            if (selection.isEmpty())
                setvalue.setDisable(true);
            else
                setvalue.setDisable(false);

            // TODO Clear and re-fill the menu
            // ContextMenuHelper.addSupportedEntries(table, menu);
        });

        table.setContextMenu(menu);
    }
}
