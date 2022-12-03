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

public class ContextMenuCompositeSnapshot extends ContextMenuBase {

    public ContextMenuCompositeSnapshot(SaveAndRestoreController saveAndRestoreController, SimpleBooleanProperty multipleItemsSelected) {
        super(saveAndRestoreController, multipleItemsSelected);

        Image editConfigurationIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/edit_saveset.png");

        MenuItem openCompositeSnapshotMenuItem = new MenuItem(Messages.contextMenuOpenCompositeSnapshotForRestore, new ImageView(editConfigurationIcon));
        openCompositeSnapshotMenuItem.setOnAction(ae -> saveAndRestoreController.openCompositeSnapshotForRestore());

        MenuItem editCompositeSnapshotMenuItem = new MenuItem(Messages.contextMenuEdit, new ImageView(editConfigurationIcon));
        editCompositeSnapshotMenuItem.disableProperty().bind(multipleItemsSelected);
        editCompositeSnapshotMenuItem.setOnAction(ae -> {
            saveAndRestoreController.nodeDoubleClicked();
        });

        getItems().addAll(openCompositeSnapshotMenuItem, editCompositeSnapshotMenuItem, renameNodeMenuItem, deleteNodesMenuItem, copyUniqueIdToClipboardMenuItem);
    }
}
