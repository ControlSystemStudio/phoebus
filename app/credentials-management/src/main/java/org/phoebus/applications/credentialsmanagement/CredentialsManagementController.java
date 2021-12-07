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
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.util.Callback;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.authorization.ServiceAuthenticationProvider;
import org.phoebus.security.store.SecureStore;
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

    @FXML
    private Node parent;

    @FXML
    private TableView<ServiceItem> tableView;
    @FXML
    private TableColumn<ServiceItem, Void> actionButtonColumn;
    @FXML
    private TableColumn<ServiceItem, String> usernameColumn;
    @FXML
    private TableColumn<ServiceItem, String> passwordColumn;
    @FXML
    private Button clearAllCredentialsButton;

    private final SimpleBooleanProperty listEmpty = new SimpleBooleanProperty(true);
    private final ObservableList<ServiceItem> serviceItems =
            FXCollections.observableArrayList();
    private final SecureStore secureStore;
    private static final Logger LOGGER = Logger.getLogger(CredentialsManagementController.class.getName());
    private final List<ServiceAuthenticationProvider> authenticationProviders;

    public CredentialsManagementController(List<ServiceAuthenticationProvider> authenticationProviders, SecureStore secureStore) {
        this.authenticationProviders = authenticationProviders;
        this.secureStore = secureStore;
    }

    @FXML
    public void initialize() {

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        clearAllCredentialsButton.disableProperty().bind(listEmpty);
        Callback<TableColumn<ServiceItem, Void>, TableCell<ServiceItem, Void>> actionColumnCellFactory = new Callback<>() {
            @Override
            public TableCell<ServiceItem, Void> call(final TableColumn<ServiceItem, Void> param) {
                final TableCell<ServiceItem, Void> cell = new TableCell<>() {

                    private final Button btn = new Button(Messages.LogoutButtonText);
                    {
                        btn.getStyleClass().add("button-style");
                        btn.setOnAction((ActionEvent event) -> {
                            ServiceItem serviceItem = getTableView().getItems().get(getIndex());
                            if(serviceItem.isLoginAction()){
                                login(serviceItem);
                            }
                            else{
                                logOut(serviceItem.getScope());
                            }
                        });
                    }

                    @Override
                    public void updateItem(Void o, boolean empty) {
                        super.updateItem(o, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            if(getTableRow() != null && getTableRow().getItem() != null){
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

        updateTable();
    }

    @FXML
    public void logOutFromAll() {
        try {
            secureStore.deleteAllScopedAuthenticationTokens();
            updateTable();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete all authentication tokens from key store", e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
    }

    private void login(ServiceItem serviceItem){
        try {
            serviceItem.getServiceAuthenticationProvider().authenticate(serviceItem.getUsername(), serviceItem.getPassword());
            try {
                secureStore.setScopedAuthentication(new ScopedAuthenticationToken(serviceItem.getScope(),
                        serviceItem.getUsername(),
                        serviceItem.getPassword()));
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Failed to store credentials", exception);
            }
            updateTable();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to login to service", exception);
            ExceptionDetailsErrorDialog.openError(parent, "Login Failure", "Failed to login to service", exception);
        }
    }

    private void logOut(String scope) {
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
                        authenticationProviders.stream().filter(p-> p.getServiceName().equals(token.getScope())).findFirst().orElse(null);
                return new ServiceItem(provider, token.getUsername(), token.getPassword());
            }).collect(Collectors.toList());
            // Also need to add ServiceItems for providers not matched with a saved token, i.e. for logged-out services
            authenticationProviders.stream().forEach(p -> {
                Optional<ServiceItem> serviceItem =
                        serviceItems.stream().filter(si ->
                            p.getServiceName().equals(si.getScope())).findFirst();
                if(serviceItem.isEmpty()){
                    serviceItems.add(new ServiceItem(p));
                }
            });
            serviceItems.sort(Comparator.comparing(ServiceItem::getScope));
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
        private ServiceAuthenticationProvider serviceAuthenticationProvider;
        private String username;
        private String password;
        private boolean loginAction = false;

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider, String username, String password) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            this.username = username;
            this.password = password;
        }

        public ServiceItem(ServiceAuthenticationProvider serviceAuthenticationProvider) {
            this.serviceAuthenticationProvider = serviceAuthenticationProvider;
            loginAction = true;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username){
            this.username = username;
        }

        public String getScope() {
            return serviceAuthenticationProvider != null ?
                    serviceAuthenticationProvider.getServiceName() : "";
        }

        public String getPassword(){
            return password;
        }

        public void setPassword(String password){
            this.password = password;
        }

        public ServiceAuthenticationProvider getServiceAuthenticationProvider() {
            return serviceAuthenticationProvider;
        }

        public boolean isLoginAction(){
            return loginAction;
        }
    }
    private class UsernameTableCell extends TableCell<ServiceItem, String>{
        private TextField textField = new TextField();

        public UsernameTableCell(){
            textField.getStyleClass().add("text-field-styling");
            // Update model on key up
            textField.setOnKeyReleased(ke -> getTableRow().getItem().setUsername(textField.getText()));
        }

        @Override
        protected void updateItem(String item, final boolean empty)
        {
            super.updateItem(item, empty);
            if(empty){
                setGraphic(null);
            }
            else{
                textField.setText(item);
                if(getTableRow() != null && getTableRow().getItem() != null){
                    // Disable field if user is logged in.
                    textField.disableProperty().set(!getTableRow().getItem().loginAction);
                }
                setGraphic(textField);
            }
        }
    }

    private class PasswordTableCell extends TableCell<ServiceItem, String>{
        private PasswordField passwordField = new PasswordField();

        public PasswordTableCell(){
            passwordField.getStyleClass().add("text-field-styling");
            // Update model on key up
            passwordField.setOnKeyReleased(ke -> getTableRow().getItem().setPassword(passwordField.getText()));
        }

        @Override
        protected void updateItem(String item, final boolean empty)
        {
            super.updateItem(item, empty);
            if(empty){
                setGraphic(null);
            }
            else{
                passwordField.setText(item == null ? item : "dummypass"); // Hack to no reveal password length
                if(getTableRow() != null && getTableRow().getItem() != null) {
                    // Disable field if user is logged in.
                    passwordField.disableProperty().set(!getTableRow().getItem().loginAction);
                }
                setGraphic(passwordField);
            }
        }
    }
}
