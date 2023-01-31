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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.ui.javafx.ImageCache;

public abstract class ContextMenuBase extends ContextMenu {

    protected Image renameIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/rename_col.png");
    protected Image deleteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/delete.png");
    protected Image csvImportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_import.png");

    protected MenuItem deleteNodesMenuItem;
    protected MenuItem renameNodeMenuItem;
    protected MenuItem copyUniqueIdToClipboardMenuItem;

    public ContextMenuBase(SaveAndRestoreController saveAndRestoreController, SimpleBooleanProperty multipleItemsSelected){
        deleteNodesMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteNodesMenuItem.setOnAction(ae -> {
            saveAndRestoreController.deleteNodes();
        });

        renameNodeMenuItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameNodeMenuItem.disableProperty().bind(multipleItemsSelected);
        renameNodeMenuItem.setOnAction(ae -> {
            saveAndRestoreController.renameNode();
        });

        copyUniqueIdToClipboardMenuItem = new MenuItem(Messages.copyUniqueIdToClipboard, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
        copyUniqueIdToClipboardMenuItem.disableProperty().bind(multipleItemsSelected);
        copyUniqueIdToClipboardMenuItem.setOnAction(ae -> {
            saveAndRestoreController.copyUniqueNodeIdToClipboard();
        });
    }
}
