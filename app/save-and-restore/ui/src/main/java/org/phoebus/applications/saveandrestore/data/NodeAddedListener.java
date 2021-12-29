package org.phoebus.applications.saveandrestore.data;

import org.phoebus.applications.saveandrestore.model.Node;

import java.util.List;

public interface NodeAddedListener {

    /**
     * To be called when a new node has been created (typically new snapshot node).
     * @param parentNode The parent of the new node as defined in the back-end data model.
     * @param newNodes The list of {@link Node}s added to the parent {@link Node}.
     */
    void nodesAdded(Node parentNode, List<Node> newNodes);
}
