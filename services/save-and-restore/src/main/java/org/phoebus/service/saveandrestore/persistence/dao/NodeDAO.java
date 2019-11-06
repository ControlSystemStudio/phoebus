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

import java.util.List;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

/**
 * @author georgweiss Created 11 Mar 2019
 */
public interface NodeDAO {
	
	/**
	 * Retrieves all child nodes of the specified node.
	 * @param uniqueNodeId The unique id of the node
	 * @return A potentially empty list of child {@link Node}s.
	 */
	public List<Node> getChildNodes(String uniqueNodeId);

	/**
	 * Retrieve the node identified by the unique node id
	 * 
	 * @param uniqueNodeId
	 *            The unique node id
	 * @return A {@link Node} object
	 */
	public Node getNode(String uniqueNodeId);

	/**
	 * Deletes a {@link Node}, folder or configuration. If the node is a folder, the
	 * entire sub-tree of the folder is deleted, including the snapshots associated
	 * with configurations in the sub-tree.
	 * 
	 * @param uniqueNodeId
	 *            The node id node to delete.
	 */
	public void deleteNode(String uniqueNodeId);

	/**
	 * Creates a new node in the tree.
	 * 
	 * @param parentsUniqueId
	 *            The unique id of the parent node.
	 * @param node
	 *            The new node to create
	 * @return The created node.
	 * 
	 */
	public Node createNode(String parentsUniqueId, Node node);


	public Node getParentNode(String uniqueNodeId);

	/**
	 * Moves a node (folder or config) to a new parent node.
	 * 
	 * @param sourceNodeId
	 *            The node to move
	 * @param targetNodeId
	 *            The new parent node
	 * @param userName
	 *            The (account) name of the user performing the operation.
	 * @return The target {@link Node} object that is the new parent of the moved source {@link Node}
	 */
	public Node moveNode(String sourceNodeId, String targetNodeId, String userName);

	/**
	 * Updates an existing configuration, e.g. changes its name or list of PVs.
	 * 
	 * @param configToUpdate The configuration to update.
	 * @param configPvList The updated list of {@link ConfigPv}s
	 * @return The updated configuration object
	 */
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList);


	/**
	 * Convenience method
	 * 
	 * @return The root {@link Node} of the tree structure.
	 */
	public Node getRootNode();

	/**
	 * Get snapshots for the specified configuration id.
	 * 
	 * @param uniqueNodeId
	 *            The database unique id of the configuration (i.e. the snapshots'
	 *            parent node) see {@link Node#getUniqueId()}
	 * @return A list of snapshot {@link Node} objects associated with the specified
	 *         configuration id. Snapshots that have not yet been committed (=saved
	 *         with comment) are not included.
	 */
	public List<Node> getSnapshots(String uniqueNodeId);

	/**
	 * Get a snapshot.
	 * 
	 * @param uniqueNodeId
	 *            The database unique id of the snapshot, see
	 *            {@link Node#getUniqueId()}.
	 * @return A {@link Node} object. <code>null</code> is returned if there is
	 *         no snapshot corresponding to the specified snapshot id, or if
	 *         <code>committedOnly=true</code> and for a snapshot with matching id
	 *         that has not been committed.
	 */
	public Node getSnapshot(String uniqueNodeId);
	
	public Node saveSnapshot(String parentsUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment, String userName);
	
	public List<ConfigPv> getConfigPvs(String configUniqueId);
	
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId);
	
	/**
	 * Updates a {@link Node} with respect to name or properties, or both. Node type cannot
	 * be changed, of course.
	 * @param nodeToUpdate The {@link Node} subject to update.
	 * @return The {@link Node} object as read from the persistence implementation.
	 */
	public Node updateNode(Node nodeToUpdate);
	
	/**
	 * Renames a {@link ConfigPv} whereby replacing the current PV name and optionally 
	 * the read-back PV name.
	 * @param currentPvName Identity of the PV name subject to change
	 * @param newPvName New PV name
	 * @param currentReadbackPvName Identity of the read-back PV name. Optional, i.e. may be null.
	 * @param newReadbackPvName New read-back PV name. Optional, i.e. may be null.
	 * @return The updated {@link ConfigPv} object.
	 */
	public ConfigPv updateSingleConfigPv(String currentPvName, String newPvName, String currentReadbackPvName, String newReadbackPvName);

}
