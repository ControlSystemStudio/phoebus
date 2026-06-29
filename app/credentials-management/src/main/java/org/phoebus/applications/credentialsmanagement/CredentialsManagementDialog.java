package org.phoebus.applications.credentialsmanagement;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.phoebus.framework.nls.NLS;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link Dialog} for the Credentials Management UI.
 */
public class CredentialsManagementDialog extends Dialog<Void> {

    public CredentialsManagementDialog(List<ServiceAuthenticationProvider> authenticationProviders, SecureStore secureStore){
        super();

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
                controller.setDialog(this);
                return controller;

            } catch (Exception e) {
                Logger.getLogger(CredentialsManagementDialog.class.getName()).log(Level.SEVERE, "Failed to construct CredentialsManagementController", e);
            }
            return null;
        });
        try {
            fxmlLoader.load();
            getDialogPane().setContent(fxmlLoader.getRoot());
            getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        } catch (Exception exception) {
            Logger.getLogger(CredentialsManagementDialog.class.getName()).log(Level.WARNING, "Unable to load fxml for log Credentials Management UI", exception);
        }
    }
}
