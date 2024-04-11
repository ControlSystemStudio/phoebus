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
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.framework.nls.NLS;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

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
 *     Note that this class is used also to show the snapshot view for {@link Node}s of type {@link NodeType#COMPOSITE_SNAPSHOT}.
 * </p>
 */
public class SnapshotTab extends SaveAndRestoreTab {

    public SaveAndRestoreService saveAndRestoreService;

    private final SimpleObjectProperty<Image> tabGraphicImageProperty = new SimpleObjectProperty<>();


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
                } else if (clazz.isAssignableFrom(SnapshotTableViewController.class)) {
                    return clazz.getConstructor().newInstance();
                } else if (clazz.isAssignableFrom(SnapshotControlsViewController.class)) {
                    return clazz.getConstructor().newInstance();
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

        ImageView imageView = new ImageView();
        imageView.imageProperty().bind(tabGraphicImageProperty);

        setGraphic(imageView);

        textProperty().set(node.getNodeType().equals(NodeType.SNAPSHOT) ? node.getName() : Messages.unnamedSnapshot);
        setTabImage(node);

        setOnCloseRequest(event -> {
            if (controller != null && !((SnapshotController) controller).handleSnapshotTabClosed()) {
                event.consume();
            } else {
                SaveAndRestoreService.getInstance().removeNodeChangeListener(this);
            }
        });


        SaveAndRestoreService.getInstance().addNodeChangeListener(this);
    }

    public void updateTabTitle(String name) {
        Platform.runLater(() -> textProperty().set(name));
    }

    /**
     * Set tab image based on node type, and optionally golden tag
     * @param node A snapshot {@link Node}
     */
    private void setTabImage(Node node) {
        if(node.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)){
            tabGraphicImageProperty.set(ImageRepository.COMPOSITE_SNAPSHOT);
        }
        else{
            boolean golden = node.getTags() != null && node.getTags().stream().anyMatch(t -> t.getName().equals(Tag.GOLDEN));
            if (golden) {
                tabGraphicImageProperty.set(ImageRepository.GOLDEN_SNAPSHOT);
            }
            else {
                tabGraphicImageProperty.set(ImageRepository.SNAPSHOT);
            }
        }
    }

    /**
     * Loads and configures a view for the use case of taking a new snapshot.
     *
     * @param configurationNode The {@link Node} of type {@link NodeType#CONFIGURATION} listing PVs for which
     *                          a snapshot will be created.
     */
    public void newSnapshot(org.phoebus.applications.saveandrestore.model.Node configurationNode) {
        setId(null);
        ((SnapshotController) controller).newSnapshot(configurationNode);
    }

    /**
     * Loads and configures a view for the use case of restoring a snapshot.
     *
     * @param snapshotNode The {@link Node} of type {@link NodeType#SNAPSHOT} containing snapshot data.
     */
    public void loadSnapshot(Node snapshotNode) {
        updateTabTitle(snapshotNode.getName());
        setId(snapshotNode.getUniqueId());
        ((SnapshotController) controller).loadSnapshot(snapshotNode);
    }

    public void addSnapshot(org.phoebus.applications.saveandrestore.model.Node node) {
        ((SnapshotController) controller).addSnapshot(node);
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            Platform.runLater(() -> {
                ((SnapshotController) controller).setSnapshotNameProperty(node.getName());
                setTabImage(node);
            });
        }
    }

    public Node getSnapshotNode() {
        return ((SnapshotController) controller).getSnapshot().getSnapshotNode();
    }

    public Node getConfigNode() {
        return ((SnapshotController) controller).getConfigurationNode();
    }

}
