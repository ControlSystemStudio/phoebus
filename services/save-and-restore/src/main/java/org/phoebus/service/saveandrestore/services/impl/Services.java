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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Services implements IServices {

	@Autowired
	@SuppressWarnings("unused")
	private NodeDAO nodeDAO;

	private final Logger logger = Logger.getLogger(Services.class.getName());

	@Override
	public Node getParentNode(String uniqueNodeId) {
		return nodeDAO.getParentNode(uniqueNodeId);
	}

	@Override
	public List<Node> getSnapshots(String configUniqueId) {
		logger.log(Level.INFO,"Obtaining snapshot for config id="+ configUniqueId);
		return nodeDAO.getSnapshots(configUniqueId);
	}

	@Override
	public Node getSnapshot(String snapshotUniqueId) {
		Node snapshot = nodeDAO.getSnapshot(snapshotUniqueId);
		if (snapshot == null) {
			String message = String.format("Snapshot with id=%s not found", snapshotUniqueId);
			logger.log(Level.WARNING, message);
			throw new SnapshotNotFoundException(message);
		}
		logger.log(Level.INFO,"Retrieved snapshot id=" + snapshotUniqueId);
		return snapshot;
	}

	@Override
	@Transactional
	public Node createNode(String parentsUniqueId, Node node) {

		Node parentNode = getNode(parentsUniqueId);

		if (parentNode == null) {
			throw new NodeNotFoundException(
					String.format("Cannot create new node as parent unique_id=%s does not exist.", parentsUniqueId));
		}

		// Folder and Config can only be created in a Folder
		if ((node.getNodeType().equals(NodeType.FOLDER) || node.getNodeType().equals(NodeType.CONFIGURATION))
				&& !parentNode.getNodeType().equals(NodeType.FOLDER)) {
			throw new IllegalArgumentException(
					"Parent node is not a folder, cannot create new node of type " + node.getNodeType());
		}
		// Snapshot can only be created in Config
		if (node.getNodeType().equals(NodeType.SNAPSHOT) && !parentNode.getNodeType().equals(NodeType.CONFIGURATION)) {
			throw new IllegalArgumentException("Parent node is not a configuration, cannot create snapshot");
		}

		// The node to be created cannot have same name and type as any of the parent's
		// child nodes
		List<Node> parentsChildNodes = getChildNodes(parentNode.getUniqueId());

		if (!isNodeNameValid(node, parentsChildNodes)) {
			throw new IllegalArgumentException("Node of same name and type already exists in parent node.");
		}

		node = nodeDAO.createNode(parentNode, node);
		logger.log(Level.INFO,"Created new node: " + node);
		return node;
	}

	@Override
	@Transactional
	public Node moveNode(String sourceNodeId, String targetNodeId, String userName) {
		logger.log(Level.INFO, "Moving node id=" + sourceNodeId + " to target node id=" + targetNodeId);
		Node sourceNode = getNode(sourceNodeId);

		if (sourceNode == null) {
			throw new NodeNotFoundException(String.format("Source node with unqiue id=%s not found", sourceNodeId));
		}

		if(sourceNode.getNodeType().equals(NodeType.SNAPSHOT)) {
			throw new IllegalArgumentException(String.format("Moving node of type %s not supported", NodeType.SNAPSHOT.toString()));
		}

		Node targetNode = getNode(targetNodeId);
		if(targetNode == null || !targetNode.getNodeType().equals(NodeType.FOLDER)) {
			throw new IllegalArgumentException("Target node does not exist or is not a folder");
		}

		List<Node> parentsChildNodes = getChildNodes(targetNodeId);
		if (!isNodeNameValid(sourceNode, parentsChildNodes)) {
			throw new IllegalArgumentException("Node of same name and type already exists in target node.");
		}
		return nodeDAO.moveNode(sourceNode, targetNode, userName);
	}

	@Override
	@Transactional
	public Node moveNodes(List<String> nodes, String targetUniqueId, String userName){
		logger.info("Moving nodes to target node id=" + targetUniqueId);
		if(userName.isEmpty() || targetUniqueId.isEmpty()){
			throw new IllegalArgumentException("Target node id or user name (or both) empty");
		}
		Node targetNode = getNode(targetUniqueId);

		if (targetNode == null) {
			throw new NodeNotFoundException(String.format("Target node with unqiue id=%s not found", targetUniqueId));
		}

		return null;
	}

	@Override
	@Transactional
	public void deleteNode(String uniqueNodeId) {
		logger.log(Level.INFO,"Deleting node id=" + uniqueNodeId);
		Node nodeToDelete = getNode(uniqueNodeId);
		if(nodeToDelete == null) {
			throw new NodeNotFoundException(String.format("Node with id=%s not found", uniqueNodeId));
		}
		if (nodeToDelete.getId() == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Root node cannot be deleted");
		}

		nodeDAO.deleteNode(nodeToDelete);
	}

	@Override
	@Transactional
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvs) {
		logger.log(Level.INFO, "Updating configuration unique id=" + configToUpdate.getUniqueId());
		return nodeDAO.updateConfiguration(configToUpdate, configPvs);
	}

	@Override
	@Transactional
	public Node updateNode(Node nodeToUpdate) {
		return updateNode(nodeToUpdate, false);
	}

	@Override
	@Transactional
	public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
		logger.log(Level.INFO,"Updating node unique id=" + nodeToUpdate.getUniqueId());
		Node persistedNode = getNode(nodeToUpdate.getUniqueId());

		if(persistedNode == null) {
			throw new NodeNotFoundException(String.format("Node with unique id=%s not found", nodeToUpdate.getUniqueId()));
		}

		if (persistedNode.getId() == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Cannot update root node");
		}

		if(!persistedNode.getNodeType().equals(nodeToUpdate.getNodeType())) {
			throw new IllegalArgumentException("Changing node type is not supported");
		}

		Node parentNode = getParentNode(persistedNode.getUniqueId());

		List<Node> parentsChildNodes = getChildNodes(parentNode.getUniqueId());

		if(!isNodeNameValid(nodeToUpdate, parentsChildNodes)) {
			throw new IllegalArgumentException(String.format("A node with same type and name (%s) already exists in the same folder", nodeToUpdate.getName()));
		}
		return nodeDAO.updateNode(nodeToUpdate, customTimeForMigration);
	}

	@Override
	public Node getNode(String nodeId) {
		logger.log(Level.INFO,"Getting node id=" + nodeId);
		return nodeDAO.getNode(nodeId);
	}

	@Override
	public List<Node> getChildNodes(String nodeUniqueId) {
		logger.log(Level.INFO,"Getting child nodes for node unique id=" + nodeUniqueId);
		return nodeDAO.getChildNodes(nodeUniqueId);
	}

	@Override
	public Node getRootNode() {
		return nodeDAO.getRootNode();
	}

	@Override
	public List<ConfigPv> getConfigPvs(String configUniqueId) {
		logger.log(Level.INFO,"Getting config pvs config id=" + configUniqueId);
		return nodeDAO.getConfigPvs(configUniqueId);
	}

	@Override
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {
		logger.log(Level.INFO,"Getting snapshot items for snapshot id=" + snapshotUniqueId);
		return nodeDAO.getSnapshotItems(snapshotUniqueId);
	}

	@Override
	@Transactional
	public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName,
			String userName, String comment) {

		logger.log(Level.INFO,"Saving snapshot for config id=" + configUniqueId);
		logger.log(Level.INFO,"Snapshot name: " + snapshotName);
		logger.log(Level.INFO,"PV count: " + snapshotItems.size());
		long start = System.currentTimeMillis();
		Node node = nodeDAO.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment, userName);
		logger.log(Level.INFO,"Saved snapshot in " +  (System.currentTimeMillis() - start) + " ms");
		return node;
	}

	@Override
	public List<Tag> getTags(String snapshotUniqueId) {
		logger.log(Level.INFO, "Getting tags of snapshot id=" + snapshotUniqueId);

		return nodeDAO.getTags(snapshotUniqueId);
	}

	@Override
	public List<Tag> getAllTags() {
		logger.info("Getting all tags");

		return nodeDAO.getAllTags();
	}

	@Override
	public List<Node> getAllSnapshots() {
		logger.info("Getting all snapshots");

		return nodeDAO.getAllSnapshots();
	}

	@Override
	public List<Node> getFromPath(String path){
		return nodeDAO.getFromPath(path);
	}

	@Override
	public String getFullPath(String uniqueNodeId){
		return nodeDAO.getFullPath(uniqueNodeId);
	}

	@Override
	public void copy(List<Node> sourceNodes, String targetNodeId){
		// First get the target node
		Node targetNode = getNode(targetNodeId);
		if(targetNode == null){
			throw new NodeNotFoundException("Target node " + targetNodeId + " not found");
		}
		if(!targetNode.getNodeType().equals(NodeType.FOLDER)){
			throw new IllegalArgumentException("Target node " + targetNodeId + " is not a folder node");
		}
		List<Node> targetsChildNodes = getChildNodes(targetNodeId);
		// TODO: finish up!
	}

	private boolean isNodeNameValid(Node nodeToCheck, List<Node> parentsChildNodes) {
		for (Node node : parentsChildNodes) {
			if (node.getName().equals(nodeToCheck.getName()) &&
					node.getNodeType().equals(nodeToCheck.getNodeType())) {
				return false;
			}
		}
		return true;
	}

}
