/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket.client;

public interface WebSocketMessageHandler {
    void handleWebSocketMessage(String message);
}
