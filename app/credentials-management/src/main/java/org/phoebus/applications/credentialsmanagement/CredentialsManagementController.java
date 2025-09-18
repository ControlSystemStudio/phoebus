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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
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
    private TableColumn<ServiceItem, StringProperty> usernameColumn;
    @SuppressWarnings("unused")
    @FXML
    private TableColumn<ServiceItem, StringProperty> passwordColumn;
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
    private final IntegerProperty providerCount = new SimpleIntegerProperty(0);

    /**
     * <code>true</code> if user is logged in to at least one service (scope).
     */
    private final BooleanProperty loggedInProperty = new SimpleBooleanProperty();

    private Stage stage;

    public CredentialsManagementController(List<ServiceAuthenticationProvider> authenticationProviders, SecureStore secureStore) {
        this.authenticationProviders = authenticationProviders;
        this.secureStore = secureStore;
        providerCount.set(this.authenticationProviders.size());
    }

    @SuppressWarnings("unused")
    @FXML
    public void initialize() {

        tableView.getStylesheets().add(getClass().getResource("/css/credentials-management-style.css").toExternalForm());

        logoutFromAllButton.disableProperty().bind(listEmpty);

        usernameColumn.setCellFactory(c -> new UsernameTableCell());
        passwordColumn.setCellFactory(c -> new PasswordTableCell());

        loginToAllUsernameTextField.visibleProperty().bind(Bindings.createBooleanBinding(() -> providerCount.get() > 1, providerCount));
        loginToAllPasswordTextField.visibleProperty().bind(Bindings.createBooleanBinding(() -> providerCount.get() > 1, providerCount));
        loginToAllUsernameTextField.textProperty().bindBidirectional(loginToAllUsernameProperty);
        loginToAllPasswordTextField.textProperty().bindBidirectional(loginToAllPasswordProperty);

        loginToAllButton.visibleProperty().bind(Bindings.createBooleanBinding(() -> providerCount.get() > 1, providerCount));
        // Login to all button enabled only if non-empty username and password is present
        loginToAllButton.disableProperty().bind(Bindings.createBooleanBinding(() -> loginToAllUsernameProperty.get() == null ||
                        loginToAllUsernameProperty.get().isEmpty() ||
                        loginToAllPasswordProperty.get() == null ||
                        loginToAllPasswordProperty.get().isEmpty(),
                loginToAllUsernameProperty, loginToAllPasswordProperty));

        logoutFromAllButton.disableProperty().bind(loggedInProperty.not());

        actionButtonColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));

        configureCellFactory();

        updateTable();

        // Don't want focus on the username field for "login to all" as that obscures the prompt.
        // Let table request focus.
        Platform.runLater(() -> tableView.requestFocus());

    }

    private void configureCellFactory(){
        Callback<TableColumn<ServiceItem, ServiceItem>, TableCell<ServiceItem, ServiceItem>> actionColumnCellFactory = new Callback<>() {
            @Override
            public TableCell<ServiceItem, ServiceItem> call(final TableColumn<ServiceItem, ServiceItem> param) {
                final TableCell<ServiceItem, ServiceItem> cell = new TableCell<>() {

                    private final Button btn = new Button(Messages.LogoutButtonText);{
                        btn.getStyleClass().add("button-style");
                        btn.setOnAction((ActionEvent event) -> {
                            ServiceItem serviceItem = getTableRow().getItem();
                            if (serviceItem != null && serviceItem.userLoggedIn.get()) {
                                logOut(serviceItem);
                            } else {
                                login(serviceItem);
                            }
                        });
                    }

                    @Override
                    public void updateItem(ServiceItem serviceItem, boolean empty) {
                        super.updateItem(serviceItem, empty);
                        if (empty) {
                            setGraphic(null);
                        }
                        else {
                            btn.textProperty().bind(serviceItem.buttonTextProperty);
                            btn.disableProperty().bind(Bindings.createBooleanBinding(() ->
                                            serviceItem.username.isNull().get() || serviceItem.username.get().isEmpty() ||
                                                    serviceItem.password.isNull().get() || serviceItem.password.get().isEmpty(),
                                    serviceItem.username, serviceItem.password));
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        actionButtonColumn.setCellFactory(actionColumnCellFactory);
    }

    @SuppressWarnings("unused")
    @FXML
    public void logoutFromAll() {
        try {
            secureStore.deleteAllScopedAuthenticationTokens();
            updateTable();
            loggedInProperty.set(false);
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
            serviceItem.getServiceAuthenticationProvider().authenticate(serviceItem.getUsername().get(), serviceItem.getPassword().get());
            try {
                secureStore.setScopedAuthentication(new ScopedAuthenticationToken(serviceItem.getAuthenticationScope(),
                        serviceItem.getUsername().get(),
                        serviceItem.getPassword().get()));
                loggedInProperty.set(true);
                serviceItem.userLoggedIn.set(true);
                //stage.close();
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to store credentials", exception);
            }
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to login to service", exception);
            ExceptionDetailsErrorDialog.openError(parent, "Login Failure", "Failed to login to service", exception);
        }
    }

    private void logOut(ServiceItem serviceItem) {
        try {
            secureStore.deleteScopedAuthenticationToken(serviceItem.getAuthenticationScope());
            serviceItem.setLoggedOut();
            Platform.runLater(() -> tableView.requestFocus());
            //updateTable();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to logout from service " + serviceItem.getDisplayName(), e);
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
    public class ServiceItem {
        private final ServiceAuthenticationProvider serviceAuthenticationProvider;
        private StringProperty username = new SimpleStringProperty();
        private StringProperty password = new SimpleStringProperty();
        private final BooleanProperty userLoggedIn = new SimpleBooleanProperty();
        private final StringProperty buttonTextProperty = new SimpleStringProperty();

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider, String username, String password) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            this.username.set(username);
            this.password.set(password);
            buttonTextProperty.set(Messages.LogoutButtonText);
            userLoggedIn.addListener((obs, o, n) -> buttonTextProperty.set(n ? Messages.LogoutButtonText : Messages.LoginButtonText));
        }

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            this.userLoggedIn.set(false);
            buttonTextProperty.set(Messages.LoginButtonText);
            userLoggedIn.addListener((obs, o, n) -> buttonTextProperty.set(n ? Messages.LogoutButtonText : Messages.LoginButtonText));
        }

        public StringProperty getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username.set(username);
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

        public StringProperty getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password.set(password);

        }

        public ServiceAuthenticationProvider getServiceAuthenticationProvider() {
            return serviceAuthenticationProvider;
        }

        public BooleanProperty getUserLoggedIn() {
            return userLoggedIn;
        }

        public String getButtonTextProperty() {
            return buttonTextProperty.get();
        }

        public StringProperty buttonTextPropertyProperty() {
            return buttonTextProperty;
        }

        public void setLoggedOut(){
            userLoggedIn.set(false);
            username.set(null);
            password.set(null);
        }
    }

    private class UsernameTableCell extends TableCell<ServiceItem, StringProperty> {

        @Override
        public void updateItem(StringProperty item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                TextField textField = new TextField();
                textField.getStyleClass().add("text-field-styling");
                textField.textProperty().bindBidirectional(getTableRow().getItem().username);
                textField.disableProperty().bind(getTableRow().getItem().userLoggedIn);
                ServiceItem serviceItem = getTableRow().getItem();
                textField.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode() == KeyCode.ENTER &&
                            !serviceItem.username.isNull().get() &&
                            !serviceItem.username.get().isEmpty() &&
                            !serviceItem.password.isNull().get() &&
                            !serviceItem.password.get().isEmpty()) {
                        CredentialsManagementController.this.login(serviceItem);
                    }
                });
                setGraphic(textField);
            }
        }
    }



    private class PasswordTableCell extends TableCell<ServiceItem, StringProperty> {

        @Override
        protected void updateItem(StringProperty item, final boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                PasswordField passwordField = new PasswordField();
                passwordField.getStyleClass().add("text-field-styling");

                passwordField.setText(item.get() == null ? null : "dummypass"); // Hack to not reveal password length
                passwordField.textProperty().bindBidirectional(getTableRow().getItem().password);
                passwordField.disableProperty().bind(getTableRow().getItem().userLoggedIn);
                ServiceItem serviceItem = getTableRow().getItem();
                passwordField.setOnKeyPressed(keyEvent -> {
                    if (keyEvent.getCode() == KeyCode.ENTER &&
                            !serviceItem.username.isNull().get() &&
                            !serviceItem.username.get().isEmpty() &&
                            !serviceItem.password.isNull().get() &&
                            !serviceItem.password.get().isEmpty()) {
                        CredentialsManagementController.this.login(serviceItem);
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
