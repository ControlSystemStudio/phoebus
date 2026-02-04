/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.core.websocket.common;

/**
 * Record encapsulating a {@link MessageType} and a payload of arbitrary type.
 * <p>
 *     The deserialization process of a web socket message into a concrete {@link WebSocketMessage} must be
 *     delegated to a custom deserializer.
 * </p>
 * @param messageType The {@link MessageType} of a web socket message. Apps can implement as needed.
 * @param payload The payload like a {@link String}, or something more specific for the actual use case, e.g. a
 *                logbook entry or save-and-restore object.
 */
public record WebSocketMessage<T>(MessageType messageType, T payload) {
}
