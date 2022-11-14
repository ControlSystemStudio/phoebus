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

package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

public class ElasticsearchDAO implements NodeDAO {

    @SuppressWarnings("unused")
    @Autowired
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @SuppressWarnings("unused")
    @Autowired
    private ConfigurationDataRepository configurationDataRepository;

    @SuppressWarnings("unused")
    @Autowired
    private SnapshotDataRepository snapshotDataRepository;

    private static final Logger logger = Logger.getLogger(ElasticsearchDAO.class.getName());


    @Override
    public List<Node> getChildNodes(String uniqueNodeId) {
        Optional<ESTreeNode> elasticsearchNode =
                elasticsearchTreeRepository.findById(uniqueNodeId);
        if (elasticsearchNode.isEmpty()) {
            return null;
        }
        if (elasticsearchNode.get().getChildNodes() == null) {
            return Collections.emptyList();
        }
        Iterable<ESTreeNode> childNodesIterable = elasticsearchTreeRepository.findAllById(elasticsearchNode.get().getChildNodes());
        List<Node> childNodes = new ArrayList<>();
        childNodesIterable.forEach(element -> childNodes.add(element.getNode()));
        return childNodes;
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        Optional<ESTreeNode> optional =
                elasticsearchTreeRepository.findById(uniqueNodeId);
        if (optional.isEmpty()) {
            return null;
        }
        ESTreeNode elasticsearchNode = optional.get();
        return elasticsearchNode.getNode();
    }

