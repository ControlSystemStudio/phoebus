package org.phoebus.applications.saveandrestore.data;

import org.phoebus.applications.saveandrestore.model.Node;

public interface NodeAddedListener {

    /**
     * To be called when a new node has been created (typically new snapshot node).
     * @param parentNode The parent of the new node as defined in the back-end data model.
     * @param newNode The new node
     */
    void nodeAdded(Node parentNode, Node newNode);
}
