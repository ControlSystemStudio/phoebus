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

import javafx.beans.binding.Bindings;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Context menu for {@link Node}s of type {@link org.phoebus.applications.saveandrestore.model.NodeType#FOLDER}.
 * All item actions require user to be authenticated, and if that is not the case,
 * the context menu is hidden rather than showing a list of disabled context menu items.
 */
public class ContextMenuRootFolder extends ContextMenuBase {

    public ContextMenuRootFolder(SaveAndRestoreController saveAndRestoreController) {
        super(saveAndRestoreController);

        MenuItem newFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(ImageRepository.FOLDER));
        newFolderMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty));
        newFolderMenuItem.setOnAction(ae -> saveAndRestoreController.createNewFolder());

        getItems().addAll(
                loginMenuItem,
                newFolderMenuItem);

    }

    /**
     * Execute common checks (see {@link ContextMenuBase#runChecks()}) and hides the menu
     * if user is not authenticated.
     */
    @Override
    protected void runChecks() {
        super.runChecks();
        mayPasteProperty.set(saveAndRestoreController.mayPaste());
    }
}
