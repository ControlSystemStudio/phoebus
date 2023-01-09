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

package org.phoebus.applications.saveandrestore;

import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;

import javax.ws.rs.core.MultivaluedMap;
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
     * @param uniqueNodeId The unique id of the {@link Node} to retrieve.
     * @return The {@link Node} object, if it exists.
     */
    Node getNode(String uniqueNodeId);

    List<Node> getCompositeSnapshotReferencedNodes(String uniqueNodeId);

    List<SnapshotItem> getCompositeSnapshotItems(String uniqueNodeId);

    /**
     * @param unqiueNodeId Unique id of a {@link Node}
     * @return The parent {@link Node} of the specified id. May be null if the unique id is associated with the root
     * {@link Node}
     */
    Node getParentNode(String unqiueNodeId);

    /**
     * @param uniqueNodeId Id of an existing {@link Node}
     * @return A list of child {@link Node}s. May be empty.
     * @throws SaveAndRestoreClientException If error occurs when retrieving data
     */
    List<Node> getChildNodes(String uniqueNodeId) throws SaveAndRestoreClientException;

    /**
     * Creates a new {@link Node}
     *
     * @param parentsUniqueId Unique id of the parent {@link Node} for the new {@link Node}
     * @param node            A {@link Node} object that should be created (=persisted).
     * @return The created {@link Node}.
     */
    Node createNewNode(String parentsUniqueId, Node node);

    /**
     * Updates a node, e.g. if user wishes to add or remove tags from a snapshot {@link Node}
     *
     * @param nodeToUpdate The {@link Node} subject to update.
     * @return The updated {@link Node}.
     */
    Node updateNode(Node nodeToUpdate);

    /**
     * Updates a node, e.g. if user wishes to add or remove tags from a snapshot {@link Node}
     *
     * @param nodeToUpdate           The {@link Node} subject to update.
     * @param customTimeForMigration <code>true</code> if the created date of the {@link Node} should be used rather
     *                               than current time.
     * @return The updated {@link Node}.
     */
    Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

    void deleteNode(String uniqueNodeId);

    /**
     * Deletes a list of {@link Node}s
     *
     * @param nodeIds List of unique {@link Node} ids.
     */
    void deleteNodes(List<String> nodeIds);

    /**
     * @return All {@link Tag}s persisted on the remote service.
     */
    List<Tag> getAllTags();

    /**
     * @return All snapshot {@link Node}s persisted on the remote service
     */
    List<Node> getAllSnapshots();

    /**
     * Move a set of {@link Node}s to a new parent {@link Node}
     *
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId  The unique id of the parent {@link Node} to which the source {@link Node}s are moved.
     * @return The target {@link Node}.
     */
    Node moveNodes(List<String> sourceNodeIds, String targetNodeId);

    /**
     * Copy a set of {@link Node}s to some parent {@link Node}
     *
     * @param sourceNodeIds List of unique {@link Node} ids.
     * @param targetNodeId  The unique id of the parent {@link Node} to which the source {@link Node}s are copied.
     * @return The target {@link Node}.
     */
    Node copyNodes(List<String> sourceNodeIds, String targetNodeId);

    String getFullPath(String uniqueNodeId);

    List<Node> getFromPath(String path);

    ConfigurationData getConfigurationData(String nodeId);

    Configuration createConfiguration(String parentNodeId, Configuration configuration);

    Configuration updateConfiguration(Configuration configuration);


    SnapshotData getSnapshotData(String uniqueId);

    Snapshot saveSnapshot(String parentNodeId, Snapshot snapshot);

    CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot);

    CompositeSnapshotData getCompositeSnapshotData(String uniqueId);

    /**
     * Utility for the purpose of checking whether a set of snapshots contain duplicate PV names.
     * The input snapshot ids may refer to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     * and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @param snapshotNodeIds List of {@link Node} ids corresponding to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     *      and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @return A list of PV names that occur more than once across the list of {@link Node}s corresponding
     * to the input. Empty if no duplicates are found.
     */
    List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds);

    /**
     * Updates a composite snapshot. Note that the list of referenced snapshots must be the full list of wanted
     * snapshots, i.e. there is no way to only add new references, or only remove unwanted references.
     * @param compositeSnapshot A {@link CompositeSnapshot} object hold data.
     * @return The updates {@link CompositeSnapshot} object.
     */
    CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot);

    /**
     * Search for {@link Node}s based on the specified search parameters.
     * @param searchParams {@link MultivaluedMap} holding search parameters.
     * @return A {@link SearchResult} with potentially empty list of matching {@link Node}s
     */
    SearchResult search(MultivaluedMap<String, String> searchParams);

    /**
     * Save a new or updated {@link Filter}
     * @param filter The {@link Filter} to save
     * @return The saved {@link Filter}
     */
    Filter saveFilter(Filter filter);

    /**
     * @return All persisted {@link Filter}s.
     */
    List<Filter> getAllFilters();

    /**
     * Deletes a {@link Filter} based on its name.
     * @param name
     */
    void deleteFilter(String name);
}
