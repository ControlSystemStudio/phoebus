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
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SaveAndRestoreService {

    private final ExecutorService executor;

    private final List<NodeChangedListener> nodeChangeListeners = Collections.synchronizedList(new ArrayList<>());
    private final List<NodeAddedListener> nodeAddedListeners = Collections.synchronizedList(new ArrayList<>());

    private static final Logger LOG = Logger.getLogger(SaveAndRestoreService.class.getName());

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
        Future<Node> future = executor.submit(saveAndRestoreClient::getRoot);
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.log(Level.SEVERE, "Unable to retrieve root node, cause: " + ie.getMessage());
        }
        return null;
    }

    public Node getNode(String uniqueNodeId) {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.getNode(uniqueNodeId));
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.log(Level.SEVERE,"Unable to retrieve node " + uniqueNodeId + ", cause: " + ie.getMessage());
        }
        return null;
    }

    public List<Node> getChildNodes(Node node) {
        Future<List<Node>> future = executor.submit(() -> saveAndRestoreClient.getChildNodes(node.getUniqueId()));
        try {
            return future.get();
        } catch (Exception ie) {
            LOG.log(Level.SEVERE,"Unable to retrieve child nodes of node " + node.getUniqueId() + ", cause: " + ie.getMessage());
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

    public Node createNode(String parentNodeId, Node newTreeNode) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.createNewNode(parentNodeId, newTreeNode));
        notifyNodeAddedListeners(getNode(parentNodeId), Collections.singletonList(newTreeNode));
        return future.get();
    }

    public void deleteNode(String uniqueNodeId) throws Exception {
        executor.submit(() -> saveAndRestoreClient.deleteNode(uniqueNodeId)).get();
    }

    public void deleteNodes(List<String> nodeIds) throws Exception {
        executor.submit(() -> saveAndRestoreClient.deleteNodes(nodeIds)).get();
    }

    public String getServiceIdentifier() {
        return saveAndRestoreClient.getServiceUrl();
    }

    public Node getParentNode(String uniqueNodeId) throws Exception {
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.getParentNode(uniqueNodeId));
        return future.get();
    }

    public Node tagSnapshotAsGolden(final Node node, boolean golden) throws Exception {
        Future<Node> future = executor.submit(() -> {
            String userName = System.getProperty("user.name");
            List<Tag> tags = node.getTags();
            Tag goldenTag;
            if (tags == null) {
                tags = new ArrayList<>();
                node.setTags(tags);
            }
            if (node.hasTag(Tag.GOLDEN)) {
                goldenTag = tags.stream().filter(t -> t.getName().equals(Tag.GOLDEN)).findFirst().get();
            } else {
                goldenTag = Tag.goldenTag(userName);
            }

            if (golden) {
                tags.add(goldenTag);
            } else {
                tags.remove(goldenTag);
            }

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

    public Configuration createConfiguration(final Node parentNode, final Configuration configuration) throws Exception {
        Future<Configuration> future = executor.submit(() -> saveAndRestoreClient.createConfiguration(parentNode.getUniqueId(), configuration));
        Configuration newConfiguration = future.get();
        notifyNodeChangeListeners(parentNode);
        return newConfiguration;
    }

    public Configuration updateConfiguration(Configuration configuration) throws Exception {
        Future<Configuration> future = executor.submit(() -> saveAndRestoreClient.updateConfiguration(configuration));
        Configuration updatedConfiguration = future.get();
        // Associated configuration Node may have a new name
        notifyNodeChangeListeners(configuration.getConfigurationNode());
        return updatedConfiguration;
    }

    public List<Tag> getAllTags() throws Exception {
        Future<List<Tag>> future = executor.submit(saveAndRestoreClient::getAllTags);
        return future.get();
    }

    public List<Node> getAllSnapshots() throws Exception {
        Future<List<Node>> future = executor.submit(saveAndRestoreClient::getAllSnapshots);
        return future.get();
    }

    public void addNodeChangeListener(NodeChangedListener nodeChangeListener) {
        nodeChangeListeners.add(nodeChangeListener);
    }

    public void removeNodeChangeListener(NodeChangedListener nodeChangeListener) {
        nodeChangeListeners.remove(nodeChangeListener);
    }

    private void notifyNodeChangeListeners(Node changedNode) {
        nodeChangeListeners.forEach(listener -> listener.nodeChanged(changedNode));
    }

    public void addNodeAddedListener(NodeAddedListener nodeAddedListener) {
        nodeAddedListeners.add(nodeAddedListener);
    }

    public void removeNodeAddedListener(NodeAddedListener nodeAddedListener) {
        nodeAddedListeners.remove(nodeAddedListener);
    }

    private void notifyNodeAddedListeners(Node parentNode, List<Node> newNodes) {
        nodeAddedListeners.forEach(listener -> listener.nodesAdded(parentNode, newNodes));
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
     * @throws Exception if move operation fails.
     */
    public Node moveNodes(List<Node> sourceNodes, Node targetNode) throws Exception {
        // Map list of nodes to list of unique ids
        List<String> sourceNodeIds = sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList());
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.moveNodes(sourceNodeIds, targetNode.getUniqueId()));
        return future.get();
    }

    public Node copyNode(List<Node> sourceNodes, Node targetNode) throws Exception {
        // Map list of nodes to list of unique ids
        List<String> sourceNodeIds = sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList());
        Future<Node> future = executor.submit(() -> saveAndRestoreClient.copyNodes(sourceNodeIds, targetNode.getUniqueId()));
        return future.get();
    }

    public ConfigurationData getConfiguration(String nodeId) {
        Future<ConfigurationData> future = executor.submit(() -> saveAndRestoreClient.getConfigurationData(nodeId));
        try {
            return future.get();
        } catch (Exception e) {
            // Configuration might not exist yet
            return null;
        }
    }

    public SnapshotData getSnapshot(String nodeId) {
        Future<SnapshotData> future = executor.submit(() -> saveAndRestoreClient.getSnapshotData(nodeId));
        try {
            return future.get();
        } catch (Exception e) {
            // SnapshotData might not exist yet
            return null;
        }
    }

    public Snapshot saveSnapshot(Node configurationNode, Snapshot snapshot) throws Exception {
        // Some beautifying is needed to ensure successful serialization.
        List<SnapshotItem> beautifiedItems = snapshot.getSnapshotData().getSnapshotItems().stream().map(snapshotItem -> {
            if (snapshotItem.getValue() instanceof VNoData || snapshotItem.getValue() instanceof VDisconnectedData) {
                snapshotItem.setValue(null);
            }
            if (snapshotItem.getReadbackValue() instanceof VNoData || snapshotItem.getReadbackValue() instanceof VDisconnectedData) {
                snapshotItem.setReadbackValue(null);
            }
            return snapshotItem;
        }).collect(Collectors.toList());
        snapshot.getSnapshotData().setSnasphotItems(beautifiedItems);
        Future<Snapshot> future = executor.submit(() -> saveAndRestoreClient.saveSnapshot(configurationNode.getUniqueId(), snapshot));
        Snapshot updatedSnapshot = future.get();
        // Notify listeners as the configuration node has a new child node.
        notifyNodeChangeListeners(configurationNode);
        return updatedSnapshot;
    }


    public CompositeSnapshotData getCompositeSnapshot(String compositeSnapshotNodeUniqueId) throws Exception{
        Future<CompositeSnapshotData> future =
                executor.submit(() -> saveAndRestoreClient.getCompositeSnapshotData(compositeSnapshotNodeUniqueId));
       return future.get();
    }

    public List<Node> getCompositeSnapshotNodes(String compositeSnapshotNodeUniqueId) throws Exception{
        Future<List<Node>> future =
                executor.submit(() -> saveAndRestoreClient.getCompositeSnapshotReferencedNodes(compositeSnapshotNodeUniqueId));
        return future.get();
    }

    public List<SnapshotItem> getCompositeSnapshotItems(String compositeSnapshotNodeUniqueId) throws Exception{
        Future<List<SnapshotItem>> future =
                executor.submit(() -> saveAndRestoreClient.getCompositeSnapshotItems(compositeSnapshotNodeUniqueId));
        return future.get();
    }

    public CompositeSnapshot saveCompositeSnapshot(Node parentNode, CompositeSnapshot compositeSnapshot) throws Exception{
        Future<CompositeSnapshot> future =
                executor.submit(() -> saveAndRestoreClient.createCompositeSnapshot(parentNode.getUniqueId(), compositeSnapshot));
        CompositeSnapshot newCompositeSnapshot = future.get();
        notifyNodeChangeListeners(parentNode);
        return newCompositeSnapshot;
    }

    public CompositeSnapshot updateCompositeSnapshot(final CompositeSnapshot compositeSnapshot) throws Exception{
        Future<CompositeSnapshot> future = executor.submit(() -> saveAndRestoreClient.updateCompositeSnapshot(compositeSnapshot));
        CompositeSnapshot updatedCompositeSnapshot = future.get();
        // Associated composite snapshot Node may have a new name
        notifyNodeChangeListeners(updatedCompositeSnapshot.getCompositeSnapshotNode());
        return updatedCompositeSnapshot;
    }

    /**
     * Utility for the purpose of checking whether a set of snapshots contain duplicate PV names.
     * The input snapshot ids may refer to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     * and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @param snapshotNodeIds List of {@link Node} ids corresponding to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     *      and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @return A list of PV names that occur more than once across the list of {@link Node}s corresponding
     * to the input. Empty if no duplicates are found.
     */
    public List<String> checkCompositeSnapshotConsistency(List<String> snapshotNodeIds) throws Exception{
        return saveAndRestoreClient.checkCompositeSnapshotConsistency(snapshotNodeIds);
    }

    /**
     * Search for {@link Node}s based on the specified search parameters.
     * @param searchParams {@link MultivaluedMap} holding search parameters.
     * @return A {@link SearchResult} with potentially empty list of matching {@link Node}s
     */
    public SearchResult search(MultivaluedMap<String, String> searchParams) throws Exception{
        return saveAndRestoreClient.search(searchParams);
    }

    /**
     * Save a new or updated {@link Filter}
     * @param filter The {@link Filter} to save
     * @return The saved {@link Filter}
     */
    public Filter saveFilter(Filter filter) throws Exception{
        Future<Filter> future =
                executor.submit(() -> saveAndRestoreClient.saveFilter(filter));
        return future.get();
    }

    /**
     * @return All persisted {@link Filter}s.
     */
    public List<Filter> getAllFilters() throws Exception{
        Future<List<Filter>> future =
                executor.submit(() -> saveAndRestoreClient.getAllFilters());
        return future.get();
    }

    /**
     * Deletes a {@link Filter} based on its name.
     * @param name
     */
    public void deleteFilter(final String name) throws Exception{
        executor.submit(() -> saveAndRestoreClient.deleteFilter(name)).get();
    }
}
