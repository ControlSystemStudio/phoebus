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

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ContentDisplay;
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
import javafx.scene.paint.Color;
import org.phoebus.applications.saveandrestore.SaveAndRestoreApplication;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.javafx.PlatformInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A cell editor managing the different type of nodes in the save-and-restore tree.
 * Implements aspects like icon selection, text layout, context menu and editing.
 */
public class BrowserTreeCell extends TreeCell<Node> {

    private javafx.scene.Node folderBox;
    private javafx.scene.Node saveSetBox;
    private javafx.scene.Node snapshotBox;

    private ContextMenu folderContextMenu;
    private ContextMenu saveSetContextMenu;
    private ContextMenu snapshotContextMenu;
    private ContextMenu rootFolderContextMenu;

    private static final Border BORDER = new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID,
            new CornerRadii(5.0), BorderStroke.THIN));

    public BrowserTreeCell(ContextMenu folderContextMenu, ContextMenu saveSetContextMenu,
                           ContextMenu snapshotContextMenu, ContextMenu rootFolderContextMenu,
                           SaveAndRestoreController saveAndRestoreCotroller) {

        FXMLLoader loader = new FXMLLoader();

        try {
            loader.setLocation(BrowserTreeCell.class.getResource("TreeCellGraphic.fxml"));
            javafx.scene.Node rootNode = loader.load();
            folderBox = rootNode.lookup("#folder");
            saveSetBox = rootNode.lookup("#saveset");
            snapshotBox = rootNode.lookup("#snapshot");

        } catch (IOException e) {
            Logger.getLogger(BrowserTreeCell.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml.");
        }

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        this.folderContextMenu = folderContextMenu;
        this.saveSetContextMenu = saveSetContextMenu;
        this.snapshotContextMenu = snapshotContextMenu;
        this.rootFolderContextMenu = rootFolderContextMenu;

        setOnDragDetected(event -> {
            if(!saveAndRestoreCotroller.checkMultipleSelection()){
                return;
            }
            final ClipboardContent content = new ClipboardContent();
            Node node = getItem();
            // Drag-n-drop not supported for root node and snapshot nodes
            if (node != null && !node.getNodeType().equals(NodeType.SNAPSHOT) && !node.getName().equals("Root folder")) {
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
                if(sourceNodes.contains(targetNode)){
                    return;
                }
                TransferMode transferMode = event.getTransferMode();
                getTreeView().getSelectionModel().clearSelection(); // This is needed to help controller implement selection restrictions
                saveAndRestoreCotroller.performCopyOrMove(sourceNodes, targetNode, transferMode);
            }
            event.setDropCompleted(true);
            event.consume();
        });

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

        switch (node.getNodeType()) {
            case SNAPSHOT:
                ((Label) snapshotBox.lookup("#primaryLabel"))
                        .setText(node.getName());
                ((Label) snapshotBox.lookup("#secondaryLabel")).setText(node.getCreated() + " (" + node.getUserName() + ")");
                snapshotBox.lookup("#tagIcon").setVisible(node.getTags() != null && !node.getTags().isEmpty());
                setGraphic(snapshotBox);
                if (node.getProperty("golden") != null && Boolean.valueOf(node.getProperty("golden"))) {
                    ((ImageView) snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot-golden.png"));
                } else {
                    ((ImageView) snapshotBox.lookup("#snapshotIcon")).setImage(ImageCache.getImage(BrowserTreeCell.class, "/icons/save-and-restore/snapshot.png"));
                }
                setContextMenu(snapshotContextMenu);
                String comment = node.getProperty("comment");
                if (comment != null && !comment.isEmpty()) {
                    setTooltip(new Tooltip(comment));
                }
                setEditable(false);
                break;
            case CONFIGURATION:
                ((Label) saveSetBox.lookup("#savesetLabel")).setText(node.getName());
                setGraphic(saveSetBox);
                String description = node.getProperty("description");
                if (description != null && !description.isEmpty()) {
                    setTooltip(new Tooltip(description));
                }
                setContextMenu(saveSetContextMenu);
                break;
            case FOLDER:
                String labelText = node.getName();
                if (node.getProperty("root") != null && Boolean.valueOf(node.getProperty("root"))) {
                    setContextMenu(rootFolderContextMenu);
                } else {
                    setContextMenu(folderContextMenu);
                }
                ((Label) folderBox.lookup("#folderLabel")).setText(labelText);
                setGraphic(folderBox);
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
