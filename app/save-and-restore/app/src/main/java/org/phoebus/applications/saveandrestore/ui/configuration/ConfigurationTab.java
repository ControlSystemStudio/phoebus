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
import org.phoebus.applications.saveandrestore.ui.ImageRepository;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreService;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreTab;
import org.phoebus.framework.nls.NLS;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationTab extends SaveAndRestoreTab {

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
            controller = loader.getController();
            setGraphic(new ImageView(ImageRepository.CONFIGURATION));
        } catch (Exception e) {
            Logger.getLogger(ConfigurationTab.class.getName())
                    .log(Level.SEVERE, "Failed to load fxml", e);
            return;
        }

        setOnCloseRequest(event -> {
            if (!((ConfigurationController) controller).handleConfigurationTabClosed()) {
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
        textProperty().set(configurationNode.getName());
        ((ConfigurationController) controller).loadConfiguration(configurationNode);
    }

    public void configureForNewConfiguration(Node parentNode) {
        textProperty().set(Messages.contextMenuNewConfiguration);
        ((ConfigurationController) controller).newConfiguration(parentNode);
    }

    @Override
    public void nodeChanged(Node node) {
        if (node.getUniqueId().equals(getId())) {
            Platform.runLater(() -> textProperty().set(node.getName()));
        }
    }

    /**
     * Updates tab title, e.g. if user has renamed the configuration.
     *
     * @param tabTitle The wanted tab title.
     */
    private void updateTabTitle(String tabTitle) {
        Platform.runLater(() -> textProperty().set(tabTitle));
    }

    /**
     * Updates the tab to indicate if the data is dirty and needs to be saved.
     * @param dirty If <code>true</code>, an asterisk is prepended, otherwise
     *              only the name {@link org.phoebus.applications.saveandrestore.model.Configuration}
     *              is rendered.
     */
    public void annotateDirty(boolean dirty) {
        String tabTitle = textProperty().get();
        if (dirty && !tabTitle.startsWith("*")) {
            updateTabTitle("* " + tabTitle);
        } else if (!dirty && tabTitle.startsWith("*")) {
            updateTabTitle(tabTitle.substring(2));
        }
    }
}
