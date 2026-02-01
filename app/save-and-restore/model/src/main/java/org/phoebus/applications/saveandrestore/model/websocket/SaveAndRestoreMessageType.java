/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

import org.phoebus.core.websocket.MessageType;

/**
 * Enum to indicate what type of web socket message the service is sending to clients.
 */
public enum SaveAndRestoreMessageType implements MessageType {
    NODE_ADDED,
    NODE_UPDATED,
    NODE_REMOVED,
    FILTER_ADDED_OR_UPDATED,
    FILTER_REMOVED
}
