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
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.ui.snapshot.tag.TagWidget;
import org.phoebus.ui.javafx.ImageCache;

/**
 * Context menu for {@link org.phoebus.applications.saveandrestore.model.Node}s of type
 * {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}.
 */
public class ContextMenuSnapshot extends ContextMenuBase {

    protected Image compareSnapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/compare.png");
    protected Image csvExportIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/csv_export.png");


    private final MenuItem tagGoldenMenuItem;

    private final Menu tagWithComment;

    private final SimpleBooleanProperty mayTagProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty mayCompareSnapshotsProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty mayTagOrUntagGoldenProperty = new SimpleBooleanProperty();

    public ContextMenuSnapshot(SaveAndRestoreController saveAndRestoreController) {
        super(saveAndRestoreController);

        MenuItem compareSnapshotsMenuItem = new MenuItem(Messages.contextMenuCompareSnapshots, new ImageView(compareSnapshotIcon));
        compareSnapshotsMenuItem.setOnAction(ae -> saveAndRestoreController.compareSnapshot());
        compareSnapshotsMenuItem.disableProperty().bind(mayCompareSnapshotsProperty.not());

        ImageView snapshotTagsWithCommentIconImage = new ImageView(ImageRepository.SNAPSHOT_ADD_TAG_WITH_COMMENT);

        tagWithComment = new Menu(Messages.contextMenuTagsWithComment, snapshotTagsWithCommentIconImage);
        tagWithComment.setOnShowing(event -> saveAndRestoreController.tagWithComment(tagWithComment));
        tagWithComment.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty));

        MenuItem addTagWithCommentMenuItem = TagWidget.AddTagWithCommentMenuItem();
        addTagWithCommentMenuItem.setOnAction(action -> saveAndRestoreController.addTagToSnapshots());
        addTagWithCommentMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || mayTagProperty.not().get(),
                multipleNodesSelectedProperty, mayTagProperty));

        tagWithComment.getItems().addAll(addTagWithCommentMenuItem);

        MenuItem findReferencesMenuItem = new MenuItem(Messages.findSnapshotReferences, new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
        findReferencesMenuItem.setOnAction(ae -> saveAndRestoreController.findSnapshotReferences());

        ImageView exportSnapshotIconImageView = new ImageView(csvExportIcon);
        exportSnapshotIconImageView.setFitWidth(18);
        exportSnapshotIconImageView.setFitHeight(18);

        MenuItem exportSnapshotMenuItem = new MenuItem(Messages.exportSnapshotLabel, exportSnapshotIconImageView);
        exportSnapshotMenuItem.disableProperty().bind(multipleNodesSelectedProperty);
        exportSnapshotMenuItem.setOnAction(ae -> saveAndRestoreController.exportSnapshot());

        tagGoldenMenuItem = new MenuItem(Messages.contextMenuTagAsGolden, new ImageView(ImageRepository.SNAPSHOT));
        tagGoldenMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                        multipleNodesSelectedProperty.get() || userIsAuthenticatedProperty.not().get() || mayTagOrUntagGoldenProperty.not().get(),
                multipleNodesSelectedProperty, userIsAuthenticatedProperty, mayTagOrUntagGoldenProperty));

        Image copyIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/copy.png");
        MenuItem copyMenuItem = new MenuItem(Messages.copy, new ImageView(copyIcon));
        copyMenuItem.setOnAction(action -> saveAndRestoreController.copySelectionToClipboard());
        copyMenuItem.disableProperty().bind(mayCopyProperty.not());

        getItems().addAll(deleteNodesMenuItem,
                compareSnapshotsMenuItem,
                tagGoldenMenuItem,
                tagWithComment,
                copyMenuItem,
                copyUniqueIdToClipboardMenuItem,
                exportSnapshotMenuItem);
    }


    /**
     * Execute common checks (see {@link ContextMenuBase#runChecks()}) and:
     * <ul>
     *     <li>If tagging is possible on selected {@link org.phoebus.applications.saveandrestore.model.Node}s</li>
     *     <li>If comparing snapshots is possible</li>
     *     <li>If setting/unsetting golden tag is possible</li>
     * </ul>
     */
    @Override
    protected void runChecks() {
        super.runChecks();
        mayTagProperty.set(saveAndRestoreController.checkTaggable());
        mayCompareSnapshotsProperty.set(saveAndRestoreController.compareSnapshotsPossible());
        mayTagOrUntagGoldenProperty.set(saveAndRestoreController.configureGoldenItem(tagGoldenMenuItem));
    }
}
