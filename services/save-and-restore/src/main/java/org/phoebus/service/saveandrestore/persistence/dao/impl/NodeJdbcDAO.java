/*
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

package org.phoebus.service.saveandrestore.persistence.dao.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.model.internal.SnapshotPv;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.SnapshotDataConverter;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;
import org.phoebus.service.saveandrestore.services.exception.SnapshotNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author georgweiss
 * Created 11 Mar 2019
 */
public class NodeJdbcDAO implements NodeDAO {

    @SuppressWarnings("unused")
    @Autowired
    private SimpleJdbcInsert configurationEntryInsert;

    @SuppressWarnings("unused")
    @Autowired
    private SimpleJdbcInsert configurationEntryRelationInsert;

    @SuppressWarnings("unused")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unused")
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @SuppressWarnings("unused")
    @Autowired
    private NamedParameterJdbcTemplate nodeListParameterJdbcTemplate;

    @SuppressWarnings("unused")
    @Autowired
    private NamedParameterJdbcTemplate descendantCheckParameterJdbcTemplate;

    @SuppressWarnings("unused")
    @Autowired
    private SimpleJdbcInsert nodeInsert;

    @SuppressWarnings("unused")
    @Autowired
    private SimpleJdbcInsert pvInsert;

    @SuppressWarnings("unused")
    @Autowired
    private SimpleJdbcInsert snapshotPvInsert;

    private static final int NO_ID = -1;
    private static final Logger LOG = Logger.getLogger(NodeJdbcDAO.class.getName());

    /**
     * This object is used to synchronize operations that otherwise might result in data corruption.
     * For instance, if one user is moving (using drag-n-drop) nodes to a target folder that another
     * user is deleting, the moved nodes - while still existing the database - would then not be
     * reachable through any tree path. While the probability for this to happen should be low, recovery
     * after such a failed move would require manual update of the database.
     * <p>
     * Consequently a synchronization object is used to synchronize deletion and move operations.
     */
    private final Object deleteNodeSyncObject = new Object();

    private Node getParentNode(int nodeId) {

        // Root folder is its own parent
        if (nodeId == Node.ROOT_NODE_ID) {
            return getNode(Node.ROOT_NODE_ID);
        }

        try {
            int parentNodeId = jdbcTemplate.queryForObject(
                    "select ancestor from node_closure where descendant=? and depth=1", new Object[]{nodeId},
                    Integer.class);
            return getNode(parentNodeId);
        } catch (DataAccessException e) {
            return null;
        }

    }

    @Override
    public Node getParentNode(String uniqueNodeId) {
        int nodeId;

        try {
            nodeId = jdbcTemplate.queryForObject("select id from node where unique_id=?", new Object[]{uniqueNodeId}, Integer.class);
            return getParentNode(nodeId);
        } catch (DataAccessException e) {
            return null;
        }

    }

    /**
     * Retrieves a {@link Node} associated with the specified node id.
     *
     * @param nodeId The node id.
     * @return <code>null</code> if the node id is not found, otherwise either a
     * {@link Node} object.
     */
    private Node getNode(int nodeId) {

        try {
            Node node = jdbcTemplate.queryForObject("select * from node where id=?", new Object[]{nodeId},
                    new NodeRowMapper());
            node.setProperties(getProperties(node.getId()));
            node.setTags(getTags(node.getUniqueId()));
            return node;
        } catch (DataAccessException e) {
            return null;
        }

    }

    /**
     * Retrieves a {@link Node} associated with the specified node id.
     *
     * @param uniqueNodeId The node id.
     * @return <code>null</code> if the node id is not found, otherwise either a
     * {@link Node} object.
     */
    @Override
    public Node getNode(String uniqueNodeId) {
        int nodeId;

        try {
            nodeId = jdbcTemplate.queryForObject("select id from node where unique_id=?", new Object[]{uniqueNodeId}, Integer.class);
            return getNode(nodeId);
        } catch (DataAccessException e) {
            return null;
        }

    }

    @Override
    public List<Node> getChildNodes(String uniqueNodeId) {

        Node parentNode = getNode(uniqueNodeId);
        // Node may have been deleted -> client's data is out-of-sync with server's data
        if (parentNode == null) {
            throw new NodeNotFoundException(String.format("Cannot get child nodes of unique id %s as it does not exist", uniqueNodeId));
        }

        return getChildNodes(parentNode.getId());
    }

