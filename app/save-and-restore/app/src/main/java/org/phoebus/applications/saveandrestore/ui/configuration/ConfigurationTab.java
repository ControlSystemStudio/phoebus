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
package org.phoebus.applications.saveandrestore.ui.configuration;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.ui.javafx.ImageCache;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationTab extends Tab implements NodeChangedListener {

    private ConfigurationController configurationController;

    private final SimpleStringProperty tabTitleProperty = new SimpleStringProperty();


    public ConfigurationTab(Node node, SaveAndRestoreService saveAndRestoreService) {

        setId(node.getUniqueId());

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(ConfigurationTab.class.getResource("ConfigurationEditor.fxml"));
            setContent(loader.load());
            configurationController = loader.getController();
            tabTitleProperty.set(node.getName());
            setGraphic(getTabGraphic());
            configurationController.loadConfiguration(node);
        } catch (Exception e) {
            Logger.getLogger(ConfigurationTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        setOnCloseRequest(event -> {
            if (!configurationController.handleSaveSetTabClosed()) {
                event.consume();
            } else {
                saveAndRestoreService.removeNodeChangeListener(this);
            }
        });

        saveAndRestoreService.addNodeChangeListener(this);
    }

    private javafx.scene.Node getTabGraphic() {
        HBox container = new HBox();
        Image icon = ImageCache.getImage(SnapshotTab.class, "/icons/save-and-restore/configuration.png");
        ImageView imageView = new ImageView(icon);
        Label label = new Label("");
        label.textProperty().bindBidirectional(tabTitleProperty);
        HBox.setMargin(label, new Insets(3, 5, 0, 5));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            tabTitleProperty.set(node.getName());
        }
    }
}
