/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

/**
 * Record encapsulating a {@link MessageType} and a payload.
 * @param messageType The {@link MessageType} of a web socket message
 * @param payload The payload, e.g. {@link String} or {@link org.phoebus.applications.saveandrestore.model.Node}
 */
public record SaveAndRestoreWebSocketMessage<T>(MessageType messageType, T payload) {
}
