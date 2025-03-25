/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.ui;

public interface NodeAddedListener {

    /**
     * To be called when a new node has been created (typically new snapshot node).
     *
     * @param parentNodeId The unique id of the new node's parent node.
     */
    void nodeAdded(String parentNodeId);
}
