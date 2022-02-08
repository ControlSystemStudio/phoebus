/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.service;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.util.List;

/**
 * Implementations of this interface shall handle the communication to the save-and-restore service.
 * Note that this API does <b>not</b> provide means to interact with IOCs/PVs as described in the
 * data objects maintained by the save-and-restore service.
 */
public interface SaveAndRestoreClient {

    /**
     * @return The URL to the remote save-and-restore service.
     */
    String getServiceUrl();

    /**
     * @return The root {@link Node} of the save-and-restore data set.
     * By definition of type {@link org.phoebus.applications.saveandrestore.model.NodeType#FOLDER}.
     */
    Node getRoot();

    /**
     *
     * @param uniqueNodeId The unique id of the {@link Node} to retrieve.
     * @return The {@link Node} object, if it exists.
     */
    Node getNode(String uniqueNodeId);

    /**
     *
     * @param unqiueNodeId Unique id of a {@link Node}
     * @return The parent {@link Node} of the specified id. May be null if the unique id is associated with the root
     * {@link Node}
     */
    Node getParentNode(String unqiueNodeId);

    /**
     *
     * @param node An existing {@link Node}
     * @return A list of child {@link Node}s. May be empty.
     * @throws SaveAndRestoreClientException
     */
    List<Node> getChildNodes(Node node) throws SaveAndRestoreClientException;

    /**
     *
     * @param snapshotUniqueId Unique id of a snapshot {@link Node}
     * @return A list of {@link SnapshotItem}s associated with the snapshot.
     */
    List<SnapshotItem> getSnapshotItems(String snapshotUniqueId);

    /**
     * Persists a new snapshot.
     * @param configUniqueId The unique id of the save set (config) node associated with the snapshot.
     * @param snapshotItems List of {@link SnapshotItem}s that will be bound to the new snapshot.
     * @param snapshotName A name for the snapshot
     * @param comment A comment for the snapshot
     * @return The created snapshot {@link Node}.
     */
    Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment);

    /**
     *
     * @param configUniqueId Unique id of a save set (config) {@link Node}
     * @return A list of {@link ConfigPv}s associated with the save set. May be empty.
     */
    List<ConfigPv> getConfigPvs(String configUniqueId);

    /**
     * Creates a new {@link Node}
     * @param parentsUniqueId Unique id of the parent {@link Node} for the new {@link Node}
     * @param node A {@link Node} object that should be created (=persisted).
     * @return The created {@link Node}.
     */
    Node createNewNode(String parentsUniqueId, Node node);

    /**
     * Updates a node, e.g. if user wishes to add or remove tags from a snapshot {@link Node}
     * @param nodeToUpdate The {@link Node} subject to update.
     * @return The updated {@link Node}.
     */
    Node updateNode(Node nodeToUpdate);

    /**
     * Updates a node, e.g. if user wishes to add or remove tags from a snapshot {@link Node}
     * @param nodeToUpdate The {@link Node} subject to update.
     * @param customTimeForMigration <code>true</code> if the created date of the {@link Node} should be used rather
     *                               than current time.
     * @return The updated {@link Node}.
     */
    Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

    @Deprecated
    void deleteNode(String uniqueNodeId);

    /**
     * Deletes a list of {@link Node}s
     * @param nodeIds List of unique {@link Node} ids.
     */
    void deleteNodes(List<String> nodeIds);

    /**
     * Update a save set (config) {@link Node}, e.g. when new PVs are added.
     * @param configToUpdate The unique id of the {@link Node} subject to update.
     * @param configPvList The list of {@link ConfigPv}s associated with the save set.
     * @return The updated {@link Node}
     */
    Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList);

    /**
     * @return All {@link Tag}s persisted on the remote service.
     */
    List<Tag> getAllTags();

    /**
     *
     * @return All snapshot {@link Node}s persisted on the remote service
     */
    List<Node> getAllSnapshots();

    /**
     * Move a set of {@link Node}s to a new parent {@link Node}
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId The unique id of the parent {@link Node} to which the source {@link Node}s are moved.
     * @return The target {@link Node}.
     */
    Node moveNodes(List<String> sourceNodeIds, String targetNodeId);

    /**
     * Copy a set of {@link Node}s to some parent {@link Node}
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId The unique id of the parent {@link Node} to which the source {@link Node}s are copied.
     * @return The target {@link Node}.
     */
    Node copyNodes(List<String> sourceNodeIds, String targetNodeId);
}
