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
package org.phoebus.service.saveandrestore.services.impl;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.services.IServices;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;
import org.phoebus.service.saveandrestore.services.exception.SnapshotNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class Services implements IServices {

	@Autowired
	private NodeDAO nodeDAO;

	private Logger logger = LoggerFactory.getLogger(Services.class);

	@Override
	public Node getParentNode(String uniqueNodeId) {
		return nodeDAO.getParentNode(uniqueNodeId);
	}

	@Override
	public List<Node> getSnapshots(String configUniqueId) {
		logger.info("Obtaining snapshot for config id={}", configUniqueId);
		return nodeDAO.getSnapshots(configUniqueId);
	}

	@Override
	public Node getSnapshot(String snapshotUniqueId) {
		Node snapshot = nodeDAO.getSnapshot(snapshotUniqueId);
		if (snapshot == null) {
			String message = String.format("Snapshot with id=%s not found", snapshotUniqueId);
			logger.error(message);
			throw new SnapshotNotFoundException(message);
		}
		logger.info("Retrieved snapshot id={}", snapshotUniqueId);
		return snapshot;
	}

	@Override
	public Node createNode(String parentsUniqueId, Node node) {

		Node parentFolder = nodeDAO.getNode(parentsUniqueId);
		if (parentFolder == null || !parentFolder.getNodeType().equals(NodeType.FOLDER)) {
			String message = String.format("Cannot create new folder as parent folder with id=%s does not exist.",
					parentsUniqueId);
			logger.error(message);
			throw new IllegalArgumentException(message);
		}

		node = nodeDAO.createNode(parentsUniqueId, node);
		logger.info("Created new node: {}", node);
		return node;
	}

	@Override
	@Transactional
	public Node moveNode(String nodeId, String targetNodeId, String userName) {
		logger.info("Moving node id {} to raget node id {}", nodeId, targetNodeId);
		return nodeDAO.moveNode(nodeId, targetNodeId, userName);
	}

	@Override
	@Transactional
	public void deleteNode(String nodeId) {
		logger.info("Deleting node id={}", nodeId);
		nodeDAO.deleteNode(nodeId);
	}

	@Override
	@Transactional
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvs) {
		logger.info("Updating configuration unique id: {}", configToUpdate.getUniqueId());
		return nodeDAO.updateConfiguration(configToUpdate, configPvs);
	}

	@Override
	public Node updateNode(Node nodeToUpdate) {
		return updateNode(nodeToUpdate, false);
	}

	@Override
	public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
		logger.info("Updating node unique id: {}", nodeToUpdate.getUniqueId());
		return nodeDAO.updateNode(nodeToUpdate, customTimeForMigration);
	}

	@Override
	public Node getNode(String nodeId) {
		logger.info("Getting node {}", nodeId);
		return nodeDAO.getNode(nodeId);
	}

	@Override
	public List<Node> getChildNodes(String nodeUniqueId) {
		logger.info("Getting child nodes for node unique id={}", nodeUniqueId);
		return nodeDAO.getChildNodes(nodeUniqueId);
	}

	@Override
	public Node getRootNode() {
		return nodeDAO.getRootNode();
	}

	@Override
	public List<ConfigPv> getConfigPvs(String configUniqueId) {
		logger.info("Getting config pvs config id id {}", configUniqueId);
		return nodeDAO.getConfigPvs(configUniqueId);
	}

	@Override
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {
		logger.info("Getting snapshot items for snapshot id {}", snapshotUniqueId);
		return nodeDAO.getSnapshotItems(snapshotUniqueId);
	}

	@Override
	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName,
			String userName, String comment) {

		logger.info("Saving snapshot for config id {}", configUniqueId);
		logger.info("Snapshot name: {}, values:", snapshotName);
		for (SnapshotItem snapshotItem : snapshotItems) {
			logger.info(snapshotItem.toString());
		}

		return nodeDAO.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment, userName);
	}

	@Override
	public List<Tag> getTags(String snapshotUniqueId) {
		logger.info("Getting tags of snapshot id={}", snapshotUniqueId);

		return nodeDAO.getTags(snapshotUniqueId);
	}

	@Override
	public List<Tag> getAllTags() {
		logger.info("Getting all tags");

		return nodeDAO.getAllTags();
	}

	@Override
	public List<Node> getFromPath(String path){
		return nodeDAO.getFromPath(path);
	}

	@Override
	public String getFullPath(String uniqueNodeId){
		return nodeDAO.getFullPath(uniqueNodeId);
	}
}
