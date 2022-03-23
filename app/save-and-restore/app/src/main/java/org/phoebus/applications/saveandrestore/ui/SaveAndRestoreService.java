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

package org.phoebus.applications.saveandrestore.ui;

import org.phoebus.applications.saveandrestore.SaveAndRestoreClient;
import org.phoebus.applications.saveandrestore.common.VDisconnectedData;
import org.phoebus.applications.saveandrestore.common.VNoData;
import org.phoebus.applications.saveandrestore.impl.SaveAndRestoreJerseyClient;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SaveAndRestoreService {

    private final ExecutorService executor;

    private final List<NodeChangedListener> nodeChangeListeners = Collections.synchronizedList(new ArrayList());
    private final List<NodeAddedListener> nodeAddedListeners = Collections.synchronizedList(new ArrayList());

    private static final Logger LOG = LoggerFactory.getLogger(SaveAndRestoreService.class.getName());

    private static SaveAndRestoreService instance;

    private final SaveAndRestoreClient saveAndRestoreClient;

    private SaveAndRestoreService() {
        saveAndRestoreClient = new SaveAndRestoreJerseyClient();
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public static SaveAndRestoreService getInstance() {
        if (instance == null) {
            instance = new SaveAndRestoreService();
        }
        return instance;
    }

    public Node getRootNode() {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.getRoot());
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.error("Unable to retrieve root node, cause: " + ie.getMessage());
        }
        return null;
    }

    public Node getNode(String uniqueNodeId) {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.getNode(uniqueNodeId));
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.error("Unable to retrieve node " + uniqueNodeId + ", cause: " + ie.getMessage());
        }
        return null;
    }

    public List<Node> getChildNodes(Node node) {
        Future<List<Node>> future = executor.submit(() -> saveAndRestoreClient.getChildNodes(node));
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.error("Unable to retrieve child nodes of node " + node.getId() + ", cause: " + ie.getMessage());
        }
        return null;
    }

    public Node updateNode(Node nodeToUpdate) throws Exception {
        return updateNode(nodeToUpdate, false);
    }

    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.updateNode(nodeToUpdate, customTimeForMigration));
        Node node = future.get();
        notifyNodeChangeListeners(node);
        return node;
    }

    public Node createNode(String parentsUniqueId, Node newTreeNode) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.createNewNode(parentsUniqueId, newTreeNode));
        notifyNodeAddedListeners(getNode(parentsUniqueId), Arrays.asList(newTreeNode));
        return future.get();
    }

    @Deprecated
    public void deleteNode(String uniqueNodeId) throws Exception {
        executor.submit(() -> saveAndRestoreClient.deleteNode(uniqueNodeId)).get();
    }

    public void deleteNodes(List<String> nodeIds) throws Exception {
        executor.submit(() -> saveAndRestoreClient.deleteNodes(nodeIds)).get();
    }

    public List<ConfigPv> getConfigPvs(String uniqueNodeId) throws Exception {
        Future<List<ConfigPv>> future = executor.submit(() -> saveAndRestoreClient.getConfigPvs(uniqueNodeId));
        return future.get();
    }

    public Node updateSaveSet(Node configToUpdate, List<ConfigPv> configPvList) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.updateConfiguration(configToUpdate, configPvList));
        Node updatedNode = future.get();
        notifyNodeChangeListeners(updatedNode);
        return updatedNode;
    }


    public String getServiceIdentifier() {
        return saveAndRestoreClient.getServiceUrl();
    }

    public List<SnapshotItem> getSnapshotItems(String uniqueNodeId) throws Exception {
        Future<List<SnapshotItem>> future = executor.submit(() -> saveAndRestoreClient.getSnapshotItems(uniqueNodeId));
        return future.get();
    }

    public Node getParentNode(String uniqueNodeId) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.getParentNode(uniqueNodeId));
        return future.get();
    }

    public Node tagSnapshotAsGolden(final Node node, boolean golden) throws Exception {
        Future<Node> future = executor.submit(() -> {
            node.getProperties().put("golden", golden ? "true" : "false");
            return saveAndRestoreClient.updateNode(node);
        });

        Node updatedNode = future.get();
        notifyNodeChangeListeners(updatedNode);
        return updatedNode;
    }

    public Node addTagToSnapshot(final Node node, final Tag tag) throws Exception {
        Future<Node> future = executor.submit(() -> {
            node.addTag(tag);
            return saveAndRestoreClient.updateNode(node);
        });

        Node updatedNode = future.get();
        notifyNodeChangeListeners(updatedNode);
        return updatedNode;
    }

    public Node removeTagFromSnapshot(final Node node, final Tag tag) throws Exception {
        Future<Node> future = executor.submit(() -> {
            node.removeTag(tag);

            return saveAndRestoreClient.updateNode(node);
        });

        Node updatedNode = future.get();
        notifyNodeChangeListeners(updatedNode);
        return updatedNode;
    }

    public Node saveSnapshot(Node saveSetNode, List<SnapshotItem> snapshotItems, String snapshotName, String comment) throws Exception {
        // Some beautifying is needed to ensure successful serialization.
        List<SnapshotItem> beautifiedItems = snapshotItems.stream().map(snapshotItem -> {
            if (snapshotItem.getValue() instanceof VNoData || snapshotItem.getValue() instanceof VDisconnectedData) {
                snapshotItem.setValue(null);
            }
            if (snapshotItem.getReadbackValue() instanceof VNoData || snapshotItem.getReadbackValue() instanceof VDisconnectedData) {
                snapshotItem.setReadbackValue(null);
            }
            return snapshotItem;
        }).collect(Collectors.toList());
        Future<Node> future =
                executor.submit(() -> saveAndRestoreClient.saveSnapshot(saveSetNode.getUniqueId(), beautifiedItems, snapshotName, comment));

        Node savedSnapshot = future.get();
        notifyNodeAddedListeners(saveSetNode, Arrays.asList(savedSnapshot));
        return savedSnapshot;
    }

    public List<Tag> getAllTags() throws Exception {
        Future<List<Tag>> future = executor.submit(() -> saveAndRestoreClient.getAllTags());
        return future.get();
    }

    public List<Node> getAllSnapshots() throws Exception {
        Future<List<Node>> future = executor.submit(() -> saveAndRestoreClient.getAllSnapshots());
        return future.get();
    }

    public void addNodeChangeListener(NodeChangedListener nodeChangeListener) {
        nodeChangeListeners.add(nodeChangeListener);
    }

    public void removeNodeChangeListener(NodeChangedListener nodeChangeListener) {
        nodeChangeListeners.remove(nodeChangeListener);
    }

    private void notifyNodeChangeListeners(Node changedNode) {
        nodeChangeListeners.stream().forEach(listener -> listener.nodeChanged(changedNode));
    }

    public void addNodeAddedListener(NodeAddedListener nodeAddedListener) {
        nodeAddedListeners.add(nodeAddedListener);
    }

    public void removeNodeAddedListener(NodeAddedListener nodeAddedListener) {
        nodeAddedListeners.remove(nodeAddedListener);
    }

    private void notifyNodeAddedListeners(Node parentNode, List<Node> newNodes) {
        nodeAddedListeners.stream().forEach(listener -> listener.nodesAdded(parentNode, newNodes));
    }

    /**
     * Moves the <code>sourceNode</code> to the <code>targetNode</code>. The target {@link Node} may not contain
     * any {@link Node} of same name and type as the source {@link Node}.
     * <p>
     * Once the move completes successfully in the remote service, this method will updated both the source node's parent
     * as well as the target node. This is needed in order to keep the view updated with the changes performed.
     *
     * @param sourceNodes A list of {@link Node}s of type {@link NodeType#FOLDER} or {@link NodeType#CONFIGURATION}.
     * @param targetNode  A {@link Node} of type {@link NodeType#FOLDER}.
     * @return The target {@link Node} containing the source {@link Node} along with any other {@link Node}s
     * @throws Exception
     */
    public Node moveNodes(List<Node> sourceNodes, Node targetNode) throws Exception {
        // Create a reference to the source node's parent before the move
        Node parentNode = getParentNode(sourceNodes.get(0).getUniqueId());
        // Map list of nodes to list of unique ids
        List<String> sourceNodeIds = sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList());
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.moveNodes(sourceNodeIds, targetNode.getUniqueId()));
        Node updatedNode = future.get();
        return updatedNode;
    }

    public Node copyNode(List<Node> sourceNodes, Node targetNode) throws Exception {
        // Create a reference to the source node's parent before the move
        Node parentNode = getParentNode(sourceNodes.get(0).getUniqueId());
        // Map list of nodes to list of unique ids
        List<String> sourceNodeIds = sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList());
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.copyNodes(sourceNodeIds, targetNode.getUniqueId()));
        Node updatedNode = future.get();
        return updatedNode;
    }
}
