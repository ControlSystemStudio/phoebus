/*
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.service.saveandrestore.persistence.dao;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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
import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author georgweiss Created 11 Mar 2019
 */
public interface NodeDAO {

    /**
     * Retrieves all child nodes of the specified node.
     *
     * @param uniqueNodeId The unique id of the node
     * @return A potentially empty list of child {@link Node}s.
     */
    List<Node> getChildNodes(String uniqueNodeId);

    /**
     * Retrieve the node identified by the unique node id
     *
     * @param uniqueNodeId The unique node id
     * @return A {@link Node} object
     */
    Node getNode(String uniqueNodeId);

    /**
     * Retrieve the nodes identified by the list of unique node ids
     * @param uniqueNodeIds List of unique node ids
     * @return List of matching nodes
     */
    List<Node> getNodes(List<String> uniqueNodeIds);

    /**
     * Deletes a {@link Node}, folder or configuration. If the node is a folder, the
     * entire sub-tree of the folder is deleted, including the snapshots associated
     * with configurations in the sub-tree.
     *
     * @param nodeId The unique id of the node to delete.
     */
    void deleteNode(String nodeId);

    /**
     * Creates a new node in the tree.
     *
     * @param parentNodeId The unique id of the parent node in which to create the new {@link Node}.
     * @param node         The new {@link Node} to create
     * @return The created node.
     */
    Node createNode(String parentNodeId, Node node);


    Node getParentNode(String uniqueNodeId);

    /**
     * Moves {@link Node}s (folder or config) to a new parent node.
     *
     * @param nodeIds  List of unique node ids subject to move
     * @param targetId Unique id of new parent node
     * @param userName The (account) name of the user performing the operation.
     * @return The target {@link Node} object that is the new parent of the moved source {@link Node}
     */
    Node moveNodes(List<String> nodeIds, String targetId, String userName);

    /**
     * Copies {@link Node}s (folder or config) to some parent node.
     *
     * @param nodeIds  List of unique node ids subject to move
     * @param targetId Unique id of target node
     * @param userName The (account) name of the user performing the operation.
     * @return The target {@link Node} object that is the new parent of the moved source {@link Node}
     */
    Node copyNodes(List<String> nodeIds, String targetId, String userName);


    /**
     * Convenience method
     *
     * @return The root {@link Node} of the tree structure.
     */
    Node getRootNode();

    /**
     * Get snapshots for the specified configuration id.
     *
     * @param uniqueNodeId The database unique id of the configuration (i.e. the snapshots'
     *                     parent node) see {@link Node#getUniqueId()}
     * @return A list of snapshot {@link Node} objects associated with the specified
     * configuration id. Snapshots that have not yet been committed (=saved
     * with comment) are not included.
     */
    List<Node> getSnapshots(String uniqueNodeId);

    /**
     * Saves the {@link org.phoebus.applications.saveandrestore.model.Snapshot} to the persistence layer.
     * @param parentNodeId The unique id of the parent {@link Node} for the new {@link Snapshot}.
     * @param snapshot The {@link Snapshot} data.
     * @return The persisted {@link Snapshot} data.
     */
    Snapshot saveSnapshot(String parentNodeId, Snapshot snapshot);

    /**
     * Updates a {@link Node} with respect to name, description/comment and tags. No other properties of the
     * node can be modified, but last updated date will be set accordingly.
     *
     * @param nodeToUpdate           The {@link Node} subject to update.
     * @param customTimeForMigration A boolean for setting created time. This is intended for migration
     *                               purposes only.
     * @return The {@link Node} object as read from the persistence implementation.
     */
    Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

    /**
     * @return All {@link Tag}s across all {@link Node}s
     */
    List<Tag> getAllTags();

    /**
     * @return All snapshot {@link Node}s, irrespective of location in the tree structure.
     */
    List<Node> getAllSnapshots();

    /**
     * Given a file path like /node1/node2/nodeX, find matching node(s). Since a folder node may
     * contain a folder node and a configuration node with the same name, the returned list may
     * contain one or two {@link Node} objects, or will be <code>null</code> if the path does not correspond
     * to an existing node.
     *
     * @param path A non-null "file path" that must start with a forward slash, otherwise an empty list
     *             is returned. Search will start at the
     *             tree root, i.e. the top level folder named "Save &amp; Restore Root".
     * @return A {@link List    } one or two elements, or <code>null</code>.
     */
    List<Node> getFromPath(String path);

    /**
     * Given an unique node id, find the full path of the node matching the node id. The
     * returned string will start with a forward slash and omit the name of the top level root
     * node named "Save &amp; Restore Root". If the specified node id does not exist, <code>null</code>
     * is returned.
     *
     * @param uniqueNodeId Unique id of a {@link Node}.
     * @return Full path of the {@link Node} if found, otherwise <code>null</code>.
     */
    String getFullPath(String uniqueNodeId);

    /**
     * Saves the {@link org.phoebus.applications.saveandrestore.model.Configuration} to the persistence layer.
     * @param parentNodeId The unique id of the parent {@link Node} for the new {@link Configuration}.
     * @param configuration The {@link Configuration} data.
     * @return The persisted {@link Configuration} data.
     */
    Configuration createConfiguration(String parentNodeId, Configuration configuration);

