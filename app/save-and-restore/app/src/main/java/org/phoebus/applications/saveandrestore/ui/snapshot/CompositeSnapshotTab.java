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
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tab for creating or editing composite snapshots,
 * i.e. for node type {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}.
 */
public class CompositeSnapshotTab extends Tab implements NodeChangedListener {

    private CompositeSnapshotController compositeSnapshotController;

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty(Messages.contextMenuNewCompositeSnapshot);

    private SaveAndRestoreController saveAndRestoreController;

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

        compositeSnapshotController = loader.getController();

        setContent(rootNode);
        setGraphic(getTabGraphic());

        setOnCloseRequest(event -> {
            if (!compositeSnapshotController.handleCompositeSnapshotTabClosed()) {
                event.consume();
            } else {
                SaveAndRestoreService.getInstance().removeNodeChangeListener(this);
            }
        });

        SaveAndRestoreService.getInstance().addNodeChangeListener(this);
    }

    public void updateTabTitle(String name) {
        Platform.runLater(() -> tabTitleProperty.set(name));
    }

    private javafx.scene.Node getTabGraphic() {
        HBox container = new HBox();
        ImageView imageView = new ImageView(ImageRepository.COMPOSITE_SNAPSHOT);
        Label label = new Label("");
        label.textProperty().bindBidirectional(tabTitleProperty);
        HBox.setMargin(label, new Insets(1, 5, 0, 3));
        HBox.setMargin(imageView, new Insets(1, 2, 0, 3));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    public void configureForNewCompositeSnapshot(Node parentNode) {
        compositeSnapshotController.newCompositeSnapshot(parentNode);
    }

    /**
     * Configures UI to edit an existing composite snapshot {@link Node}
     *
     * @param compositeSnapshotNode non-null configuration {@link Node}
     */
    public void editCompositeSnapshot(Node compositeSnapshotNode) {
        setId(compositeSnapshotNode.getUniqueId());
        tabTitleProperty.set(compositeSnapshotNode.getName());
        compositeSnapshotController.loadCompositeSnapshot(compositeSnapshotNode);
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            // May be called by non-UI thread
            Platform.runLater(() -> tabTitleProperty.set(node.getName()));
        }
    }
}
