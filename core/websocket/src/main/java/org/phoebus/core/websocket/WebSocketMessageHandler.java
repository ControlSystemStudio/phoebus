/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket;

/**
 * Handler for raw web socket string messages.
 */
public interface WebSocketMessageHandler {

    void handleWebSocketMessage(String message);
}
