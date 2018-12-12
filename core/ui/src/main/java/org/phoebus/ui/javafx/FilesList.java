/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.javafx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.phoebus.ui.application.ApplicationLauncherService;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** "List" of files with buttons to add/remove.
 *  @author Evan Smith
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class FilesList extends VBox
{
    private class FileCell extends ListCell<File>
    {
        private final Hyperlink hyperlink = new Hyperlink();

        public FileCell()
        {
            // Open file in default tool
            hyperlink.setOnAction(event ->
            {
                // Clicking on the hyperlink should select the table cell.
                getListView().getSelectionModel().select(getItem());

                // Try to open the file in default editor
                ApplicationLauncherService.openFile(getItem(), false, null);
            });
        }

        @Override
        public void updateItem(final File file, final boolean empty)
        {
            super.updateItem(file, empty);
            if (empty)
                setGraphic(null);
            else
            {
                hyperlink.setText(file.getName());
                setGraphic(hyperlink);
            }
        }
    }

    final ListView<File> files = new ListView<>();

    public FilesList()
    {
        files.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        files.setCellFactory(list -> new FileCell());

        final Node buttons = createButtons();

        setSpacing(5);
        getChildren().setAll(new Label(Messages.AttachedFiles), files, buttons);
        setPadding(new Insets(5));
    }

    /** @return Files listed in tab */
    public List<File> getFiles()
    {
        return files.getItems();
    }

    /** @param files Files to list in tab */
    public void setFiles(final List<File> files)
    {
        this.files.getItems().setAll(files);
    }

    private Node createButtons()
    {
        final Button attach = new Button(Messages.AttachFile);
        final Button remove = new Button(Messages.RemoveSelected, ImageCache.getImageView(ImageCache.class, "/icons/delete.png"));

        attach.setTooltip(new Tooltip(Messages.AddImageLog));
        remove.setTooltip(new Tooltip(Messages.RemoveSelectedFiles));

        // Only enable 'remove' when file(s) selected
        remove.disableProperty().bind(Bindings.isEmpty(files.getSelectionModel().getSelectedItems()));

        attach.setOnAction(event ->
        {
            final FileChooser dialog = new FileChooser();
            dialog.setInitialDirectory(new File(System.getProperty("user.home")));
            final List<File> to_add = dialog.showOpenMultipleDialog(getScene().getWindow());
            if (null != to_add)
                files.getItems().addAll(to_add);
        });

        remove.setOnAction(event ->
        {
            final List<File> selected = new ArrayList<>(files.getSelectionModel().getSelectedItems());
            if (selected.size() > 0)
                files.getItems().removeAll(selected);
        });

        final HBox row = new HBox(10, attach, remove);
        // Have buttons equally split the available width
        attach.setMaxWidth(Double.MAX_VALUE);
        remove.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(attach, Priority.ALWAYS);
        HBox.setHgrow(remove, Priority.ALWAYS);

        return row;
    }
}
