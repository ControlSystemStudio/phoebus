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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.ui.javafx.ImageCache;

public class ContextMenuSnapshot extends ContextMenuBase {

    protected Image compareSnapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/compare.png");
    protected Image snapshotTagsWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tags.png");
    protected Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");


    public ContextMenuSnapshot(SaveAndRestoreController saveAndRestoreController,
                               boolean csvEnabled,
                               SimpleStringProperty toggleGoldenMenuItemText,
                               SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty,
                               SimpleBooleanProperty multipleItemsSelected) {
        super(saveAndRestoreController, multipleItemsSelected);

        MenuItem deleteSnapshotMenuItem = new MenuItem(Messages.contextMenuDelete, new ImageView(deleteIcon));
        deleteSnapshotMenuItem.setOnAction(ae -> {
            saveAndRestoreController.deleteSnapshots();
        });

        MenuItem renameSnapshotItem = new MenuItem(Messages.contextMenuRename, new ImageView(renameIcon));
        renameSnapshotItem.disableProperty().bind(multipleItemsSelected);
        renameSnapshotItem.setOnAction(ae -> {
            saveAndRestoreController.renameNode();
        });

        MenuItem compareConfigurationMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, new ImageView(compareSnapshotIcon));
        compareConfigurationMenuItem.setOnAction(ae -> {
            saveAndRestoreController.comapreSnapshot();
        });

        MenuItem tagAsGolden = new javafx.scene.control.MenuItem(Messages.contextMenuTagAsGolden, new ImageView(ImageRepository.GOLDEN_SNAPSHOT));
        tagAsGolden.textProperty().bind(toggleGoldenMenuItemText);
        tagAsGolden.graphicProperty().bind(toggleGoldenImageViewProperty);
        tagAsGolden.disableProperty().bind(multipleItemsSelected);
        tagAsGolden.setOnAction(ae -> {
            saveAndRestoreController.toggleGoldenProperty();
        });

        ImageView snapshotTagsWithCommentIconImage = new ImageView(snapshotTagsWithCommentIcon);
        snapshotTagsWithCommentIconImage.setFitHeight(22);
        snapshotTagsWithCommentIconImage.setFitWidth(22);
        Menu tagWithComment = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);
        tagWithComment.disableProperty().bind(multipleItemsSelected);
        tagWithComment.setOnShowing(event -> {
            saveAndRestoreController.tagWithComment(tagWithComment.getItems());
        });

        CustomMenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.disableProperty().bind(multipleItemsSelected);
        addTagWithCommentMenuItem.setOnAction(action -> {
            saveAndRestoreController.addTagToSnapshot();
        });

        tagWithComment.getItems().addAll(addTagWithCommentMenuItem, new SeparatorMenuItem());

        MenuItem findReferencesMenuItem = new MenuItem(Messages.findSnapshotReferences, new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
        findReferencesMenuItem.setOnAction(ae -> {
            saveAndRestoreController.findSnapshotReferences();
        });

        getItems().addAll(renameSnapshotItem,
                deleteSnapshotMenuItem,
                compareConfigurationMenuItem,
                tagAsGolden,
                tagWithComment,
                copyUniqueIdToClipboardMenuItem/*,
                findReferencesMenuItem*/);


        if (csvEnabled) {
            ImageView exportSnapshotIconImageView = new ImageView(csvExportIcon);
            exportSnapshotIconImageView.setFitWidth(18);
            exportSnapshotIconImageView.setFitHeight(18);

            MenuItem exportSnapshotMenuItem = new MenuItem(Messages.exportSnapshotLabel, exportSnapshotIconImageView);
            exportSnapshotMenuItem.disableProperty().bind(multipleItemsSelected);
            exportSnapshotMenuItem.setOnAction(ae -> {
                saveAndRestoreController.exportSnapshot();
            });
            getItems().add(exportSnapshotMenuItem);
        }
    }
}
