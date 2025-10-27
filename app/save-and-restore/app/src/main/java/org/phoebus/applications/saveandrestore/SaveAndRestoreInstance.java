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

package org.phoebus.applications.saveandrestore;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.phoebus.applications.saveandrestore.ui.SaveAndRestoreController;
import org.phoebus.framework.nls.NLS;
import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.docking.DockItemWithInput;
import org.phoebus.ui.docking.DockPane;

import java.net.URI;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SaveAndRestoreInstance implements AppInstance {


    private final AppDescriptor appDescriptor;
    private final SaveAndRestoreController saveAndRestoreController;
    private DockItemWithInput dockItem;

    public static SaveAndRestoreInstance INSTANCE;

    public SaveAndRestoreInstance(AppDescriptor appDescriptor) {
        this.appDescriptor = appDescriptor;

        dockItem = null;

        FXMLLoader loader = new FXMLLoader();
        try {
            ResourceBundle resourceBundle = NLS.getMessages(Messages.class);
            loader.setResources(resourceBundle);
            loader.setLocation(SaveAndRestoreApplication.class.getResource("ui/SaveAndRestoreUI.fxml"));
            dockItem = new DockItemWithInput(this, loader.load(), null, null, null);
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestoreInstance.class.getName()).log(Level.SEVERE, "Failed loading fxml", e);
        }

        saveAndRestoreController = loader.getController();

        // When user closes Phoebus...
        dockItem.addCloseCheck(() -> {
            boolean mayClose = mayClose();
            if (mayClose) {
                saveAndRestoreController.handleTabClosed();
                INSTANCE = null;
            }
            return CompletableFuture.completedFuture(mayClose);
        });

        // When user closes save&restore app...
        dockItem.setOnCloseRequest(event -> {
            if (mayClose()) {
                saveAndRestoreController.handleTabClosed();
                INSTANCE = null;
            } else {
                event.consume();
            }
        });

        DockPane.getActiveDockPane().addTab(dockItem);
    }

    /**
     * Checks with each tab if it may be closed.
     *
     * @return <code>false</code> if any of the tabs contains unsaved data, otherwise <code>true</code>.
     */
    private boolean mayClose() {
        if (Platform.isFxApplicationThread()) {
            return saveAndRestoreController.doCloseCheck();
        } else {
            CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
            Platform.runLater(() -> {
                try {
                    boolean okToClose = saveAndRestoreController.doCloseCheck();
                    completableFuture.complete(okToClose);
                } catch (Exception e) {
                    Logger.getLogger(SaveAndRestoreInstance.class.getName()).log(Level.SEVERE,
                        "Got exception when checking if OK to close", e);
                    completableFuture.complete(true);
                }
            });
            try {
                return completableFuture.get();
            } catch (Exception e) {
                Logger.getLogger(SaveAndRestoreInstance.class.getName()).log(Level.WARNING, "Got exception when waiting for close check evaluation", e);
                return true;
            }
        }
    }

    @Override
    public AppDescriptor getAppDescriptor() {
        return appDescriptor;
    }

    @Override
    public void save(Memento memento) {
        saveAndRestoreController.saveLocalState();
    }

    public void openResource(URI uri) {
        saveAndRestoreController.openResource(uri);
    }

    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        saveAndRestoreController.secureStoreChanged(validTokens);
    }

    public void raise() {
        dockItem.select();
    }
}
