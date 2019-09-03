/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.Arrays;
import java.util.logging.Level;

import org.csstudio.scan.command.Comparison;
import org.csstudio.scan.command.IfCommand;
import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.ui.editor.ScanEditor;
import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.ui.autocomplete.PVAutocompleteMenu;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Editor for properties of a scan command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Properties extends TitledPane
{
    private final ScanEditor editor;
    private final TreeView<ScanCommand> scan_tree;
    private final UndoableActionManager undo;
    private final ScrollPane scroll = new ScrollPane();

    public Properties(final ScanEditor editor, final TreeView<ScanCommand> scan_tree, final UndoableActionManager undo)
    {
        this.editor = editor;
        this.scan_tree = scan_tree;
        this.undo = undo;
        scroll.setFitToWidth(true);
        scroll.setMinHeight(0);

        setText("Command Detail");
        setContent(scroll);
        setCollapsible(false);
        setMaxHeight(Double.MAX_VALUE);

        // Scan tree allows selecting multiple items,
        // so could listen to getSelectedItems(),
        // but that one does not update when a new item it added
        // and the ScanCommandTree programmatically selects the new item.
        // The property view handles a single item only, anyway,
        // and selectedItemProperty() calls listener even when set
        // programmatically, i.e. for newly added command
        scan_tree.getSelectionModel().selectedItemProperty().addListener((p, old, item) -> setCommand(item));
    }

    private void setCommand(final TreeItem<ScanCommand> tree_item)
    {
        final GridPane prop_grid = new GridPane();
        prop_grid.setPadding(new Insets(5));
        prop_grid.setHgap(5);
        prop_grid.setVgap(5);

        if (tree_item != null)
        {
            int row = 0;
            for (ScanCommandProperty prop : tree_item.getValue().getProperties())
            {
                final Label label = new Label(prop.getName());
                prop_grid.add(label, 0, row);

                try
                {
                    final Node editor = createEditor(tree_item, prop);
                    GridPane.setHgrow(editor, Priority.ALWAYS);
                    GridPane.setFillWidth(editor, true);

                    // Label defaults to vertical center,
                    // which is good for one-line editors.
                    if (editor instanceof StringArrayEditor)
                        GridPane.setValignment(label, VPos.TOP);

                    prop_grid.add(editor, 1, row++);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot create editor for " + prop, ex);
                }
                ++row;
            }
        }

        scroll.setContent(prop_grid);
    }

    private Node createEditor(final TreeItem<ScanCommand> tree_item,
                              final ScanCommandProperty property) throws Exception
    {
        final ScanCommand command = tree_item.getValue();

        if (property.getType() == String.class)
        {
            return new PropertyTextField(command, property,
                                         text -> updateProperty(tree_item, property, text));
        }
        else if (property.getType() == Boolean.class)
        {
            final CheckBox editor = new CheckBox();
            editor.setSelected((Boolean) command.getProperty(property));
            editor.setOnAction(event -> updateProperty(tree_item, property, editor.isSelected()));
            return editor;
        }
        else if (property.getType() == DeviceInfo.class)
        {
            final PropertyTextField editor = new PropertyTextField(command, property,
                                                                   text -> updateProperty(tree_item, property, text));
            PVAutocompleteMenu.INSTANCE.attachField(editor);
            return editor;
        }
        else if (property.getType() == Double.class)
        {
            return new PropertyTextField(command, property, text ->
            {
                try
                {
                    updateProperty(tree_item, property, Double.parseDouble(text));
                }
                catch (Exception ex)
                {
                    // Cannot parse number, reset to original value
                }
            });
        }
        else if (property.getType() == Object.class)
        {
            return new PropertyTextField(command, property,
                                         text -> updateProperty(tree_item, property, StringOrDouble.parse(text)))
            {
                @Override
                protected String value2text(final Object value)
                {
                    return StringOrDouble.quote(value);
                }
            };
        }
        else if (property.getType() == String[].class  ||
                 property.getType() == DeviceInfo[].class)
        {
            final String[] values = (String[]) command.getProperty(property);
            final StringArrayEditor editor;
            if (property.getType() == DeviceInfo[].class)
                editor = new StringArrayEditor()
                {
                    @Override
                    protected void configureTextField(final TextInputControl text_field)
                    {
                        PVAutocompleteMenu.INSTANCE.attachField(text_field);
                    }
                };
            else
                editor = new StringArrayEditor();
            editor.setValues(Arrays.asList(values));
            editor.setValueHandler(list -> updateProperty(tree_item, property, list.toArray(new String[list.size()])));
            return editor;
        }
        else if (property.getType().isEnum())
        {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enum_type = (Class<? extends Enum<?>>) property.getType();
            final ComboBox<String> editor = new ComboBox<>();
            for (Enum<?> ev : enum_type.getEnumConstants())
            {
                // Skip the 'increase'/'decrease' options for IfCommand
                if (tree_item.getValue() instanceof IfCommand  &&
                    enum_type == Comparison.class              &&
                    ev.ordinal() >= Comparison.INCREASE_BY.ordinal())
                    break;
                editor.getItems().add(ev.toString());
            }
            editor.setValue(command.getProperty(property).toString());
            editor.valueProperty().addListener((p, o, value) -> updateProperty(tree_item, property, value));
            return editor;
        }
        logger.log(Level.WARNING, "Cannot edit property type " + property);
        return new Label(command.getProperty(property).toString());
    }

    private void updateProperty(final TreeItem<ScanCommand> tree_item,
                                final ScanCommandProperty property,
                                final Object value)
    {
        try
        {
            undo.execute(new ChangeProperty(editor, this, tree_item, property, value));
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot set property " + property + " to new value " + value, ex);
        }
    }

    public void refresh()
    {
        setCommand(scan_tree.getSelectionModel().getSelectedItem());
    }
}
