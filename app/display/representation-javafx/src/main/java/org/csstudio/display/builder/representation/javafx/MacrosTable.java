/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.display.builder.model.util.ModelThreadPool;
import org.phoebus.framework.macros.Macros;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.EditCell;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

/** JFX Table for editing {@link Macros}
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacrosTable
{
    private static final String EDITING = "editing";
    private final AtomicBoolean editing = new AtomicBoolean(false);
    /** Java FX type observable property for a macro (name, value) pair */
    public static class MacroItem
    {
        private StringProperty name, value;

        public MacroItem(final String name, final String value)
        {
            this.name = new SimpleStringProperty(name);
            this.value = new SimpleStringProperty(value);
        }

        public StringProperty nameProperty()     { return name;                  }
        public void setName(final String name)   { nameProperty().set(name);     }
        public String getName()                  { return nameProperty().get();  }

        public StringProperty valueProperty()    { return value;                 }
        public void setValue(final String value) { valueProperty().set(value);   }
        public String getValue()                 { return valueProperty().get(); }
    };

    /** Top-level UI node */
    private final GridPane content = new GridPane();

    /** Data that is linked to the table */
    private final ObservableList<MacroItem> data = FXCollections.observableArrayList();

    private List<InvalidationListener> listeners = new CopyOnWriteArrayList<>();

    private boolean enterHit = false;
    
    /** Create dialog
     *  @param initial_macros Initial {@link Macros}
     */
    public MacrosTable(final Macros initial_macros)
    {
        final TableView<MacroItem> table = new TableView<>(data);

        // Layout:
        //
        // | table |  [Add]
        // | table |  [Remove]
        // | table |
        // | table |
        content.setHgap(10);
        content.setVgap(10);
//        content.setBackground(new Background(new BackgroundFill(Color.PALEGREEN, CornerRadii.EMPTY, Insets.EMPTY)));
//        content.setGridLinesVisible(true); // For debugging

        // Create table with editable columns
        final TableColumn<MacroItem, String> name_col = new TableColumn<>(Messages.MacrosDialog_NameCol);
        final TableColumn<MacroItem, String> value_col = new TableColumn<>(Messages.MacrosDialog_ValueCol);

        table.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                enterHit = true;
            }
        });

        name_col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<MacroItem,String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(final CellDataFeatures<MacroItem, String> param)
            {
                final String name = param.getValue().getName();
                if (name.isEmpty())
                {
                    return new ReadOnlyStringWrapper(Messages.MacrosTable_NameHint);
                }
                return new ReadOnlyStringWrapper(name);
            }
        });
        name_col.setCellFactory((column) -> new EditCell<>(new DefaultStringConverter() {
            /** {@inheritDoc} */
            @Override public String toString(String value) {
                if(editing.get())
                {
                    if(value != null && value.equalsIgnoreCase(Messages.MacrosTable_NameHint))
                        return "";
                }
                return (value != null) ? value : "";
            }

        }));
        name_col.setOnEditStart(event ->
        {
            editing.set(true);
        });
        name_col.setOnEditCancel(event -> {
            editing.set(false);
        });
        name_col.setOnEditCommit(event ->
        {
            editing.set(false);
            final int row = event.getTablePosition().getRow();
            final String name = event.getNewValue();
            final String error = Macros.checkMacroName(name);
            // Empty name is an error, but we allow that for deleting a row
            if (name.isEmpty()  ||  error == null)
            {
                if(row < data.size())
                    data.get(row).setName(name);
                if (!enterHit)
                {
                    name_col.setVisible(false);
                    name_col.setVisible(true);
                }
                fixup(row);
            }
            else
            {
                final Alert alert = new Alert(AlertType.ERROR);
                alert.setHeaderText(error);
                DialogHelper.positionDialog(alert, table, -300, -200);
                alert.showAndWait();
                // Table will continue to show the entered name,
                // not the actual name. Hack to 'refresh' table.
                name_col.setVisible(false);
                name_col.setVisible(true);
                return;
            }
            // Next edit the value
            if (enterHit)
            {
                enterHit = false;
                ModelThreadPool.getTimer().schedule(() ->
                {
                    Platform.runLater(() -> table.edit(row, value_col));
                }, 123, TimeUnit.MILLISECONDS);
            }
        });

        value_col.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<MacroItem,String>, ObservableValue<String>>()
        {
            @Override
            public ObservableValue<String> call(final CellDataFeatures<MacroItem, String> param)
            {
                final String name = param.getValue().getValue();
                if (name.isEmpty())
                    return new ReadOnlyStringWrapper(Messages.MacrosTable_ValueHint);
                return new ReadOnlyStringWrapper(name);
            }
        });
        value_col.setCellFactory((column) -> new EditCell<>(new DefaultStringConverter() {
            /** {@inheritDoc} */
            @Override public String toString(String value) {
                if(editing.get())
                {
                    if(value != null && value.equalsIgnoreCase(Messages.MacrosTable_ValueHint))
                        return "";
                }
                return (value != null) ? value : "";
            }

        }));
        value_col.setOnEditStart(event ->
        {
            editing.set(true);
        });
        value_col.setOnEditCancel(event -> {
            editing.set(false);
        });
        value_col.setOnEditCommit(event ->
        {
            editing.set(false);
            final int row = event.getTablePosition().getRow();
            data.get(row).setValue(event.getNewValue());
            if (!enterHit)
            {
                value_col.setVisible(false);
                value_col.setVisible(true);
            }
            fixup(row);
            // Edit next row
            if (enterHit)
            {
                enterHit = false;
                ModelThreadPool.getTimer().schedule(() ->
                {
                    Platform.runLater(() -> table.edit(row+1, name_col));
                }, 123, TimeUnit.MILLISECONDS);
            }
        });

        table.getColumns().add(name_col);
        table.getColumns().add(value_col);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setTooltip(new Tooltip(Messages.MacrosTable_ToolTip));

        content.add(table, 0, 0, 1, 3);
        GridPane.setHgrow(table, Priority.ALWAYS);
        GridPane.setVgrow(table, Priority.ALWAYS);

        // Buttons
        final Button add = new Button(Messages.Add, JFXUtil.getIcon("add.png"));
        add.setMaxWidth(Double.MAX_VALUE);
        content.add(add, 1, 0);
        add.setOnAction(event ->
        {
            // Start editing name of the last line, i.e. the new macro
            ModelThreadPool.getTimer().schedule(() ->
            {
                Platform.runLater(() -> table.edit(data.size()-1, name_col));
            }, 123, TimeUnit.MILLISECONDS);
        });

        final Button remove = new Button(Messages.Remove, JFXUtil.getIcon("delete.png"));
        remove.setMaxWidth(Double.MAX_VALUE);
        content.add(remove, 1, 1);
        remove.setOnAction(event ->
        {
            final int sel = table.getSelectionModel().getSelectedIndex();
            if (sel >= 0)
            {
                data.remove(sel);
                fixup(sel);
            }
        });

        // Without this filler in the bottom right corner,
        // the table's Vgrow setting stops taking effect after a certain hight
        // of the content pane?!
        final Label filler = new Label();
        content.add(filler, 1, 2);
        GridPane.setVgrow(filler, Priority.ALWAYS);

        setMacros(initial_macros);
    }

    /** Fix table: Delete empty rows in middle, but keep one empty final row
     *  @param changed_row Row to check, and remove if it's empty
     */
    private void fixup(final int changed_row)
    {
        // Check if edited row is now empty and should be deleted
        if (changed_row < data.size())
        {
            final MacroItem item = data.get(changed_row);
            final String name = item.getName().trim();
            final String value = item.getValue().trim();

            if (name.isEmpty()  &&  value.isEmpty())
                data.remove(changed_row);
        }
        // Assert one empty row at bottom
        final int len  = data.size();
        if (len <= 0  ||
            (data.get(len-1).getName().trim().length() > 0  &&
             data.get(len-1).getValue().trim().length() > 0) )
            data.add(new MacroItem("", ""));

        for (InvalidationListener listener : listeners)
            listener.invalidated(data);
    }

    /** @return Top-level UI node of this Java FX composite */
    public Parent getNode()
    {
        return content;
    }

    /** @param listener Listener that will be invoked whenever anything
     *                  in the macros is edited.
     */
    public void addListener(final InvalidationListener listener)
    {
        listeners.add(listener);
    }

    /** @param macros {@link Macros} to show in table */
    public void setMacros(final Macros macros)
    {
        data.clear();
        macros.forEach((name, value) -> data.add(new MacroItem(name, value)));
        // Add empty final row
        data.add(new MacroItem("", ""));
    }

    /** @return {@link Macros} for data in table */
    public Macros getMacros()
    {
        final Macros macros = new Macros();
        for (MacroItem item : data)
        {
            final String name = item.getName().trim();
            final String value = item.getValue().trim();
            // Skip when there's no macro name
            if (!name.isEmpty())
                macros.add(name, value);
        }
        return macros;
    }
}
