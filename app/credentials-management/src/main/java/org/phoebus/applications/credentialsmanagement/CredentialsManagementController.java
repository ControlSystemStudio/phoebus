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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * JavaFX controller for the Credentials Management UI.
 */
public class CredentialsManagementController {

    @SuppressWarnings("unused")
    @FXML
    private Node parent;

    @SuppressWarnings("unused")
    @FXML
    private TableView<ServiceItem> tableView;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ServiceItem, ServiceItem> actionButtonColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ServiceItem, String> usernameColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ServiceItem, String> passwordColumn;
    @SuppressWarnings("unused")
    @FXML
    private Button loginToAllButton;
    @SuppressWarnings("unused")
    @FXML
    private Button logoutFromAllButton;
    @SuppressWarnings("unused")
    @FXML
    private TextField loginToAllUsernameTextField;
    @SuppressWarnings("unused")
    @FXML
    private PasswordField loginToAllPasswordTextField;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ServiceItem, String> scopeColumn;

    private final SimpleBooleanProperty listEmpty = new SimpleBooleanProperty(true);
    private final ObservableList<ServiceItem> serviceItems =
            FXCollections.observableArrayList();
    private final SecureStore secureStore;
    private static final Logger LOGGER = Logger.getLogger(CredentialsManagementController.class.getName());
    private final List<ServiceAuthenticationProvider> authenticationProviders;
    private final StringProperty loginToAllUsernameProperty = new SimpleStringProperty();
    private final StringProperty loginToAllPasswordProperty = new SimpleStringProperty();

    private Stage stage;

    public CredentialsManagementController(List<ServiceAuthenticationProvider> authenticationProviders, SecureStore secureStore) {
        this.authenticationProviders = authenticationProviders;
        this.secureStore = secureStore;
    }