    @Override
    public void deleteNode(String nodeId) {
        Node nodeToDelete = getNode(nodeId);
        if (nodeToDelete == null) {
            throw new NodeNotFoundException("Cannot delete non-existing node");
        } else if (nodeToDelete.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)) {
            throw new IllegalArgumentException("Root node cannot be deleted");
        }
        deleteNode(nodeToDelete);
    }

    @Override
    public Node createNode(String parentNodeId, Node node) {
        // Retrieve parent
        Optional<ESTreeNode> parentNode = elasticsearchTreeRepository.findById(parentNodeId);
        if (parentNode.isEmpty()) {
            throw new NodeNotFoundException(
                    String.format("Cannot create new node as parent unique_id=%s does not exist.", parentNodeId));
        }
        Node parent = parentNode.get().getNode();
        // Root node may only contain FOLDER nodes
        if (parent.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID) && (node.getNodeType().equals(NodeType.CONFIGURATION) ||
                node.getNodeType().equals(NodeType.SNAPSHOT))) {
            throw new IllegalArgumentException(
                    "Root folder may only contain folder nodes.");
        }
        // Folder and Config can only be created in a Folder
        if ((node.getNodeType().equals(NodeType.FOLDER) || node.getNodeType().equals(NodeType.CONFIGURATION))
                && !parent.getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException(
                    "Parent node is not a folder, cannot create new node of type " + node.getNodeType());
        }
        // Snapshot can only be created in Config
        if (node.getNodeType().equals(NodeType.SNAPSHOT) && !parent.getNodeType().equals(NodeType.CONFIGURATION)) {
            throw new IllegalArgumentException("Parent node is not a configuration, cannot create snapshot");
        }

        // The node to be created cannot have same the name and type as any of the parent's
        // child nodes
        List<Node> parentsChildNodes = getChildNodes(parentNodeId);
        if (!isNodeNameValid(node, parentsChildNodes)) {
            throw new IllegalArgumentException("Node of same name and type already exists in parent node.");
        }

        // To facilitate migration, unique id and created date are set only if null.
        if(node.getCreated() == null){
            node.setCreated(new Date());
        }
        if(node.getUniqueId() == null){
            node.setUniqueId(UUID.randomUUID().toString());
        }
        ESTreeNode newNode = new ESTreeNode();
        newNode.setNode(node);

        elasticsearchTreeRepository.save(newNode);

        if (parentNode.get().getChildNodes() == null) {
            parentNode.get().setChildNodes(new ArrayList<>());
        }
        parentNode.get().getChildNodes().add(node.getUniqueId());
        elasticsearchTreeRepository.save(parentNode.get());

        return node;
    }

    /**
     * Checks if a {@link Node} name is valid. A {@link Node} may not have the same name as another {@link Node}
     * in the same parent {@link Node}, if they are of the same {@link NodeType}.
     *
     * @param nodeToCheck       The {@link Node} object subject to a check. It may be an existing {@link Node}, or a
     *                          new {@link Node} being created.
     * @param parentsChildNodes The list of existing {@link Node}s in the parent {@link Node}
     * @return <code>true</code> if the <code>nodeToCheck</code> has a "valid" name, otherwise <code>false</code>.
     */
    protected boolean isNodeNameValid(Node nodeToCheck, List<Node> parentsChildNodes) {
        if (parentsChildNodes == null || parentsChildNodes.isEmpty()) {
            return true;
        }
        for (Node node : parentsChildNodes) {
            if (node.getName().equals(nodeToCheck.getName()) &&
                    node.getNodeType().equals(nodeToCheck.getNodeType())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Node getParentNode(String uniqueNodeId) {
        if (uniqueNodeId.equals(ROOT_FOLDER_UNIQUE_ID)) { // Root node is its own parent
            return getRootNode();
        }
        ESTreeNode elasticsearchTreeNode = elasticsearchTreeRepository.getParentNode(uniqueNodeId);
        if (elasticsearchTreeNode != null) {
            return elasticsearchTreeNode.getNode();
        } else {
            throw new NodeNotFoundException("Parent node of " + uniqueNodeId + " could not be determined");
        }
    }

    @Override
    public Node moveNodes(List<String> nodeIds, String targetId, String userName) {
        Optional<ESTreeNode> targetNode = elasticsearchTreeRepository.findById(targetId);
        if (targetNode.isEmpty()) {
            throw new NodeNotFoundException(String.format("Target node with unique id=%s not found", targetId));
        }

        if (!targetNode.get().getNode().getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException("Move not allowed: target node is not a folder");
        }

        List<Node> sourceNodes = new ArrayList<>();
        nodeIds.forEach(id -> {
            Optional<ESTreeNode> esTreeNode = elasticsearchTreeRepository.findById(id);
            esTreeNode.ifPresent(treeNode -> sourceNodes.add(treeNode.getNode()));
        });

        if (sourceNodes.size() != nodeIds.size()) {
            throw new IllegalArgumentException("At least one unique node id not found.");
        }

        if (!isMoveOrCopyAllowed(sourceNodes, targetNode.get().getNode())) {
            throw new IllegalArgumentException("Prerequisites for moving source node(s) not met.");
        }

        // Remove source nodes from the list of child nodes in parent node.
        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(sourceNodes.get(0).getUniqueId());
        if (parentNode == null) {
            throw new RuntimeException("Parent node of source node " + sourceNodes.get(0).getUniqueId() + " not found. Should not happen.");
        }
        parentNode.getChildNodes().removeAll(sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList()));
        parentNode = elasticsearchTreeRepository.save(parentNode);

        // Update the target node to include the source nodes in its list of child nodes
        if (targetNode.get().getChildNodes() == null) {
            targetNode.get().setChildNodes(new ArrayList<>());
        }

        targetNode.get().getChildNodes().addAll(sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList()));
        ESTreeNode updatedTargetNode = elasticsearchTreeRepository.save(targetNode.get());

        return updatedTargetNode.getNode();
    }

    @Override
    public Node copyNodes(List<String> nodeIds, String targetId, String userName) {
        Optional<ESTreeNode> targetNode = elasticsearchTreeRepository.findById(targetId);
        if (targetNode.isEmpty()) {
            throw new NodeNotFoundException(String.format("Target node with unique id=%s not found", targetId));
        }

        if (!targetNode.get().getNode().getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException("Move not allowed: target node is not a folder");
        }

        List<Node> sourceNodes = new ArrayList<>();
        nodeIds.forEach(id -> {
            Optional<ESTreeNode> esTreeNode = elasticsearchTreeRepository.findById(id);
            esTreeNode.ifPresent(treeNode -> sourceNodes.add(treeNode.getNode()));
        });

        if (sourceNodes.size() != nodeIds.size()) {
            throw new IllegalArgumentException("At least one unique node id not found.");
        }

        if (!isMoveOrCopyAllowed(sourceNodes, targetNode.get().getNode())) {
            throw new IllegalArgumentException("Prerequisites for copying source node(s) not met.");
        }

        sourceNodes.forEach(sourceNode -> copyNode(sourceNode, targetNode.get().getNode(), userName));

        return targetNode.get().getNode();
    }

    private void copyNode(Node sourceNode, Node targetNode, String userName) {
        // In order to copy, we first create a shallow clone of the source node
        Node sourceNodeClone = Node.builder()
                .name(sourceNode.getName())
                .nodeType(sourceNode.getNodeType())
                .userName(userName)
                .tags(sourceNode.getTags())
                .description(sourceNode.getDescription())
                .build();
        final Node newSourceNode = createNode(targetNode.getUniqueId(), sourceNodeClone);

        if (sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            ConfigurationData sourceConfiguration = getConfigurationData(sourceNode.getUniqueId());
            copyConfigurationData(newSourceNode, sourceConfiguration);
        } else if (sourceNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            SnapshotData snapshotData = getSnapshotData(sourceNode.getUniqueId());
            copySnapshotData(newSourceNode, snapshotData);
        }

        if (sourceNode.getNodeType().equals(NodeType.FOLDER) || sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            List<Node> childNodes = getChildNodes(sourceNode.getUniqueId());
            childNodes.forEach(childNode -> copyNode(childNode, newSourceNode, userName));
        }
    }

    /**
     * Copies a {@link ConfigurationData}.
     *
     * @param targetConfigurationNode The configuration {@link Node} with which the copied {@link ConfigurationData} should be
     *                                associated. This must already exist in the Elasticsearch index.
     * @param sourceConfiguration     The source {@link ConfigurationData}
     */
    private ConfigurationData copyConfigurationData(Node targetConfigurationNode, ConfigurationData sourceConfiguration) {
        ConfigurationData clonedConfigurationData = ConfigurationData.clone(sourceConfiguration);
        clonedConfigurationData.setUniqueId(targetConfigurationNode.getUniqueId());
        return configurationDataRepository.save(clonedConfigurationData);
    }

    private SnapshotData copySnapshotData(Node targetSnapshotNode, SnapshotData snapshotData) {
        SnapshotData clonedSnapshotData = SnapshotData.clone(snapshotData);
        clonedSnapshotData.setUniqueId(targetSnapshotNode.getUniqueId());
        return snapshotDataRepository.save(clonedSnapshotData);
    }

    @Override
    public Node getRootNode() {
        ESTreeNode root = elasticsearchTreeRepository.findById(ROOT_FOLDER_UNIQUE_ID).get();
        return root.getNode();
    }

    @Override
    public List<Node> getSnapshots(String uniqueNodeId) {
        Optional<ESTreeNode> configNodeOptional =
                elasticsearchTreeRepository.findById(uniqueNodeId);
        if (configNodeOptional.isEmpty()) {
            throw new IllegalArgumentException("Cannot get snapshots for config with id " + uniqueNodeId + " as it does not exist");
        }
        if (configNodeOptional.get().getChildNodes() == null) {
            return Collections.emptyList();
        } else {
            return getChildNodes(configNodeOptional.get().getNode().getUniqueId());
        }
    }

    @Override
    public Node updateNode(final Node nodeToUpdate, boolean customTimeForMigration) {
        Optional<ESTreeNode> nodeOptional = elasticsearchTreeRepository.findById(nodeToUpdate.getUniqueId());
        if (nodeOptional.isEmpty()) {
            throw new IllegalArgumentException(String.format("Node with unique id=%s not found", nodeToUpdate.getUniqueId()));
        } else if (nodeOptional.get().getNode().getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)) {
            throw new IllegalArgumentException("Updating root node is not allowed");
        }

        Node existingNode = nodeOptional.get().getNode();

        // Changing node type is not supported
        if (!existingNode.getNodeType().equals(nodeToUpdate.getNodeType())) {
            throw new IllegalArgumentException("Changing node type is not allowed");
        }

        // Retrieve parent node and its child nodes
        ESTreeNode elasticsearchParentTreeNode = elasticsearchTreeRepository.getParentNode(nodeToUpdate.getUniqueId());
        if (elasticsearchParentTreeNode == null) { // Should not happen in a rename operation, unless there is a race condition (e.g. another client deletes parent node)
            throw new NodeNotFoundException("Cannot update node as parent node cannot be determined.");
        }

        if (!existingNode.getName().equals(nodeToUpdate.getName())) {
            // The node to be created cannot have same the name and type as any of the parent's
            // child nodes
            List<Node> parentsChildNodes = getChildNodes(elasticsearchParentTreeNode.getNode().getUniqueId());
            if (!isNodeNameValid(nodeToUpdate, parentsChildNodes)) {
                throw new IllegalArgumentException("Node of same name and type already exists in parent node.");
            }
        }

        Date now = new Date();
        if (customTimeForMigration) {
            nodeToUpdate.setCreated(now);
        }
        nodeToUpdate.setLastModified(now);
        nodeOptional.get().setNode(nodeToUpdate);

        elasticsearchTreeRepository.save(nodeOptional.get());

        return elasticsearchTreeRepository.findById(nodeToUpdate.getUniqueId()).get().getNode();
    }

    @Override
    public List<Tag> getAllTags() {
        List<ESTreeNode> esTreeNodes = elasticsearchTreeRepository.searchNodesForTag(false);
        List<Tag> tags = new ArrayList<>();
        esTreeNodes.forEach(node -> tags.addAll(node.getNode().getTags()));
        return tags;
    }

    @Override
    public List<Node> getAllSnapshots() {
        List<ESTreeNode> esTreeNodes = elasticsearchTreeRepository.getAllNodesByType(NodeType.SNAPSHOT);
        return esTreeNodes.stream().map(ESTreeNode::getNode).collect(Collectors.toList());
    }

    @Override
    public List<Node> getFromPath(String path) {
        if (path == null || !path.startsWith("/") || path.endsWith("/")) {
            return null;
        }
        String[] splitPath = path.split("/");
        Node parentOfLastPathElement = findParentFromPathElements(getRootNode(), splitPath, 1);
        if (parentOfLastPathElement == null) { // Path is "invalid"
            return null;
        }
        List<Node> childNodes = getChildNodes(parentOfLastPathElement.getUniqueId());
        List<Node> foundNodes = childNodes.stream()
                .filter(node -> node.getName().equals(splitPath[splitPath.length - 1])).collect(Collectors.toList());
        if (foundNodes.isEmpty()) {
            return null;
        }
        return foundNodes;
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        if (uniqueNodeId == null || uniqueNodeId.isEmpty()) {
            throw new IllegalArgumentException("Cannot determine full path for node id: " + uniqueNodeId);
        }
        List<String> pathElements = new ArrayList<>();
        resolvePath(uniqueNodeId, pathElements);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = pathElements.size() - 1; i >= 0; i--) {
            stringBuilder.append(pathElements.get(i));
        }
        return stringBuilder.toString();
    }

    private void resolvePath(String nodeId, List<String> pathElements) {
        if (nodeId.equals(ROOT_FOLDER_UNIQUE_ID)) {
            if (pathElements.isEmpty()) {
                pathElements.add("/");
            }
            return;
        }
        Optional<ESTreeNode> esTreeNodeOptional = elasticsearchTreeRepository.findById(nodeId);
        // First check if node exists
        if (esTreeNodeOptional.isEmpty()) {
            throw new NodeNotFoundException("Node " + nodeId + " does not exist");
        }
        pathElements.add("/" + esTreeNodeOptional.get().getNode().getName());
        Node parent = getParentNode(nodeId);
        resolvePath(parent.getUniqueId(), pathElements);
    }

    private void deleteNode(Node nodeToDelete) {
        for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
            deleteNode(node);
        }

        // Update the parent node to update its list of child nodes
        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(nodeToDelete.getUniqueId());
        parentNode.getChildNodes().remove(nodeToDelete.getUniqueId());
        elasticsearchTreeRepository.save(parentNode);

        // Delete the node
        elasticsearchTreeRepository.deleteById(nodeToDelete.getUniqueId());

        if (nodeToDelete.getNodeType().equals(NodeType.CONFIGURATION)) {
            configurationDataRepository.deleteById(nodeToDelete.getUniqueId());
        } else if (nodeToDelete.getNodeType().equals(NodeType.SNAPSHOT)) {
            snapshotDataRepository.deleteById(nodeToDelete.getUniqueId());
        }
    }

    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {

        configuration.getConfigurationNode().setNodeType(NodeType.CONFIGURATION); // Force node type
        Node newConfigurationNode = createNode(parentNodeId, configuration.getConfigurationNode());
        configuration.getConfigurationData().setUniqueId(newConfigurationNode.getUniqueId());

        ConfigurationData newConfigurationData = null;
        try {
            newConfigurationData = configurationDataRepository.save(configuration.getConfigurationData());
        } catch (Exception e) {
            // Saving configuration data failed, delete node for sake of consistency
            deleteNode(newConfigurationNode);
            throw new RuntimeException(e);
        }

        Configuration newConfiguration = new Configuration();
        newConfiguration.setConfigurationNode(newConfigurationNode);
        newConfiguration.setConfigurationData(newConfigurationData);

        return newConfiguration;
    }

    @Override
    public Configuration updateConfiguration(final Configuration configuration) {

        Node existingConfigurationNode = getNode(configuration.getConfigurationNode().getUniqueId());

        // Set name and description, even if unchanged.
        existingConfigurationNode.setName(configuration.getConfigurationNode().getName());
        existingConfigurationNode.setDescription(configuration.getConfigurationNode().getDescription());
        // Update last modified date
        existingConfigurationNode.setLastModified(new Date());
        existingConfigurationNode = updateNode(existingConfigurationNode, false);

        ConfigurationData updatedConfigurationData = configurationDataRepository.save(configuration.getConfigurationData());

        return Configuration.builder()
                .configurationData(updatedConfigurationData)
                .configurationNode(existingConfigurationNode)
                .build();
    }

    /**
     * Move of list of {@link Node}s is allowed only if:
     * <ul>
     *     <li>All elements are of same {@link NodeType}.</li>
     *     <li>All elements have same parent.</li>
     *     <li>All elements are folder nodes if the target is the root node.</li>
     *     <li>None of the elements is a snapshot node, or the root node.</li>
     *     <li>The target node - which must be a folder - does not contain any direct child nodes
     *     with same name and node type as any of the source nodes.</li>
     *     <li>Target node is not a descendant at any depth of any of the source nodes.</li>
     * </ul>
     *
     * @param sourceNodes List of source {@link Node}s
     * @param targetNode  The wanted target {@link Node}
     * @return <code>true</code> if move criteria are met, otherwise <code>false</code>
     */
    @Override
    public boolean isMoveOrCopyAllowed(List<Node> sourceNodes, Node targetNode) {
        // Does target node even exist?
        Optional<ESTreeNode> esTargetTreeNodeOptional = elasticsearchTreeRepository.findById(targetNode.getUniqueId());
        if (esTargetTreeNodeOptional.isEmpty()) {
            throw new NodeNotFoundException("Target node " + targetNode.getUniqueId() + " does not exist.");
        }
        // Check that the target node is not any of the source nodes, i.e. a node cannot be copied/moved to itself.
        for (Node sourceNode : sourceNodes) {
            if (sourceNode.getUniqueId().equals(esTargetTreeNodeOptional.get().getNode().getUniqueId())) {
                return false;
            }
        }
        // Root node cannot be copied/moved. Individual snapshot nodes cannot be copies/moved.
        Optional<Node> rootOrSnapshotNode = sourceNodes.stream()
                .filter(node -> node.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID) ||
                        node.getNodeType().equals(NodeType.SNAPSHOT)).findFirst();
        if (rootOrSnapshotNode.isPresent()) {
            logger.info("Move/copy not allowed: source node(s) list contains snapshot or root node.");
            return false;
        }
        // Check if selection contains configuration or snapshot node.
        Optional<Node> saveSetNode = sourceNodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.CONFIGURATION)).findFirst();
        // Configuration nodes may not be moved/copied to root node.
        if (saveSetNode.isPresent() && targetNode.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)) {
            logger.info("Move/copy of configuration node(s) to root node not allowed.");
            return false;
        }
        if (sourceNodes.size() > 1) {
            // Check that all elements are of same type and have the same parent.
            NodeType firstElementType = sourceNodes.get(0).getNodeType();
            Node parentNodeOfFirst = getParentNode(sourceNodes.get(0).getUniqueId());
            for (int i = 1; i < sourceNodes.size(); i++) {
                if (!sourceNodes.get(i).getNodeType().equals(firstElementType)) {
                    logger.info("Move not allowed: all source nodes must be of same type.");
                    return false;
                }
                Node parent = getParentNode(sourceNodes.get(i).getUniqueId());
                if (!parent.getUniqueId().equals(parentNodeOfFirst.getUniqueId())) {
                    logger.info("Move not allowed: all source nodes must have same parent node.");
                    return false;
                }
            }
        }
        // Check if there is any name/type clash
        List<Node> parentsChildNodes = getChildNodes(targetNode.getUniqueId());
        for (Node node : sourceNodes) {
            if (!isNodeNameValid(node, parentsChildNodes)) {
                logger.info("Move/copy not allowed: target node already contains child node with same name and type: " + node.getName());
                return false;
            }
        }

        boolean containedInSubtree = false;
        for (Node sourceNode : sourceNodes) {
            containedInSubtree = isContainedInSubtree(sourceNode.getUniqueId(), targetNode.getUniqueId());
            if (containedInSubtree) {
                break;
            }
        }

        return !containedInSubtree;
    }

    @Override
    public ConfigurationData getConfigurationData(String uniqueId) {
        Optional<ConfigurationData> configuration = configurationDataRepository.findById(uniqueId);
        if (configuration.isEmpty()) {
            throw new NodeNotFoundException("Configuration with id " + uniqueId + " not found");
        }
        return configuration.get();
    }

    @Override
    public Snapshot saveSnapshot(String parentNodeId, Snapshot snapshot) {
        snapshot.getSnapshotNode().setNodeType(NodeType.SNAPSHOT); // Force node type
        Node newSnapshotNode = createNode(parentNodeId, snapshot.getSnapshotNode());
        snapshot.getSnapshotData().setUniqueId(newSnapshotNode.getUniqueId());

        SnapshotData newSnapshotData = null;
        try {
            newSnapshotData = snapshotDataRepository.save(snapshot.getSnapshotData());
        } catch (Exception e) {
            // Failed to save snapshot data, delete node for the sake of consistency
            deleteNode(newSnapshotNode);
            throw new RuntimeException(e);
        }

        Snapshot newSnapshot = new Snapshot();
        newSnapshot.setSnapshotData(newSnapshotData);
        newSnapshot.setSnapshotNode(newSnapshotNode);

        return newSnapshot;
    }

    @Override
    public SnapshotData getSnapshotData(String uniqueId) {
        Optional<SnapshotData> snapshot = snapshotDataRepository.findById(uniqueId);
        if (snapshot.isEmpty()) {
            throw new NodeNotFoundException("SnapshotData with id " + uniqueId + " not found");
        }
        return snapshot.get();
    }

    @Override
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
    public Node findParentFromPathElements(Node parentNode, String[] splitPath, int index) {
        if (index == splitPath.length - 1) {
            return parentNode;
        }
        String nextPathElement = splitPath[index];
        List<Node> childNodes = getChildNodes(parentNode.getUniqueId());
        for (Node node : childNodes) {
            if (node.getName().equals(nextPathElement) && node.getNodeType().equals(NodeType.FOLDER)) {
                return findParentFromPathElements(node, splitPath, ++index);
            }
        }
        return null;
    }

    public boolean isContainedInSubtree(String startNode, String nodeToLookFor) {
        Optional<ESTreeNode> esStartNode = elasticsearchTreeRepository.findById(startNode);
        if (esStartNode.isEmpty()) {
            throw new NodeNotFoundException("Node id " + startNode + " not found");
        }
        if (esStartNode.get().getChildNodes() == null || esStartNode.get().getChildNodes().isEmpty()) {
            return false;
        }
        boolean isContainedInSubTree = false;
        if (esStartNode.get().getChildNodes().contains(nodeToLookFor)) {
            isContainedInSubTree = true;
        } else {
            for (String childNode : esStartNode.get().getChildNodes()) {
                isContainedInSubTree = isContainedInSubtree(childNode, nodeToLookFor);
                if (isContainedInSubTree) {
                    break;
                }
            }
        }
        return isContainedInSubTree;
    }
}
