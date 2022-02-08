/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
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
import org.springframework.beans.factory.annotation.Autowired;

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
        logger.log(Level.INFO, "Obtaining snapshot for config id=" + configUniqueId);
        return nodeDAO.getSnapshots(configUniqueId);
    }

    @Override
    public Node getSnapshot(String snapshotUniqueId) {
        return nodeDAO.getSnapshot(snapshotUniqueId);
    }

    @Override
    public Node createNode(String parentNodeId, Node node) {

        node = nodeDAO.createNode(parentNodeId, node);
        logger.log(Level.INFO, "Created new node: " + node);
        return node;
    }

    @Override
    public Node moveNodes(List<String> sourceNodeIds, String targetNodeId, String userName) {
        logger.info("Moving nodes to target node id=" + targetNodeId);
        return nodeDAO.moveNodes(sourceNodeIds, targetNodeId, userName);
    }

    @Override
    @Deprecated
    public void deleteNode(String nodeId) {
        logger.log(Level.INFO, "Deleting node id=" + nodeId);
        nodeDAO.deleteNode(nodeId);
    }

    public void deleteNodes(List<String> nodeIds){
        nodeDAO.deleteNodes(nodeIds);
    }

    @Override
    public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvs) {
        logger.log(Level.INFO, "Updating configuration unique id=" + configToUpdate.getUniqueId());
        return nodeDAO.updateConfiguration(configToUpdate, configPvs);
    }

    @Override
    public Node updateNode(Node nodeToUpdate) {
        return updateNode(nodeToUpdate, false);
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
        logger.log(Level.INFO, "Updating node unique id=" + nodeToUpdate.getUniqueId());
        return nodeDAO.updateNode(nodeToUpdate, customTimeForMigration);
    }

    @Override
    public Node getNode(String nodeId) {
        logger.log(Level.INFO, "Getting node id=" + nodeId);
        return nodeDAO.getNode(nodeId);
    }

    @Override
    public List<Node> getChildNodes(String nodeUniqueId) {
        logger.log(Level.INFO, "Getting child nodes for node unique id=" + nodeUniqueId);
        return nodeDAO.getChildNodes(nodeUniqueId);
    }

    @Override
    public Node getRootNode() {
        return nodeDAO.getRootNode();
    }

    @Override
    public List<ConfigPv> getConfigPvs(String configUniqueId) {
        logger.log(Level.INFO, "Getting config pvs config id=" + configUniqueId);
        return nodeDAO.getConfigPvs(configUniqueId);
    }

    @Override
    public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {
        logger.log(Level.INFO, "Getting snapshot items for snapshot id=" + snapshotUniqueId);
        return nodeDAO.getSnapshotItems(snapshotUniqueId);
    }

    @Override
    public Node saveSnapshot(String configUniqueId, List<SnapshotItem> snapshotItems, String snapshotName,
                             String userName, String comment) {

        logger.log(Level.INFO, "Saving snapshot for config id=" + configUniqueId);
        logger.log(Level.INFO, "Snapshot name: " + snapshotName);
        logger.log(Level.INFO, "PV count: " + snapshotItems.size());
        long start = System.currentTimeMillis();
        Node node = nodeDAO.saveSnapshot(configUniqueId, snapshotItems, snapshotName, comment, userName);
        logger.log(Level.INFO, "Saved snapshot in " + (System.currentTimeMillis() - start) + " ms");
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
    public List<Node> getFromPath(String path) {
        return nodeDAO.getFromPath(path);
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        return nodeDAO.getFullPath(uniqueNodeId);
    }

    @Override
    public Node copy(List<String> sourceNodes, String targetNodeId, String userName) {
        return nodeDAO.copyNodes(sourceNodes, targetNodeId, userName);
    }
}
