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
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.ui.javafx.ImageCache;

public class ContextMenuSnapshot extends ContextMenuBase {

    protected Image compareSnapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/compare.png");
    protected Image snapshotTagsWithCommentIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/snapshot-tags.png");
    protected Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");

    public ContextMenuSnapshot(SaveAndRestoreController saveAndRestoreController,
                               SimpleStringProperty toggleGoldenMenuItemText,
                               SimpleObjectProperty<ImageView> toggleGoldenImageViewProperty,
                               TreeView<org.phoebus.applications.saveandrestore.model.Node> treeView) {
        super(saveAndRestoreController, treeView);

        MenuItem compareSnapshotsMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, new ImageView(compareSnapshotIcon));
        compareSnapshotsMenuItem.setOnAction(ae -> saveAndRestoreController.comapreSnapshot());

        MenuItem tagAsGolden = new MenuItem(Messages.contextMenuTagAsGolden, new ImageView(ImageRepository.GOLDEN_SNAPSHOT));
        tagAsGolden.textProperty().bind(toggleGoldenMenuItemText);
        tagAsGolden.graphicProperty().bind(toggleGoldenImageViewProperty);
        tagAsGolden.setOnAction(ae -> saveAndRestoreController.toggleGoldenProperty());

        ImageView snapshotTagsWithCommentIconImage = new ImageView(snapshotTagsWithCommentIcon);
        snapshotTagsWithCommentIconImage.setFitHeight(22);
        snapshotTagsWithCommentIconImage.setFitWidth(22);
        Menu tagWithComment = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);
        tagWithComment.setOnShowing(event -> saveAndRestoreController.tagWithComment(tagWithComment.getItems()));

        CustomMenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.setOnAction(action -> saveAndRestoreController.addTagToSnapshots());

        tagWithComment.getItems().addAll(addTagWithCommentMenuItem, new SeparatorMenuItem());

        MenuItem findReferencesMenuItem = new MenuItem(Messages.findSnapshotReferences, new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
        findReferencesMenuItem.setOnAction(ae -> saveAndRestoreController.findSnapshotReferences());

        ImageView exportSnapshotIconImageView = new ImageView(csvExportIcon);
        exportSnapshotIconImageView.setFitWidth(18);
        exportSnapshotIconImageView.setFitHeight(18);

        MenuItem exportSnapshotMenuItem = new MenuItem(Messages.exportSnapshotLabel, exportSnapshotIconImageView);
        exportSnapshotMenuItem.disableProperty().bind(multipleSelection);
        exportSnapshotMenuItem.setOnAction(ae -> saveAndRestoreController.exportSnapshot());

        getItems().addAll(renameNodeMenuItem,
                deleteNodesMenuItem,
                compareSnapshotsMenuItem,
                tagAsGolden,
                tagWithComment,
                copyUniqueIdToClipboardMenuItem,
                exportSnapshotMenuItem);
    }


    @Override
    protected void runChecks() {
        ObservableList<TreeItem<Node>> selected =
                treeView.getSelectionModel().getSelectedItems();
        // TODO: support multiple selection to allow tagging of multiple snapshots
        if (multipleSelection.get() && !isDeletionPossible(selected)) {
            Platform.runLater(this::hide);
        }
        /*
        if (multipleSelection.get() && !isDeletionPossible(selected)) {
            deleteNodesMenuItem.disableProperty().set(true);
        }
        tagAsGolden.disableProperty().set(multipleSelection.getValue());
        compareSnapshotsMenuItem.disableProperty().set(!compareSnapshotsPossible());

         */
    }

    /**
     * Determines if comparing snapshots is possible, which is the case if all of the following holds true:
     * <ul>
     *     <li>The active tab must be a {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab} must not show an unsaved snapshot.</li>
     *     <li>The snapshot selected from the tree view must have same parent as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     *     <li>The snapshot selected from the tree view must not be the same as as the one shown in the active {@link org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab}</li>
     * </ul>
     * @return <code>true</code> if selection can be added to snapshot view for comparison.
     */
    private boolean compareSnapshotsPossible() {
        Node[] configAndSnapshotNode = saveAndRestoreController.getConfigAndSnapshotForActiveSnapshotTab();
        if(configAndSnapshotNode == null){
            return false;
        }
        TreeItem<Node> selectedItem = treeView.getSelectionModel().getSelectedItem();
        TreeItem<Node> parentItem = selectedItem.getParent();
        return configAndSnapshotNode[1].getUniqueId() != null &&
                parentItem.getValue().getUniqueId().equals(configAndSnapshotNode[0].getUniqueId()) &&
                !selectedItem.getValue().getUniqueId().equals(configAndSnapshotNode[1].getUniqueId());
    }
}
