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

public class ContextMenuSaveSet extends ContextMenuBase {

    public ContextMenuSaveSet(SaveAndRestoreController saveAndRestoreController, boolean csvEnabled, SimpleBooleanProperty multipleItemsSelected) {
        super(saveAndRestoreController, multipleItemsSelected);

        Image editSaveSetIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/edit_saveset.png");
        Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");

        MenuItem openSaveSetMenuItem = new MenuItem(Messages.contextMenuCreateSnapshot, new ImageView(saveSetIcon));
        openSaveSetMenuItem.setOnAction(ae -> {
            saveAndRestoreController.openSaveSetForSnapshot();
        });

        MenuItem editSaveSetMenuItem = new MenuItem(Messages.contextMenuEdit, new ImageView(editSaveSetIcon));
        editSaveSetMenuItem.disableProperty().bind(multipleItemsSelected);
        editSaveSetMenuItem.setOnAction(ae -> {
            saveAndRestoreController.nodeDoubleClicked();
        });

        getItems().addAll(openSaveSetMenuItem, editSaveSetMenuItem, renameNodeMenuItem, deleteNodesMenuItem, copyUniqueIdToClipboardMenuItem);

        if (csvEnabled) {
            ImageView exportSaveSetIconImageView = new ImageView(csvExportIcon);
            exportSaveSetIconImageView.setFitWidth(18);
            exportSaveSetIconImageView.setFitHeight(18);

            MenuItem exportSaveSetMenuItem = new MenuItem(Messages.exportSaveSetLabel, exportSaveSetIconImageView);
            exportSaveSetIconImageView.disableProperty().bind(multipleItemsSelected);
            exportSaveSetMenuItem.setOnAction(ae -> {
                saveAndRestoreController.exportSaveSet();
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
                getItems().addAll(exportSaveSetMenuItem, importSnapshotMenuItem);
            }
        }
    }
}