    private List<Node> getChildNodes(int nodeId) {

        List<Node> childNodes = jdbcTemplate.query("select n.* from node as n join node_closure as nc on n.id=nc.descendant where "
                + "nc.ancestor=? and nc.depth=1", new Object[]{nodeId}, new NodeRowMapper());
        childNodes.forEach(childNode -> {
            childNode.setProperties(getProperties(childNode.getId()));
            childNode.setTags(getTags(childNode.getUniqueId()));
        });
        return childNodes;
    }

    @Override
    @Deprecated
    public void deleteNode(String nodeId) {
        Node nodeToDelete = getNode(nodeId);
        if (nodeToDelete == null || nodeToDelete.getId() == Node.ROOT_NODE_ID){
            throw new IllegalArgumentException("Cannot delete non-existing node");
        }
        deleteNode(nodeToDelete);
    }

    @Override
    public void deleteNodes(List<String> nodeIds){
        synchronized (deleteNodeSyncObject) {
            // Get all nodes
            List<Node> nodes = nodeIds.stream().map(nodeId -> getNode(nodeId)).collect(Collectors.toList());
            if(nodes.stream().anyMatch(Objects::isNull)){
                throw new IllegalArgumentException("At least one element in list of nodes to delete is invalid");
            }
            nodes.forEach(node -> deleteNode(node));
        }
    }

