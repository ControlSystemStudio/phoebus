/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;


public record SaveAndRestoreWebSocketMessage<T>(MessageType messageType, T payload) {
}
