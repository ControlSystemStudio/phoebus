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
import java.util.function.BiConsumer;

import org.diirt.vtype.VType;
import org.phoebus.applications.pvtable.PVTableApplication;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.SavedValue;
import org.phoebus.applications.pvtable.model.TimestampHelper;
import org.phoebus.applications.pvtable.model.VTypeHelper;
import org.phoebus.ui.dialog.NumericInputDialog;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
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
    /** 'Magic' table item added to the end of the actual model to allow adding
     *  entries. Setting the name of this item is handled as adding a new item
     *  for that name.
     */
    final public static PVTableItem NEW_ITEM = new PVTableItem("", 0.0, null, null, null);

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
    private final TableView<PVTableItem> table;

    /** Flag to disable updates while editing */
    private boolean editing = false;

    /** Table cell for column with boolean value, selects/de-selects */
    private static class BooleanTableCell extends TableCell<PVTableItem, Boolean>
    {
        private final CheckBox checkbox = new CheckBox();
        private final BiConsumer<PVTableItem, Boolean> update_item;

        /** @param update_item Will be called when user clicks check box */
        public BooleanTableCell(final BiConsumer<PVTableItem, Boolean> update_item)
        {
            checkbox.setFocusTraversable(false);
            this.update_item = update_item;
        }

        @Override
        protected void updateItem(Boolean selected, boolean empty)
        {
            super.updateItem(selected, empty);
            final int row = getIndex();
            final ObservableList<PVTableItem> items = getTableView().getItems();
            final PVTableItem item;
            if (empty  ||  row < 0  ||  row >= items.size())
                item = null;
            else
            {
                final PVTableItem check = items.get(row);
                if (check == NEW_ITEM  ||  check.isComment())
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
                checkbox.setOnAction(event -> update_item.accept(item, checkbox.isSelected()));
            }
        }
    }

    /** Table cell for 'name' column, colors comments */
    private static class PVNameTableCell extends TextFieldTableCell<PVTableItem, String>
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
                final PVTableItem item = getTableView().getItems().get(getIndex());
                if (item == NEW_ITEM)
                {
                    setStyle(new_item_style);
                    setText(Messages.EnterNewPV);
                }
                else if (item.isComment())
                {
                    setStyle(comment_style);
                    setText(item.getComment());
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
    private static class AlarmTableCell extends TextFieldTableCell<PVTableItem, String>
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
                setText(null);
            else
            {
                final PVTableItem item = getTableView().getItems().get(getIndex());
                final VType value = item.getValue();
                if (value == null)
                    setText(null);
                else
                {
                    setText(alarm_text);
                    setStyle(alarm_styles[VTypeHelper.getSeverity(value).ordinal()]);
                }
            }
        }
    }

    /** Table cell for 'value' column, enables/disables */
    private static class ValueTableCell extends TextFieldTableCell<PVTableItem, String>
    {
        public ValueTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void updateItem(final String value, final boolean empty)
        {
            super.updateItem(value, empty);
            if (empty)
                setText(null);
            else
            {
                final PVTableItem item = getTableView().getItems().get(getIndex());
                setEditable(item.isWritable());
                setText(value);
                if (item.isChanged())
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
        public void tableItemSelectionChanged(PVTableItem item)
        {
            tableItemChanged(item);
        }

        @Override
        public void tableItemChanged(PVTableItem item)
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
            table.getItems().set(row, item);
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
        final ObservableList<PVTableItem> items = FXCollections.observableArrayList(model.getItems());
        items.add(NEW_ITEM);
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
        final MenuItem add_row = new MenuItem(Messages.Insert, new ImageView(PVTableApplication.getIcon("add.gif")));
        add_row.setOnAction(event ->
        {
            // Copy selection as it will change when we add to the model
            final List<PVTableItem> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            PVTableItem last = null;
            for (PVTableItem item : selected)
                last = model.addItemAbove(item, "# ");
            if (last != null)
                table.getSelectionModel().select(last);
        });

        final MenuItem remove_row = new MenuItem(Messages.Delete, new ImageView(PVTableApplication.getIcon("delete.gif")));
        remove_row.setOnAction(event ->
        {
            // Copy selection as it will change
            final List<PVTableItem> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            for (PVTableItem item : selected)
                model.removeItem(item);
        });

        final MenuItem tolerance = new MenuItem(Messages.Tolerance, new ImageView(PVTableApplication.getIcon("pvtable.png")));
        tolerance.setOnAction(event ->
        {
            final PVTableItem item = table.getSelectionModel().getSelectedItem();
            if (item == null  ||   item.isComment())
                return;

            final NumericInputDialog dlg = new NumericInputDialog(Messages.Tolerance,
                    "Enter tolerance for " + item.getName(),
                    item.getTolerance(),
                    number -> number >= 0 ? null : "Enter a positive tolerance value");
            dlg.promptAndHandle(number -> item.setTolerance(number));
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

        final ContextMenu menu = new ContextMenu(add_row, remove_row, new SeparatorMenuItem(), tolerance, timeout);
        table.setContextMenu(menu);
    }

    private void createTableColumns()
    {
        // Selected column
        final TableColumn<PVTableItem, Boolean> sel_col = new TableColumn<>(Messages.Selected);
        sel_col.setCellValueFactory(cell_data_features -> new SimpleBooleanProperty(cell_data_features.getValue().isSelected()));
        sel_col.setCellFactory(column -> new BooleanTableCell( (item, selected) ->  item.setSelected(selected)));
        table.getColumns().add(sel_col);

        // PV Name
        TableColumn<PVTableItem, String> col = new TableColumn<>(Messages.PV);
        col.setPrefWidth(250);
        col.setCellValueFactory(cell_data_features -> new SimpleStringProperty(cell_data_features.getValue().getName()));
        col.setCellFactory(column -> new PVNameTableCell());
        col.setOnEditStart(event -> editing = true);
        col.setOnEditCommit(event ->
        {
            editing = false;
            final PVTableItem item = event.getRowValue();
            if (item == NEW_ITEM)
                model.addItem(event.getNewValue());
            else
                item.updateName(event.getNewValue());
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

        // Description
        col = new TableColumn<>(Messages.Description);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            if (item.isComment())
                return new SimpleStringProperty();
            return new SimpleStringProperty(item.getDescription());
        });
        table.getColumns().add(col);

        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(TimestampHelper.format(VTypeHelper.getTimestamp(value)));
        });
        table.getColumns().add(col);

        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(VTypeHelper.toString(value));
        });
        col.setCellFactory(column -> new ValueTableCell());
        col.setOnEditStart(event -> editing = true);
        col.setOnEditCommit(event ->
        {
            editing = false;
            final PVTableItem item = event.getRowValue();
            item.setValue(event.getNewValue());
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
        col.setCellValueFactory(cell ->
        {
            final VType value = cell.getValue().getValue();
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(VTypeHelper.formatAlarm(value));
        });
        col.setCellFactory(column -> new AlarmTableCell());
        table.getColumns().add(col);

        // Saved value
        col = new TableColumn<>(Messages.Saved);
        col.setCellValueFactory(cell ->
        {
            final SavedValue value = cell.getValue().getSavedValue().orElse(null);
            if (value == null)
                return new SimpleStringProperty();
            return new SimpleStringProperty(value.toString());
        });
        table.getColumns().add(col);

        // Saved value's timestamp
        col = new TableColumn<>(Messages.Saved_Value_TimeStamp);
        col.setCellValueFactory(cell ->
        {
            final PVTableItem item = cell.getValue();
            return new SimpleStringProperty(item.getTime_saved());
        });
        table.getColumns().add(col);

        // Completion checkbox
        final TableColumn<PVTableItem, Boolean> compl_col = new TableColumn<>(Messages.Completion);
        compl_col.setCellValueFactory(cell_data_features -> new SimpleBooleanProperty(cell_data_features.getValue().isUsingCompletion()));
        compl_col.setCellFactory(column -> new BooleanTableCell( (item, completion) ->  item.setUseCompletion(completion)));
        table.getColumns().add(compl_col);
    }
}
