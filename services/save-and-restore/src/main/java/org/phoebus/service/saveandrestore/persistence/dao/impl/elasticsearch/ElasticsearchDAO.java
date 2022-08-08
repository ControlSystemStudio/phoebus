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

import org.epics.vtype.VType;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.SnapshotPv;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.SnapshotDataConverter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.phoebus.applications.saveandrestore.model.Node.ROOT_FOLDER_UNIQUE_ID;

public class ElasticsearchDAO implements NodeDAO {

    @Autowired
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    private static final Logger logger = Logger.getLogger(ElasticsearchDAO.class.getName());


    @Override
    public List<Node> getChildNodes(String uniqueNodeId) {
        Optional<ESTreeNode> elasticsearchNode =
                elasticsearchTreeRepository.findById(uniqueNodeId);
        if(elasticsearchNode.isEmpty()){
            return null;
        }
        return elasticsearchNode.get().getChildNodes() == null ?
                Collections.emptyList() :
                elasticsearchNode.get().getChildNodes();
    }

    @Override
    public Node getNode(String uniqueNodeId) {
        Optional<ESTreeNode> optional =
                elasticsearchTreeRepository.findById(uniqueNodeId);
        if(optional.isEmpty()){
            return null;
        }
        ESTreeNode elasticsearchNode = optional.get();
        return elasticsearchNode.getNode();
    }

