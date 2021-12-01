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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import org.phoebus.framework.jobs.JobManager;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX controller for the Credentials Management UI.
 */
public class CredentialsManagementController {

    @FXML
    private Node parent;

    @FXML
    private TableView<ScopedAuthenticationToken> tableView;
    @FXML
    private TableColumn<ScopedAuthenticationToken, Void> logoutButtonColumn;
    @FXML
    private Button logoutAll;

    private SimpleBooleanProperty listEmpty = new SimpleBooleanProperty(true);

    private ObservableList<ScopedAuthenticationToken> scopedAuthenticationTokens =
            FXCollections.observableArrayList();

    private SecureStore secureStore;

    private static final Logger LOGGER = Logger.getLogger(CredentialsManagementController.class.getName());

    @FXML
    public void initialize() {

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        logoutAll.disableProperty().bind(listEmpty);

        Callback<TableColumn<ScopedAuthenticationToken, Void>, TableCell<ScopedAuthenticationToken, Void>> cellFactory = new Callback<TableColumn<ScopedAuthenticationToken, Void>, TableCell<ScopedAuthenticationToken, Void>>() {
            @Override
            public TableCell<ScopedAuthenticationToken, Void> call(final TableColumn<ScopedAuthenticationToken, Void> param) {
                final TableCell<ScopedAuthenticationToken, Void> cell = new TableCell<>() {

                    private final Button btn = new Button(Messages.LogoutButtonText);

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            ScopedAuthenticationToken data = getTableView().getItems().get(getIndex());
                            logOut(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        logoutButtonColumn.setCellFactory(cellFactory);
        loadSavedCredentials();
    }

    @FXML
    public void logOutFromAll() {
        try {
            secureStore.deleteAllScopedAuthenticationTokens();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to delete all authentication tokens", e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
        loadSavedCredentials();
    }

    private void logOut(ScopedAuthenticationToken scopedAuthenticationToken){
        try {
            secureStore.deleteScopedAuthenticationToken(scopedAuthenticationToken.getScope());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to logout from scope " + scopedAuthenticationToken.getScope(), e);
            ExceptionDetailsErrorDialog.openError(parent, Messages.ErrorDialogTitle, Messages.ErrorDialogBody, e);
        }
        loadSavedCredentials();
    }

    private void loadSavedCredentials(){
        JobManager.schedule("Get Credentials", monitor -> {
            if(secureStore == null){
                secureStore = new SecureStore();
            }
            List<ScopedAuthenticationToken> tokens = secureStore.getAuthenticationTokens();
            Platform.runLater(() -> {
                scopedAuthenticationTokens.setAll(tokens);
                listEmpty.set(tokens.isEmpty());
                tableView.setItems(scopedAuthenticationTokens);
            });
        });
    }
}
