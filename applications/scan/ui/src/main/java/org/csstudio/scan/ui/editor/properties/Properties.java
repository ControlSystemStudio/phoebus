/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.editor.properties;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.ScanCommandProperty;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTableCell;

/** Editor for properties of a scan command
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Properties extends TabPane
{
    final TableView<PropertyDescriptor> prop_table = new TableView<>();

    public Properties(TreeView<ScanCommand> scan_tree)
    {
        createTable();

        final Tab cmd_tab = new Tab("Command Detail", prop_table);
        cmd_tab.setClosable(false);
        getTabs().add(cmd_tab);

        final MultipleSelectionModel<TreeItem<ScanCommand>> selection = scan_tree.getSelectionModel();
        final InvalidationListener listener = sel -> setCommand(selection.getSelectedItem());
        selection.getSelectedItems().addListener(listener);
    }

    private void createTable()
    {
        prop_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        prop_table.setPlaceholder(new Label("Select a command to edit it"));
        prop_table.setEditable(true);

        TableColumn<PropertyDescriptor, String> col = new TableColumn<>("Property");
        col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().property.getName()));
        prop_table.getColumns().add(col);

        col = new TableColumn<>("Value");
        col.setCellValueFactory(cell -> cell.getValue().value_text);
        // TODO Use custom PropertyDescriptorCellFactory that can handle the different types
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setEditable(true);
        prop_table.getColumns().add(col);
    }

    private void setCommand(final TreeItem<ScanCommand> tree_item)
    {
        final List<PropertyDescriptor> pd = new ArrayList<>();
        for (ScanCommandProperty prop : tree_item.getValue().getProperties())
        {
            // TODO Use different PropertyDescriptors for boolean, array elements, ...
            if (prop.getType() == Object.class)
                pd.add(new StringOrDoubleDescriptor(tree_item, prop));
            else if (prop.getType() == Double.class)
                pd.add(new DoubleDescriptor(tree_item, prop));
            else
                pd.add(new PropertyDescriptor(tree_item, prop));
        }
        prop_table.getItems().setAll(pd);
    }
}
