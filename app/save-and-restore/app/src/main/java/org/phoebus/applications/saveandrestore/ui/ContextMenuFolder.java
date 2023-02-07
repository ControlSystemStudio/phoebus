/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;

public class ContextMenuFolder extends ContextMenuBase {

    public ContextMenuFolder(SaveAndRestoreController saveAndRestoreController, boolean csvImportEnabled, SimpleBooleanProperty multipleItemsSelected) {
        super(saveAndRestoreController, multipleItemsSelected);
        MenuItem newFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(ImageRepository.FOLDER));
        newFolderMenuItem.disableProperty().bind(multipleItemsSelected);
        newFolderMenuItem.setOnAction(ae -> {
            saveAndRestoreController.createNewFolder();
        });

        MenuItem newConfigurationMenuItem = new MenuItem(Messages.contextMenuNewConfiguration, new ImageView(ImageRepository.CONFIGURATION));
        newConfigurationMenuItem.disableProperty().bind(multipleItemsSelected);
        newConfigurationMenuItem.setOnAction(ae -> {
            saveAndRestoreController.createNewConfiguration();
        });

        MenuItem newCompositeSnapshotMenuItem = new MenuItem(Messages.contextMenuNewCompositeSnapshot, new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
        newCompositeSnapshotMenuItem.setOnAction(ae -> {
            saveAndRestoreController.createNewCompositeSnapshot();
        });

        getItems().addAll(newFolderMenuItem, renameNodeMenuItem, deleteNodesMenuItem, newConfigurationMenuItem, newCompositeSnapshotMenuItem, copyUniqueIdToClipboardMenuItem);

        if (csvImportEnabled) {

            ImageView importConfigurationIconImageView = new ImageView(csvImportIcon);
            importConfigurationIconImageView.setFitWidth(18);
            importConfigurationIconImageView.setFitHeight(18);

            MenuItem importConfigurationMenuItem = new MenuItem(Messages.importConfigurationLabel, importConfigurationIconImageView);
            importConfigurationMenuItem.disableProperty().bind(multipleItemsSelected);
            importConfigurationMenuItem.setOnAction(ae -> {
                saveAndRestoreController.importConfiguration();
            });

            getItems().add(importConfigurationMenuItem);
        }
    }
}
