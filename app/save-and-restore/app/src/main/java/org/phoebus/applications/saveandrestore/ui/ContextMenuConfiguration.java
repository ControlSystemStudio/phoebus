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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.ui.javafx.ImageCache;

public class ContextMenuConfiguration extends ContextMenuBase {

    public ContextMenuConfiguration(SaveAndRestoreController saveAndRestoreController, boolean csvEnabled, SimpleBooleanProperty multipleItemsSelected) {
        super(saveAndRestoreController, multipleItemsSelected);

        Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");

        MenuItem openConfigurationMenuItem = new MenuItem(Messages.contextMenuCreateSnapshot, new ImageView(ImageRepository.CONFIGURATION));
        openConfigurationMenuItem.setOnAction(ae -> {
            saveAndRestoreController.openConfigurationForSnapshot();
        });

        MenuItem editConfigurationMenuItem = new MenuItem(Messages.contextMenuEdit, new ImageView(ImageRepository.EDIT_CONFIGURATION));
        editConfigurationMenuItem.disableProperty().bind(multipleItemsSelected);
        editConfigurationMenuItem.setOnAction(ae -> {
            saveAndRestoreController.nodeDoubleClicked();
        });

        getItems().addAll(openConfigurationMenuItem, editConfigurationMenuItem, renameNodeMenuItem, deleteNodesMenuItem, copyUniqueIdToClipboardMenuItem);

        if (csvEnabled) {
            ImageView exportConfigurationIconImageView = new ImageView(csvExportIcon);
            exportConfigurationIconImageView.setFitWidth(18);
            exportConfigurationIconImageView.setFitHeight(18);

            MenuItem exportConfigurationMenuItem = new MenuItem(Messages.exportConfigurationLabel, exportConfigurationIconImageView);
            exportConfigurationIconImageView.disableProperty().bind(multipleItemsSelected);
            exportConfigurationMenuItem.setOnAction(ae -> {
                saveAndRestoreController.exportConfiguration();
            });

            ImageView importSnapshotIconImageView = new ImageView(csvImportIcon);
            importSnapshotIconImageView.setFitWidth(18);
            importSnapshotIconImageView.setFitHeight(18);

            MenuItem importSnapshotMenuItem = new MenuItem(Messages.importSnapshotLabel, importSnapshotIconImageView);
            importSnapshotMenuItem.disableProperty().bind(multipleItemsSelected);
            importSnapshotMenuItem.setOnAction(ae -> {
                saveAndRestoreController.importSnapshot();
            });

            if (csvEnabled) {
                getItems().addAll(exportConfigurationMenuItem, importSnapshotMenuItem);
            }
        }
    }
}
