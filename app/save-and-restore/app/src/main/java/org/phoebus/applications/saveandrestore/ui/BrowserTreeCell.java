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

    private static final Border BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
            new CornerRadii(5.0), BorderStroke.THIN));

    public BrowserTreeCell(){
        this(null, null, null, null, null, null);
    }

    public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu configurationContextMenu,
                           ContextMenu snapshotContextMenu, ContextMenu rootFolderContextMenu,
                           ContextMenu compositeSnapshotContextMenu,
                           SaveAndRestoreController saveAndRestoreCotroller) {
        this.folderContextMenu = folderContextMenu;
        this.configurationContextMenu = configurationContextMenu;
        this.snapshotContextMenu = snapshotContextMenu;
        this.rootFolderContextMenu = rootFolderContextMenu;
        this.compositeSnapshotContextMenu = compositeSnapshotContextMenu;

        setOnDragDetected(event -> {
            if (!saveAndRestoreCotroller.checkMultipleSelection()) {
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
                saveAndRestoreCotroller.performCopyOrMove(sourceNodes, targetNode, transferMode);
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
            setText(null);
            setContextMenu(null);
            return;
        }
        setEditable(false);
        switch (node.getNodeType()) {
            case SNAPSHOT:
                setText(null); // Must be set to null since text is handled in custom layout.
                HBox hBox = new HBox();
                if (node.hasTag(Tag.GOLDEN)) {
                    hBox.getChildren().add(new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-golden.png")));
                } else {
                    hBox.getChildren().add(new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot.png")));
                }
                hBox.getChildren().add(new Label(node.getName()));
                if (node.getTags() != null && !node.getTags().isEmpty()) {
                    ImageView tagImage = new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-tags.png"));
                    tagImage.setFitHeight(13);
                    tagImage.setPreserveRatio(true);
                    hBox.getChildren().add(tagImage);
                }
                setGraphic(hBox);
                setContextMenu(snapshotContextMenu);
                String comment = node.getDescription();
                StringBuffer stringBuffer = new StringBuffer();
                if (comment != null && !comment.isEmpty()) {
                    stringBuffer.append(comment).append(System.lineSeparator());
                }
                stringBuffer.append(node.getCreated()).append(" (").append(node.getUserName()).append(")");
                setTooltip(new Tooltip(stringBuffer.toString()));
                break;
            case COMPOSITE_SNAPSHOT:
                setText(node.getName());
                setGraphic(new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/composite-snapshot.png")));
                if (node.getDescription() != null && !node.getDescription().isEmpty()) {
                    setTooltip(new Tooltip(node.getDescription()));
                }
                setContextMenu(compositeSnapshotContextMenu);
                break;
            case CONFIGURATION:
                setText(node.getName());
                setGraphic(new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/configuration.png")));
                String description = node.getDescription();
                if (description != null && !description.isEmpty()) {
                    setTooltip(new Tooltip(description));
                }
                setContextMenu(configurationContextMenu);
                break;
            case FOLDER:
                if (node.getUniqueId().equals(Node.ROOT_FOLDER_UNIQUE_ID)) {
                    setContextMenu(rootFolderContextMenu);
                } else {
                    setContextMenu(folderContextMenu);
                }
                setText(node.getName());
                setGraphic(new ImageView(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/folder.png")));
                break;
        }
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
