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

package org.phoebus.applications.saveandrestore.ui;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.ImageView;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.applications.saveandrestore.ui.snapshot.SnapshotTab;
import org.phoebus.security.tokens.ScopedAuthenticationToken;
import org.phoebus.ui.javafx.ImageCache;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for save-n-restore {@link Tab}s containing common functionality.
 */
public abstract class SaveAndRestoreTab extends Tab implements WebSocketMessageHandler {

    protected SaveAndRestoreBaseController controller;

    public SaveAndRestoreTab() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem closeAll = new MenuItem(org.phoebus.ui.application.Messages.DockCloseAll,
                new ImageView(ImageCache.getImage(SnapshotTab.class, "/icons/remove_multiple.png")));
        closeAll.setOnAction(e -> getTabPane().getTabs().removeAll(getTabPane().getTabs()));

        MenuItem closeOthers = new MenuItem(org.phoebus.ui.application.Messages.DockCloseOthers,
                new ImageView(ImageCache.getImage(SnapshotTab.class, "/icons/remove_multiple.png")));
        List<Tab> tabsToClose = new ArrayList<>();
        closeOthers.setOnAction(e -> {
            List<Tab> tabs = getTabPane().getTabs();
            tabs.forEach(t -> {
                if (!t.equals(this)) {
                    tabsToClose.add(t);
                }
            });
            Platform.runLater(() -> tabsToClose.forEach(tabs::remove));
        });

        contextMenu.getItems().addAll(closeAll, closeOthers);
        setContextMenu(contextMenu);

        setOnCloseRequest(event -> {
            if (doCloseCheck()) {
                handleTabClosed();
            } else {
                event.consume();
            }
        });
    }

    /**
     * Called when user has signed in or out.
     *
     * @param validTokens List of valid {@link ScopedAuthenticationToken}s.
     */
    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        controller.secureStoreChanged(validTokens);
    }

    @Override
    public void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage) {

    }

    /**
     * Performs suitable cleanup, e.g. close web socket and PVs (where applicable).
     */
    public void handleTabClosed() {
        controller.handleTabClosed();
    }

    /**
     * Checks if the tab may be closed, e.g. if data managed in the UI has been saved.
     * @return <code>false</code> if tab contains unsaved data, otherwise <code>true</code>
     */
    public boolean doCloseCheck() {
        return controller.doCloseCheck();
    }
}
