/**
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
package org.phoebus.service.saveandrestore.services;

import java.util.List;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;


public interface IServices {

	Node createNode(String parentsUniqueId, Node node);

	Node getNode(String nodeId);

	List<Node> getChildNodes(String parentsUniqueId);

	List<ConfigPv> getConfigPvs(String configUniqueId);

	Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String userName, String comment);

	List<Node> getSnapshots(String configUniqueId);

	Node getSnapshot(String snapshotUniqueId);

	List<SnapshotItem> getSnapshotItems(String snapshotUniqueId);

	default Node moveNodes(List<String> sourceNodeIds, String targetNodeId, String userName){
		throw new RuntimeException("Move nodes operation not supported");
	}

	@Deprecated
	void deleteNode(String uniqueNodeId);

	void deleteNodes(List<String> nodeIds);

	Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList);

	Node updateNode(Node nodeToUpdate);

	Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

	Node getRootNode();

	Node getParentNode(String uniqueNodeId);

	List<Tag> getTags(String uniqueSnapshotId);

	List<Tag> getAllTags();

	List<Node> getAllSnapshots();

	/**
	 * See {@link org.phoebus.service.saveandrestore.persistence.dao.NodeDAO#getFromPath(String)}
	 * @param path A full path like /topLevelFolder/folderNode/node
	 * @return A list of {@link Node} objects if the full path is valid, otherwise <code>null</code>.
	 */
	List<Node> getFromPath(String path);

	/**
	 * See {@link org.phoebus.service.saveandrestore.persistence.dao.NodeDAO#getFullPath(String)}
	 * @param uniqueNodeId Non-null unique node id.
	 * @return A full path like /topLevelFolder/folderNode/node corresponding to the specified unique
	 * node id, or <code>null</code> if the full path is invalid.
	 */
	String getFullPath(String uniqueNodeId);

	/**
	 * Copies the list of source {@link Node}s to the target identified by the (unique) <code>targetNodeId</code>.
	 * This operation is potentially expensive as it performs a deep copy, i.e. all child nodes of folder
	 * and save set {@link Node}s are considered. Client should consider asynchronous call.
	 * @param sourceNodes List of source node ids to copy
	 * @param targetNodeId Target node of the copy operation
	 * @param userName Id of the user performing the operation
	 */
	default Node copy(List<String> sourceNodes, String targetNodeId, String userName){
		throw new RuntimeException("Copy operation not supported");
	}
}
