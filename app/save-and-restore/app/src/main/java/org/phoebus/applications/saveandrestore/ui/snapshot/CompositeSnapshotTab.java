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
 *
 */

package org.phoebus.applications.saveandrestore.ui.snapshot;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab for creating or editing composite snapshots,
 * i.e. for node type {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}.
 *
 * <p>
 *     Note that this class is only for editing of {@link Node}s of type {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}.
 *     {@link SnapshotTab} is used to show actual snapshot data.
 * </p>
 */
public class CompositeSnapshotTab extends SaveAndRestoreTab {

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty(Messages.contextMenuNewCompositeSnapshot);

    private final SaveAndRestoreController saveAndRestoreController;

    public CompositeSnapshotTab(SaveAndRestoreController saveAndRestoreController) {
        this.saveAndRestoreController = saveAndRestoreController;
        configure();
    }

    private void configure() {
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(SnapshotTab.class.getResource("CompositeSnapshotEditor.fxml"));

        loader.setControllerFactory(clazz -> {
            try {
                if (clazz.isAssignableFrom(CompositeSnapshotController.class)) {
                    return clazz.getConstructor(CompositeSnapshotTab.class, SaveAndRestoreController.class)
                            .newInstance(this, saveAndRestoreController);
                }
            } catch (Exception e) {
                ExceptionDetailsErrorDialog.openError("Error",
                        "Failed to open composite snapshot tab", e);
            }
            return null;
        });

        javafx.scene.Node rootNode;
        try {
            rootNode = loader.load();
        } catch (IOException e) {
            Logger.getLogger(SnapshotTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        controller = loader.getController();

        setContent(rootNode);
        setGraphic(new ImageView(ImageRepository.COMPOSITE_SNAPSHOT));
        textProperty().bind(tabTitleProperty);

        setOnCloseRequest(event -> {
            if (!((CompositeSnapshotController) controller).handleCompositeSnapshotTabClosed()) {
                event.consume();
            }
        });
    }

    public void setNodeName(String nodeName) {
        Platform.runLater(() -> tabTitleProperty.set("[" + Messages.Edit + "] " + nodeName));
    }

    public void annotateDirty(boolean dirty) {
        String tabTitle = tabTitleProperty.get();
        if (dirty) {
            Platform.runLater(() -> tabTitleProperty.set("* " + tabTitle));
        }
    }

    public void configureForNewCompositeSnapshot(Node parentNode, List<Node> snapshotNodes) {
        tabTitleProperty.set(Messages.contextMenuNewCompositeSnapshot);
        ((CompositeSnapshotController) controller).newCompositeSnapshot(parentNode, snapshotNodes);
    }

    /**
     * Configures UI to edit an existing composite snapshot {@link Node}
     *
     * @param compositeSnapshotNode non-null configuration {@link Node}
     * @param snapshotNodes         A potentially empty (but non-null) list of snapshot nodes that should
     *                              be added to the list of references snapshots.
     */
    public void editCompositeSnapshot(Node compositeSnapshotNode, List<Node> snapshotNodes) {
        setId("edit_" + compositeSnapshotNode.getUniqueId());
        setNodeName(compositeSnapshotNode.getName());
        ((CompositeSnapshotController) controller).loadCompositeSnapshot(compositeSnapshotNode, snapshotNodes);
    }

    /**
     * Adds additional snapshot nodes to an existing composite snapshot.
     *
     * @param snapshotNodes Potentially empty (but non-null) list of snapshot nodes to include into
     *                      the composite snapshot.
     */
    public void addToCompositeSnapshot(List<Node> snapshotNodes) {
        ((CompositeSnapshotController) controller).addToCompositeSnapshot(snapshotNodes);
    }

    @Override
    public void nodeChanged(Node node) {

    }
}
