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

import javafx.beans.property.SimpleStringProperty;
import org.phoebus.applications.saveandrestore.authentication.SaveAndRestoreAuthenticationScope;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SaveAndRestoreBaseController {

    protected final SimpleStringProperty userIdentity = new SimpleStringProperty();
    protected final WebSocketClientService webSocketClientService;
    protected final SaveAndRestoreService saveAndRestoreService;

    public SaveAndRestoreBaseController() {
        this.webSocketClientService = WebSocketClientService.getInstance();
        this.saveAndRestoreService = SaveAndRestoreService.getInstance();
        try {
            SecureStore secureStore = new SecureStore();
            ScopedAuthenticationToken token =
                    secureStore.getScopedAuthenticationToken(new SaveAndRestoreAuthenticationScope());
            if (token != null) {
                userIdentity.set(token.getUsername());
            } else {
                userIdentity.set(null);
            }
        } catch (Exception e) {
            Logger.getLogger(SaveAndRestoreBaseController.class.getName()).log(Level.WARNING, "Unable to retrieve authentication token for " +
                    new SaveAndRestoreAuthenticationScope().getScope()+ " scope", e);
        }
    }

    public void secureStoreChanged(List<ScopedAuthenticationToken> validTokens) {
        Optional<ScopedAuthenticationToken> token =
                validTokens.stream()
                        .filter(t -> t.getAuthenticationScope().getScope().equals(new SaveAndRestoreAuthenticationScope().getScope())).findFirst();
        if (token.isPresent()) {
            userIdentity.set(token.get().getUsername());
        } else {
            userIdentity.set(null);
        }
    }

    public SimpleStringProperty getUserIdentity() {
        return userIdentity;
    }

    /**
     * Default no-op implementation of a handler for {@link SaveAndRestoreWebSocketMessage}s.
     * @param webSocketMessage See {@link SaveAndRestoreWebSocketMessage}
     */
    protected void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> webSocketMessage){
    }

    /**
     * Performs suitable cleanup, e.g. close web socket and PVs (where applicable).
     */
    public abstract void handleTabClosed();

    /**
     * Checks if the tab may be closed, e.g. if data managed in the UI has been saved.
     * @return <code>false</code> if tab contains unsaved data, otherwise <code>true</code>
     */
    public abstract boolean doCloseCheck();
}