    /**
     * Retrieves the {@link ConfigurationData} for the specified (unique) id.
     * @param uniqueId Id of the configuration {@link Node}
     * @return A {@link ConfigurationData} object.
     */
    ConfigurationData getConfigurationData(String uniqueId);

    /**
     * Updates an existing {@link ConfigurationData}. In practice an overwrite operation as for instance
     * the {@link ConfigurationData#getPvList()} may contain both added and removed elements compared to
     * the persisted object.
     *
     * @param configuration The object to be updated
     * @return The updated {@link ConfigurationData}
     */
    Configuration updateConfiguration(Configuration configuration);

    /**
     * Retrieves the {@link SnapshotData} for the specified (unique) id.
     * @param uniqueId Id of the snapshot {@link Node}
     * @return A {@link SnapshotData} object.
     */
    SnapshotData getSnapshotData(String uniqueId);

    /**
     * Determines of a move or copy operation is allowed.
     * @param nodesToMove List of {@link Node}s subject to move/copy.
     * @param targetNode The target {@link Node} of the move/copy operation
     * @return <code>true</code> if the list of {@link Node}s can be moved/copied,
     * otherwise <code>false</code>.
     */
    boolean isMoveOrCopyAllowed(List<Node> nodesToMove, Node targetNode);

    /**
     * Finds the {@link Node} corresponding to the parent of last element in the split path. For instance, given a
     * path like /pathelement1/pathelement2/pathelement3/pathelement4, this method returns the {@link Node}
     * for pathelement3. For the special case /pathelement1, this method returns the root {@link Node}.
     * If any of the path elements cannot be found, or if the last path
     * element is not a folder, <code>null</code> is returned.
     *
     * @param parentNode The parent node from which to continue search.
     * @param splitPath  An array of path elements assumed to be ordered from top level
     *                   folder and downwards.
     * @param index      The index in the <code>splitPath</code> to match node names.
     * @return The {@link Node} corresponding to the last path element, or <code>null</code>.
     */
    Node findParentFromPathElements(Node parentNode, String[] splitPath, int index);

    /**
     * Saves the {@link org.phoebus.applications.saveandrestore.model.CompositeSnapshot} to the persistence layer.
     * @param parentNodeId The unique id of the parent {@link Node} for the new {@link CompositeSnapshot}.
     * @param compositeSnapshot The {@link CompositeSnapshot} data.
     * @return The persisted {@link CompositeSnapshot} data.
     */
    CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot);

    /**
     * @param uniqueId Unique id of a {@link Node} of type {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @return A {@link CompositeSnapshotData} object.
     */
    CompositeSnapshotData getCompositeSnapshotData(String uniqueId);

    /**
     * @return List of persisted {@link CompositeSnapshotData} objects.
     */
    List<CompositeSnapshotData> getAllCompositeSnapshotData();

    /**
     * Checks for duplicate PV names in the specified list of snapshot or composite snapshot {@link Node}s.
     * @param snapshotIds list of snapshot or composite snapshot {@link Node}s
     * @return A list if PV names that occur multiple times in the specified snapshot nodes or snapshot nodes
     * referenced in composite snapshots. If no duplicates are found, an empty list is returned.
     */
    List<String> checkForPVNameDuplicates(List<String> snapshotIds);

    /**
     * Updates an existing {@link CompositeSnapshotData}. In practice an overwrite operation as for instance
     * the {@link CompositeSnapshotData#getReferencedSnapshotNodes()} may contain both added and removed elements compared to
     * the persisted object.
     *
     * @param compositeSnapshot The object to be updated
     * @return The updated {@link ConfigurationData}
     */
    CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot);

    /**
     * Aggregates a list of {@link SnapshotItem}s from a composite snapshot node. Note that since a
     * composite snapshot may reference other composite snapshots, the implementation may need to recursively
     * locate all referenced single snapshots.
     * @param compositeSnapshotNodeId The if of an existing composite snapshot {@link Node}
     * @return A list of {@link SnapshotItem}s.
     */
    List<SnapshotItem> getSnapshotItemsFromCompositeSnapshot(String compositeSnapshotNodeId);

    /**
     * Checks if all the referenced snapshot {@link Node}s in a {@link CompositeSnapshot} are
     * of the supported type, i.e. {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * or {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     * @param compositeSnapshot An existing {@link CompositeSnapshot}.
     * @return <code>true</code> if all referenced snapshot {@link Node}s in a {@link CompositeSnapshot} are all
     *      * of the supported type.
     */
    boolean checkCompositeSnapshotReferencedNodeTypes(CompositeSnapshot compositeSnapshot);

    /**
     * Performs a search based on the provided search parameters.
     * @param searchParameters Map of keyword/value pairs defining the search criteria.
     * @return A {@link SearchResult} object with a potentially empty list of {@link Node}s.
     */
    SearchResult search(MultiValueMap<String, String> searchParameters);

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

    /**
     * Deletes all {@link Filter}s.
     */
    void deleteAllFilters();

}
