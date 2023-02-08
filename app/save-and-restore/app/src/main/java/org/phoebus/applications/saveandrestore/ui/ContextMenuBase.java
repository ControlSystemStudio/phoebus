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

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.ui.javafx.ImageCache;

public abstract class ContextMenuBase extends ContextMenu {

    protected Image renameIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/rename_col.png");
    protected Image deleteIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/delete.png");
    protected Image csvImportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_import.png");

    protected MenuItem deleteNodesMenuItem;
    protected MenuItem renameNodeMenuItem;
    protected MenuItem copyUniqueIdToClipboardMenuItem;

    protected TreeView<Node> treeView;

    protected SimpleBooleanProperty multipleSelection = new SimpleBooleanProperty();

    protected SaveAndRestoreController saveAndRestoreController;

    public ContextMenuBase(SaveAndRestoreController saveAndRestoreController, TreeView<Node> treeView) {
        this.treeView = treeView;
        this.saveAndRestoreController = saveAndRestoreController;
        deleteNodesMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteNodesMenuItem.setOnAction(ae -> saveAndRestoreController.deleteNodes());

        renameNodeMenuItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameNodeMenuItem.setOnAction(ae -> saveAndRestoreController.renameNode());

        copyUniqueIdToClipboardMenuItem = new MenuItem(Messages.copyUniqueIdToClipboard, ImageCache.getImageView(ImageCache.class, "/icons/copy.png"));
        copyUniqueIdToClipboardMenuItem.setOnAction(ae -> saveAndRestoreController.copyUniqueNodeIdToClipboard());

        treeView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<TreeItem<Node>>() {
            @Override
            public void onChanged(Change<? extends TreeItem<Node>> c) {
                multipleSelection.set(treeView.getSelectionModel().getSelectedItems().size() > 1);
            }
        });

        renameNodeMenuItem.disableProperty().bind(multipleSelection);
        copyUniqueIdToClipboardMenuItem.disableProperty().bind(multipleSelection);

        setOnShowing(event -> runChecks());
    }

    /**
     * Applies logic to determine which context menu items to disable as some actions (e.g. rename) do not
     * make sense if multiple items are selected. Special case is if nodes in different parent nodes
     * are selected, in this case none of the menu items make sense, to the context menu is suppressed.
     */
    protected void runChecks() {
        ObservableList<TreeItem<Node>> selected =
                treeView.getSelectionModel().getSelectedItems();
        if (multipleSelection.get() && !isDeletionPossible(selected)) {
            Platform.runLater(this::hide);
        }
    }

    /**
     * Deletion of tree nodes when multiple nodes are selected is allowed only if all of the
     * selected nodes have the same parent node. This method checks the parent node(s) of
     * the selected nodes accordingly.
     *
     * @param selectedItems The selected tree nodes.
     * @return <code>true</code> if all selected nodes have the same parent node, <code>false</code> otherwise.
     */
    protected boolean isDeletionPossible(ObservableList<TreeItem<Node>> selectedItems) {
        Node parentNode = selectedItems.get(0).getParent().getValue();
        for (TreeItem<Node> treeItem : selectedItems) {
            if (!treeItem.getParent().getValue().getUniqueId().equals(parentNode.getUniqueId())) {
                return false;
            }
        }
        return true;
    }
}
