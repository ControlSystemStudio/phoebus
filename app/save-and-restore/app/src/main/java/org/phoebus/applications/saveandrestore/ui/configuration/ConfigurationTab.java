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
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.NodeChangedListener;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.framework.nls.NLS;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationTab extends Tab implements NodeChangedListener {

    private ConfigurationController configurationController;

    private SimpleStringProperty tabTitleProperty = new SimpleStringProperty(Messages.contextMenuNewConfiguration);

    public ConfigurationTab() {
        configure();
    }

    private void configure() {
        try {
            FXMLLoader loader = new FXMLLoader();
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setResources(resourceBundle);
            loader.setLocation(ConfigurationTab.class.getResource("ConfigurationEditor.fxml"));
            loader.setControllerFactory(clazz -> {
                try {
                    if (clazz.isAssignableFrom(ConfigurationController.class)) {
                        return clazz.getConstructor(ConfigurationTab.class)
                                .newInstance(this);
                    }
                } catch (Exception e) {
                    Logger.getLogger(ConfigurationTab.class.getName()).log(Level.SEVERE, "Failed to construct ConfigurationController", e);

                }
                return null;
            });
            setContent(loader.load());
            configurationController = loader.getController();
            setGraphic(getTabGraphic());
        } catch (Exception e) {
            Logger.getLogger(ConfigurationTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        setOnCloseRequest(event -> {
            if (!configurationController.handleConfigurationTabClosed()) {
                event.consume();
            } else {
                SaveAndRestoreService.getInstance().removeNodeChangeListener(this);
            }
        });

        SaveAndRestoreService.getInstance().addNodeChangeListener(this);
    }

    /**
     * Configures UI to edit an existing configuration {@link Node}
     *
     * @param configurationNode non-null configuration {@link Node}
     */
    public void editConfiguration(Node configurationNode) {
        setId(configurationNode.getUniqueId());
        tabTitleProperty.set(configurationNode.getName());
        configurationController.loadConfiguration(configurationNode);
    }

    public void configureForNewConfiguration(Node parentNode) {
        configurationController.newConfiguration(parentNode);
    }

    private javafx.scene.Node getTabGraphic() {
        HBox container = new HBox();
        ImageView imageView = new ImageView(ImageRepository.CONFIGURATION);
        Label label = new Label("");
        label.textProperty().bindBidirectional(tabTitleProperty);
        HBox.setMargin(label, new Insets(1, 5, 0, 3));
        HBox.setMargin(imageView, new Insets(1, 2, 0, 3));
        container.getChildren().addAll(imageView, label);

        return container;
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            tabTitleProperty.set(node.getName());
        }
    }

    /**
     * Updates tab title, e.g. if user has renamed the configuration.
     *
     * @param tabTitle The wanted tab title.
     */
    public void updateTabTitle(String tabTitle) {
        tabTitleProperty.set(tabTitle);
    }
}
