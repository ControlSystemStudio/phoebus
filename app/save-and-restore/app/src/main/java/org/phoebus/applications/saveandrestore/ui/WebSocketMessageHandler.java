/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;

public interface WebSocketMessageHandler {

    void handleWebSocketMessage(SaveAndRestoreWebSocketMessage<?> saveAndRestoreWebSocketMessage);
}
