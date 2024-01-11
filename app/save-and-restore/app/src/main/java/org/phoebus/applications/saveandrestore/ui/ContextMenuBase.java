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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Abstract base class for context menus.
 */
public abstract class ContextMenuBase extends ContextMenu {

    protected Image csvImportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_import.png");

    protected MenuItem deleteNodesMenuItem;
    protected MenuItem copyUniqueIdToClipboardMenuItem;

    protected SimpleBooleanProperty multipleNodesSelectedProperty = new SimpleBooleanProperty();

    protected SaveAndRestoreController saveAndRestoreController;

    /**
     * Property showing if user has signed in or not. Context menus should in <code>onShowing</code>
     * check the sign-in status and set the property accordingly to determine which
     * menu items to disable (e.g. create or delete data).
     */
    protected SimpleBooleanProperty userIsAuthenticatedProperty =
            new SimpleBooleanProperty();

    /**
     * Property showing if selected {@link Node}s have the same parent {@link Node}. Context menus should
     * in <code>onShowing</code> check the selection and determine which menu items to disable.
     */
    protected SimpleBooleanProperty hasSameParentProperty =
            new SimpleBooleanProperty();

    /**
     * Property updated based on check of multiple {@link Node} selection,
     * e.g. selection of different type of {@link Node}s. Context menus should
     * in <code>onShowing</code> check the selection and determine which menu items to disable.
     */
    protected SimpleBooleanProperty nodesOfSameTypeProperty =
            new SimpleBooleanProperty();

    protected SimpleBooleanProperty mayPasteProperty =
            new SimpleBooleanProperty();

    protected SimpleBooleanProperty mayCopyProperty =
            new SimpleBooleanProperty();

    public ContextMenuBase(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
        deleteNodesMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(ImageRepository.DELETE));
        deleteNodesMenuItem.setOnAction(ae -> saveAndRestoreController.deleteNodes());
        deleteNodesMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        userIsAuthenticatedProperty.not().get() ||
                                hasSameParentProperty.not().get(),
                userIsAuthenticatedProperty, hasSameParentProperty));

        copyUniqueIdToClipboardMenuItem = new MenuItem(Messages.copyUniqueIdToClipboard, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
        copyUniqueIdToClipboardMenuItem.setOnAction(ae -> saveAndRestoreController.copyUniqueNodeIdToClipboard());
        copyUniqueIdToClipboardMenuItem.disableProperty().bind(multipleNodesSelectedProperty);

        // Controller may be null, e.g. when adding PVs from channel table
        if(saveAndRestoreController != null){
            setOnShowing(event -> runChecks());
        }
    }


    /**
     * Applies logic to determine if the user is authenticated and if multiple {@link Node}s have been selected.
     * Subclasses use this to disable menu items if needed, e.g. disable delete if user has not signed in.
     */
    protected void runChecks() {
        userIsAuthenticatedProperty.set(saveAndRestoreController.getUserIdentity().isNotNull().get());
        boolean multipleNodesSelected = saveAndRestoreController.multipleNodesSelected();
        multipleNodesSelectedProperty.set(multipleNodesSelected);
        if (multipleNodesSelected) { // No need to check this if only one node was selected
            hasSameParentProperty.set(saveAndRestoreController.hasSameParent());
            nodesOfSameTypeProperty.set(saveAndRestoreController.selectedNodesOfSameType());
        } else {
            hasSameParentProperty.set(true);
            nodesOfSameTypeProperty.set(true);
        }
    }
}
