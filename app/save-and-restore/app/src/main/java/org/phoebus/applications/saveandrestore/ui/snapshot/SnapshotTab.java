/**
 * Copyright (C) 2024 European Spallation Source ERIC.
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
package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.applications.saveandrestore.ui.WebSocketMessageHandler;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Tab} subclass showing a view for the purpose of creating or restoring a snapshot.
 * These two use cases/views are split in terms of fxml files and controller classes in order to facilitate development
 * and maintenance.
 *
 * <p>
 * Note that this class is used also to show the snapshot view for {@link Node}s of type {@link NodeType#COMPOSITE_SNAPSHOT}.
 * </p>
 */
public class SnapshotTab extends SaveAndRestoreTab implements WebSocketMessageHandler {

    public SaveAndRestoreService saveAndRestoreService;

    protected Image compareSnapshotIcon = ImageCache.getImage(SaveAndRestoreController.class, "/icons/save-and-restore/compare.png");

    public SnapshotTab(org.phoebus.applications.saveandrestore.model.Node node, SaveAndRestoreService saveAndRestoreService) {

        this.saveAndRestoreService = saveAndRestoreService;

        if (node.getNodeType().equals(NodeType.SNAPSHOT)) {
            setId(node.getUniqueId());
        }

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(SnapshotTab.class.getResource("SnapshotView.fxml"));

        loader.setControllerFactory(clazz -> {
            try {
                if (clazz.isAssignableFrom(SnapshotController.class)) {
                    return clazz.getConstructor(SnapshotTab.class)
                            .newInstance(this);
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError("Error",
                        "Failed to open new snapshot tab", e);
            }
            return null;
        });

        try {
            setContent(loader.load());
            controller = loader.getController();
        } catch (IOException e) {
            Logger.getLogger(SnapshotTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        MenuItem compareSnapshotToArchiverDataMenuItem = new MenuItem(Messages.contextMenuCompareSnapshotWithArchiverData, new ImageView(compareSnapshotIcon));
        compareSnapshotToArchiverDataMenuItem.setOnAction(ae -> addSnapshotFromArchive());

        getContextMenu().setOnShowing(e -> {
            Snapshot snapshot = ((SnapshotController) controller).getSnapshot();
            if (snapshot.getSnapshotData().getSnapshotItems().isEmpty()) {
                compareSnapshotToArchiverDataMenuItem.disableProperty().set(true);
            }
            compareSnapshotToArchiverDataMenuItem.disableProperty().set(snapshot.getSnapshotNode().getUniqueId() == null);
        });
        getContextMenu().getItems().add(compareSnapshotToArchiverDataMenuItem);
    }

    /**
     * Loads and configures a view for the use case of taking a new snapshot.
     *
     * @param configurationNode The {@link Node} of type {@link NodeType#CONFIGURATION} listing PVs for which
     *                          a snapshot will be created.
     */
    public void newSnapshot(org.phoebus.applications.saveandrestore.model.Node configurationNode) {
        ((SnapshotController) controller).initializeViewForNewSnapshot(configurationNode);
    }

    /**
     * Loads and configures a view for the use case of restoring a snapshot.
     *
     * @param snapshotNode The {@link Node} of type {@link NodeType#SNAPSHOT} containing snapshot data.
     */
    public void loadSnapshot(Node snapshotNode) {
        ((SnapshotController) controller).loadSnapshot(snapshotNode);
    }

    public void addSnapshot(Node node) {
        ((SnapshotController) controller).addSnapshot(node);
    }

    private void addSnapshotFromArchive() {
        ((SnapshotController) controller).addSnapshotFromArchiver();
    }

    public Node getSnapshotNode() {
        return ((SnapshotController) controller).getSnapshot().getSnapshotNode();
    }

    public Node getConfigNode() {
        return ((SnapshotController) controller).getConfigurationNode();
    }

    @Override
    public void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage) {
        if (saveAndRestoreWebSocketMessage.messageType().equals(MessageType.NODE_REMOVED)) {
            String nodeId = (String) saveAndRestoreWebSocketMessage.payload();
            if (getId() != null && nodeId.equals(getId())) {
                Platform.runLater(() -> getTabPane().getTabs().remove(this));
            }
        }
    }
}
