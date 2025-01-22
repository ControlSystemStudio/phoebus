/*******************************************************************************
 * Copyright (c) 2017-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.applications.pvtable.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.epics.vtype.VBoolean;
import org.epics.vtype.VType;
import org.phoebus.applications.pvtable.PVTableApplication;
import org.phoebus.applications.pvtable.Settings;
import org.phoebus.applications.pvtable.model.PVTableItem;
import org.phoebus.applications.pvtable.model.PVTableModel;
import org.phoebus.applications.pvtable.model.PVTableModelListener;
import org.phoebus.core.types.ProcessVariable;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.framework.selection.Selection;
import org.phoebus.framework.selection.SelectionService;
import org.phoebus.security.authorization.AuthorizationService;
import org.phoebus.ui.application.ContextMenuHelper;
import org.phoebus.ui.application.ContextMenuService;
import org.phoebus.ui.application.SaveSnapshotAction;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.dialog.NumericInputDialog;
import org.phoebus.ui.dnd.DataFormats;
import org.phoebus.ui.javafx.FocusUtil;
import org.phoebus.ui.javafx.PrintAction;
import org.phoebus.ui.javafx.Screenshot;
import org.phoebus.ui.javafx.ToolbarHelper;
import org.phoebus.ui.pv.SeverityColors;
import org.phoebus.ui.selection.AppSelection;
import org.phoebus.ui.spi.ContextMenuEntry;
import org.phoebus.util.text.CompareNatural;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.converter.DefaultStringConverter;


/** PV Table and its toolbar
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTable extends VBox
{
    private static final String comment_style = "-fx-text-fill: blue;";
    private static final String new_item_style = "-fx-text-fill: gray;";
    private static final String changed_style = "-fx-background-color: -fx-table-cell-border-color, cyan;-fx-background-insets: 0, 0 0 1 0;";
    private static final String SPLIT_PV = "[ \\t\\n\\r,]+";

    /** When sorting, keep the 'NEW_ITEM' row at the bottom **/
    private static final Comparator<TableItemProxy> SORT_NEW_ITEM_LAST = (a, b) ->
    {
        if (a == TableItemProxy.NEW_ITEM)
            return 1;
        else if (b == TableItemProxy.NEW_ITEM)
            return -1;
        return 0;
    };

    /** Model of all PV table items */
    private final PVTableModel model;

    /** TableItemProxy rows for table view, with callback to trigger updates
     *  not only when item is added/removed, but also when (selected)
     *  properties of the item (== column values) change
     */
    private final ObservableList<TableItemProxy> rows = FXCollections.observableArrayList(TableItemProxy.CHANGING_PROPERTIES);
    /** Sorted view of the rows.
     *  Order of 'rows' is preserved, but comparator of this list changes to sort.
     */
    private final SortedList<TableItemProxy> sorted = rows.sorted();
    private final TableView<TableItemProxy> table = new TableView<>(sorted);

    private TableColumn<TableItemProxy, String>  saved_value_col;
    private TableColumn<TableItemProxy, String>  saved_time_col;
    private TableColumn<TableItemProxy, Boolean> completion_col;

    private ToolBar toolbar;
    private Button  snapshot_button;
    private Button  restore_button;

    private boolean saveRestoreDisabled = false;

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
                if (check == TableItemProxy.NEW_ITEM  ||  check.getItem().isComment())
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
    
    private static class DescriptionTableCell extends TableCell<TableItemProxy, String>
    {
        @Override
        protected void updateItem(final String item, final boolean empty)
        {
            super.updateItem(item, empty);
            setText(item);
            final int row = getIndex();
            final List<TableItemProxy> items = getTableView().getItems();
            if (! empty  &&  row >= 0  &&  row < items.size()) {
                final TableItemProxy itemCell = items.get(row);
                if(itemCell != null && itemCell.getItem() != null) {
                    setTooltip(new Tooltip(itemCell.getItem().getDescriptionName()));
                }
            }
        }
    }

    /** Table cell for 'name' column, colors comments */
    private static class PVNameTableCell extends TextFieldTableCell<TableItemProxy, String>
    {
        private TextInputControl textField;
        private static ContextMenu contextMenu;
        
        public PVNameTableCell()
        {
            super(new DefaultStringConverter());
        }

        @Override
        public void startEdit()
        {
            super.startEdit();
            final int index = getIndex();
            boolean newPv = index == getTableView().getItems().size() - 1;
            if(newPv) {
                textField = new TextArea();
                textField.setMaxHeight(100);
                if(contextMenu == null) {
                    MenuItem addPVMenu = new MenuItem(Messages.AddPVList);
                    addPVMenu.setOnAction(event -> commitEdit(textField.getText()));
                    contextMenu = new ContextMenu(addPVMenu);
                }
                textField.setContextMenu(contextMenu);
            }
            else {
                textField = new TextField();
                ((TextField)textField).setOnAction(event -> commitEdit(textField.getText()));
            }
            PVAutocompleteMenu.INSTANCE.attachField(textField);
            showCurrentValue();
        }
      

        private void showCurrentValue()
        {
            final ObservableList<TableItemProxy> items = getTableView().getItems();
            final int index = getIndex();
            if (index < 0  ||  index >= items.size())
            {
                setText(null);
                setGraphic(null);
                return;
            }
            final TableItemProxy item = items.get(index);
            if (isEditing())
            {
                if (item == TableItemProxy.NEW_ITEM)
                    textField.setText("");
                else
                    textField.setText(item.getItem().getName());
                setText(null);
                setGraphic(textField);
            }
            else
            {
                setGraphic(null);
                if (isEmpty())
                    setText(null);
                else
                {
                    if (item == TableItemProxy.NEW_ITEM)
                    {
                        setStyle(new_item_style);
                        setText(Messages.EnterNewPV);
                    }
                    else if (item.getItem().isComment())
                    {
                        setStyle(comment_style);
                        setText(item.getItem().getComment());
                    }
                    else
                    {
                        setStyle(null);
                        setText(item.getItem().getName());
                    }
                }
            }
        }

        @Override
        public void updateItem(final String name, final boolean empty)
        {
            super.updateItem(name, empty);
            showCurrentValue();
        }

        @Override
        public void commitEdit(final String newValue)
        {
            textField = null;
            super.commitEdit(newValue);
        }

        @Override
        public void cancelEdit()
        {
            textField = null;
            super.cancelEdit();
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
                setTextFill(Color.BLACK);
            else
            {
                final VType value = proxy.getItem().getValue();
                if (value != null)
                    setTextFill(SeverityColors.getTextColor(VTypeHelper.getSeverity(value)));
            }
        }
    }

    /** Table cell for 'value' column, enables/disables and indicates changed value */
    private static class ValueTableCell extends TableCell<TableItemProxy, String>
    {
        private final PVTableModel model;

        public ValueTableCell(PVTableModel model)
        {
            this.model = model;
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
            
            String[] valueOptions = proxy.getItem().getValueOptions();
            
            boolean isBoolean = proxy.getItem().getValue() instanceof VBoolean;
            int index = -1;
            if(isBoolean) {
                if (valueOptions == null || valueOptions.length == 0 ) {
                    //Use a Combo box to write boolean value without ONAM or ZNAM
                    valueOptions = new String[] {"false", "true"};
                }
                index = ((VBoolean)proxy.getItem().getValue()).getValue() ? 1 : 0;
            }
        
            if (valueOptions != null && valueOptions.length > 0)
            {
                final ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll(valueOptions);
                index = !isBoolean ? proxy.getItem().getIndex() : index;
                if(index >=0) {
                    combo.getSelectionModel().select(index);
                }

                if(isBoolean) {
                    combo.setOnAction(event ->
                    {
                        // Need to write boolean, using the enum index
                        commitEdit(Boolean.toString(combo.getSelectionModel().getSelectedIndex() == 1));
                        event.consume();
                    });
                }
                else {
                    combo.setOnAction(event ->
                    {
                        // Need to write String, using the enum index
                        commitEdit(Integer.toString(combo.getSelectionModel().getSelectedIndex()));
                        event.consume();
                    });
                }
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
        public void commitEdit(final String newValue)
        {
            setGraphic(null);
            super.commitEdit(newValue);
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
                    setEditable(proxy.getItem().isWritable());
                    if (proxy.getItem().isChanged() && model.isSaveRestoreEnabled())
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
            // XXX Replace linear lookup of row w/ member variable in PVTableItem?
            final int row = model.getItems().indexOf(item);

            // System.out.println(item + " changed in row " + row + " on " + Thread.currentThread().getName());
            final TableItemProxy proxy = rows.get(row);
            if (proxy.getItem() != item)
                throw new IllegalStateException("*** Looking for " + item.getName() + " but found " + proxy.name.get());
            proxy.update(item);
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

    /** @param model Data model */
    public PVTable(final PVTableModel model)
    {
        this.model = model;
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        model.setUpdateSuppressor(() -> table.getEditingCell() != null);

        // Initial sort: No columns, just new item at bottom
        sorted.setComparator(SORT_NEW_ITEM_LAST);

        // When user clicks on column headers, table.comparatorProperty updates
        // to sort by those columns.
        // SortedList should fundamentally use that order, i.e.
        //   sorted.comparatorProperty().bind(table.comparatorProperty());
        // but in addition the NEW_ITEM should remain last.
        final InvalidationListener sort_changed = p ->
        {
            final Comparator<? super TableItemProxy> column_comparator = table.getComparator();
            // System.out.println("Table column sort: " + column_comparator);
            if (column_comparator == null)
                sorted.setComparator(SORT_NEW_ITEM_LAST);
            else
                sorted.setComparator(SORT_NEW_ITEM_LAST.thenComparing(column_comparator));
        };

        // The InvalidationListener is called when sort order is set up, down or null.
        // Iffy: A ChangeListener was only called when sort order is set up or null,
        // no change when sorting up vs. down, so failed to set the combined
        // SORT_NEW_ITEM_LAST.thenComparing(column_comparator)
        table.comparatorProperty().addListener(sort_changed);

        // TableView.DEFAULT_SORT_POLICY will check if table items are a SortedList.
        // If so, it warns unless  sortedList.comparator == table.comparator,
        // which is not the case since we wrap it in SORT_NEW_ITEM_LAST
        table.setSortPolicy(table ->
        {
            // Underlying 'rows' are kept in their original order
            // System.out.println("Data:");
            // for (TableItemProxy proxy : rows)
            //    System.out.println(proxy.name.get() + (proxy == TableItemProxy.NEW_ITEM ? " (NEW)" : ""));
            // The 'sorted' list uses the changing comparator
            // System.out.println("Sorted:");
            // for (TableItemProxy proxy : sorted)
            //    System.out.println(proxy.name.get() + (proxy == TableItemProxy.NEW_ITEM ? " (NEW)" : ""));

            // Nothing to do, sorted list already handles everything
            return true;
        });

        // Select complete rows
        final TableViewSelectionModel<TableItemProxy> table_sel = table.getSelectionModel();
        table_sel.setCellSelectionEnabled(false);
        table_sel.setSelectionMode(SelectionMode.MULTIPLE);

        // Publish selected PV
        final InvalidationListener sel_changed = change ->
        {
            final List<ProcessVariable> pvs = getSelectedItems()
                 .map(proxy -> new ProcessVariable(proxy.getItem().getName()))
                 .collect(Collectors.toList());
            SelectionService.getInstance().setSelection("PV Table", pvs);
        };
        table_sel.getSelectedItems().addListener(sel_changed);

        createTableColumns();

        table.setEditable(true);

        toolbar = createToolbar();

        // Have table use the available space
        setMargin(table, new Insets(5));
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().setAll(toolbar, table);

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
                    .filter(proxy -> proxy != TableItemProxy.NEW_ITEM  &&  ! proxy.getItem().isComment());
    }

    private void setItemsFromModel()
    {
        if (! model.isSaveRestoreEnabled())
            disableSaveRestore();

        final List<TableItemProxy> items = new ArrayList<>(model.getItems().size() + 1);
        for (PVTableItem item : model.getItems())
            items.add(new TableItemProxy(item));
        items.add(TableItemProxy.NEW_ITEM);
        rows.setAll(items);
        table.refresh();
    }

    private void disableSaveRestore()
    {
        if (saveRestoreDisabled)
            return;

        toolbar.getItems().remove(snapshot_button);
        toolbar.getItems().remove(restore_button);
        table.getColumns().remove(saved_value_col);
        table.getColumns().remove(saved_time_col);
        table.getColumns().remove(completion_col);

        saveRestoreDisabled = true;

        table.refresh();
        model.fireModelChange();
    }

    private ToolBar createToolbar()
    {
        return new ToolBar(
            ToolbarHelper.createSpring(),
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
            ToolbarHelper.createStrut(),
            snapshot_button = createButton("snapshot.png", Messages.Snapshot_TT, event -> model.save()),
            restore_button  = createButton("restore.png", Messages.Restore_TT, event -> model.restore())
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
            dialog.setContentText(getSelectedItems().map(proxy -> proxy.getItem().toString())
                                                    .collect(Collectors.joining("\n")));
            dialog.getDialogPane().setPrefWidth(800.0);
            dialog.setResizable(true);
            DialogHelper.positionDialog(dialog, table, -400, -100);
            dialog.showAndWait();
        });

        final MenuItem save = createMenuItem(Messages.SnapshotSelection, "snapshot.png", event ->
        {
            model.save(getSelectedItems().map(proxy -> proxy.getItem())
                                         .collect(Collectors.toList()));
        });

        final MenuItem restore = createMenuItem(Messages.RestoreSelection, "restore.png", event ->
        {
            model.restore(getSelectedItems().map(proxy -> proxy.getItem())
                                            .collect(Collectors.toList()));
        });

        final MenuItem add_row = createMenuItem(Messages.Insert, "add.gif", event ->
        {
            // Copy selection as it will change when we add to the model
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            if (selected.isEmpty())
                return;
            final int last = table.getSelectionModel().getSelectedIndex();
            // addItemAbove() handles proxy.getItem() == null for the NEW_ITEM
            for (TableItemProxy proxy : selected)
                model.addItemAbove(proxy.getItem(), "# ");
            table.getSelectionModel().select(last);
        });

        final MenuItem remove_row = createMenuItem(Messages.Delete, "delete.gif", event ->
        {
            // Copy selection as it will change
            final List<TableItemProxy> selected = new ArrayList<>(table.getSelectionModel().getSelectedItems());
            // Don't remove the 'last' item
            for (TableItemProxy proxy : selected)
                if (proxy != TableItemProxy.NEW_ITEM)
                    model.removeItem(proxy.getItem());
        });

        final MenuItem tolerance = createMenuItem(Messages.Tolerance, "pvtable.png", event ->
        {
            final TableItemProxy proxy = table.getSelectionModel().getSelectedItem();
            if (proxy == null  ||   proxy.getItem().isComment())
                return;

            final NumericInputDialog dlg = new NumericInputDialog(Messages.Tolerance,
                    "Enter tolerance for " + proxy.getItem().getName(),
                    proxy.getItem().getTolerance(),
                    number -> number >= 0 ? null : "Enter a positive tolerance value");
            // Would be nice to position on selected row, but hard to get location of selected cell??
            DialogHelper.positionDialog(dlg, table, -100, -100);
            dlg.promptAndHandle(number -> proxy.getItem().setTolerance(number));
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
            menu.getItems().addAll(info, new SeparatorMenuItem());

            if (model.isSaveRestoreEnabled())
                menu.getItems().addAll(save, restore, new SeparatorMenuItem());

            menu.getItems().addAll(add_row, remove_row, new SeparatorMenuItem(), tolerance);

            if (model.isSaveRestoreEnabled())
                menu.getItems().add(timeout);

            menu.getItems().add(new SeparatorMenuItem());

            if (maySetToSaveRestore() && model.isSaveRestoreEnabled())
            {
                MenuItem disableSaveRestore = createMenuItem(Messages.DisableSaveRestore, "timeout.png", event1 ->
                {
                    Alert alert = new Alert(AlertType.CONFIRMATION, "", ButtonType.NO, ButtonType.YES);
                    alert.setHeaderText("Are you sure you want to disable save/restore functionality for this table?");
                    DialogHelper.positionDialog(alert, this, -100, -100);
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.YES)
                    {
                        model.setSaveRestore(false);
                        disableSaveRestore();
                    }
                });
                menu.getItems().addAll(disableSaveRestore, new SeparatorMenuItem());
            }

            // Add PV entries
            if (ContextMenuHelper.addSupportedEntries(FocusUtil.setFocusOn(table), menu))
                menu.getItems().add(new SeparatorMenuItem());

            menu.getItems().add(new PrintAction(this));
            menu.getItems().add(new SaveSnapshotAction(table));

            // Add context menu actions based on the selection (i.e. email, logbook, etc...)
            final Selection originalSelection = SelectionService.getInstance().getSelection();
            final List<AppSelection> newSelection = Arrays.asList(AppSelection.of(table, "PV Snapshot", "See attached screenshot.", () -> Screenshot.imageFromNode(this)));
            SelectionService.getInstance().setSelection("PV Table", newSelection);
            List<ContextMenuEntry> supported = ContextMenuService.getInstance().listSupportedContextMenuEntries();
            supported.stream().forEach(action -> {
                MenuItem menuItem = new MenuItem(action.getName(), new ImageView(action.getIcon()));
                menuItem.setOnAction((e) -> {
                    try
                    {
                        SelectionService.getInstance().setSelection("PV Table", newSelection);
                        action.call(table, SelectionService.getInstance().getSelection());
                    } catch (Exception ex)
                    {
                        PVTableApplication.logger.log(Level.WARNING, "Failed to execute " + action.getName() + " from PV Table.", ex);
                    }
                });
                menu.getItems().add(menuItem);
            });
            SelectionService.getInstance().setSelection("AlarmUI", originalSelection);

            menu.show(table.getScene().getWindow(), event.getScreenX(), event.getScreenY());

        });

        table.setContextMenu(menu);
    }

    private boolean maySetToSaveRestore()
    {
        return AuthorizationService.hasAuthorization("disable_save_restore");
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
            final String new_name = event.getNewValue().trim();
            final TableItemProxy proxy = event.getRowValue();
            if (proxy == TableItemProxy.NEW_ITEM)
            {
                if (!new_name.isEmpty()) {
                    //Can be a list of pv
                    final String[] pvs = new_name.split(SPLIT_PV);
                    //Add a list
                    for(String pv : pvs) {
                         model.addItem(pv);
                    }
                }
                // else: No name entered, do nothing
            }
            else
            {
                // Set name, even if empty, assuming user wants to continue
                // editing the existing row.
                // To remove row, use context menu.
                proxy.getItem().updateName(new_name);
                proxy.update(proxy.getItem());
                // Content of model changed.
                // Triggers full table update.
                model.fireModelChange();
            }
        });
        // Use natural order for PV name
        col.setComparator(CompareNatural.INSTANCE);
        table.getColumns().add(col);

        // Description
        if (Settings.show_description)
        {
            col = new TableColumn<>(Messages.Description);
            col.setCellValueFactory(cell -> cell.getValue().desc_value);
            col.setCellFactory(column -> new DescriptionTableCell());
            table.getColumns().add(col);
        }

        // Time Stamp
        col = new TableColumn<>(Messages.Time);
        col.setCellValueFactory(cell ->  cell.getValue().time);
        table.getColumns().add(col);

        // Editable value
        col = new TableColumn<>(Messages.Value);
        col.setCellValueFactory(cell -> cell.getValue().value);
        col.setCellFactory(column -> new ValueTableCell(model));
        col.setOnEditCommit(event ->
        {
            event.getRowValue().getItem().setValue(event.getNewValue());
            // Since updates were suppressed, refresh table
            model.performPendingUpdates();
        });
        col.setOnEditCancel(event ->
        {
            // Since updates were suppressed, refresh table
            model.performPendingUpdates();
        });
        // Use natural order for value
        col.setComparator(CompareNatural.INSTANCE);
        table.getColumns().add(col);

        // Alarm
        col = new TableColumn<>(Messages.Alarm);
        col.setCellValueFactory(cell -> cell.getValue().alarm);
        col.setCellFactory(column -> new AlarmTableCell());
        table.getColumns().add(col);

        // Saved value
        col = new TableColumn<>(Messages.Saved);
        col.setCellValueFactory(cell -> cell.getValue().saved);
        // Use natural order for saved value
        col.setComparator(CompareNatural.INSTANCE);
        saved_value_col = col;
        table.getColumns().add(col);

        // Saved value's timestamp
        col = new TableColumn<>(Messages.Saved_Value_TimeStamp);
        col.setCellValueFactory(cell -> cell.getValue().time_saved);
        saved_time_col = col;
        table.getColumns().add(col);

        // Completion checkbox
        final TableColumn<TableItemProxy, Boolean> compl_col = new TableColumn<>(Messages.Completion);
        compl_col.setCellValueFactory(cell -> cell.getValue().use_completion);
        compl_col.setCellFactory(column -> new BooleanTableCell());
        completion_col = compl_col;
        table.getColumns().add(compl_col);
    }

    /** Set to currently dragged items to allow 'drop' to move them instead of
     *  adding duplicates.
     */
    private List<TableItemProxy> dragged_items = null;

    /** @param node Node
     *  @return <code>true</code> if node is in a table cell, and not the table header
     */
    private static boolean isTableCell(Node node)
    {
        while (node != null)
        {
            if (node instanceof TableRow<?>)
                return true;
            node = node.getParent();
        }
        return false;
    }
    private void hookDragAndDrop()
    {
        // Drag PV names as string. Also locally remember dragged_items
        table.setOnDragDetected(event ->
        {
            // Ignore 'drag' of table header, because that would
            // interfere with the resizing and re-ordering of table
            // columns
            if (!isTableCell(event.getPickResult().getIntersectedNode()))
                return;

            final Dragboard db = table.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            final ClipboardContent content = new ClipboardContent();

            final List<ProcessVariable> pvs = new ArrayList<>();
            dragged_items = new ArrayList<>();
            for (TableItemProxy proxy : table.getSelectionModel().getSelectedItems())
                if (proxy != TableItemProxy.NEW_ITEM)
                {
                    dragged_items.add(proxy);
                    if (! proxy.getItem().isComment())
                        pvs.add(new ProcessVariable(proxy.getItem().getName()));
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
            if (event.getDragboard().hasString() ||
                event.getDragboard().hasContent(DataFormats.ProcessVariables))
            {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
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
                    model.removeItem(proxy.getItem());
                    model.addItemAbove(existing, proxy.getItem());
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
                }
                else
                    return; // Don't accept, pass event on
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private void addPVsFromString(final PVTableItem existing, final String pv_text)
    {
        final String[] pvs = pv_text.split(SPLIT_PV);
        for (String pv : pvs)
            if (! pv.isEmpty())
                model.addItemAbove(existing, pv);
    }
}
