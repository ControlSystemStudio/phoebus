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
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Context menu for {@link org.phoebus.applications.saveandrestore.model.Node}s of type
 * {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}.
 */
public class ContextMenuCompositeSnapshot extends ContextMenuBase {

    public ContextMenuCompositeSnapshot(SaveAndRestoreController saveAndRestoreController) {
        super(saveAndRestoreController);

        Image snapshotTagsIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tags.png");

        MenuItem editCompositeSnapshotMenuItem = new MenuItem(Messages.Edit, new ImageView(ImageRepository.EDIT_CONFIGURATION));
        editCompositeSnapshotMenuItem.disableProperty().bind(multipleNodesSelectedProperty);
        editCompositeSnapshotMenuItem.setOnAction(ae ->
                saveAndRestoreController.editCompositeSnapshot());
        editCompositeSnapshotMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                userIsAuthenticatedProperty.not().get() || multipleNodesSelectedProperty.get(),
                userIsAuthenticatedProperty, multipleNodesSelectedProperty));

        ImageView snapshotTagsIconImage = new ImageView(snapshotTagsIcon);
        snapshotTagsIconImage.setFitHeight(22);
        snapshotTagsIconImage.setFitWidth(22);

        Menu tags = new Menu(Messages.contextMenuTags, snapshotTagsIconImage);
        tags.setOnShowing(event -> saveAndRestoreController.tag(tags));
        tags.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty));

        CustomMenuItem addTagMenuItem = TagWidget.AddTagMenuItem();
        addTagMenuItem.setOnAction(action -> saveAndRestoreController.addTagToSnapshots());

        tags.getItems().addAll(addTagMenuItem, new SeparatorMenuItem());

        Image copyIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/copy.png");
        MenuItem copyMenuItem = new MenuItem(Messages.copy, new ImageView(copyIcon));
        copyMenuItem.setOnAction(action -> saveAndRestoreController.copySelectionToClipboard());
        copyMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        userIsAuthenticatedProperty.not().get() || mayCopyProperty.not().get(),
                userIsAuthenticatedProperty, mayCopyProperty));

        getItems().addAll(
                loginMenuItem,
                editCompositeSnapshotMenuItem,
                copyMenuItem,
                deleteNodesMenuItem,
                copyUniqueIdToClipboardMenuItem,
                tags);
    }

    /**
     * Execute common checks (see {@link ContextMenuBase#runChecks()}) and:
     * <ul>
     *     <li>If copy operation is possible on selected {@link org.phoebus.applications.saveandrestore.model.Node}s</li>
     * </ul>
     */
    @Override
    public void runChecks() {
        super.runChecks();
        mayCopyProperty.set(saveAndRestoreController.mayCopy());
    }
}
