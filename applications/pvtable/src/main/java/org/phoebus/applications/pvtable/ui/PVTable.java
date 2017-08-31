/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.diirt.vtype.VType;
import org.phoebus.applications.pvtable.PVTableApplication;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.VTypeHelper;
import org.phoebus.ui.dialog.NumericInputDialog;

import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.converter.DefaultStringConverter;

/** PV Table and its toolbar
 *  @author Kay Kasemir
 */
public class PVTable extends BorderPane
{
    private static final String comment_style = "-fx-text-fill: blue;";

    private static final String new_item_style = "-fx-text-fill: gray;";

    private static final String changed_style = "-fx-background-color: -fx-table-cell-border-color, cyan;-fx-background-insets: 0, 0 0 1 0;";

    private static final String[] alarm_styles = new String[]
    {
        null,
        "-fx-text-fill: orange;",
        "-fx-text-fill: red;",
        "-fx-text-fill: purple;",
        "-fx-text-fill: pink;",
    };

    private final PVTableModel model;
    private final TableView<TableItemProxy> table;

    /** Flag to disable updates while editing */
    private boolean editing = false;

    /** Table cell for boolean column, empty for 'comment' items */
    private static class BooleanTableCell extends TableCell<TableItemProxy, Boolean>
    {
        private final CheckBox checkbox = new CheckBox();

        @Override
        protected void updateItem(final Boolean selected, final boolean empty)
        {
            super.updateItem(selected, empty);
            final int row = getIndex();
            final List<TableItemProxy> items = getTableView().getItems();
            final TableItemProxy item;
            if (empty  ||  row < 0  ||  row >= items.size())
                item = null;
            else
            {
                final TableItemProxy check = items.get(row);
                if (check == TableItemProxy.NEW_ITEM  ||  check.item.isComment())
                    item = null;
                else
                    item = check;
            }
            if (item == null)
                setGraphic(null);
            else
            {
                setGraphic(checkbox);
                checkbox.setSelected(selected);

                BooleanProperty cell_property = (BooleanProperty) getTableColumn().getCellObservableValue(row);
                checkbox.setOnAction(event -> cell_property.set(checkbox.isSelected()));
            }
        }
    }

