/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.Objects;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;
import org.csstudio.scan.command.UnknownScanCommandPropertyException;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.util.StringOrDouble;
import org.phoebus.ui.javafx.TreeHelper;

import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Editor for properties of a scan command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Properties extends TabPane
{
    private final ScrollPane scroll = new ScrollPane();

    public Properties(TreeView<ScanCommand> scan_tree)
    {
        scroll.setFitToWidth(true);
        scroll.setMinHeight(0);

        final Tab cmd_tab = new Tab("Command Detail", scroll);
        cmd_tab.setClosable(false);
        getTabs().add(cmd_tab);

        final MultipleSelectionModel<TreeItem<ScanCommand>> selection = scan_tree.getSelectionModel();
        final InvalidationListener listener = sel -> setCommand(selection.getSelectedItem());
        selection.getSelectedItems().addListener(listener);
    }

    private void setCommand(final TreeItem<ScanCommand> tree_item)
    {
        final GridPane prop_grid = new GridPane();
        prop_grid.setPadding(new Insets(5));
        prop_grid.setHgap(5);
        prop_grid.setVgap(5);

        int row = 0;
        for (ScanCommandProperty prop : tree_item.getValue().getProperties())
        {
            prop_grid.add(new Label(prop.getName()), 0, row);


            try
            {
                final Node editor = createEditor(tree_item, prop);
                GridPane.setHgrow(editor, Priority.ALWAYS);
                GridPane.setFillWidth(editor, true);
                prop_grid.add(editor, 1, row++);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot create editor for " + prop, ex);
            }
            ++row;
        }

        scroll.setContent(prop_grid);
    }

    private Node createEditor(final TreeItem<ScanCommand> tree_item,
                              final ScanCommandProperty property) throws Exception
    {
        final ScanCommand command = tree_item.getValue();
        if (property.getType() == String.class)
        {
            final TextField editor = new TextField(Objects.toString(command.getProperty(property)));
            return editor;
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
            final TextField editor = new TextField(Objects.toString(command.getProperty(property)));
            return editor;
        }
        else if (property.getType() == Double.class)
        {
            final TextField editor = new TextField(Objects.toString(command.getProperty(property)));
            return editor;
        }
        else if (property.getType() == Object.class)
        {
            final TextField editor = new TextField();
            final Runnable reset = () ->
            {
                try
                {
                    editor.setText(StringOrDouble.quote(command.getProperty(property)));
                }
                catch (UnknownScanCommandPropertyException ex)
                {
                    // Ignore
                }
            };
            final Runnable update = () ->
            {
                System.out.println("Entered " + editor.getText());
                updateProperty(tree_item, property, StringOrDouble.parse(editor.getText()));
                reset.run();
            };
            editor.setOnAction(event -> update.run());
            editor.setOnKeyPressed(event ->
            {
                if (event.getCode() == KeyCode.ESCAPE)
                    reset.run();
            });
            editor.focusedProperty().addListener((f, old, focus) ->
            {
                if (! focus)
                    reset.run();
            });
            reset.run();
            return editor;
        }
        else if (property.getType() == String[].class)
        {
            // TODO
            final TextField editor = new TextField(StringOrDouble.quote(command.getProperty(property)));
            return editor;
        }
        else if (property.getType() == DeviceInfo[].class)
        {
            // TODO
            final TextField editor = new TextField(StringOrDouble.quote(command.getProperty(property)));
            return editor;
        }
        else if (property.getType().isEnum())
        {
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enum_type = (Class<? extends Enum<?>>) property.getType();
            final ComboBox<String> editor = new ComboBox<>();
            for (Enum<?> ev : enum_type.getEnumConstants())
                editor.getItems().add(ev.toString());
            editor.setValue(command.getProperty(property).toString());
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
            tree_item.getValue().setProperty(property, value);
        }
        catch (UnknownScanCommandPropertyException ex)
        {
            logger.log(Level.WARNING, "Cannot set property " + property + " to new value " + value, ex);
        }
        TreeHelper.triggerTreeItemRefresh(tree_item);
    }
}
