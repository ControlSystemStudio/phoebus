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
import java.util.stream.Stream;

import org.diirt.vtype.VEnum;
import org.diirt.vtype.VType;
import org.phoebus.applications.pvtable.PVTableApplication;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.applications.pvtable.model.VTypeHelper;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.dialog.NumericInputDialog;
import org.phoebus.ui.dnd.DataFormats;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.converter.DefaultStringConverter;

/** PV Table and its toolbar
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTable extends BorderPane
{
    private static final String comment_style = "-fx-text-fill: blue;";
    private static final String new_item_style = "-fx-text-fill: gray;";
    private static final String changed_style = "-fx-background-color: -fx-table-cell-border-color, cyan;-fx-background-insets: 0, 0 0 1 0;";
    private static final String[] alarm_styles = new String[]
    {
        null,                      // NONE
        "-fx-text-fill: orange;",  // MINOR
        "-fx-text-fill: red;",     // MAJOR
        "-fx-text-fill: purple;",  // INVALID
        "-fx-text-fill: magenta;", // UNDEFINED
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
            if (proxy == TableItemProxy.NEW_ITEM)
                setStyle(null);
            else
            {
                final VType value = proxy.item.getValue();
                if (value != null)
                    setStyle(alarm_styles[VTypeHelper.getSeverity(value).ordinal()]);
            }
        }
    }

    /** Table cell for 'value' column, enables/disables and indicates changed value */
    private static class ValueTableCell extends TableCell<TableItemProxy, String>
    {

        public ValueTableCell()
        {
            getStyleClass().add("text-field-table-cell");
        }

        @Override
        public void startEdit()
        {
            super.startEdit();
            if (! isEditing())
                return;

            setText(null);

            final TableItemProxy proxy = getTableView().getItems().get(getIndex());
            final VType value = proxy.item.getValue();
            if (value instanceof VEnum)
            {
                // Use combo for Enum-valued data
                final VEnum enumerated = (VEnum) value;
                final ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll(enumerated.getLabels());
                combo.getSelectionModel().select(enumerated.getIndex());

                combo.setOnAction(event ->
                {
                    // Need to write String, using the enum index
                    commitEdit(Integer.toString(combo.getSelectionModel().getSelectedIndex()));
                    event.consume();
                });
                combo.setOnKeyReleased(event ->
                {
                    if (event.getCode() == KeyCode.ESCAPE)
                    {
                        cancelEdit();
                        event.consume();
                    }
                });
                setGraphic(combo);
                Platform.runLater(() -> combo.requestFocus());
                Platform.runLater(() -> combo.show());
            }
            else
            {
                final TextField text_field = new TextField(getItem());
                text_field.setOnAction(event ->
                {
                    commitEdit(text_field.getText());
                    event.consume();
                });
                text_field.setOnKeyReleased(event ->
                {
                    if (event.getCode() == KeyCode.ESCAPE)
                    {
                        cancelEdit();
                        event.consume();
                    }
                });
                setGraphic(text_field);
                text_field.selectAll();
                text_field.requestFocus();
            }
        }

        @Override
        public void cancelEdit()
        {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
        }

        @Override
        public void updateItem(final String value, final boolean empty)
        {
            super.updateItem(value, empty);
            if (empty)
                setText(null);
            else
            {
                setText(value);
                final TableItemProxy proxy = getTableView().getItems().get(getIndex());
                if (proxy == TableItemProxy.NEW_ITEM)
                {
                    setEditable(false);
                    setStyle(null);
                }
                else
                {
                    setEditable(proxy.item.isWritable());
                    if (proxy.item.isChanged())
                        setStyle(changed_style);
                    else
                        setStyle(null);
                }
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
            setItemsFromModel();
        }

        @Override
        public void modelChanged()
        {
            setItemsFromModel();
        }
    };


    public PVTable(final PVTableModel model)
    {
        this.model = model;
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        // Select complete rows
        final TableViewSelectionModel<TableItemProxy> table_sel = table.getSelectionModel();
        table_sel.setCellSelectionEnabled(false);
        table_sel.setSelectionMode(SelectionMode.MULTIPLE);

        // Publish selected PV
        final InvalidationListener sel_changed = change ->
        {
            final List<ProcessVariable> pvs = getSelectedItems()
                 .map(proxy -> new ProcessVariable(proxy.item.getName()))
                 .collect(Collectors.toList());
            SelectionService.getInstance().setSelection("PV Table", pvs);
        };
        table_sel.getSelectedItems().addListener(sel_changed);

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

        hookDragAndDrop();
    }

    /** @return Stream of selected items (only PVs, no comment etc.) */
    private Stream<TableItemProxy> getSelectedItems()
    {
        return table.getSelectionModel()
                    .getSelectedItems()
                    .stream()
                    .filter(proxy -> proxy != TableItemProxy.NEW_ITEM  &&  ! proxy.item.isComment());
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
        final MenuItem info = createMenuItem("Info", "pvtable.png", event ->
        {
            final Alert dialog = new Alert(AlertType.INFORMATION);
            dialog.setTitle("PV Information");
            dialog.setHeaderText("Details of PVs in marked table rows");
            dialog.setContentText(getSelectedItems().map(proxy -> proxy.item.toString())
                                                    .collect(Collectors.joining("\n")));
            dialog.getDialogPane().setPrefWidth(800.0);
            dialog.setResizable(true);
            dialog.showAndWait();
        });

        final MenuItem save = createMenuItem(Messages.SnapshotSelection, "snapshot.png", event ->
        {
            model.save(getSelectedItems().map(proxy -> proxy.item)
                                         .collect(Collectors.toList()));
        });

        final MenuItem restore = createMenuItem(Messages.RestoreSelection, "restore.png", event ->
        {
            model.restore(getSelectedItems().map(proxy -> proxy.item)
                                            .collect(Collectors.toList()));
        });

        final MenuItem add_row = createMenuItem(Messages.Insert, "add.gif", event ->
        {
            // Copy selection as it will change when we add to the model
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty())
                return;
            final int last = table.getSelectionModel().getSelectedIndex();
            // addItemAbove() handles proxy.item == null for the NEW_ITEM
            for (TableItemProxy proxy : selected)
                model.addItemAbove(proxy.item, "# ");
            table.getSelectionModel().select(last);
        });

        final MenuItem remove_row = createMenuItem(Messages.Delete, "delete.gif", event ->
        {
            // Copy selection as it will change
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            // Don't remove the 'last' item
            for (TableItemProxy proxy : selected)
                if (proxy != TableItemProxy.NEW_ITEM)
                    model.removeItem(proxy.item);
        });

        final MenuItem tolerance = createMenuItem(Messages.Tolerance, "pvtable.png", event ->
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

        final MenuItem timeout = createMenuItem(Messages.Timeout, "timeout.png", event ->
        {
            final NumericInputDialog dlg = new NumericInputDialog(Messages.Timeout,
                    "Enter the timeout in seconds used\n" +
                    "for all items that are restored\n" +
                    "with 'completion' (put-callback)",
                    model.getCompletionTimeout(),
                    number -> number > 0 ? null : "Enter a positive number of seconds");
            dlg.promptAndHandle(number -> model.setCompletionTimeout(number.longValue()));
        });

        final ContextMenu menu = new ContextMenu();

        table.setOnContextMenuRequested(event ->
        {
            // Start with fixed entries
            menu.getItems().clear();
            menu.getItems().addAll(info, new SeparatorMenuItem(),
                    save, restore, new SeparatorMenuItem(),
                    add_row, remove_row, new SeparatorMenuItem(),
                    tolerance, timeout, new SeparatorMenuItem());
            // Add PV entries
            ContextMenuHelper.addSupportedEntries(table, menu);
        });
        table.setContextMenu(menu);
    }

    private MenuItem createMenuItem(final String label, final String icon,
                                    final EventHandler<ActionEvent> handler)
    {
        final MenuItem item = new MenuItem(label,
                                           new ImageView(PVTableApplication.getIcon(icon)));
        item.setOnAction(handler);
        return item;
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
            {
                proxy.item.updateName(event.getNewValue());
                proxy.update(proxy.item);
                // Content of model changed.
                // Triggers full table update.
                model.fireModelChange();
            }
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

    /** Set to currently dragged items to allow 'drop' to move them instead of
     *  adding duplicates.
     */
    private List<TableItemProxy> dragged_items = null;

    private void hookDragAndDrop()
    {
        // Drag PV names as string. Also locally remember dragged_items
        table.setOnDragDetected(event ->
        {
            final Dragboard db = table.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            final ClipboardContent content = new ClipboardContent();

            final List<ProcessVariable> pvs = new ArrayList<>();
            dragged_items = new ArrayList<>();
            for (TableItemProxy proxy : table.getSelectionModel().getSelectedItems())
                if (proxy != TableItemProxy.NEW_ITEM)
                {
                    dragged_items.add(proxy);
                    if (! proxy.item.isComment())
                        pvs.add(new ProcessVariable(proxy.item.getName()));
                }

            final StringBuilder buf = new StringBuilder();
            for (TableItemProxy proxy : dragged_items)
            {
                if (buf.length() > 0)
                    buf.append(" ");
                buf.append(proxy.name.get());
            }
            content.putString(buf.toString());
            content.put(DataFormats.ProcessVariables, pvs);
            db.setContent(content);
            event.consume();
        });

        // Clear dragged items
        table.setOnDragDone(event ->
        {
            dragged_items = null;
        });

        table.setOnDragOver(event ->
        {
            if (event.getDragboard().hasString())
                event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });

        table.setOnDragDropped(event ->
        {
            // Locate cell on which we dropped
            Node node = event.getPickResult().getIntersectedNode();
            while (node != null  &&  !(node instanceof TableCell))
                node = node.getParent();
            final TableCell<?,?> cell = (TableCell<?,?>)node;

            // Table item before which to drop?
            PVTableItem existing = null;
            if (cell != null)
            {
                final int row = cell.getIndex();
                if (row < model.getItems().size())
                    existing = model.getItems().get(row);
            }

            if (dragged_items != null)
            {   // Move items within this table
                for (TableItemProxy proxy : dragged_items)
                {
                    model.removeItem(proxy.item);
                    model.addItemAbove(existing, proxy.item);
                }
            }
            else
            {
                final Dragboard db = event.getDragboard();
                if (db.hasContent(DataFormats.ProcessVariables))
                {   // Add PVs
                    @SuppressWarnings("unchecked")
                    final List<ProcessVariable> pvs = (List<ProcessVariable>) db.getContent(DataFormats.ProcessVariables);
                    for (ProcessVariable pv : pvs)
                        model.addItemAbove(existing, pv.getName());
                }
                else if (db.hasString())
                {   // Add new items from string
                    addPVsFromString(existing, db.getString());
                    event.setDropCompleted(true);
                }
            }
            event.consume();
        });
    }

    private void addPVsFromString(final PVTableItem existing, final String pv_text)
    {
        final String[] pvs = pv_text.split("[ \\t\\n\\r,]+");
        for (String pv : pvs)
            if (! pv.isEmpty())
                model.addItemAbove(existing, pv);
    }
}
