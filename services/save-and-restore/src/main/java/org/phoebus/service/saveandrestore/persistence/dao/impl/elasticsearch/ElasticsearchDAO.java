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

import org.apache.commons.collections4.CollectionUtils;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchQueryUtil;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

/**
 * Data Access Object interfacing Elasticsearch as defined by {@link NodeDAO} interface.
 */
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

    @SuppressWarnings("unused")
    @Autowired
    private FilterRepository filterRepository;

    @SuppressWarnings("unused")
    @Autowired
    private CompositeSnapshotDataRepository compositeSnapshotDataRepository;

    @SuppressWarnings("unused")
    @Autowired
    private SearchUtil searchUtil;

    private final Pattern NODE_NAME_PATTERN = Pattern.compile(".*(\\scopy(\\s\\d*)?$)");

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
    public List<Node> getNodes(List<String> uniqueNodeIds) {
        List<Node> nodes = new ArrayList<>();
        elasticsearchTreeRepository.findAllById(uniqueNodeIds).forEach(n -> nodes.add(n.getNode()));
        return nodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void deleteNode(String nodeId) {
        deleteNodes(List.of(nodeId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteNodes(List<String> nodeIds) {
        List<Node> nodes = new ArrayList<>();
        for (String nodeId : nodeIds) {
            Node nodeToDelete = getNode(nodeId);
            if (nodeToDelete == null) {
                throw new NodeNotFoundException("Cannot delete non-existing node");
            } else if (nodeToDelete.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)) {
                throw new IllegalArgumentException("Root node cannot be deleted");
            }
            nodes.add(nodeToDelete);
        }
        nodes.forEach(this::deleteNode);
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
        if (node.getCreated() == null) {
            node.setCreated(new Date());
        }
        if (node.getUniqueId() == null) {
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
        // Move root node is not allowed
        if (nodeIds.contains(ROOT_FOLDER_UNIQUE_ID)) {
            throw new IllegalArgumentException("Move root node not supported");
        }

        // Check that the target node is not any of the source nodes, i.e. a node cannot be copied to itself.
        if (nodeIds.stream().anyMatch(i -> i.equals(targetId))) {
            throw new IllegalArgumentException("At least one source node is same as target node");
        }

        // Get target node. If it does not exist, a NodeNotFoundException is thrown.
        Optional<ESTreeNode> targetNodeOptional;

        try {
            targetNodeOptional = elasticsearchTreeRepository.findById(targetId);
        } catch (NodeNotFoundException e) {
            throw new IllegalArgumentException("Target node does not exist");
        }

        ESTreeNode targetNode = targetNodeOptional.get();

        List<Node> sourceNodes = new ArrayList<>();
        nodeIds.forEach(id -> {
            Optional<ESTreeNode> esTreeNode = elasticsearchTreeRepository.findById(id);
            esTreeNode.ifPresent(treeNode -> sourceNodes.add(treeNode.getNode()));
        });

        // Get node type of first element...
        NodeType nodeTypeOfFirstSourceNode = sourceNodes.get(0).getNodeType();

        // All nodes must be of same type
        if (sourceNodes.stream().anyMatch(n -> !n.getNodeType().equals(nodeTypeOfFirstSourceNode))) {
            throw new IllegalArgumentException("Move nodes supported only if all source nodes are of same type");
        }

        // All nodes must have same parent node
        String parentNodeOfFirstSourceNode =
                elasticsearchTreeRepository.getParentNode(sourceNodes.get(0).getUniqueId()).getNode().getUniqueId();
        if (sourceNodes.stream().anyMatch(n ->
                !parentNodeOfFirstSourceNode.equals(elasticsearchTreeRepository.getParentNode(n.getUniqueId()).getNode().getUniqueId()))) {
            throw new IllegalArgumentException("All source nodes must have same parent node");
        }

        // Configuration, composite snapshot and folder can be moved only to folder
        if ((nodeTypeOfFirstSourceNode.equals(NodeType.FOLDER) ||
                nodeTypeOfFirstSourceNode.equals(NodeType.CONFIGURATION) ||
                nodeTypeOfFirstSourceNode.equals(NodeType.COMPOSITE_SNAPSHOT))
                && !targetNode.getNode().getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException(nodeTypeOfFirstSourceNode + " cannot be moved to " + targetNode.getNode().getNodeType() + " node");
        }

        // Snapshot may only be moved to a configuration node.
        if (nodeTypeOfFirstSourceNode.equals(NodeType.SNAPSHOT)
                && !targetNode.getNode().getNodeType().equals(NodeType.CONFIGURATION)) {
            throw new IllegalArgumentException(nodeTypeOfFirstSourceNode + " cannot be moved to " + targetNode.getNode().getNodeType() + " node");
        }

        // Snapshot nodes' PV list must match target configuration's PV list. This is checked for all source snapshots:
        // if one mismatch is found, the move operation is aborted -> no snapshots moved.
        if (nodeTypeOfFirstSourceNode.equals(NodeType.SNAPSHOT)) {
            for (Node node : sourceNodes) {
                if (!mayMoveOrCopySnapshot(node, targetNode.getNode())) {
                    throw new IllegalArgumentException("At least one snapshot's PV list does not match target configuration's PV list");
                }
            }
        }

        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(sourceNodes.get(0).getUniqueId());
        if (parentNode == null) {
            throw new RuntimeException("Parent node of source node " + sourceNodes.get(0).getUniqueId() + " not found. Should not happen.");
        }

        if (targetNode.getChildNodes() != null) {
            List<Node> targetsChildNodes = new ArrayList<>();
            for (String parentChildNode : targetNode.getChildNodes()) {
                Optional<ESTreeNode> targetChildNodeOptional = elasticsearchTreeRepository.findById(parentChildNode);
                if (targetChildNodeOptional.isEmpty()) { // Should not happen, but ignore if it does.
                    continue;
                }
                targetsChildNodes.add(targetChildNodeOptional.get().getNode());
            }
            for (Node sourceNode : sourceNodes) {
                for (Node targetChildNode : targetsChildNodes) {
                    if (targetChildNode.getName().equals(sourceNode.getName()) && targetChildNode.getNodeType().equals(sourceNode.getNodeType())) {
                        throw new IllegalArgumentException("Cannot move, at least one source node has same name and type as a target child node");
                    }
                }
            }
        }

        // Remove source nodes from the list of child nodes in parent node.
        parentNode.getChildNodes().removeAll(sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList()));
        elasticsearchTreeRepository.save(parentNode);

        // Update the target node to include the source nodes in its list of child nodes
        if (targetNode.getChildNodes() == null) {
            targetNode.setChildNodes(new ArrayList<>());
        }

        targetNode.getChildNodes().addAll(sourceNodes.stream().map(Node::getUniqueId).collect(Collectors.toList()));
        ESTreeNode updatedTargetNode = elasticsearchTreeRepository.save(targetNode);

        return updatedTargetNode.getNode();
    }

    @Override
    public Node copyNodes(List<String> nodeIds, String targetId, String userName) {
        // Copy to root node not allowed, neither is copying of root folder itself
        if (targetId.equals(ROOT_FOLDER_UNIQUE_ID) || nodeIds.contains(ROOT_FOLDER_UNIQUE_ID)) {
            throw new IllegalArgumentException("Copy to root node or copy root node not supported");
        }

        // Check that the target node is not any of the source nodes, i.e. a node cannot be copied to itself.
        if (nodeIds.stream().anyMatch(i -> i.equals(targetId))) {
            throw new IllegalArgumentException("At least one source node is same as target node");
        }

        // Get target node. If it does not exist, a NodeNotFoundException is thrown.
        Optional<ESTreeNode> targetNodeOptional;

        try {
            targetNodeOptional = elasticsearchTreeRepository.findById(targetId);
        } catch (NodeNotFoundException e) {
            throw new IllegalArgumentException("Target node does not exist");
        }

        Node targetNode = targetNodeOptional.get().getNode();

        List<Node> sourceNodes = new ArrayList<>();

        try {
            for (String nodeId : nodeIds) {
                Optional<ESTreeNode> esTreeNode = elasticsearchTreeRepository.findById(nodeId);
                sourceNodes.add(esTreeNode.get().getNode());
            }
        } catch (NodeNotFoundException e) {
            throw new IllegalArgumentException("At least one source node does not exist");
        }

        // Get node type of first element...
        NodeType nodeTypeOfFirstSourceNode = sourceNodes.get(0).getNodeType();
        // ... if type is folder, abort
        if (nodeTypeOfFirstSourceNode.equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException("Copy of folder(s) not supported");
        }
        // All nodes must be of same type
        if (sourceNodes.stream().anyMatch(n -> !n.getNodeType().equals(nodeTypeOfFirstSourceNode))) {
            throw new IllegalArgumentException("Copy nodes supported only if all source nodes are of same type");
        }
        // All nodes must have same parent node
        String parentNodeOfFirstSourceNode =
                elasticsearchTreeRepository.getParentNode(sourceNodes.get(0).getUniqueId()).getNode().getUniqueId();
        if (sourceNodes.stream().anyMatch(n ->
                !parentNodeOfFirstSourceNode.equals(elasticsearchTreeRepository.getParentNode(n.getUniqueId()).getNode().getUniqueId()))) {
            throw new IllegalArgumentException("All source nodes must have same parent node");
        }

        // Configuration and composite snapshot nodes may be copied only to folder
        if ((nodeTypeOfFirstSourceNode.equals(NodeType.CONFIGURATION) || nodeTypeOfFirstSourceNode.equals(NodeType.COMPOSITE_SNAPSHOT))
                && !targetNode.getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException(nodeTypeOfFirstSourceNode + " cannot be copied to " + targetNode.getNodeType() + " node");
        }

        // Snapshot may only be copied to a configuration node.
        if (nodeTypeOfFirstSourceNode.equals(NodeType.SNAPSHOT)
                && !targetNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            throw new IllegalArgumentException(nodeTypeOfFirstSourceNode + " cannot be copied to " + targetNode.getNodeType() + " node");
        }

        // Snapshot nodes' PV list must match target configuration's PV list. This is checked for all source snapshots:
        // if one mismatch is found, the copy operation is aborted -> no snapshots copied.
        if (nodeTypeOfFirstSourceNode.equals(NodeType.SNAPSHOT)) {
            for (Node node : sourceNodes) {
                if (!mayMoveOrCopySnapshot(node, targetNode)) {
                    throw new IllegalArgumentException("Snapshot not compatible with configuration");
                }
            }
        }

        sourceNodes.forEach(sourceNode -> copyNode(sourceNode, targetNode, userName));

        return targetNode;
    }

    /**
     * Creates a copy (clone) of the source {@link Node} and associated data like so:
     * <ol>
     *     <li>Determine the new {@link Node}'s name.</li>
     *     <li>Create the new {@link Node}.</li>
     *     <li>Clone the data, which of course varies depending on the {@link NodeType} of the source {@link Node}.</li>
     * </ol>
     *
     * @param sourceNode       The source {@link Node} to be copied (cloned).
     * @param targetParentNode The parent {@link Node} of the copy.
     * @param userName         Username of the individual performing the action.
     */
    private void copyNode(Node sourceNode, Node targetParentNode, String userName) {
        List<Node> targetsChildNodes = getChildNodes(targetParentNode.getUniqueId());
        String newNodeName = determineNewNodeName(sourceNode, targetsChildNodes);
        // First create a clone of the source Node object
        Node sourceNodeClone = Node.builder()
                .name(newNodeName)
                .nodeType(sourceNode.getNodeType())
                .userName(userName)
                .tags(sourceNode.getTags())
                .description(sourceNode.getDescription())
                .build();
        final Node newSourceNode = createNode(targetParentNode.getUniqueId(), sourceNodeClone);

        // Next copy data and associate it with the cloned Node object
        if (sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            ConfigurationData sourceConfiguration = getConfigurationData(sourceNode.getUniqueId());
            copyConfigurationData(newSourceNode, sourceConfiguration);
        } else if (sourceNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            SnapshotData snapshotData = getSnapshotData(sourceNode.getUniqueId());
            copySnapshotData(newSourceNode, snapshotData);
        } else if (sourceNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
            CompositeSnapshotData compositeSnapshotData =
                    getCompositeSnapshotData(sourceNode.getUniqueId());
            copyCompositeSnapshotData(newSourceNode, compositeSnapshotData);
        }
    }

    protected boolean mayMoveOrCopySnapshot(Node sourceNode, Node targetParentNode) {
        SnapshotData snapshotData = getSnapshotData(sourceNode.getUniqueId());
        ConfigurationData configurationData = getConfigurationData(targetParentNode.getUniqueId());

        List<String> pvsInSnapshot =
                snapshotData.getSnapshotItems().stream().map(si -> si.getConfigPv().getPvName()).collect(Collectors.toList());
        List<String> pvsInConfiguration =
                configurationData.getPvList().stream().map(ConfigPv::getPvName).collect(Collectors.toList());
        return CollectionUtils.containsAll(pvsInSnapshot, pvsInConfiguration) && CollectionUtils.containsAll(pvsInConfiguration, pvsInSnapshot);
    }

    /**
     * Copies a {@link ConfigurationData}.
     *
     * @param targetConfigurationNode The configuration {@link Node} with which the copied {@link ConfigurationData} should be
     *                                associated. This must already exist in the Elasticsearch index.
     * @param sourceConfiguration     The source {@link ConfigurationData}
     */
    private void copyConfigurationData(Node targetConfigurationNode, ConfigurationData sourceConfiguration) {
        ConfigurationData clonedConfigurationData = ConfigurationData.clone(sourceConfiguration);
        clonedConfigurationData.setUniqueId(targetConfigurationNode.getUniqueId());
        configurationDataRepository.save(clonedConfigurationData);
    }

    private void copySnapshotData(Node targetSnapshotNode, SnapshotData snapshotData) {
        SnapshotData clonedSnapshotData = new SnapshotData();
        clonedSnapshotData.setSnapshotItems(snapshotData.getSnapshotItems());
        clonedSnapshotData.setUniqueId(targetSnapshotNode.getUniqueId());
        snapshotDataRepository.save(clonedSnapshotData);
    }

    private void copyCompositeSnapshotData(Node targetCompositeSnapshotNode, CompositeSnapshotData compositeSnapshotData) {
        CompositeSnapshotData clonedCompositeSnapshotData = new CompositeSnapshotData();
        clonedCompositeSnapshotData.setReferencedSnapshotNodes(compositeSnapshotData.getReferencedSnapshotNodes());
        clonedCompositeSnapshotData.setUniqueId(targetCompositeSnapshotNode.getUniqueId());
        compositeSnapshotDataRepository.save(clonedCompositeSnapshotData);
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
        MultiValueMap<String, String> searchParams = new LinkedMultiValueMap<>();
        searchParams.add("type", NodeType.SNAPSHOT.toString());
        SearchResult searchResult = elasticsearchTreeRepository.search(searchParams);
        return searchResult.getNodes();
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

        if (nodeToDelete.getNodeType().equals(NodeType.CONFIGURATION)) {
            configurationDataRepository.deleteById(nodeToDelete.getUniqueId());
        } else if (nodeToDelete.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
            compositeSnapshotDataRepository.deleteById(nodeToDelete.getUniqueId());
        } else if (nodeToDelete.getNodeType().equals(NodeType.SNAPSHOT)) {
            Node compositeSnapshotReferencingTheSnapshot =
                    mayDeleteSnapshot(nodeToDelete);
            if (compositeSnapshotReferencingTheSnapshot != null) {
                throw new IllegalArgumentException("Cannot delete snapshot \"" + nodeToDelete.getName() +
                        "\" as it is referenced in composite snapshot \"" + compositeSnapshotReferencingTheSnapshot.getName() + "\"");
            }
            snapshotDataRepository.deleteById(nodeToDelete.getUniqueId());
        }

        // Update the parent node to update its list of child nodes
        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(nodeToDelete.getUniqueId());
        parentNode.getChildNodes().remove(nodeToDelete.getUniqueId());
        elasticsearchTreeRepository.save(parentNode);

        // Delete the node
        elasticsearchTreeRepository.deleteById(nodeToDelete.getUniqueId());

    }

    @Override
    public Configuration createConfiguration(String parentNodeId, Configuration configuration) {

        ConfigurationData sanitizedConfigurationData = removeDuplicatePVNames(configuration.getConfigurationData());
        configuration.setConfigurationData(sanitizedConfigurationData);

        configuration.getConfigurationNode().setNodeType(NodeType.CONFIGURATION); // Force node type
        Node newConfigurationNode = createNode(parentNodeId, configuration.getConfigurationNode());
        configuration.getConfigurationData().setUniqueId(newConfigurationNode.getUniqueId());

        ConfigurationData newConfigurationData;
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
    public Configuration updateConfiguration(Configuration configuration) {

        ConfigurationData sanitizedConfigurationData = removeDuplicatePVNames(configuration.getConfigurationData());
        configuration.setConfigurationData(sanitizedConfigurationData);

        Node existingConfigurationNode = getNode(configuration.getConfigurationNode().getUniqueId());

        // Set name, description and user even if unchanged.
        existingConfigurationNode.setName(configuration.getConfigurationNode().getName());
        existingConfigurationNode.setDescription(configuration.getConfigurationNode().getDescription());
        existingConfigurationNode.setUserName(configuration.getConfigurationNode().getUserName());
        // Update last modified date
        existingConfigurationNode.setLastModified(new Date());
        existingConfigurationNode = updateNode(existingConfigurationNode, false);

        ConfigurationData updatedConfigurationData = configurationDataRepository.save(configuration.getConfigurationData());

        return Configuration.builder()
                .configurationData(updatedConfigurationData)
                .configurationNode(existingConfigurationNode)
                .build();
    }

    @Override
    public ConfigurationData getConfigurationData(String uniqueId) {
        Optional<ConfigurationData> configurationData = configurationDataRepository.findById(uniqueId);
        if (configurationData.isEmpty()) {
            throw new NodeNotFoundException("Configuration with id " + uniqueId + " not found");
        }
        return removeDuplicatePVNames(configurationData.get());
    }

    @Override
    public Snapshot createSnapshot(String parentNodeId, Snapshot snapshot) {

        SnapshotData sanitizedSnapshotData = removeDuplicateSnapshotItems(snapshot.getSnapshotData());
        snapshot.setSnapshotData(sanitizedSnapshotData);

        snapshot.getSnapshotNode().setNodeType(NodeType.SNAPSHOT); // Force node type
        Node newSnapshotNode = createNode(parentNodeId, snapshot.getSnapshotNode());
        snapshot.getSnapshotData().setUniqueId(newSnapshotNode.getUniqueId());
        SnapshotData newSnapshotData;
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
    public Snapshot updateSnapshot(Snapshot snapshot) {

        SnapshotData sanitizedSnapshotData = removeDuplicateSnapshotItems(snapshot.getSnapshotData());
        snapshot.setSnapshotData(sanitizedSnapshotData);

        snapshot.getSnapshotNode().setNodeType(NodeType.SNAPSHOT); // Force node type
        SnapshotData newSnapshotData;
        Snapshot newSnapshot = new Snapshot();
        try {
            newSnapshotData = snapshotDataRepository.save(snapshot.getSnapshotData());
            Node updatedNode = updateNode(snapshot.getSnapshotNode(), false);
            newSnapshot.setSnapshotData(newSnapshotData);
            newSnapshot.setSnapshotNode(updatedNode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return newSnapshot;
    }

    @Override
    public SnapshotData getSnapshotData(String uniqueId) {
        Optional<SnapshotData> snapshotData = snapshotDataRepository.findById(uniqueId);
        if (snapshotData.isEmpty()) {
            throw new NodeNotFoundException("SnapshotData with id " + uniqueId + " not found");
        }
        return removeDuplicateSnapshotItems(snapshotData.get());
    }

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
    @Override
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

    /**
     * Checks if a {@link Node} is present in a subtree. This is called recursively.
     *
     * @param startNode     {@link Node} id from which the search will start.
     * @param nodeToLookFor Self-explanatory.
     * @return <code>true</code> if the #nodeToLookFor is found in the subtree, otherwise <code>false</code>.
     */
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

    /**
     * Removes duplicate PV names if found in the {@link ConfigurationData}. While user and client should
     * take measures to not add duplicates, this is to safeguard that only sanitized data is persisted.
     *
     * @param configurationData The {@link ConfigurationData} subject to sanitation.
     * @return The sanitized {@link ConfigurationData} object.
     */
    protected ConfigurationData removeDuplicatePVNames(ConfigurationData configurationData) {
        if (configurationData == null) {
            return null;
        }
        if (configurationData.getPvList() == null) {
            return configurationData;
        }
        Map<String, ConfigPv> sanitizedMap = new HashMap<>();
        for (ConfigPv configPv : configurationData.getPvList()) {
            if (sanitizedMap.containsKey(configPv.getPvName())) {
                continue;
            }
            sanitizedMap.put(configPv.getPvName(), configPv);
        }
        ConfigurationData sanitizedConfigurationData = new ConfigurationData();
        sanitizedConfigurationData.setUniqueId(configurationData.getUniqueId());
        List<ConfigPv> sanitizedList = new ArrayList<>(sanitizedMap.values());
        sanitizedConfigurationData.setPvList(sanitizedList);
        return sanitizedConfigurationData;
    }

    /**
     * Removes duplicate PV names if found in the {@link SnapshotData}. While user and client should
     * take measures to not add duplicates, this is to safeguard that only sanitized data is persisted.
     *
     * @param snapshotData The {@link SnapshotData} subject to sanitation.
     * @return The sanitized {@link SnapshotData} object.
     */
    protected SnapshotData removeDuplicateSnapshotItems(SnapshotData snapshotData) {
        if (snapshotData == null) {
            return null;
        }
        if (snapshotData.getSnapshotItems() == null) {
            return snapshotData;
        }
        Map<String, SnapshotItem> sanitizedMap = new HashMap<>();
        for (SnapshotItem snapshotItem : snapshotData.getSnapshotItems()) {
            if (sanitizedMap.containsKey(snapshotItem.getConfigPv().getPvName())) {
                continue;
            }
            sanitizedMap.put(snapshotItem.getConfigPv().getPvName(), snapshotItem);
        }
        SnapshotData sanitizedSnapshotData = new SnapshotData();
        List<SnapshotItem> sanitizedList = new ArrayList<>(sanitizedMap.values());
        sanitizedSnapshotData.setSnapshotItems(sanitizedList);
        return sanitizedSnapshotData;
    }

    @Override
    public CompositeSnapshot createCompositeSnapshot(String parentNodeId, CompositeSnapshot compositeSnapshot) {
        if (!checkCompositeSnapshotReferencedNodeTypes(compositeSnapshot)) {
            throw new IllegalArgumentException("Found unsupported node type in list of referenced nodes");
        }
        List<String> duplicatePVNames = checkForPVNameDuplicates(compositeSnapshot.getCompositeSnapshotData().getReferencedSnapshotNodes());
        if (!duplicatePVNames.isEmpty()) {
            throw new IllegalArgumentException("Found duplicate PV names in referenced snapshots");
        }
        compositeSnapshot.getCompositeSnapshotNode().setNodeType(NodeType.COMPOSITE_SNAPSHOT); // Force node type
        Node newCompositeSnapshotNode = createNode(parentNodeId, compositeSnapshot.getCompositeSnapshotNode());
        compositeSnapshot.getCompositeSnapshotData().setUniqueId(newCompositeSnapshotNode.getUniqueId());

        CompositeSnapshotData newCompositeSnapshotData;
        try {
            newCompositeSnapshotData = compositeSnapshotDataRepository.save(compositeSnapshot.getCompositeSnapshotData());
        } catch (Exception e) {
            // Saving configuration data failed, delete node for sake of consistency
            deleteNode(newCompositeSnapshotNode);
            throw new RuntimeException(e);
        }

        CompositeSnapshot newCompositeSnapshot = new CompositeSnapshot();
        newCompositeSnapshot.setCompositeSnapshotNode(newCompositeSnapshotNode);
        newCompositeSnapshot.setCompositeSnapshotData(newCompositeSnapshotData);

        return newCompositeSnapshot;
    }

    @Override
    public CompositeSnapshotData getCompositeSnapshotData(String uniqueId) {
        Optional<CompositeSnapshotData> snapshotData = compositeSnapshotDataRepository.findById(uniqueId);
        if (snapshotData.isEmpty()) {
            throw new NodeNotFoundException("CompositeSnapshotData with id " + uniqueId + " not found");
        }
        return snapshotData.get();
    }

    @Override
    public List<CompositeSnapshotData> getAllCompositeSnapshotData() {
        List<CompositeSnapshotData> list = new ArrayList<>();
        Iterable<CompositeSnapshotData> iterable = compositeSnapshotDataRepository.findAll();
        iterable.forEach(list::add);
        return list;
    }

    /**
     * Checks if a snapshot is contained in any composite snapshot.
     *
     * @param snapshotNode The {@link Node} subject to check.
     * @return A <code>non-null</code> {@link Node} object in which the checked node is referenced,
     * otherwise <code>null</code>. Note that this returns the first composite snapshot node where the checked snapshot is
     * encountered. References in other composite snapshots may exist.
     */
    private Node mayDeleteSnapshot(Node snapshotNode) {
        // This is needed to check if a snapshot node is referenced in a composite snapshot
        List<CompositeSnapshotData> allCompositeSnapshotData = getAllCompositeSnapshotData();
        for (CompositeSnapshotData compositeSnapshotData : allCompositeSnapshotData) {
            Iterable<ESTreeNode> treeNodes =
                    elasticsearchTreeRepository.findAllById(compositeSnapshotData.getReferencedSnapshotNodes());
            for (ESTreeNode treeNode : treeNodes) {
                if (treeNode.getNode().getUniqueId().equals(snapshotNode.getUniqueId())) {
                    return getNode(compositeSnapshotData.getUniqueId());
                }
            }
        }
        return null;
    }

    @Override
    public List<String> checkForPVNameDuplicates(List<String> snapshotIds) {
        // Collect list of all PV names
        List<String> allPVNames = new ArrayList<>();
        for (String snapshotNodeId : snapshotIds) {
            nextSnapshotNode(snapshotNodeId, allPVNames);
        }

        return extractDuplicates(allPVNames);
    }

    private void nextSnapshotNode(String snapshotNode, List<String> allPVNames) {
        Node node = getNode(snapshotNode);
        if (node == null) {
            return;
        }
        NodeType nodeType = node.getNodeType();
        if (nodeType.equals(NodeType.COMPOSITE_SNAPSHOT)) {
            CompositeSnapshotData compositeSnapshotData = getCompositeSnapshotData(node.getUniqueId());
            for (String referencedNode : compositeSnapshotData.getReferencedSnapshotNodes()) {
                nextSnapshotNode(referencedNode, allPVNames);
            }
        } else if (nodeType.equals(NodeType.SNAPSHOT)) {
            SnapshotData snapshotData = getSnapshotData(node.getUniqueId());
            for (SnapshotItem snapshotItem : snapshotData.getSnapshotItems()) {
                allPVNames.add(snapshotItem.getConfigPv().getPvName());
            }
        }
    }

    /**
     * @param allPVNames A list of strings that may contain duplicates
     * @return A list of PV names found to occur more than once in the input array,or an empty list. If a
     * PV name occurs N (>1) times, it will still occur only once in the returned list. For instance, if the
     * input list is <code>Arrays.asList("a", "b", "c", "d", "D", "a", "B", "a", "b")</code>, the returned
     * list will contain <code>"a", "b"</code>.
     */
    protected List<String> extractDuplicates(List<String> allPVNames) {
        List<String> uniqueDuplicates = new ArrayList<>();
        // Collect PV names that occur only once
        for (String pvName : allPVNames) {
            if (Collections.frequency(allPVNames, pvName) > 1 && !uniqueDuplicates.contains(pvName)) {
                uniqueDuplicates.add(pvName);
            }
        }
        return uniqueDuplicates;
    }

    @Override
    public CompositeSnapshot updateCompositeSnapshot(CompositeSnapshot compositeSnapshot) {
        if (!checkCompositeSnapshotReferencedNodeTypes(compositeSnapshot)) {
            throw new IllegalArgumentException("Found unsupported node type in list of referenced nodes");
        }
        List<String> duplicatePVNames = checkForPVNameDuplicates(compositeSnapshot.getCompositeSnapshotData().getReferencedSnapshotNodes());
        if (!duplicatePVNames.isEmpty()) {
            throw new IllegalArgumentException("Found duplicate PV names in referenced snapshots");
        }
        Node existingCompositeSnapshotNode = getNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        // Set name and description, even if unchanged.
        existingCompositeSnapshotNode.setName(compositeSnapshot.getCompositeSnapshotNode().getName());
        existingCompositeSnapshotNode.setDescription(compositeSnapshot.getCompositeSnapshotNode().getDescription());
        // Update last modified date
        existingCompositeSnapshotNode.setLastModified(new Date());
        existingCompositeSnapshotNode = updateNode(existingCompositeSnapshotNode, false);

        CompositeSnapshotData updatedCompositeSnapshotData =
                compositeSnapshotDataRepository.save(compositeSnapshot.getCompositeSnapshotData());

        return CompositeSnapshot.builder()
                .compositeSnapshotData(updatedCompositeSnapshotData)
                .compositeSnapshotNode(existingCompositeSnapshotNode)
                .build();
    }

    @Override
    public List<SnapshotItem> getSnapshotItemsFromCompositeSnapshot(String compositeSnapshotNodeId) {
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        getSnapshotItemsFromNextNode(compositeSnapshotNodeId, snapshotItems);
        return snapshotItems;
    }

    private void getSnapshotItemsFromNextNode(String snapshotNode, List<SnapshotItem> allSnapshotItems) {
        Node node = getNode(snapshotNode);
        if (node == null) {
            return;
        }
        NodeType nodeType = node.getNodeType();
        if (nodeType.equals(NodeType.COMPOSITE_SNAPSHOT)) {
            CompositeSnapshotData compositeSnapshotData = getCompositeSnapshotData(node.getUniqueId());
            for (String referencedNode : compositeSnapshotData.getReferencedSnapshotNodes()) {
                getSnapshotItemsFromNextNode(referencedNode, allSnapshotItems);
            }
        } else if (nodeType.equals(NodeType.SNAPSHOT)) {
            SnapshotData snapshotData = getSnapshotData(node.getUniqueId());
            allSnapshotItems.addAll(snapshotData.getSnapshotItems());
        }
    }

    @Override
    public boolean checkCompositeSnapshotReferencedNodeTypes(CompositeSnapshot compositeSnapshot) {
        for (String nodeId : compositeSnapshot.getCompositeSnapshotData().getReferencedSnapshotNodes()) {
            if (!checkCompositeSnapshotReferencedNodeType(nodeId)) {
                return false;
            }
        }
        return true;
    }

    private boolean checkCompositeSnapshotReferencedNodeType(String nodeId) {
        Node node = getNode(nodeId);
        if (node.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)) {
            CompositeSnapshotData compositeSnapshotData = getCompositeSnapshotData(node.getUniqueId());
            for (String referencedNode : compositeSnapshotData.getReferencedSnapshotNodes()) {
                if (!checkCompositeSnapshotReferencedNodeType(referencedNode)) {
                    return false;
                }
            }
            return true;
        } else return node.getNodeType().equals(NodeType.SNAPSHOT);
    }

    @Override
    public SearchResult search(MultiValueMap<String, String> searchParameters) {
        return searchInternal(searchParameters);
    }

    private SearchResult searchInternal(MultiValueMap<String, String> searchParameters) {
        // Did client specify search on pv name(s)?
        if (searchParameters.keySet().stream().anyMatch(k -> k.strip().toLowerCase().contains("pvs"))) {
            List<ConfigurationData> configurationDataList = configurationDataRepository.searchOnPvName(searchParameters);
            if (configurationDataList.isEmpty()) {
                // No matching configurations found, return empty SearchResult
                return new SearchResult(0, Collections.emptyList());
            }
            List<String> uniqueIds = configurationDataList.stream().map(ConfigurationData::getUniqueId).collect(Collectors.toList());
            MultiValueMap<String, String> augmented = new LinkedMultiValueMap<>();
            augmented.putAll(searchParameters);
            augmented.put("uniqueid", uniqueIds);
            return elasticsearchTreeRepository.search(augmented);
        } else {
            return elasticsearchTreeRepository.search(searchParameters);
        }
    }

    /**
     * Saves a {@link Filter}. The query string is analyzed and sanitized to make sure that:
     * <ul>
     *     <li>Only valid keys are saved.</li>
     *     <li>Values (search terms) with multiple elements are formatted correctly.</li>
     * </ul>
     *
     * @param filter The {@link Filter} to save
     * @return The saved {@link Filter}
     */
    @Override
    public Filter saveFilter(Filter filter) {
        // Parse the search query to make sure only supported keys are accepted
        Map<String, String> queryParams = SearchQueryUtil.parseHumanReadableQueryString(filter.getQueryString());
        // Format query again before saving it
        filter.setQueryString(SearchQueryUtil.toQueryString(queryParams));
        return filterRepository.save(filter);
    }


    @Override
    public List<Filter> getAllFilters() {
        Iterable<Filter> filtersIterable = filterRepository.findAll();
        List<Filter> filters = new ArrayList<>();
        filtersIterable.forEach(filters::add);
        return filters;
    }

    @Override
    public void deleteFilter(String name) {
        filterRepository.deleteById(name);
    }

    @Override
    public void deleteAllFilters() {
        filterRepository.deleteAll();
    }

    @Override
    public List<Node> addTag(TagData tagData) {
        List<Node> updatedNodes = new ArrayList<>();
        tagData.getUniqueNodeIds().forEach(nodeId -> {
            try {
                Node node = getNode(nodeId);
                Node updatedNode = Node.builder()
                        .nodeType(node.getNodeType())
                        .userName(node.getUserName())
                        .description(node.getDescription())
                        .name(node.getName())
                        .uniqueId(node.getUniqueId())
                        .created(node.getCreated())
                        .build();
                List<Tag> tags = node.getTags();
                if (tags == null) {
                    tags = new ArrayList<>();
                }
                tags.add(tagData.getTag());
                updatedNode.setTags(tags);
                updatedNode = updateNode(updatedNode, false);
                updatedNodes.add(updatedNode);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot add tag to node " + nodeId, e);
            }
        });
        return updatedNodes;
    }

    /**
     * Removes a {@link Tag} from specified list of target {@link Node}s. If a {@link Node} does not
     * contain the {@link Tag}, this method does not update that {@link Node}.
     *
     * @param tagData See {@link TagData}
     * @return The list of updated {@link Node}s. This may contain fewer elements than the list of
     * unique node ids as {@link Node}s not containing the {@link Tag} are omitted from update.
     */
    public List<Node> deleteTag(TagData tagData) {
        List<Node> updatedNodes = new ArrayList<>();
        tagData.getUniqueNodeIds().forEach(nodeId -> {
            try {
                Node node = getNode(nodeId);
                if (node != null) {
                    Node updatedNode = Node.builder()
                            .nodeType(node.getNodeType())
                            .userName(node.getUserName())
                            .description(node.getDescription())
                            .name(node.getName())
                            .uniqueId(node.getUniqueId())
                            .created(node.getCreated())
                            .build();
                    List<Tag> tags = node.getTags();
                    Optional<Tag> optional = tags.stream().filter(tag -> tag.getName().equals(tagData.getTag().getName())).findFirst();
                    if (optional.isPresent()) {
                        tags.remove(optional.get());
                        updatedNode.setTags(tags);
                        updatedNode = updateNode(updatedNode, false);
                        updatedNodes.add(updatedNode);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Cannot delete tag from node " + nodeId, e);
            }
        });
        return updatedNodes;
    }

    /**
     * Determines a name for a copied/moved node. Some (ugly) logic is applied to implement a strategy where
     * a string like "copy" or "copy 2" is appended to the node name in case the target node already contains a
     * child node with same name and type.
     *
     * @param sourceNode             The node subject to copy/move
     * @param targetParentChildNodes List of child nodes in target
     * @return A node name that does not clash with any existing node of same type in the target node.
     */
    protected String determineNewNodeName(Node sourceNode, List<Node> targetParentChildNodes) {
        // Filter to make sure only nodes of same type are considered.
        targetParentChildNodes = targetParentChildNodes.stream().filter(n -> n.getNodeType().equals(sourceNode.getNodeType())).collect(Collectors.toList());
        List<String> targetParentChildNodeNames = targetParentChildNodes.stream().map(Node::getName).collect(Collectors.toList());
        if (!targetParentChildNodeNames.contains(sourceNode.getName())) {
            return sourceNode.getName();
        }
        String newNodeBaseName = sourceNode.getName();
        Matcher matcher = NODE_NAME_PATTERN.matcher(newNodeBaseName);
        if (matcher.matches()) { // If source node already contains "copy X", then calculate the "base name".
            newNodeBaseName = newNodeBaseName.substring(0, (newNodeBaseName.length() - matcher.group(1).length()));
        }
        List<String> nodeNameCopies = new ArrayList<>();
        Pattern pattern = Pattern.compile(newNodeBaseName + "(\\scopy(\\s\\d*)?$)");
        for (Node targetChildNode : targetParentChildNodes) {
            String targetChildNodeName = targetChildNode.getName();
            if (pattern.matcher(targetChildNodeName).matches()) {
                nodeNameCopies.add(targetChildNodeName);
            }
        }
        // NOTE: nodeNameCopies may also contain an element with equal name as source node.
        if (nodeNameCopies.isEmpty()) {
            return newNodeBaseName + " copy";
        } else {
            Collections.sort(nodeNameCopies, new NodeNameComparator());
            try {
                String lastCopyName = nodeNameCopies.get(nodeNameCopies.size() - 1);
                if (lastCopyName.equals(newNodeBaseName + " copy")) {
                    return newNodeBaseName + " copy 2";
                } else {
                    int highestIndex = Integer.parseInt(nodeNameCopies.get(nodeNameCopies.size() - 1).substring((newNodeBaseName + " copy ").length()));
                    return newNodeBaseName + " copy " + (highestIndex + 1);
                }
            } catch (NumberFormatException e) { // Should not happen...
                logger.log(Level.WARNING, "Unable to determine copy name index from " + nodeNameCopies.get(nodeNameCopies.size() - 1));
                return sourceNode.getUniqueId();
            }
        }
    }

    /**
     * Compares {@link Node} names for the purpose of ordering.
     */
    public static class NodeNameComparator implements Comparator<String> {

        @Override
        public int compare(String s1, String s2) {
            if (s1.endsWith("copy") || s2.endsWith("copy")) {
                return s1.compareTo(s2);
            }
            int copyIndex1 = s1.indexOf("copy");
            int copyIndex2 = s1.indexOf("copy");
            int index1 = Integer.parseInt(s1.substring(copyIndex1 + 5));
            int index2 = Integer.parseInt(s2.substring(copyIndex2 + 5));
            return index1 - index2;
        }
    }
}
