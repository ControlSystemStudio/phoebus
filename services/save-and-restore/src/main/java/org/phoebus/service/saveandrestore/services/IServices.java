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
	
	public Node createNode(String parentsUniqueId, Node node);
	
	public Node getNode(String nodeId);
	
	public List<Node> getChildNodes(String parentsUniqueId);
			
	public List<ConfigPv> getConfigPvs(String configUniqueId);

	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String userName, String comment);
	
	public List<Node> getSnapshots(String configUniqueId);
	
	public Node getSnapshot(String snapshotUniqueId);
	
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId);
		
	public Node moveNode(String uniqueNodeId, String targetUniqueId, String userName);
	
	public void deleteNode(String uniqueNodeId);
	
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList);
	
	public Node updateNode(Node nodeToUpdate);

	public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration);

	public Node getRootNode();
	
	public Node getParentNode(String uniqueNodeId);

	public List<Tag> getTags(String uniqueSnapshotId);

	public List<Tag> getAllTags();

	/**
	 * See {@link org.phoebus.service.saveandrestore.persistence.dao.NodeDAO#getFromPath(String)}
	 * @param path A full path like /topLevelFolder/folderNode/node
	 * @return A list of {@link Node} objects if the full path is valid, otherwise <code>null</code>.
	 */
	public List<Node> getFromPath(String path);

	/**
	 * See {@link org.phoebus.service.saveandrestore.persistence.dao.NodeDAO#getFullPath(String)}
	 * @param uniqueNodeId Non-null unique node id.
	 * @return A full path like /topLevelFolder/folderNode/node corresponding to the specified unique
	 * node id, or <code>null</code> if the full path is invalid.
	 */
	public String getFullPath(String uniqueNodeId);
}