    /** Table cell for 'name' column, colors comments */
    private static class PVNameTableCell extends TextFieldTableCell<TableItemProxy, String>
    {
        public PVNameTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String name, final boolean empty)
        {
            super.updateItem(name, empty);
            if (empty)
                setText(null);
            else
            {
                final TableItemProxy item = getTableView().getItems().get(getIndex());
                if (item == TableItemProxy.NEW_ITEM)
                {
                    setStyle(new_item_style);
                    setText(Messages.EnterNewPV);
                }
                else if (item.item.isComment())
                {
                    setStyle(comment_style);
                    setText(item.item.getComment());
                }
                else
                {
                    setStyle(null);
                    setText(name);
                }
            }
        }
    }

    /** Table cell for 'alarm' column, colors alarm states */
    private static class AlarmTableCell extends TextFieldTableCell<TableItemProxy, String>
    {
        public AlarmTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String alarm_text, final boolean empty)
        {
            super.updateItem(alarm_text, empty);
            if (empty)
                return;
            final TableItemProxy proxy = getTableView().getItems().get(getIndex());
            final VType value = proxy.item.getValue();
            if (value != null)
                setStyle(alarm_styles[VTypeHelper.getSeverity(value).ordinal()]);
        }
    }

    /** Table cell for 'value' column, enables/disables and indicates changed value */
    private static class ValueTableCell extends TextFieldTableCell<TableItemProxy, String>
    {
        public ValueTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String value, final boolean empty)
        {
            super.updateItem(value, empty);
            if (!empty)
            {
                final TableItemProxy proxy = getTableView().getItems().get(getIndex());
                setEditable(proxy.item.isWritable());
                if (proxy.item.isChanged())
                    setStyle(changed_style);
                else
                    setStyle(null);
            }
        }
    }

    /** Listener to model changes */
    private final PVTableModelListener model_listener = new PVTableModelListener()
    {
        @Override
        public void tableItemSelectionChanged(final PVTableItem item)
        {
            tableItemChanged(item);
        }

        @Override
        public void tableItemChanged(final PVTableItem item)
        {
            // In principle, just suppressing updates to the single row
            // that's being edited should be sufficient,
            // but JavaFX seems to update arbitrary rows beyond the requested
            // one, so suppress all updates while editing
            if (editing)
                return;

            // XXX Replace linear lookup of row w/ member variable in PVTableItem?
            final int row = model.getItems().indexOf(item);

            // System.out.println(item + " changed in row " + row + " on " + Thread.currentThread().getName());
            table.getItems().get(row).update(item);
        }

        @Override
        public void tableItemsChanged()
        {
            System.out.println("Table items changed");
            setItemsFromModel();
        }

        @Override
        public void modelChanged()
        {
            System.out.println("Model changed");
            setItemsFromModel();
        }
    };


    public PVTable(final PVTableModel model)
    {
        this.model = model;
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        // Select complete rows
        table.getSelectionModel().setCellSelectionEnabled(false);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        createTableColumns();

        table.setEditable(true);

        final Node toolbar = createToolbar();
        setMargin(toolbar, new Insets(5, 5, 0, 5));
        setMargin(table, new Insets(5));
        setTop(toolbar);
        setCenter(table);

        setItemsFromModel();

        createContextMenu();

        model.addListener(model_listener);
    }

    private void setItemsFromModel()
    {
        table.setItems(FXCollections.emptyObservableList());
        final ObservableList<TableItemProxy> items = FXCollections.observableArrayList();
        for (PVTableItem item : model.getItems())
            items.add(new TableItemProxy(item));
        items.add(TableItemProxy.NEW_ITEM);
        table.setItems(items);
        table.refresh();
    }

    private Node createToolbar()
    {
        return new HBox(5,
            createButton("checked.gif", Messages.CheckAll_TT, event ->
            {
                for (PVTableItem item : model.getItems())
                    item.setSelected(true);
            }),
            createButton("unchecked.gif", Messages.UncheckAll_TT, event ->
            {
                for (PVTableItem item : model.getItems())
                    item.setSelected(false);
            }),

            new Separator(),

            createButton("snapshot.png", Messages.Snapshot_TT, event -> model.save()),
            createButton("restore.png", Messages.Restore_TT, event -> model.restore()),

            new Separator()
                );
    }

    private Button createButton(final String icon, final String tooltip, final EventHandler<ActionEvent> handler)
    {
        final Button button = new Button();
        button.setGraphic(new ImageView(PVTableApplication.getIcon(icon)));
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(handler);
        return button;
    }

    private void createContextMenu()
    {
        final MenuItem save = new MenuItem(Messages.SnapshotSelection, new ImageView(PVTableApplication.getIcon("snapshot.png")));
        save.setOnAction(event ->
        {
            model.save(table.getSelectionModel()
                            .getSelectedItems()
                            .stream()
                            .map(proxy -> proxy.item)
                            .collect(Collectors.toList()));
        });

        final MenuItem restore = new MenuItem(Messages.RestoreSelection, new ImageView(PVTableApplication.getIcon("restore.png")));
        restore.setOnAction(event ->
        {
            model.restore(table.getSelectionModel()
                               .getSelectedItems()
                               .stream()
                               .map(proxy -> proxy.item)
                               .collect(Collectors.toList()));
        });

        final MenuItem add_row = new MenuItem(Messages.Insert, new ImageView(PVTableApplication.getIcon("add.gif")));
        add_row.setOnAction(event ->
        {
            // Copy selection as it will change when we add to the model
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty())
                return;
            final int last = table.getSelectionModel().getSelectedIndex();
            for (TableItemProxy proxy : selected)
                model.addItemAbove(proxy.item, "# ");
            table.getSelectionModel().select(last);
        });

        final MenuItem remove_row = new MenuItem(Messages.Delete, new ImageView(PVTableApplication.getIcon("delete.gif")));
        remove_row.setOnAction(event ->
        {
            // Copy selection as it will change
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            for (TableItemProxy proxy : selected)
                model.removeItem(proxy.item);
        });

        final MenuItem tolerance = new MenuItem(Messages.Tolerance, new ImageView(PVTableApplication.getIcon("pvtable.png")));
        tolerance.setOnAction(event ->
        {
            final TableItemProxy proxy = table.getSelectionModel().getSelectedItem();
            if (proxy == null  ||   proxy.item.isComment())
                return;

            final NumericInputDialog dlg = new NumericInputDialog(Messages.Tolerance,
                    "Enter tolerance for " + proxy.item.getName(),
                    proxy.item.getTolerance(),
                    number -> number >= 0 ? null : "Enter a positive tolerance value");
            dlg.promptAndHandle(number -> proxy.item.setTolerance(number));
        });

        final MenuItem timeout = new MenuItem(Messages.Timeout, new ImageView(PVTableApplication.getIcon("timeout.png")));
        timeout.setOnAction(event ->
        {
            final NumericInputDialog dlg = new NumericInputDialog(Messages.Timeout,
                    "Enter the timeout in seconds used\n" +
                    "for all items that are restored\n" +
                    "with 'completion' (put-callback)",
                    model.getCompletionTimeout(),
                    number -> number > 0 ? null : "Enter a positive number of seconds");
            dlg.promptAndHandle(number -> model.setCompletionTimeout(number.longValue()));
        });

        final ContextMenu menu = new ContextMenu(save, restore, new SeparatorMenuItem(), add_row, remove_row, new SeparatorMenuItem(), tolerance, timeout);
        table.setContextMenu(menu);
    }

    private void createTableColumns()
    {
        // Selected column
        final TableColumn<TableItemProxy, Boolean> sel_col = new TableColumn<>(Messages.Selected);
        sel_col.setCellValueFactory(cell -> cell.getValue().selected);
        sel_col.setCellFactory(column -> new BooleanTableCell());
        table.getColumns().add(sel_col);

        // PV Name
        TableColumn<TableItemProxy, String> col = new TableColumn<>(Messages.PV);
        col.setPrefWidth(250);
        col.setCellValueFactory(cell_data_features -> cell_data_features.getValue().name);
        col.setCellFactory(column -> new PVNameTableCell());
        col.setOnEditCommit(event ->
        {
            final TableItemProxy proxy = event.getRowValue();
            if (proxy == TableItemProxy.NEW_ITEM)
                model.addItem(event.getNewValue());
            else
                proxy.item.updateName(event.getNewValue());
        });
        table.getColumns().add(col);

        // Description
        col = new TableColumn<>(Messages.Description);
        col.setCellValueFactory(cell -> cell.getValue().desc_value);
        table.getColumns().add(col);

        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell ->  cell.getValue().time);
        table.getColumns().add(col);

        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell -> cell.getValue().value);
        col.setCellFactory(column -> new ValueTableCell());
        col.setOnEditStart(event -> editing = true);
        col.setOnEditCommit(event ->
        {
            editing = false;
            event.getRowValue().item.setValue(event.getNewValue());
            // Since updates were suppressed, refresh table
            table.refresh();
        });
        col.setOnEditCancel(event ->
        {
            editing = false;
            // Since updates were suppressed, refresh table
            table.refresh();
        });
        table.getColumns().add(col);

        // Alarm
        col = new TableColumn<>(Messages.Alarm);
        col.setCellValueFactory(cell -> cell.getValue().alarm);
        col.setCellFactory(column -> new AlarmTableCell());
        table.getColumns().add(col);

        // Saved value
        col = new TableColumn<>(Messages.Saved);
        col.setCellValueFactory(cell -> cell.getValue().saved);
        table.getColumns().add(col);

        // Saved value's timestamp
        col = new TableColumn<>(Messages.Saved_Value_TimeStamp);
        col.setCellValueFactory(cell -> cell.getValue().time_saved);
        table.getColumns().add(col);

        // Completion checkbox
        final TableColumn<TableItemProxy, Boolean> compl_col = new TableColumn<>(Messages.Completion);
        compl_col.setCellValueFactory(cell -> cell.getValue().use_completion);
        compl_col.setCellFactory(column -> new BooleanTableCell());
        table.getColumns().add(compl_col);
    }
}
