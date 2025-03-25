/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.model.websocket;

public enum MessageType {
    NODE_ADDED,
    NODE_UPDATED,
    NODE_REMOVED,
    FILTER_ADDED_OR_UPDATED,
    FILTER_REMOVED;
}
