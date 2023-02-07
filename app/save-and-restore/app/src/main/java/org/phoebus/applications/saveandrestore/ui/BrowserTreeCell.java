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

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.util.time.TimestampFormats;

import java.util.ArrayList;
import java.util.List;


/**
 * A cell editor managing the different type of nodes in the save-and-restore tree.
 * Implements aspects like icon selection, text layout, context menu and editing.
 */
public class BrowserTreeCell extends TreeCell<Node> {

    private final ContextMenu folderContextMenu;
    private final ContextMenu configurationContextMenu;
    private final ContextMenu snapshotContextMenu;
    private final ContextMenu rootFolderContextMenu;
    private final ContextMenu compositeSnapshotContextMenu;
    private final SaveAndRestoreController saveAndRestoreController;

    private static final Background HIGHLIGHT_BACKGROUND =
            new Background(new BackgroundFill(Color.rgb(242, 242, 242), CornerRadii.EMPTY, new Insets(-3,0,-3,0)));


    private static final Border BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
            new CornerRadii(5.0), BorderStroke.THIN));

    public BrowserTreeCell() {
        this(null, null, null, null, null, null);
    }

    public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu configurationContextMenu,
                           ContextMenu snapshotContextMenu, ContextMenu rootFolderContextMenu,
                           ContextMenu compositeSnapshotContextMenu,
                           SaveAndRestoreController saveAndRestoreController) {
        this.folderContextMenu = folderContextMenu;
        this.configurationContextMenu = configurationContextMenu;
        this.snapshotContextMenu = snapshotContextMenu;
        this.rootFolderContextMenu = rootFolderContextMenu;
        this.compositeSnapshotContextMenu = compositeSnapshotContextMenu;
        this.saveAndRestoreController = saveAndRestoreController;

        setOnDragDetected(event -> {
            if (!saveAndRestoreController.checkMultipleSelection()) {
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            Node node = getItem();
            // Drag-n-drop not supported for root node
            if (node != null && !node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                final List<Node> nodes = new ArrayList<>();

                for (TreeItem<Node> sel : getTreeView().getSelectionModel().getSelectedItems()) {
                    nodes.add(sel.getValue());
                }
                content.put(SaveAndRestoreApplication.NODE_SELECTION_FORMAT, nodes);
                final Dragboard db = startDragAndDrop(getTransferMode(event));
                db.setContent(content);
            }
            event.consume();
        });

        setOnDragOver(event ->
        {
            final Node node = getItem();
            if (node != null && node.getNodeType().equals(NodeType.FOLDER)) {
                event.acceptTransferModes(event.getTransferMode());
                setBorder(BORDER);
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
                List<Node> sourceNodes = (List<Node>) event.getDragboard().getContent(SaveAndRestoreApplication.NODE_SELECTION_FORMAT);
                // If the drop target is contained in the selection, return silently...
                if (!mayDrop(targetNode, sourceNodes)) {
                    return;
                }
                // If selection contains a snapshot or composite snapshot node, return silently...
                TransferMode transferMode = event.getTransferMode();
                getTreeView().getSelectionModel().clearSelection(); // This is needed to help controller implement selection restrictions
                saveAndRestoreController.performCopyOrMove(sourceNodes, targetNode, transferMode);
            }
            event.setDropCompleted(true);
            event.consume();
        });
    }

    private boolean mayDrop(Node targetNode, List<Node> sourceNodes) {
        if (sourceNodes.contains(targetNode)) {
            return false;
        }
        if (sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.SNAPSHOT) ||
                n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)).findFirst().isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    public void updateItem(Node node, boolean empty) {
        super.updateItem(node, empty);
        if (empty) {
            setGraphic(null);
            getStyleClass().remove("filter-match");
            return;
        }
        // Use custom layout as this makes it easier to set opacity
        HBox hBox = new HBox();
        if (saveAndRestoreController.matchesFilter(node)) {
            getStyleClass().add("filter-match");
        }
        else{
            getStyleClass().remove("filter-match");
        }
        StringBuffer stringBuffer = new StringBuffer();
        String comment = node.getDescription();
        if (comment != null && !comment.isEmpty()) {
            stringBuffer.append(comment).append(System.lineSeparator());
        }
        stringBuffer.append(TimestampFormats.SECONDS_FORMAT.format(node.getCreated().toInstant())).append(" (").append(node.getUserName()).append(")");
        // Tooltip with at least date and user id is set on all tree items
        setTooltip(new Tooltip(stringBuffer.toString()));
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
                setContextMenu(snapshotContextMenu);
                break;
            case COMPOSITE_SNAPSHOT:
                hBox.getChildren().add(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
                hBox.getChildren().add(new Label(node.getName()));
                setContextMenu(compositeSnapshotContextMenu);
                break;
            case CONFIGURATION:
                hBox.getChildren().add(new ImageView(ImageRepository.CONFIGURATION));
                hBox.getChildren().add(new Label(node.getName()));
                setContextMenu(configurationContextMenu);
                break;
            case FOLDER:
                String name = node.getName();
                if (node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                    setContextMenu(rootFolderContextMenu);
                    name += " (" + SaveAndRestoreService.getInstance().getServiceIdentifier() +")";
                } else {
                    setContextMenu(folderContextMenu);
                }
                hBox.getChildren().add(new ImageView(ImageRepository.FOLDER));
                hBox.getChildren().add(new Label(name));
                break;
        }
        setGraphic(hBox);
    }

    /**
     * Determines the {@link TransferMode} based on the state of the modifier key.
     * This method must consider the
     * operating system as the identity of the modifier key varies (alt/option on Mac OS, ctrl on the rest).
     *
     * @param event The mouse event containing information on key press.
     * @return {@link TransferMode#COPY} if modifier key is pressed, otherwise {@link TransferMode#MOVE}.
     */
    private TransferMode getTransferMode(MouseEvent event) {
        if (event.isControlDown() && (PlatformInfo.is_linux || PlatformInfo.isWindows || PlatformInfo.isUnix)) {
            return TransferMode.COPY;
        } else if (event.isAltDown() && PlatformInfo.is_mac_os_x) {
            return TransferMode.COPY;
        }
        return TransferMode.MOVE;
    }
}
