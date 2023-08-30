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

import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.ui.javafx.ImageCache;

public class ContextMenuCompositeSnapshot extends ContextMenuBase {

    public ContextMenuCompositeSnapshot(SaveAndRestoreController saveAndRestoreController, TreeView<Node> treeView) {
        super(saveAndRestoreController, treeView);

        Image snapshotTagsWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tags.png");

        MenuItem editCompositeSnapshotMenuItem = new MenuItem(Messages.Edit, new ImageView(ImageRepository.EDIT_CONFIGURATION));
        editCompositeSnapshotMenuItem.disableProperty().bind(multipleSelection);
        editCompositeSnapshotMenuItem.setOnAction(ae ->
                saveAndRestoreController.editCompositeSnapshot());

        ImageView snapshotTagsWithCommentIconImage = new ImageView(snapshotTagsWithCommentIcon);
        snapshotTagsWithCommentIconImage.setFitHeight(22);
        snapshotTagsWithCommentIconImage.setFitWidth(22);

        Menu tagWithComment = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);
        tagWithComment.setOnShowing(event -> saveAndRestoreController.tagWithComment(tagWithComment));

        CustomMenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.setOnAction(action -> saveAndRestoreController.addTagToSnapshots());

        tagWithComment.getItems().addAll(addTagWithCommentMenuItem, new SeparatorMenuItem());

        Image copyIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/copy.png");
        MenuItem copyMenuItem = new MenuItem(Messages.copy, new ImageView(copyIcon));
        copyMenuItem.setOnAction(action -> saveAndRestoreController.copySelectionToClipboard());

        setOnShowing(event -> {
            copyMenuItem.setDisable(!saveAndRestoreController.mayCopy());
        });

        getItems().addAll(editCompositeSnapshotMenuItem,
                copyMenuItem,
                deleteNodesMenuItem,
                copyUniqueIdToClipboardMenuItem,
                tagWithComment);
    }
}
