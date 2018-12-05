/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.display.pace.gui;
import static org.csstudio.display.pace.PACEApp.logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.display.pace.Messages;
import org.csstudio.display.pace.PACEApp;
import org.csstudio.display.pace.model.Cell;
import org.csstudio.display.pace.model.Column;
import org.csstudio.display.pace.model.Instance;
import org.csstudio.display.pace.model.Model;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
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
    private final Consumer<Boolean> dirty_state_handler;
    private final AtomicBoolean was_dirty = new AtomicBoolean();
    private Model model = null;
    private TableView<Instance> table;
    private final Consumer<Cell> model_listener = this::handleModelChanges;

    public GUI(final Consumer<Boolean> dirty_state_handler)
    {
        this.dirty_state_handler = dirty_state_handler;
        setMessage("Loading...");
        setCenter(new Label(PACEApp.DISPLAY_NAME));
    }

    /** @param message  */
    public void setMessage(final String message)
    {
        Platform.runLater(() ->
        {
            final Label label;
            if (message == null  ||  message.isEmpty())
                label = null;
            else
                label = new Label(message);
            setTop(label);
        });
    }

    /** May be called off the UI thread, will wait until UI thread update completes
     *  @param model Model to represent
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
        Platform.runLater( () ->
        {
            cell.getObservable();

            final boolean is_dirty = model.isEdited();
            if (was_dirty.getAndSet(is_dirty) != is_dirty)
            {
                setMessage(is_dirty ? Messages.FileChanged : null);
                dirty_state_handler.accept(is_dirty);
            }
        });
    }

    private void createContextMenu()
    {
        // Create menu with dummy entry (otherwise it won't show up)
        final ContextMenu menu = new ContextMenu(new MenuItem());

        // Update menu based on selection
        menu.setOnShowing(event ->
        {
            // Get selected Cells and their PV names
            final List<Cell> cells = new ArrayList<>();
            final List<ProcessVariable> pvnames = new ArrayList<>();
            final List<Instance> rows = table.getItems();
            for (TablePosition<?, ?> sel : table.getSelectionModel().getSelectedCells())
            {
                final Cell cell = rows.get(sel.getRow()).getCell(sel.getColumn()-1);
                cells.add(cell);
                pvnames.add(new ProcessVariable(cell.getName()));
            }

            // Update menu
            final ObservableList<MenuItem> items = menu.getItems();
            items.clear();
            items.add(new RestoreCellValues(cells));
            items.add(new SetCellValues(table, cells));

            // Add PV name entries
            if (pvnames.size() > 0)
            {
                items.add(new SeparatorMenuItem());
                SelectionService.getInstance().setSelection("AlarmUI", pvnames);
                ContextMenuHelper.addSupportedEntries(table, menu);
            }
        });

        table.setContextMenu(menu);
    }
}