    @Override
    public void deleteNode(String nodeId) {
        Node nodeToDelete = getNode(nodeId);
        if (nodeToDelete == null){
            throw new NodeNotFoundException("Cannot delete non-existing node");
        }
        else if(nodeToDelete.getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)){
            throw new IllegalArgumentException("Root node cannot be deleted");
        }
        deleteNode(nodeToDelete);

    }

    @Override
    public Node createNode(String parentNodeId, Node node) {
        // Retrieve parent
        Optional<ESTreeNode> parentNode = elasticsearchTreeRepository.findById(parentNodeId);
        if(parentNode.isEmpty()){
            throw new NodeNotFoundException(
                    String.format("Cannot create new node as parent unique_id=%s does not exist.", parentNodeId));
        }
        Node parent = parentNode.get().getNode();
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
        List<Node> parentsChildNodes = parentNode.get().getChildNodes();
        if (!isNodeNameValid(node, parentsChildNodes)) {
            throw new IllegalArgumentException("Node of same name and type already exists in parent node.");
        }

        node.setCreated(new Date());
        node.setUniqueId(UUID.randomUUID().toString());
        ESTreeNode newNode = new ESTreeNode();
        newNode.setNode(node);

        elasticsearchTreeRepository.save(newNode);

        if(parentNode.get().getChildNodes() == null){
            parentNode.get().setChildNodes(new ArrayList<>());
        }
        parentNode.get().getChildNodes().add(node);
        elasticsearchTreeRepository.save(parentNode.get());

        return node;
    }

    private boolean isNodeNameValid(Node nodeToCheck, List<Node> parentsChildNodes) {
        if(parentsChildNodes == null || parentsChildNodes.isEmpty()){
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
        ESTreeNode elasticsearchTreeNode = elasticsearchTreeRepository.getParentNode(uniqueNodeId);
        if(elasticsearchTreeNode != null){
            return elasticsearchTreeNode.getNode();
        }
        else{
            return null;
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
            if(esTreeNode.isPresent()){
                sourceNodes.add(esTreeNode.get().getNode());
            }
        });

        if (sourceNodes.size() != nodeIds.size()) {
            throw new IllegalArgumentException("At least one unique node id not found.");
        }

        if(!isMoveOrCopyAllowed(sourceNodes, targetNode.get().getNode())){
            throw new IllegalArgumentException("Prerequisites for moving source node(s) not met.");
        }

        // Remove source nodes from the list of child nodes in parent node.
        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(sourceNodes.get(0).getUniqueId());
        if(parentNode == null){
            throw new RuntimeException("Parent node of source node " + sourceNodes.get(0).getUniqueId() + " not found. Should not happen.");
        }
        parentNode.getChildNodes().removeAll(sourceNodes);
        parentNode = elasticsearchTreeRepository.save(parentNode);

        // Update the target node to include the source nodes in its list of child nodes
        if(targetNode.get().getChildNodes() == null){
            targetNode.get().setChildNodes(new ArrayList<>());
        }
        targetNode.get().getChildNodes().addAll(sourceNodes);
        elasticsearchTreeRepository.save(targetNode.get());

        return parentNode.getNode();
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
            if(esTreeNode.isPresent()){
                sourceNodes.add(esTreeNode.get().getNode());
            }
        });

        if (sourceNodes.size() != nodeIds.size()) {
            throw new IllegalArgumentException("At least one unique node id not found.");
        }

        if(!isMoveOrCopyAllowed(sourceNodes, targetNode.get().getNode())){
            throw new IllegalArgumentException("Prerequisites for copying source node(s) not met.");
        }

        sourceNodes.forEach(sourceNode -> copyNode(sourceNode, targetNode.get().getNode(), userName));

        return targetNode.get().getNode();
    }

    private void copyNode(Node sourceNode, Node targetNode, String userName){
        Node newSourceNode = createNode(targetNode.getUniqueId(), sourceNode);
        newSourceNode.setUserName(userName);
        newSourceNode.setProperties(sourceNode.getProperties());
        newSourceNode.setTags(sourceNode.getTags());
        Node Source = updateNode(sourceNode, true);

        if (sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            Configuration sourceConfiguration = getConfiguration(sourceNode.getUniqueId());
            copyConfiguration(Source, sourceConfiguration);
            // TODO copy all snaoshot Nodes and Snapshot objects.
        }
        else if (sourceNode.getNodeType().equals(NodeType.FOLDER)) {
            List<Node> childNodes = getChildNodes(sourceNode.getUniqueId());
            childNodes.forEach(childNode -> copyNode(childNode, Source, userName));
        }
    }

    /**
     * Copies a {@link Configuration}.
     * @param targetConfigurationNode The configuration {@link Node} with which the copied {@link Configuration} should be
     * associated. This must already exist in the Elasticsearch index.
     * @param sourceConfiguration The source {@link Configuration}
     */
    private void copyConfiguration(Node targetConfigurationNode, Configuration sourceConfiguration){
        Configuration clonedConfiguration = Configuration.clone(sourceConfiguration);
        clonedConfiguration.setUniqueId(targetConfigurationNode.getUniqueId());
        saveConfiguration(clonedConfiguration);
    }

    @Override
    @Deprecated
    public Node updateConfiguration(Node configToUpdate, List<ConfigPv> configPvList) {
        Optional<ESTreeNode> nodeOptional = elasticsearchTreeRepository.findById(configToUpdate.getUniqueId());
        if(nodeOptional.isEmpty() || !nodeOptional.get().getNode().getNodeType().equals(NodeType.CONFIGURATION)){
            throw new IllegalArgumentException(String.format("Config node with unique id=%s not found or is wrong type", configToUpdate.getUniqueId()));
        }
        Optional<Configuration> elasticsearchSavesetOptional = configurationRepository.findById(configToUpdate.getUniqueId());
        Configuration elasticsearchConfiguration;
        if(elasticsearchSavesetOptional.isEmpty()){
            elasticsearchConfiguration = new Configuration();
            elasticsearchConfiguration.setUniqueId(configToUpdate.getUniqueId());
            elasticsearchConfiguration.setDescription(configToUpdate.getProperty("description"));
        }
        else{
            elasticsearchConfiguration = elasticsearchSavesetOptional.get();
        }
        elasticsearchConfiguration.setPvList(configPvList);
        configurationRepository.save(elasticsearchConfiguration);

        // Save the config node to update last modified date
        elasticsearchTreeRepository.save(nodeOptional.get());

        return elasticsearchTreeRepository.findById(configToUpdate.getUniqueId()).get().getNode();
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
        if(configNodeOptional.isEmpty()){
            throw new IllegalArgumentException("Cannot get snapshots for config with id " + uniqueNodeId + " as it does not exist");
        }
        if(configNodeOptional.get().getChildNodes() == null){
            return Collections.emptyList();
        }
        else{
            return configNodeOptional.get().getChildNodes();
        }
    }

    @Override
    public Node getSnapshotNode(String uniqueNodeId) {
        return null;
    }

    @Override
    public Node saveSnapshot(String parentsUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment, String userName) {
        Optional<ESTreeNode> configNodeOptional = elasticsearchTreeRepository.findById(parentsUniqueId);
        if(configNodeOptional.isEmpty()){
            throw new NodeNotFoundException("Config node with id " + parentsUniqueId + " not found");
        }
        else if(!configNodeOptional.get().getNode().getNodeType().equals(NodeType.CONFIGURATION)){
            throw new IllegalArgumentException("Node with id " + parentsUniqueId + " is not a config node");
        }

        ESTreeNode configNode = configNodeOptional.get();
        if(configNode.getChildNodes() == null){
            configNode.setChildNodes(new ArrayList<>());
        }
        if(configNodeOptional.get().getChildNodes().stream().filter(n -> n.getName().equals(snapshotName)).findFirst().isPresent()){
            throw new IllegalArgumentException("Snapshot with name " + snapshotName + " already exists");
        }
        Date now = new Date();
        Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT)
                .name(snapshotName)
                .userName(userName)
                .uniqueId(UUID.randomUUID().toString())
                .created(now)
                .build();
        ESTreeNode elasticsearchTreeNode = new ESTreeNode();
        elasticsearchTreeNode.setNode(snapshotNode);
        elasticsearchTreeNode = elasticsearchTreeRepository.save(elasticsearchTreeNode);

        // Update parent config node's child node list

        configNodeOptional.get().getChildNodes().add(snapshotNode);
        elasticsearchTreeRepository.save(configNodeOptional.get());

        Snapshot elasticsearchSnapshot = new Snapshot();
        elasticsearchSnapshot.setConfigId(parentsUniqueId);
        elasticsearchSnapshot.setComment(comment);
        elasticsearchSnapshot.setUniqueId(snapshotNode.getUniqueId());

        List<SnapshotPv> snapshotPvs = new ArrayList<>();
        snapshotItems.forEach(si -> {
            SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(si.getValue());
            snapshotPv.setPvName(si.getConfigPv().getPvName());
            snapshotPvs.add(snapshotPv);
        });
        elasticsearchSnapshot.setPvList(snapshotPvs);

        snapshotRepository.save(elasticsearchSnapshot);

        return elasticsearchTreeNode.getNode();
    }

    @Override
    public List<ConfigPv> getConfigPvs(String configUniqueId) {
        Optional<Configuration> elasticsearchSavesetOptional = configurationRepository.findById(configUniqueId);
        if(elasticsearchSavesetOptional.isEmpty()){
            return Collections.emptyList();
        }
        return elasticsearchSavesetOptional.get().getPvList();
    }

    @Override
    public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {
        Optional<Snapshot> elasticsearchSnapshotOptional = snapshotRepository.findById(snapshotUniqueId);
        if(elasticsearchSnapshotOptional.isEmpty()){
            return Collections.emptyList();
        }
        else {
            List<SnapshotItem> items = new ArrayList<>();
            elasticsearchSnapshotOptional.get().getPvList().forEach(pv -> {
                VType value = SnapshotDataConverter.toVType(pv);
                SnapshotItem snapshotItem = new SnapshotItem();
                snapshotItem.setValue(value);
                items.add(snapshotItem);
            });
            return items;
        }
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {
        Optional<ESTreeNode> nodeOptional = elasticsearchTreeRepository.findById(nodeToUpdate.getUniqueId());
        if(nodeOptional.isEmpty()){
            throw new IllegalArgumentException(String.format("CNode with unique id=%s not found", nodeToUpdate.getUniqueId()));
        }
        else if(nodeOptional.get().getNode().getUniqueId().equals(ROOT_FOLDER_UNIQUE_ID)){
            throw new IllegalArgumentException("Updating root node is not allowed");
        }

        Date now = new Date();
        if(customTimeForMigration){
            nodeToUpdate.setCreated(now);
        }
        nodeToUpdate.setLastModified(now);
        ESTreeNode esTreeNode = nodeOptional.get();
        esTreeNode.setNode(nodeToUpdate);

        elasticsearchTreeRepository.save(esTreeNode);

        return elasticsearchTreeRepository.findById(nodeToUpdate.getUniqueId()).get().getNode();
    }

    @Override
    public List<Tag> getAllTags() {
        return null;
    }

    @Override
    public List<Tag> getTags(String uniqueSnapshotId) {
        return null;
    }

    @Override
    public List<Node> getAllSnapshots() {
        return null;
    }

    @Override
    public List<Node> getFromPath(String path) {
        return null;
    }

    @Override
    public String getFullPath(String uniqueNodeId) {
        return null;
    }

    private void deleteNode(Node nodeToDelete){
         if (nodeToDelete.getNodeType().equals(NodeType.CONFIGURATION)) {
            // TODO: delete save set document
            for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
                deleteNode(node);
            }
        } else if (nodeToDelete.getNodeType().equals(NodeType.FOLDER)) {
            for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
                deleteNode(node);
            }
        } else if (nodeToDelete.getNodeType().equals(NodeType.SNAPSHOT)) {
            // TODO: delete snapshot document
        }

        // Update the parent node to update its list of child nodes
        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode(nodeToDelete.getUniqueId());
        parentNode.getChildNodes().remove(nodeToDelete);
        elasticsearchTreeRepository.save(parentNode);

        // Delete the node
        elasticsearchTreeRepository.deleteById(nodeToDelete.getUniqueId());
    }

    @Override
    public Configuration saveConfiguration(Configuration configuration){
        return configurationRepository.save(configuration);
    }

    @Override
    public Configuration updateConfiguration(Configuration configuration){
        Configuration existingConfiguration = getConfiguration(configuration.getUniqueId());
        existingConfiguration.setDescription(configuration.getDescription());
        existingConfiguration.setPvList(configuration.getPvList());
        return configurationRepository.save(existingConfiguration);
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
     * @param nodes      List of source {@link Node}s
     * @param targetNode The wanted target {@link Node}
     * @return <code>true</code> if move criteria are met, otherwise <code>false</code>
     */
    protected boolean isMoveOrCopyAllowed(List<Node> nodes, Node targetNode) {
        Node rootNode = getRootNode();
        // Check for root node and snapshot
        Optional<Node> rootOrSnapshotNode = nodes.stream()
                .filter(node -> node.getName().equals(rootNode.getName()) ||
                        node.getNodeType().equals(NodeType.SNAPSHOT)).findFirst();
        if (rootOrSnapshotNode.isPresent()) {
            logger.info("Move/copy not allowed: source node(s) list contains snapshot or root node.");
            return false;
        }
        // Check if selection contains save set node.
        Optional<Node> saveSetNode = nodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.CONFIGURATION)).findFirst();
        // Save set nodes may not be moved/copied to root node.
        if(saveSetNode.isPresent() && targetNode.getUniqueId().equals(rootNode.getUniqueId())){
            logger.info("Move/copy of save set node(s) to root node not allowed.");
            return false;
        }
        if (nodes.size() > 1) {
            // Check that all elements are of same type and have same parent.
            NodeType firstElementType = nodes.get(0).getNodeType();
            Node parentNodeOfFirst = getParentNode(nodes.get(0).getUniqueId());
            for (int i = 1; i < nodes.size(); i++) {
                if (!nodes.get(i).getNodeType().equals(firstElementType)) {
                    logger.info("Move not allowed: all source nodes must be of same type.");
                    return false;
                }
                Node parent = getParentNode(nodes.get(i).getUniqueId());
                if (!parent.getUniqueId().equals(parentNodeOfFirst.getUniqueId())) {
                    logger.info("Move not allowed: all source nodes must have same parent node.");
                    return false;
                }
            }
        }
        // Check if there is any name/type clash
        List<Node> parentsChildNodes = getChildNodes(targetNode.getUniqueId());
        for (Node node : nodes) {
            if (!isNodeNameValid(node, parentsChildNodes)) {
                logger.info("Move/copy not allowed: target node already contains child node with same name and type: " + node.getName());
                return false;
            }
        }

        for(Node sourceNode : nodes){
            if(containedInSubTree(sourceNode, targetNode.getUniqueId())){
                return false;
            }
        }

        return true;
    }

    private boolean containedInSubTree(Node startNode, String nodeId){
        Optional<ESTreeNode> esTreeNode = elasticsearchTreeRepository.findById(startNode.getUniqueId());
        if(esTreeNode.isEmpty()){
            throw new IllegalArgumentException("Unable to check if node " + nodeId + " is contained in subtree of node " +
                    startNode.getUniqueId() + " as the node " + startNode.getUniqueId() + " does not exist");
        }
        if(esTreeNode.get().getNode().getUniqueId().equals(nodeId)){
            return true;
        }
        else if(esTreeNode.get().getChildNodes() != null && !esTreeNode.get().getChildNodes().isEmpty()){
            for (Node childNode : esTreeNode.get().getChildNodes()){
                if(childNode.getUniqueId().equals(nodeId)){
                    return true;
                }
                else {
                    return containedInSubTree(childNode, nodeId);
                }
            }
        }
        return false;
    }

    @Override
    public Configuration getConfiguration(String nodeId) {
        Optional<Configuration> configuration = configurationRepository.findById(nodeId);
        if (configuration.isEmpty()) {
            throw new NodeNotFoundException("Configuration with id " + nodeId + " not found");
        }
        return configuration.get();
    }

    @Override
    public Snapshot saveSnapshot(Snapshot snapshot){
        return snapshotRepository.save(snapshot);
    }

    @Override
    public Snapshot getSnapshot(String nodeId){
        Optional<Snapshot> snapshot = snapshotRepository.findById(nodeId);
        if(snapshot.isEmpty()){
            throw new NodeNotFoundException("Snapshot with id " + nodeId + " not found");
        }
        return snapshot.get();
    }
}