    @SuppressWarnings("unused")
    @FXML
    public void initialize() {

        tableView.getStylesheets().add(getClass().getResource("/css/credentials-management-style.css").toExternalForm());

        logoutFromAllButton.disableProperty().bind(listEmpty);
        Callback<TableColumn<ServiceItem, ServiceItem>, TableCell<ServiceItem, ServiceItem>> actionColumnCellFactory = new Callback<>() {
            @Override
            public TableCell<ServiceItem, ServiceItem> call(final TableColumn<ServiceItem, ServiceItem> param) {
                final TableCell<ServiceItem, ServiceItem> cell = new TableCell<>() {

                    private final Button btn = new Button(Messages.LogoutButtonText);

                    {
                        btn.getStyleClass().add("button-style");
                        btn.setOnAction((ActionEvent event) -> {
                            ServiceItem serviceItem = getTableView().getItems().get(getIndex());
                            if (serviceItem.loginAction) {
                                login(serviceItem);
                            } else {
                                logOut(serviceItem.getAuthenticationScope());
                            }
                        });
                    }

                    @Override
                    public void updateItem(ServiceItem o, boolean empty) {
                        super.updateItem(o, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            if (getTableRow() != null && getTableRow().getItem() != null) {
                                btn.setText(getTableRow().getItem().loginAction ?
                                        Messages.LoginButtonText : Messages.LogoutButtonText);
                            }
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        actionButtonColumn.setCellFactory(actionColumnCellFactory);
        usernameColumn.setCellFactory(c -> new UsernameTableCell());
        passwordColumn.setCellFactory(c -> new PasswordTableCell());

        loginToAllUsernameTextField.textProperty().bindBidirectional(loginToAllUsernameProperty);
        loginToAllPasswordTextField.textProperty().bindBidirectional(loginToAllPasswordProperty);

        loginToAllButton.disableProperty().bind(Bindings.createBooleanBinding(() -> loginToAllUsernameProperty.get() == null ||
                        loginToAllUsernameProperty.get().isEmpty() ||
                        loginToAllPasswordProperty.get() == null ||
                        loginToAllPasswordProperty.get().isEmpty(),
                loginToAllUsernameProperty, loginToAllPasswordProperty));

        updateTable();

        // Don't want focus on the username field for "login to all" as that obscures the prompt.
        // Let table request focus.
        Platform.runLater(() -> tableView.requestFocus());
    }

    @SuppressWarnings("unused")
    @FXML
    public void logoutFromAll() {
        try {
            secureStore.deleteAllScopedAuthenticationTokens();
            updateTable();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete all authentication tokens from key store", e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
    }

    @SuppressWarnings("unused")
    @FXML
    public void loginToAll() {
        try {
            secureStore.deleteAllScopedAuthenticationTokens();
            updateTable();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete all authentication tokens from key store", e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
    }

    /**
     * Attempts to sign in user based on provided credentials. If sign-in succeeds, this method will close the
     * associated UI.
     *
     * @param serviceItem The {@link ServiceItem} defining the scope, and implicitly the authentication service.
     */
    private void login(ServiceItem serviceItem) {
        try {
            serviceItem.getServiceAuthenticationProvider().authenticate(serviceItem.getUsername(), serviceItem.getPassword());
            try {
                secureStore.setScopedAuthentication(new ScopedAuthenticationToken(serviceItem.getAuthenticationScope(),
                        serviceItem.getUsername(),
                        serviceItem.getPassword()));
                stage.close();
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to store credentials", exception);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to login to service", exception);
            ExceptionDetailsErrorDialog.openError(parent, "Login Failure", "Failed to login to service", exception);
        }
    }

    private void logOut(AuthenticationScope scope) {
        try {
            secureStore.deleteScopedAuthenticationToken(scope);
            updateTable();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to logout from scope " + scope, e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
    }

    private void updateTable() {
        JobManager.schedule("Get Credentials", monitor -> {
            List<ScopedAuthenticationToken> savedTokens = secureStore.getAuthenticationTokens();
            // Match saved tokens with an authentication provider, where applicable
            List<ServiceItem> serviceItems = savedTokens.stream().map(token -> {
                ServiceAuthenticationProvider provider =
                        authenticationProviders.stream().filter(p -> p.getAuthenticationScope().getScope().equals(token.getAuthenticationScope().getScope())).findFirst().orElse(null);
                return new ServiceItem(provider, token.getUsername(), token.getPassword());
            }).collect(Collectors.toList());
            // Also need to add ServiceItems for providers not matched with a saved token, i.e. for logged-out services
            authenticationProviders.forEach(p -> {
                Optional<ServiceItem> serviceItem =
                        serviceItems.stream().filter(si ->
                                p.getAuthenticationScope().getScope().equals(si.getAuthenticationScope().getScope())).findFirst();
                if (serviceItem.isEmpty()) {
                    serviceItems.add(new ServiceItem(p));
                }
            });
            serviceItems.sort(Comparator.comparing(i -> i.getAuthenticationScope().getDisplayName()));

            Platform.runLater(() -> {
                this.serviceItems.setAll(serviceItems);
                listEmpty.set(savedTokens.isEmpty());
                tableView.setItems(this.serviceItems);
            });
        });
    }

    /**
     * Model class for the table view
     */
    public static class ServiceItem {
        private final ServiceAuthenticationProvider serviceAuthenticationProvider;
        private String username;
        private String password;
        private boolean loginAction;

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider, String username, String password) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            this.username = username;
            this.password = password;
        }

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            this.loginAction = true;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public AuthenticationScope getAuthenticationScope() {
            return serviceAuthenticationProvider != null ?
                    serviceAuthenticationProvider.getAuthenticationScope() : null;
        }

        /**
         * @return String representation of the authentication scope.
         */
        @SuppressWarnings("unused")
        public String getScope() {
            return serviceAuthenticationProvider != null ?
                    serviceAuthenticationProvider.getAuthenticationScope().getScope() : "";
        }

        @SuppressWarnings("unused")
        public String getDisplayName() {
            return serviceAuthenticationProvider != null ?
                    serviceAuthenticationProvider.getAuthenticationScope().getDisplayName() : "";
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;

        }

        public ServiceAuthenticationProvider getServiceAuthenticationProvider() {
            return serviceAuthenticationProvider;
        }

        public boolean getLoginAction() {
            return loginAction;
        }
    }

    private static class UsernameTableCell extends TableCell<ServiceItem, String> {
        private final TextField textField = new TextField();

        public UsernameTableCell() {
            textField.getStyleClass().add("text-field-styling");
            // Update model on key up
            textField.setOnKeyReleased(ke -> {
                getTableRow().getItem().setUsername(textField.getText());
            });
        }

        @Override
        protected void updateItem(String item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                textField.setText(item);
                if (getTableRow() != null && getTableRow().getItem() != null) {
                    // Disable field if user is logged in.
                    textField.disableProperty().set(!getTableRow().getItem().loginAction);
                }
                setGraphic(textField);
            }
        }
    }

    private class PasswordTableCell extends TableCell<ServiceItem, String> {
        private final PasswordField passwordField = new PasswordField();

        public PasswordTableCell() {
            passwordField.getStyleClass().add("text-field-styling");
            // Update model on key up
            passwordField.setOnKeyReleased(ke -> getTableRow().getItem().setPassword(passwordField.getText()));
        }

        @Override
        protected void updateItem(String item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                passwordField.setText(item == null ? item : "dummypass"); // Hack to not reveal password length

                if (getTableRow() != null && getTableRow().getItem() != null) {
                    // Disable field if user is logged in.
                    passwordField.disableProperty().set(!getTableRow().getItem().loginAction);
                }
                passwordField.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode() == KeyCode.ENTER) {
                        CredentialsManagementController.this.login(getTableRow().getItem());
                    }
                });
                setGraphic(passwordField);
            }
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
}
