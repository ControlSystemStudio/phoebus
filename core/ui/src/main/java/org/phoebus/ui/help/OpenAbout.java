/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.help;

import java.util.Arrays;
import java.util.List;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.framework.workbench.Locations;
import org.phoebus.ui.javafx.ImageCache;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;

/** Menu entry to open 'about'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class OpenAbout implements MenuEntry
{
    @Override
    public String getName()
    {
        return "About";
    }

    @Override
    public String getMenuPath()
    {
        return "Help/About";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(getClass(), "/icons/info.png");
    }

    @Override
    public Void call()
    {
        // TODO Useful, but ugly
        // TODO Add version information
        final Alert dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle("About Phoebus");
        dialog.setHeaderText("Phoebus Installation Information");

        // Table with Name, Value columns
        final ObservableList<List<String>> infos = FXCollections.observableArrayList();
        infos.add(Arrays.asList("Installation Location", Locations.install().toString()));
        infos.add(Arrays.asList("User Settings Location", Locations.user().toString()));
        infos.add(Arrays.asList("Java Version", System.getProperty("java.specification.vendor") + " " + System.getProperty("java.runtime.version")));

        // Display in TableView
        final TableView<List<String>> info_table = new TableView<>(infos);
        info_table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        info_table.setPrefHeight(200.0);

        final TableColumn<List<String>, String> name_col = new TableColumn<>("Name");
        name_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(0)));
        info_table.getColumns().add(name_col);

        final TableColumn<List<String>, String> value_col = new TableColumn<>("Value");
        value_col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(1)));
        info_table.getColumns().add(value_col);

        dialog.getDialogPane().setContent(info_table);

        // Info for expandable "Show Details" section
        final StringBuilder details = new StringBuilder();
        details.append("Application Features\n");
        details.append("====================\n");
        ApplicationService.getApplications()
                          .forEach(app -> details.append(app.getDisplayName()).append("\n"));

        details.append("\n");
        details.append("System Properties\n");
        details.append("=================\n");
        System.getProperties()
              .stringPropertyNames()
              .stream()
              .sorted()
              .forEach(prop ->  details.append(prop).append(" = ").append(System.getProperty(prop)).append("\n"));

        // User can copy details out of read-only text area
        final TextArea trace = new TextArea(details.toString());
        trace.setEditable(false);
        dialog.getDialogPane().setExpandableContent(trace);

        dialog.setResizable(true);
        dialog.getDialogPane().setPrefWidth(800);

        dialog.showAndWait();

        return null;
    }
}
