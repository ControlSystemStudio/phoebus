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

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.Messages;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.applications.saveandrestore.ui.WebSocketMessageHandler;
import org.phoebus.framework.nls.NLS;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationTab extends SaveAndRestoreTab implements WebSocketMessageHandler {

    public ConfigurationTab() {
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
            controller = loader.getController();
            setGraphic(new ImageView(ImageRepository.CONFIGURATION));
        } catch (Exception e) {
            Logger.getLogger(ConfigurationTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
        }
    }

    /**
     * Configures UI to edit an existing configuration {@link Node}
     *
     * @param configurationNode non-null configuration {@link Node}
     */
    public void editConfiguration(Node configurationNode) {
        ((ConfigurationController) controller).loadConfiguration(configurationNode);
    }

    /**
     * Configures for new configuration
     *
     * @param parentNode Parent {@link Node} for the new configuration.
     */
    public void configureForNewConfiguration(Node parentNode) {
        ((ConfigurationController) controller).newConfiguration(parentNode);
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
