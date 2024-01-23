/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.List;

/**
 * A cell editor managing the different type of nodes in the save-and-restore tree.
 * Implements aspects like icon selection, text layout, context menu and editing.
 */
public class BrowserTreeCell extends TreeCell<Node> {

    private final SaveAndRestoreController saveAndRestoreController;

    private static final Border BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
            new CornerRadii(5.0), BorderStroke.THIN));

    public BrowserTreeCell() {
        this(null);
    }

    public BrowserTreeCell(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;

        // This is need in order to suppress the context menu when right-clicking in a portion of the
        // tree view where no tree items are rendered.
        setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown() && event.getTarget() instanceof TreeCell) {
                TreeCell<Node> treeCell = ((TreeCell<Node>) event.getTarget());
                if (treeCell.getTreeItem() == null) {
                    setContextMenu(null);
                }
            }
        });

        setOnDragDetected(event -> {
            if (saveAndRestoreController.getUserIdentity().isNull().get() || !saveAndRestoreController.selectedNodesOfSameType()) {
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            Node node = getItem();
            // Drag-n-drop not supported for root node
            if (node != null &&
                    !node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                final List<Node> nodes = new ArrayList<>();

                for (TreeItem<Node> sel : getTreeView().getSelectionModel().getSelectedItems()) {
                    nodes.add(sel.getValue());
                }
                content.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, nodes);
                // Only move supported!
                final Dragboard db = startDragAndDrop(TransferMode.MOVE);
                db.setContent(content);
            }
            event.consume();
        });

        setOnDragOver(event ->
        {
            final Node node = getItem();
            if (node != null) {
                List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
                if (DragNDropUtil.mayDrop(event.getTransferMode(), node, sourceNodes)) {
                    event.acceptTransferModes(event.getTransferMode());
                    setBorder(BORDER);
                }
            }
            event.consume();
        });

        setOnDragExited(event ->
        {
            setBorder(null);
            event.consume();
        });

        setOnDragDropped(event ->
        {
            Node targetNode = getItem();
            if (targetNode != null) {
                TransferMode transferMode = event.getTransferMode();
                List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
                if (!DragNDropUtil.mayDrop(transferMode, targetNode, sourceNodes)) {
                    return;
                }
                if (DragNDropUtil.snapshotsOrCompositeSnapshotsOnly(sourceNodes) && targetNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
                    saveAndRestoreController.editCompositeSnapshot(targetNode, sourceNodes);
                } else {
                    getTreeView().getSelectionModel().clearSelection(); // This is needed to help controller implement selection restrictions
                    saveAndRestoreController.moveNodes(sourceNodes, targetNode, transferMode);
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        // This is to suppress expansion of the TreeItem on double-click.
        addEventFilter(MouseEvent.MOUSE_PRESSED, (MouseEvent e) -> {
            if (e.getClickCount() % 2 == 0 && e.getButton().equals(MouseButton.PRIMARY))
                e.consume();
        });
    }

    @Override
    public void updateItem(Node node, boolean empty) {
        super.updateItem(node, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
            setTooltip(null);
            getStyleClass().remove("filter-match");
            return;
        }
        // Use custom layout as this makes it easier to set opacity
        HBox hBox = new HBox();
        // saveAndRestoreController is null if configuration management is from OPI or channel table
        if (saveAndRestoreController != null && !saveAndRestoreController.matchesFilter(node)) {
            hBox.setOpacity(0.4);
        }
        StringBuilder stringBuilder = new StringBuilder();
        String comment = node.getDescription();
        if (comment != null && !comment.isEmpty()) {
            stringBuilder.append(comment).append(System.lineSeparator());
        }
        if (node.getCreated() != null) { // Happens if configuration management is accessed from context menu
            stringBuilder.append(TimestampFormats.SECONDS_FORMAT
                    .format(node.getLastModified() != null ? node.getLastModified().toInstant() : node.getCreated().toInstant())).append(" (").append(node.getUserName()).append(")");
        }
        // Tooltip with at least date and user id is set on all tree items
        setTooltip(new Tooltip(stringBuilder.toString()));
        switch (node.getNodeType()) {
            case SNAPSHOT:
                if (node.hasTag(Tag.GOLDEN)) {
                    hBox.getChildren().add(new ImageView(ImageRepository.GOLDEN_SNAPSHOT));
                } else {
                    hBox.getChildren().add(new ImageView(ImageRepository.SNAPSHOT));
                }
                hBox.getChildren().add(new Label(node.getName()));
                if (node.getTags() != null && !node.getTags().isEmpty()) {
                    ImageView tagImage = new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-tags.png"));
                    tagImage.setFitHeight(13);
                    tagImage.setPreserveRatio(true);
                    hBox.getChildren().add(tagImage);
                }
                if(saveAndRestoreController != null){
                    setContextMenu(new ContextMenuSnapshot(saveAndRestoreController));
                }
                break;
            case COMPOSITE_SNAPSHOT:
                hBox.getChildren().add(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
                hBox.getChildren().add(new Label(node.getName()));
                if (node.getTags() != null && !node.getTags().isEmpty()) {
                    ImageView tagImage = new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-tags.png"));
                    tagImage.setFitHeight(13);
                    tagImage.setPreserveRatio(true);
                    getChildren().add(tagImage);
                }
                if(saveAndRestoreController != null){
                    setContextMenu(new ContextMenuCompositeSnapshot(saveAndRestoreController));
                }
                break;
            case CONFIGURATION:
                hBox.getChildren().add(new ImageView(ImageRepository.CONFIGURATION));
                hBox.getChildren().add(new Label(node.getName()));
                if(saveAndRestoreController != null){
                    setContextMenu(new ContextMenuConfiguration(saveAndRestoreController));
                }
                break;
            case FOLDER:
                hBox.getChildren().add(new ImageView(ImageRepository.FOLDER));
                hBox.getChildren().add(new Label(node.getName()));
                if (node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                    setTooltip(new Tooltip(SaveAndRestoreService.getInstance().getServiceIdentifier()));
                    ContextMenu rootFolderContextMenu = new ContextMenu();
                    MenuItem newRootFolderMenuItem = new MenuItem(Messages.contextMenuNewFolder, new ImageView(ImageRepository.FOLDER));
                    newRootFolderMenuItem.setOnAction(ae -> saveAndRestoreController.createNewFolder());
                    rootFolderContextMenu.getItems().add(newRootFolderMenuItem);
                    rootFolderContextMenu.setOnShowing(event -> {
                        if (saveAndRestoreController.getUserIdentity().isNull().get()) {
                            Platform.runLater(() -> rootFolderContextMenu.hide());
                        }
                    });
                    if(saveAndRestoreController != null){
                        setContextMenu(rootFolderContextMenu);
                    }

                } else if (saveAndRestoreController != null){
                    setContextMenu(new ContextMenuFolder(saveAndRestoreController));
                }
                break;
        }
        setGraphic(hBox);
    }
}
