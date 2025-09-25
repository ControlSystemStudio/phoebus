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
 */

package org.phoebus.applications.credentialsmanagement;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.phoebus.framework.nls.NLS;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Stage} for the Credentials Management UI.
 */
public class CredentialsManagementStage extends Stage {

    public CredentialsManagementStage(List<ServiceAuthenticationProvider> authenticationProviders, SecureStore secureStore) {

        initModality(Modality.APPLICATION_MODAL);
        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setResources(resourceBundle);
        URL url = this.getClass().getResource("CredentialsManagement.fxml");
        fxmlLoader.setLocation(url);
        fxmlLoader.setControllerFactory(clazz -> {
            try {
                CredentialsManagementController controller =
                        (CredentialsManagementController) clazz.getConstructor(List.class, SecureStore.class)
                                .newInstance(authenticationProviders, secureStore);
                controller.setStage(this);
                return controller;

            } catch (Exception e) {
                Logger.getLogger(CredentialsManagementStage.class.getName()).log(Level.SEVERE, "Failed to construct CredentialsManagementController", e);
            }
            return null;
        });
        try {
            fxmlLoader.load();
            Scene scene = new Scene(fxmlLoader.getRoot());
            setTitle(Messages.Title);
            setScene(scene);
        } catch (Exception exception) {
            Logger.getLogger(CredentialsManagementStage.class.getName()).log(Level.WARNING, "Unable to load fxml for log Credentials Management UI", exception);
        }
    }
}