    private void deleteNode(Node nodeToDelete){
        Node parentNode = getParentNode(nodeToDelete.getUniqueId());
        if (nodeToDelete.getNodeType().equals(NodeType.CONFIGURATION)) {
            List<Integer> configPvIds = jdbcTemplate.queryForList(
                    "select config_pv_id from config_pv_relation where config_id=?", new Object[]{nodeToDelete.getId()},
                    Integer.class);
            deleteOrphanedPVs(configPvIds);
            for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
                deleteNode(node);
            }
        } else if (nodeToDelete.getNodeType().equals(NodeType.FOLDER)) {
            for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
                deleteNode(node);
            }
        } else if (nodeToDelete.getNodeType().equals(NodeType.SNAPSHOT)) {
            jdbcTemplate.update("delete from snapshot_node_pv where snapshot_node_id=?", nodeToDelete.getId());
        }

        jdbcTemplate.update("delete from node where unique_id=?", nodeToDelete.getUniqueId());

        // Update last modified date of the parent node
        jdbcTemplate.update("update node set last_modified=? where id=?", Timestamp.from(Instant.now()), parentNode.getId());
    }

    /**
     * Creates a new node in the tree. An {@link IllegalArgumentException} is thrown if:
     * <ul>
     * <li>The <code>parentsUniqueId</code> argument is null or identifies a non-existing node</li>
     * <li>If the node's type is {@link NodeType#FOLDER} or {@link NodeType#CONFIGURATION} and the parent node is not of type {@link NodeType#FOLDER}.</li>
     * <li>If the node's type is {@link NodeType#SNAPSHOT} and the parent node is not of type {@link NodeType#CONFIGURATION}.</li>
     * <li>If the parent node already contains a child node of the same type and name.</li>
     * </ul>
     * <p>
     * Note that this method is synchronized to avoid creation of {@link Node}s with same name and type in
     * a folder {@link Node}.
     */
    @Override
    public Node createNode(String parentNodeId, final Node node) {

        Timestamp now = Timestamp.from(Instant.now());
        return createNodeInternal(parentNodeId, node, now, now);
    }

    private synchronized Node createNodeInternal(String parentNodeId, final Node node, Timestamp createdDate, Timestamp lastModified) {
        Node parentNode = getNode(parentNodeId);

        if (parentNode == null) {
            throw new NodeNotFoundException(
                    String.format("Cannot create new node as parent unique_id=%s does not exist.", parentNodeId));
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

        String uniqueId = UUID.randomUUID().toString();

        Map<String, Object> params = new HashMap<>(2);
        params.put("type", node.getNodeType().toString());
        params.put("created", createdDate);
        params.put("last_modified", lastModified);
        params.put("unique_id", uniqueId);
        String name = node.getName();
        if (node.getNodeType().equals(NodeType.SNAPSHOT) && name != null && name.isEmpty()) {
            name = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(createdDate);
        }
        params.put("name", name);
        params.put("username", node.getUserName());


        int newNodeId = nodeInsert.executeAndReturnKey(params).intValue();

        jdbcTemplate.update(
                "insert into node_closure (ancestor, descendant, depth) " + "select t.ancestor, " + newNodeId
                        + ", t.depth + 1  from node_closure as t where t.descendant = ? union all select ?, ?, 0",
                parentNode.getId(), newNodeId, newNodeId);

        // Update the last modified date of the parent folder
        jdbcTemplate.update("update node set last_modified=? where id=?", Timestamp.from(Instant.now()),
                parentNode.getId());

        updateProperties(newNodeId, node.getProperties());
        updateTags(uniqueId, node.getTags());

        return getNode(uniqueId);
    }

    @Override
    public List<ConfigPv> getConfigPvs(String configUniqueNodeId) {

        return jdbcTemplate.query("select cp.id, pv1.name as name, pv2.name as readback_name, cp.readonly from config_pv as cp " +
                        "join config_pv_relation as cpr on cp.id=cpr.config_pv_id " +
                        "join node as n on n.id=cpr.config_id " +
                        "left join pv pv1 on cp.pv_id=pv1.id " +
                        "left join pv pv2 on cp.readback_pv_id=pv2.id " +
                        "where cpr.config_id=(select id from node where unique_id=?)",
                new Object[]{configUniqueNodeId}, new ConfigPvRowMapper());
    }

    @Override
    public Node getRootNode() {
        return getNode(Node.ROOT_NODE_ID);
    }


    private void saveConfigPv(int nodeId, ConfigPv configPv) {
        List<Integer> list;
        if (configPv.getReadbackPvName() == null) {
            list = jdbcTemplate.queryForList("select cp.id from config_pv as cp " +
                            "left join pv pv1 on cp.pv_id=pv1.id " +
                            "left join pv pv2 on cp.readback_pv_id=pv2.id " +
                            "where pv1.name=? and pv2.name is NULL and cp.readonly=?",
                    new Object[]{configPv.getPvName(), configPv.isReadOnly()}, Integer.class);
        } else {
            list = jdbcTemplate.queryForList("select cp.id from config_pv as cp " +
                            "left join pv pv1 on cp.pv_id=pv1.id " +
                            "left join pv pv2 on cp.readback_pv_id=pv2.id " +
                            "where pv1.name=? and pv2.name=? and cp.readonly=?",
                    new Object[]{configPv.getPvName(), configPv.getReadbackPvName(), configPv.isReadOnly()}, Integer.class);
        }


        int configPvId;

        if (!list.isEmpty()) {
            configPvId = list.get(0);
        } else {
            Map<String, Object> params = new HashMap<>(4);
            params.put("pv_id", getOrInsertPvName(configPv.getPvName().trim()));
            int readbackPvId = getOrInsertPvName(configPv.getReadbackPvName());
            if (readbackPvId != NO_ID) {
                params.put("readback_pv_id", readbackPvId);
            }
            params.put("readonly", configPv.isReadOnly());
            configPvId = configurationEntryInsert.executeAndReturnKey(params).intValue();
        }

        Map<String, Object> params = new HashMap<>(2);
        params.put("config_id", nodeId);
        params.put("config_pv_id", configPvId);

        configurationEntryRelationInsert.execute(params);
    }

    private int getOrInsertPvName(String pvName) {
        if (pvName == null || pvName.isEmpty()) {
            return NO_ID;
        }
        int pvId = getPvId(pvName);
        if (pvId == NO_ID) {
            pvId = addPvName(pvName);
        }
        return pvId;
    }

    private int getPvId(String pvName) {
        try {
            return jdbcTemplate.queryForObject("select id from pv where name=?",
                    new Object[]{pvName}, Integer.class);
        } catch (DataAccessException e) {
            return NO_ID;
        }
    }

    private int addPvName(String pvName) {
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Map<String, Object> params = new HashMap<>(3);
        params.put("name", pvName);
        params.put("created", time);
        params.put("last_modified", time);

        return pvInsert.executeAndReturnKey(params).intValue();
    }

    private void deleteOrphanedPVs(Collection<Integer> pvList) {
        for (Integer pvId : pvList) {
            int count = jdbcTemplate.queryForObject("select count(*) from config_pv_relation where config_pv_id=?",
                    new Object[]{pvId}, Integer.class);

            if (count == 0) {
                jdbcTemplate.update("delete from config_pv where id=?", pvId);
            }
        }
    }

    /**
     * Moves a list of nodes from one parent to another. A number of checks are done to make sure the operation
     * is possible and allowed:
     * <ul>
     *     <li>Snapshot {@link Node}s cannot be moved.</li>
     *     <li>Root node cannot be moved</li>
     *     <li>All nodes must be of same {@link NodeType}</li>
     * </ul>
     *
     * @param nodeIds  The list of source unique node ids subject to the move operation.
     * @param targetId The  unique target node id, which must be a folder {@link Node}.
     * @param userName The (account) name of the user performing the operation.
     * @return The target {@link Node} the client should update in order to sync with the changes.
     * @throws NodeNotFoundException    if the target {@link Node} does not exist.
     * @throws IllegalArgumentException if target {@link Node} does not exist, if target {@link Node} is not
     *                                  a folder {@link Node}, or if the target {@link Node} already contains {@link Node}s of same name and type
     *                                  as the any of the elements in the source {@link Node} list.
     */
    @Override
    public Node moveNodes(List<String> nodeIds, String targetId, String userName) {
        synchronized (deleteNodeSyncObject) {
            Node targetNode = getNode(targetId);
            if (targetNode == null) {
                throw new NodeNotFoundException(String.format("Target node with unique id=%s not found", targetId));
            }

            if (!targetNode.getNodeType().equals(NodeType.FOLDER)) {
                throw new IllegalArgumentException("Move not allowed: target node is not a folder");
            }

            MapSqlParameterSource params =
                    new MapSqlParameterSource().addValue("nodeIds", nodeIds);
            List<Node> sourceNodes = nodeListParameterJdbcTemplate.query("select * from node where unique_id in (:nodeIds) ", params, new NodeRowMapper());
            // Size of return nodes must match size of node ids.
            if (sourceNodes.size() != nodeIds.size()) {
                throw new IllegalArgumentException("At least one unique node id not found.");
            }

            // Now check if any of the list of source nodes can be moved
            if (!isMoveOrCopyAllowed(sourceNodes, targetNode)) {
                throw new IllegalArgumentException("Prerequisites for moving source node(s) not met.");
            }

            for (Node sourceNode : sourceNodes) {
                List<Integer> descendants = null;
                try {
                    descendants = jdbcTemplate.queryForList(
                            "select descendant from node_closure where ancestor=?", new Object[]{sourceNode.getId()},
                            Integer.class);
                } catch (DataAccessException e) {
                    descendants = Collections.emptyList();
                }

                List<Integer> ancestors;

                try {
                    ancestors = jdbcTemplate.queryForList(
                            "select ancestor from node_closure where descendant=? and ancestor != descendant",
                            new Object[]{sourceNode.getId()},
                            Integer.class);
                } catch (DataAccessException e) {
                    ancestors = Collections.emptyList();
                }

                params = new MapSqlParameterSource().addValue("descendants", descendants)
                        .addValue("ancestors", ancestors);

                // Sub-selects in the below will not work on Mysql as table being updated cannot
                // be used in sub-select.
                // So: list of descendants and ancestors must be retrieved as separate queries.
                namedParameterJdbcTemplate.update("delete from node_closure where "
                        + "descendant in (:descendants) "
                        + "and ancestor in (:ancestors)", params);

                jdbcTemplate.update("insert into node_closure (ancestor, descendant, depth) "
                        + "select supertree.ancestor, subtree.descendant, supertree.depth + subtree.depth + 1 AS depth "
                        + "from node_closure as supertree " + "cross join node_closure as subtree "
                        + "where supertree.descendant=? and subtree.ancestor=?", targetNode.getId(), sourceNode.getId());

                // Update the last modified date of the source and target folder.
                jdbcTemplate.update("update node set last_modified=?, username=? where id=? or id=?",
                        Timestamp.from(Instant.now()), userName, targetNode.getId(), sourceNode.getId());
            }

            return getNode(targetNode.getId());
        }
    }

    @Override
    public Node updateConfiguration(Node configToUpdate, List<ConfigPv> updatedConfigPvList) {

        Node configNode = getNode(configToUpdate.getUniqueId());

        if (configNode == null || !configNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            throw new IllegalArgumentException(String.format("Config node with unique id=%s not found or is wrong type", configToUpdate.getUniqueId()));
        }

        List<ConfigPv> existingConfigPvList = getConfigPvs(configToUpdate.getUniqueId());

        Collection<ConfigPv> pvsToRemove = CollectionUtils.removeAll(existingConfigPvList,
                updatedConfigPvList);
        Collection<Integer> pvIdsToRemove = CollectionUtils.collect(pvsToRemove, ConfigPv::getId);

        // Remove PVs from relation table
        pvIdsToRemove.forEach(id -> jdbcTemplate.update(
                "delete from config_pv_relation where config_id=? and config_pv_id=?",
                configNode.getId(),
                id));

        // Check if any of the PVs is orphaned
        deleteOrphanedPVs(pvIdsToRemove);

        Collection<ConfigPv> pvsToAdd = CollectionUtils.removeAll(updatedConfigPvList,
                existingConfigPvList);

        // Add new PVs
        pvsToAdd.stream().forEach(configPv -> saveConfigPv(configNode.getId(), configPv));

        updateProperties(configNode.getId(), configToUpdate.getProperties());
        updateTags(configNode.getUniqueId(), configToUpdate.getTags());

        jdbcTemplate.update("update node set username=?, last_modified=? where id=?",
                configToUpdate.getUserName(), Timestamp.from(Instant.now()), configNode.getId());

        return getNode(configNode.getId());
    }

    @Override
    public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {

        Node persistedNode = getNode(nodeToUpdate.getUniqueId());

        if (persistedNode == null) {
            throw new NodeNotFoundException(String.format("Node with unique id=%s not found", nodeToUpdate.getUniqueId()));
        }

        if (persistedNode.getId() == Node.ROOT_NODE_ID) {
            throw new IllegalArgumentException("Cannot update root node");
        }

        if (!persistedNode.getNodeType().equals(nodeToUpdate.getNodeType())) {
            throw new IllegalArgumentException("Changing node type is not supported");
        }

        Node parentNode = getParentNode(persistedNode.getUniqueId());

        List<Node> parentsChildNodes = getChildNodes(parentNode.getUniqueId());
        // Remove the node subject to update as this should not be part of the check
        parentsChildNodes.remove(nodeToUpdate);
        if (!isNodeNameValid(nodeToUpdate, parentsChildNodes)) {
            throw new IllegalArgumentException(String.format("A node with same type and name (%s) already exists in the same folder", nodeToUpdate.getName()));
        }

        if (customTimeForMigration) {
            jdbcTemplate.update("update node set name=?, created=?, last_modified=?, username=? where id=?",
                    nodeToUpdate.getName(), nodeToUpdate.getCreated(), Timestamp.from(Instant.now()), nodeToUpdate.getUserName(), nodeToUpdate.getId());
        } else {
            jdbcTemplate.update("update node set name=?, last_modified=?, username=? where id=?",
                    nodeToUpdate.getName(), Timestamp.from(Instant.now()), nodeToUpdate.getUserName(), nodeToUpdate.getId());
        }

        updateProperties(nodeToUpdate.getId(), nodeToUpdate.getProperties());
        updateTags(nodeToUpdate.getUniqueId(), nodeToUpdate.getTags());

        return getNode(nodeToUpdate.getId());
    }

    @Override
    public Node saveSnapshot(String parentsUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment, String userName) {
        Timestamp now = Timestamp.from(Instant.now());
        return saveSnapshotInternal(parentsUniqueId, snapshotItems, snapshotName, comment, userName, now, now);
    }

    private Node saveSnapshotInternal(String parentsUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment, String userName, Timestamp created, Timestamp lastModified) {
        Node snapshotNode = createNodeInternal(parentsUniqueId, Node.builder()
                .name(snapshotName)
                .nodeType(NodeType.SNAPSHOT)
                .build(),
                created,
                lastModified);

        List<Map<String, Object>> paramsForBatch = new ArrayList<>();
        for (SnapshotItem snapshotItem : snapshotItems) {
            Map<String, Object> params = new HashMap<>(6);
            params.put("snapshot_node_id", snapshotNode.getId());

            params.put("config_pv_id", snapshotItem.getConfigPv().getId());
            // Should not happen, but if the snapshot value has not been set, continue...
            if (snapshotItem.getValue() == null) {
                continue;
            }

            SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(snapshotItem.getValue());
            params.put("severity", snapshotPv.getAlarmSeverity().toString());
            params.put("status", snapshotPv.getAlarmStatus().toString());
            params.put("time", snapshotPv.getTime());
            params.put("timens", snapshotPv.getTimens());
            params.put("sizes", snapshotPv.getSizes());
            params.put("data_type", snapshotPv.getDataType().toString());
            params.put("value", snapshotPv.getValue());

            if (snapshotItem.getReadbackValue() != null) {
                SnapshotPv snapshotReadbackPv = SnapshotDataConverter.fromVType(snapshotItem.getReadbackValue());
                params.put("readback_severity", snapshotReadbackPv.getAlarmSeverity().toString());
                params.put("readback_status", snapshotReadbackPv.getAlarmStatus().toString());
                params.put("readback_time", snapshotReadbackPv.getTime());
                params.put("readback_timens", snapshotReadbackPv.getTimens());
                params.put("readback_sizes", snapshotReadbackPv.getSizes());
                params.put("readback_data_type", snapshotReadbackPv.getDataType().toString());
                params.put("readback_value", snapshotReadbackPv.getValue());
            }
            paramsForBatch.add(params);
        }
        snapshotPvInsert.executeBatch(paramsForBatch.toArray(new Map[paramsForBatch.size()]));

        jdbcTemplate.update("update node set name=?, username=?, last_modified=? where unique_id=?", snapshotName, userName, lastModified, snapshotNode.getUniqueId());
        insertOrUpdateProperty(snapshotNode.getId(), new AbstractMap.SimpleEntry<String, String>("comment", comment));

        return getSnapshot(snapshotNode.getUniqueId());
    }

    /**
     * Retrieves saved snapshot nodes.
     *
     * @param uniqueNodeId The unique node id of the snapshot
     */
    @Override
    public List<Node> getSnapshots(String uniqueNodeId) {

        List<Node> snapshots = jdbcTemplate.query("select n.*, nc.ancestor from node as n " +
                        "join node_closure as nc on n.id=nc.descendant " +
                        "where n.username is not NULL and nc.ancestor=(select id from node where unique_id=?) and nc.depth=1", new Object[]{uniqueNodeId},
                new NodeRowMapper());

        for (Node snapshot : snapshots) {
            snapshot.setProperties(getProperties(snapshot.getId()));
            snapshot.setTags(getTags(snapshot.getUniqueId()));
        }

        return snapshots;

    }

    @Override
    public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId) {

        List<SnapshotItem> snapshotItems = jdbcTemplate.query("select snp.*, pv1.name, pv2.name as readback_name, cp.readonly, cp.id as id from snapshot_node_pv as snp " +
                        "join config_pv as cp on snp.config_pv_id=cp.id " +
                        "left join pv pv1 on cp.pv_id=pv1.id " +
                        "left join pv pv2 on cp.readback_pv_id=pv2.id " +
                        "where snapshot_node_id=(select id from node where unique_id=?)",
                new Object[]{snapshotUniqueId},
                new SnapshotItemRowMapper());

        return snapshotItems;
    }

    @Override
    public Node getSnapshot(String uniqueNodeId) {

        Node snapshotNode = getNode(uniqueNodeId);
        if (snapshotNode == null || !snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            throw new SnapshotNotFoundException("Snapshot with id " + uniqueNodeId + " not found");
        }

        return snapshotNode;
    }

    /**
     * Deletes all properties for the specified node id, and then inserts the properties
     * as specified in the <code>properties</code> parameter. The client hence must make sure
     * that any existing properties that should not be deleted are present in the map.
     * <p>
     * Keys and values of the map must all be non-null and non-empty in order to
     * be inserted.
     * <p>
     * Specifying a <code>null<code> map of properties will delete all existing.
     *
     * @param nodeId     The id of the {@link Node}
     * @param properties Map of properties to insert.
     */
    private void updateProperties(int nodeId, Map<String, String> properties) {

        jdbcTemplate.update("delete from property where node_id=?", nodeId);

        if (properties == null || properties.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            insertOrUpdateProperty(nodeId, entry);
        }
    }

    /**
     * This method is intentionally not using "on duplicate key" insert since that
     * is tricky to set up for the H2 database used in unit testing.
     *
     * @param nodeId The node id identifying the owning node.
     * @param entry  The map entry (including key and value).
     */
    private void insertOrUpdateProperty(int nodeId, Map.Entry<String, String> entry) {
        if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null || entry.getValue().isEmpty()) {
            return; // Ignore rather than throwing exception in order to not break callee's loop.
        }
        // Disallow setting the "root" property. It is set by Flyway.
        if ("root".equals(entry.getKey())) {
            return;
        }
        // First check if there is an existing property for the combination of node id and key
        int numberOfHits = jdbcTemplate.queryForObject("select count(*) from property where node_id=? and property_name=?",
                new Object[]{nodeId, entry.getKey()},
                Integer.class);
        if (numberOfHits == 0) {
            jdbcTemplate.update("insert into property values(?, ?, ?)", nodeId, entry.getKey(), entry.getValue());
        } else {
            jdbcTemplate.update("update property set value=? where node_id=? and property_name=?", entry.getValue(), nodeId, entry.getKey());
        }
    }

    private Map<String, String> getProperties(int nodeId) {
        return jdbcTemplate.query("select * from property where node_id=?",
                new Object[]{nodeId}, new PropertiesRowMapper());
    }

    public List<Tag> getAllTags() {
        return jdbcTemplate.query("select * from tag", new TagsRowMapper());
    }

    public List<Tag> getTags(String uniqueSnapshotId) {
        return jdbcTemplate.query("select * from tag where snapshot_id=?", new Object[]{uniqueSnapshotId}, new TagsRowMapper());
    }

    private void updateTags(String uniqueSnapshotId, List<Tag> tags) {
        jdbcTemplate.update("delete from tag where snapshot_id=?", uniqueSnapshotId);

        if (tags == null || tags.isEmpty()) {
            return;
        }

        tags.stream().forEach(tag -> {
            if (tag.getCreated() == null) {
                jdbcTemplate.update("insert into tag (snapshot_id, name, comment, username) values(?, ?, ?, ?)",
                        tag.getSnapshotId(), tag.getName(), tag.getComment(), tag.getUserName());
            } else {
                jdbcTemplate.update("insert into tag (snapshot_id, name, comment, created, username) values(?, ?, ?, ?, ?)",
                        tag.getSnapshotId(), tag.getName(), tag.getComment(), tag.getCreated(), tag.getUserName());
            }
        });
    }

    public List<Node> getAllSnapshots() {
        List<Node> snapshotList = jdbcTemplate.query("select * from node where type=?", new Object[]{NodeType.SNAPSHOT.toString()}, new NodeRowMapper());

        snapshotList.parallelStream().forEach(node -> {
            node.setProperties(getProperties(node.getId()));
            node.setTags(getTags(node.getUniqueId()));
        });

        return snapshotList;
    }


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
    protected Node findParentFromPathElements(Node parentNode, String[] splitPath, int index) {
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

    public String getFullPath(String uniqueNodeId) {
        if (uniqueNodeId == null || uniqueNodeId.isEmpty()) {
            return null;
        }
        Node node = getNode(uniqueNodeId);
        if (node == null) {
            return null;
        } else if (node.getId() == Node.ROOT_NODE_ID) {
            return "/";
        }

        return prependParent("", node);
    }

    /**
     * Prepends the name of the parent node and - if the parent
     * node is not the root node - recursively calls this method to continue up
     * the hierarchy until the root node is reached.
     *
     * @param path Non-null path to be prepended with the name of the parent.
     * @param node Non-null {@link Node} object as retrieved from the persistence layer.
     * @return The name of the specified node, prepended by its parent's name.
     */
    protected String prependParent(String path, Node node) {
        path = "/" + node.getName() + path;
        Node parentNode = getParentNode(node.getUniqueId());
        if (parentNode.getId() == Node.ROOT_NODE_ID) {
            return path;
        } else {
            return prependParent(path, parentNode);
        }
    }

    /**
     * Checks if any of a node's child nodes has same name and type as the node being validated.
     *
     * @param nodeToCheck       The {@link Node} to validate
     * @param parentsChildNodes List of {@link Node} against which to run the comparison.
     * @return <code>true</code> if name and type of <code>nodeToCheck</code> does not clash with
     * any {@link Node} in <code>parentsChildNodes</code>, otherwise <code>false</code>.
     */
    private boolean isNodeNameValid(Node nodeToCheck, List<Node> parentsChildNodes) {
        for (Node node : parentsChildNodes) {
            if (node.getName().equals(nodeToCheck.getName()) &&
                    node.getNodeType().equals(nodeToCheck.getNodeType())) {
                return false;
            }
        }
        return true;
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
            LOG.info("Move/copy not allowed: source node(s) list contains snapshot or root node.");
            return false;
        }
        // Check if selection contains save set node.
        Optional<Node> saveSetNode = nodes.stream()
                .filter(node -> node.getNodeType().equals(NodeType.CONFIGURATION)).findFirst();
        // Save set nodes may not be moved/copied to root node.
        if(saveSetNode.isPresent() && targetNode.getUniqueId().equals(rootNode.getUniqueId())){
            LOG.info("Move/copy of save set node(s) to root node not allowed.");
            return false;
        }
        if (nodes.size() > 1) {
            // Check that all elements are of same type and have same parent.
            NodeType firstElementType = nodes.get(0).getNodeType();
            Node parentNodeOfFirst = getParentNode(nodes.get(0).getUniqueId());
            for (int i = 1; i < nodes.size(); i++) {
                if (!nodes.get(i).getNodeType().equals(firstElementType)) {
                    LOG.info("Move not allowed: all source nodes must be of same type.");
                    return false;
                }
                Node parent = getParentNode(nodes.get(i).getUniqueId());
                if (!parent.getUniqueId().equals(parentNodeOfFirst.getUniqueId())) {
                    LOG.info("Move not allowed: all source nodes must have same parent node.");
                    return false;
                }
            }
        }
        // Check if there is any name/type clash
        List<Node> parentsChildNodes = getChildNodes(targetNode.getUniqueId());
        for (Node node : nodes) {
            if (!isNodeNameValid(node, parentsChildNodes)) {
                LOG.info("Move/copy not allowed: target node already contains child node with same name and type: " + node.getName());
                return false;
            }
        }

        // Check if the target node is a descendant (at any depth) of any of the source nodes.
        // In a drag-n-drop case this would indicate that user has dropped the selection on a node contained
        // in the selection, or a node in a sub-tree of an element in the selection.
        List<Integer> sourceNodeIds = nodes.stream().map(Node::getId).collect(Collectors.toList());
        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("ancestors", sourceNodeIds)
                        .addValue("descendant", targetNode.getId());
        int count = descendantCheckParameterJdbcTemplate.queryForObject(
                "select count(*) from node_closure where descendant=(:descendant) and ancestor in (:ancestors)",
                params, Integer.class);
        return count == 0;
    }

    @Override
    public Node copyNodes(List<String> nodeIds, String targetNodeId, String userName) {
        // First get the target node
        Node targetNode = getNode(targetNodeId);
        if (targetNode == null) {
            throw new NodeNotFoundException("Target node " + targetNodeId + " not found");
        }
        if (!targetNode.getNodeType().equals(NodeType.FOLDER)) {
            throw new IllegalArgumentException("Target node " + targetNodeId + " is not a folder node");
        }
        MapSqlParameterSource params =
                new MapSqlParameterSource().addValue("nodeIds", nodeIds);
        List<Node> sourceNodes = nodeListParameterJdbcTemplate.query("select * from node where unique_id in (:nodeIds) ", params, new NodeRowMapper());
        // Size of return nodes must match size of node ids.
        if (sourceNodes.size() != nodeIds.size()) {
            throw new IllegalArgumentException("At least one unique node id not found.");
        }

        // Check that all source nodes can be copied to the target node
        if (!isMoveOrCopyAllowed(sourceNodes, targetNode)) {
            throw new IllegalArgumentException("Prerequisites for copying source node(s) not met.");
        }

        sourceNodes.forEach(sourceNode -> copyNode(sourceNode, targetNode, userName));

        return targetNode;
    }

    /**
     *
     * @param sourceNode The (existing) source {@link Node} to be copied.
     * @param targetNode The parent (target) {@link Node} for the copy.
     * @param userName User's identity.
     */
    private void copyNode(Node sourceNode, Node targetNode, String userName) {

        Timestamp created = new Timestamp(sourceNode.getCreated().getTime());
        Timestamp lastModified = new Timestamp(sourceNode.getLastModified().getTime());
        Node newSourceNode = createNodeInternal(targetNode.getUniqueId(), sourceNode, created, lastModified);
        Map<String, String> properties = getProperties(sourceNode.getId());
        properties.put("username", userName);
        newSourceNode.setProperties(properties);
        updateProperties(newSourceNode.getId(), properties);

        if (sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
            List<ConfigPv> configPvs = getConfigPvs(sourceNode.getUniqueId());
            updateConfiguration(newSourceNode, configPvs);
            List<Node> childNodes = getChildNodes(sourceNode.getUniqueId());
            childNodes.forEach(childNode -> {
                Map<String, String> snapshotProperties = getProperties(childNode.getId());
                List<SnapshotItem> snapshotItems = getSnapshotItems(childNode.getUniqueId());
                Timestamp snapshotCreated = Timestamp.from(childNode.getCreated().toInstant());
                Timestamp snapshotLastModified = Timestamp.from(childNode.getLastModified().toInstant());
                Node newSnapshotNode =
                        saveSnapshotInternal(newSourceNode.getUniqueId(),
                                snapshotItems,
                                childNode.getName(),
                                snapshotProperties.get("comment"),
                                userName,
                                snapshotCreated,
                                snapshotLastModified);
                newSnapshotNode.setProperties(snapshotProperties);
                List<Tag> tags = childNode.getTags();
                tags.forEach(tag -> tag.setSnapshotId(newSnapshotNode.getUniqueId()));
                newSnapshotNode.setTags(tags);
                updateProperties(newSnapshotNode.getId(), newSnapshotNode.getProperties());
                updateTags(newSnapshotNode.getUniqueId(), newSnapshotNode.getTags());
            });
        } else if (sourceNode.getNodeType().equals(NodeType.FOLDER)) {
            List<Node> childNodes = getChildNodes(sourceNode.getUniqueId());
            childNodes.forEach(childNode -> copyNode(childNode, newSourceNode, userName));
        }
    }
}
