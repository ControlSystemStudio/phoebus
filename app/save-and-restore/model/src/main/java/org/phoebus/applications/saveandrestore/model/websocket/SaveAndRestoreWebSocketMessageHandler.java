/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

import org.phoebus.core.websocket.common.WebSocketMessage;

/**
 * Handler for web socket messages that have already been deserialized
 * to a {@link WebSocketMessage}.
 */
public interface SaveAndRestoreWebSocketMessageHandler {
    void handleSaveAndRestoreWebSocketMessage(WebSocketMessage<?> webSocketMessage);
}
