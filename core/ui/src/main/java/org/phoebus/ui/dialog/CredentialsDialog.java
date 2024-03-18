/*
 * Copyright (C) 2024 European Spallation Source ERIC.
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

package org.phoebus.ui.dialog;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.util.Pair;
import org.phoebus.framework.nls.NLS;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.Messages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom {@link Dialog} used to prompt user for credentials. It is used together with a {@link ServiceAuthenticationProvider}
 * responsible for identifying a scope (=service) and that also executes authentication.
 * <p>
 * If user's credentials are contained in the credentials cache, the dialog silently returns, i.e. it is not rendered
 * on screen.
 * <p>
 * Typical use case is to precede a call to a remote service (that requires authentication) with a call to
 * this class' {@link #prompt} method.
 */
@SuppressWarnings("unused")
public class CredentialsDialog extends Dialog<Pair<String, String>> {

    private Boolean loginSuccessful;

    private CredentialsDialog(ServiceAuthenticationProvider serviceAuthenticationProvider) {

        setTitle(MessageFormat.format(Messages.credentialsDialogHeader, serviceAuthenticationProvider.getAuthenticationScope().getName()));

        ButtonType loginButtonType = new ButtonType(Messages.credentialsDialogLogin, ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
        FXMLLoader loader = new FXMLLoader();
        loader.setResources(resourceBundle);
        loader.setLocation(this.getClass().getResource("CredentialsDialogContent.fxml"));

        try {
            getDialogPane().setContent(loader.load());
        } catch (IOException e) {
            Logger.getLogger(CredentialsDialogController.class.getName())
                    .log(Level.WARNING, "Failed to load credentials dialog content");
            throw new RuntimeException("Failed to load credentials dialog content");
        }

        CredentialsDialogController credentialsDialogController = loader.getController();
        credentialsDialogController.setFocus();

        setResultConverter(dialogButton -> new Pair<>(credentialsDialogController.getUsernameStringProperty().get(),
                credentialsDialogController.getPasswordStringProperty().get()));

        final Button login = (Button) getDialogPane().lookupButton(loginButtonType);
        login.disableProperty().bind(Bindings.createBooleanBinding(() -> credentialsDialogController.getUsernameStringProperty().isEmpty().get() ||
                        credentialsDialogController.getPasswordStringProperty().isEmpty().get(),
                credentialsDialogController.getUsernameStringProperty(),
                credentialsDialogController.getPasswordStringProperty()));
        login.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                serviceAuthenticationProvider.authenticate(credentialsDialogController.getUsernameStringProperty().get(),
                        credentialsDialogController.getPasswordStringProperty().get());
                loginSuccessful = true;
            } catch (Exception e) {
                Logger.getLogger(CredentialsDialogController.class.getName())
                        .log(Level.WARNING, "Failed to login to " + serviceAuthenticationProvider.getAuthenticationScope());
                credentialsDialogController.setErrorStringProperty(Messages.credentialsDialogError);
                event.consume();
            }
        });
    }

    /**
     * Prompts user for credentials and attempts to log in to service as implemented by the
     * provided {@link ServiceAuthenticationProvider}. If login is successful, the credentials are
     * put in the credentials cache.
     *
     * @param serviceAuthenticationProvider A non-null {@link ServiceAuthenticationProvider} instance
     * @return <code>true</code> if login is successful, otherwise false.
     */
    public static boolean prompt(ServiceAuthenticationProvider serviceAuthenticationProvider) {
        if (serviceAuthenticationProvider == null) {
            return false;
        }
        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken =
                    secureStore.getScopedAuthenticationToken(serviceAuthenticationProvider.getAuthenticationScope());
            if (scopedAuthenticationToken == null) { // No cached user credentials
                Optional<Pair<String, String>> result = new CredentialsDialog(serviceAuthenticationProvider).showAndWait();
                if (result.isPresent() &&
                        result.get().getKey() != null &&
                        !result.get().getKey().isEmpty() &&
                        result.get().getValue() != null &&
                        !result.get().getValue().isEmpty()) {
                    secureStore.setScopedAuthentication(new ScopedAuthenticationToken(serviceAuthenticationProvider.getAuthenticationScope(),
                            result.get().getKey(), result.get().getValue()));
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        } catch (Exception e) {
            Logger.getLogger(CredentialsDialog.class.getName())
                    .log(Level.WARNING, "Failed to interact with secure store", e);
            return false;
        }
    }
}