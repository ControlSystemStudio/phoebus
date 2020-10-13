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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author georgweiss
 * Created 11 Mar 2019
 */
public class NodeJdbcDAO implements NodeDAO {

	@Autowired
	private SimpleJdbcInsert configurationEntryInsert;

	@Autowired
	private SimpleJdbcInsert configurationEntryRelationInsert;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SimpleJdbcInsert nodeInsert;

	@Autowired
	private SimpleJdbcInsert pvInsert;

	@Autowired
	private SimpleJdbcInsert snapshotPvInsert;

	private static final int NO_ID = -1;


	private Node getParentNode(int nodeId) {

		// Root folder is its own parent
		if (nodeId == Node.ROOT_NODE_ID) {
			return getNode(Node.ROOT_NODE_ID);
		}

		try {
			int parentNodeId = jdbcTemplate.queryForObject(
					"select ancestor from node_closure where descendant=? and depth=1", new Object[] { nodeId },
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
			nodeId = jdbcTemplate.queryForObject("select id from node where unique_id=?", new Object[] {uniqueNodeId}, Integer.class);
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
	 *         {@link Node} object.
	 */
	private Node getNode(int nodeId) {

		try {
			Node node = jdbcTemplate.queryForObject("select * from node where id=?", new Object[] { nodeId },
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
	 *         {@link Node} object.
	 */
	@Transactional
	@Override
	public Node getNode(String uniqueNodeId) {

		int nodeId;
		try {
			nodeId = jdbcTemplate.queryForObject("select id from node where unique_id=?", new Object[] {uniqueNodeId}, Integer.class);
			return getNode(nodeId);
		} catch (DataAccessException e) {
			return null;
		}
	}

	@Override
	@Transactional
	public List<Node> getChildNodes(String uniqueNodeId) {

		Node parentNode = getNode(uniqueNodeId);
		// Node may have been deleted -> client's data is out-of-sync with server's data
		if(parentNode == null) {
			throw new NodeNotFoundException(String.format("Cannot get child nodes of unique id %s as it does not exist", uniqueNodeId));
		}
		return getChildNodes(parentNode.getId());
	}

	private List<Node> getChildNodes(int nodeId){
		List<Node> childNodes = jdbcTemplate.query("select n.* from node as n join node_closure as nc on n.id=nc.descendant where "
				+ "nc.ancestor=? and nc.depth=1", new Object[] { nodeId }, new NodeRowMapper());
		childNodes.forEach(childNode -> {
			childNode.setProperties(getProperties(childNode.getId()));
			childNode.setTags(getTags(childNode.getUniqueId()));
		});

		return childNodes;

	}

	@Transactional
	@Override
	public void deleteNode(String uniqueNodeId) {

		if(uniqueNodeId == null || uniqueNodeId.isEmpty()) {
			throw new IllegalArgumentException(String.format("Cannot delete node, invalid unique node id specified:%s", uniqueNodeId));
		}

		Node nodeToDelete = getNode(uniqueNodeId);
		if(nodeToDelete == null) {
			throw new NodeNotFoundException(String.format("Node with id=%s not found", uniqueNodeId));
		}
		if (nodeToDelete.getId() == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Root node cannot be deleted");
		}

		Node parent = getParentNode(nodeToDelete.getId());

		if(parent == null) {
			throw new IllegalArgumentException(String.format("Parent node of node id=%d cannot be found. Should not happen!", nodeToDelete.getId()));
		}

		if (nodeToDelete.getNodeType().equals(NodeType.CONFIGURATION)) {
			List<Integer> configPvIds = jdbcTemplate.queryForList(
					"select config_pv_id from config_pv_relation where config_id=?", new Object[] { nodeToDelete.getId() },
					Integer.class);
			deleteOrphanedPVs(configPvIds);
			for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
				deleteNode(node.getUniqueId());
			}
		} else if (nodeToDelete.getNodeType().equals(NodeType.FOLDER)) {
			for (Node node : getChildNodes(nodeToDelete.getUniqueId())) {
				deleteNode(node.getUniqueId());
			}
		} else if(nodeToDelete.getNodeType().equals(NodeType.SNAPSHOT)) {
			jdbcTemplate.update("delete from snapshot_node_pv where snapshot_node_id=?", nodeToDelete.getId());
		}

		jdbcTemplate.update("delete from node where unique_id=?", uniqueNodeId);



		// Update last modified date of the parent node
		jdbcTemplate.update("update node set last_modified=? where id=?", Timestamp.from(Instant.now()), parent.getId());
	}

	/**
	 * Creates a new node in the tree. An {@link IllegalArgumentException} is thrown if:
	 * <ul>
	 * <li>The <code>parentsUniqueId</code> argument is null or identifies a non-existing node</li>
	 * <li>If the node's type is {@link NodeType#FOLDER} or {@link NodeType#CONFIGURATION} and the parent node is not of type {@link NodeType#FOLDER}.</li>
	 * <li>If the node's type is {@link NodeType#SNAPSHOT} and the parent node is not of type {@link NodeType#CONFIGURATION}.</li>
	 * <li>If the parent node already contains a child node of the same type and name.</li>
	 * </ul>
	 */
	@Transactional
	@Override
	public Node createNode(String parentsUniqueId, final Node node) {
		if(parentsUniqueId == null) {
			throw new IllegalArgumentException("Cannot create node, parent unique id not specified.");
		}

		Node parentNode = getNode(parentsUniqueId);

		if (parentNode == null) {
			throw new IllegalArgumentException(
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

		Timestamp now = Timestamp.from(Instant.now());
		String uniqueId = node.getUniqueId() == null ? UUID.randomUUID().toString() : node.getUniqueId();

		Map<String, Object> params = new HashMap<>(2);
		params.put("type", node.getNodeType().toString());
		params.put("created", now);
		params.put("last_modified", now);
		params.put("unique_id", uniqueId);
		params.put("name",
				node.getNodeType().equals(NodeType.SNAPSHOT)
						? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now)
						: node.getName());
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
	@Transactional
	public List<ConfigPv> getConfigPvs(String configUniqueNodeId){

		return jdbcTemplate.query("select cp.id, pv1.name as name, pv2.name as readback_name, cp.readonly from config_pv as cp " +
						"join config_pv_relation as cpr on cp.id=cpr.config_pv_id " +
						"join node as n on n.id=cpr.config_id " +
						"left join pv pv1 on cp.pv_id=pv1.id " +
						"left join pv pv2 on cp.readback_pv_id=pv2.id " +
						"where cpr.config_id=(select id from node where unique_id=?)",
				new Object[] { configUniqueNodeId }, new ConfigPvRowMapper());
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

	@Override
	public Node getRootNode() {
		return getNode(Node.ROOT_NODE_ID);
	}


	private void saveConfigPv(int nodeId, ConfigPv configPv) {

		List<Integer> list;

		if(configPv.getReadbackPvName() == null) {
			list = jdbcTemplate.queryForList("select cp.id from config_pv as cp " +
							"left join pv pv1 on cp.pv_id=pv1.id " +
							"left join pv pv2 on cp.readback_pv_id=pv2.id " +
							"where pv1.name=? and pv2.name is NULL and cp.readonly=?",
					new Object[] { configPv.getPvName(), configPv.isReadOnly() }, Integer.class);
		}
		else {
			list = jdbcTemplate.queryForList("select cp.id from config_pv as cp " +
							"left join pv pv1 on cp.pv_id=pv1.id " +
							"left join pv pv2 on cp.readback_pv_id=pv2.id " +
							"where pv1.name=? and pv2.name=? and cp.readonly=?",
					new Object[] { configPv.getPvName(), configPv.getReadbackPvName(), configPv.isReadOnly() }, Integer.class);
		}


		int configPvId = 0;

		if (!list.isEmpty()) {
			configPvId = list.get(0);
		} else {
			Map<String, Object> params = new HashMap<>(4);
			params.put("pv_id", getOrInsertPvName(configPv.getPvName().trim()));
			int readbackPvId = getOrInsertPvName(configPv.getReadbackPvName());
			if(readbackPvId != NO_ID) {
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

	private int getOrInsertPvName(String pvName){
		if(pvName == null || pvName.isEmpty()){
			return NO_ID;
		}
		int pvId = getPvId(pvName);
		if(pvId == NO_ID){
			pvId = addPvName(pvName);
		}
		return pvId;
	}

	private int getPvId(String pvName){
		try {
			return jdbcTemplate.queryForObject("select id from pv where name=?",
					new Object[] { pvName }, Integer.class);
		} catch (DataAccessException e) {
			return NO_ID;
		}
	}

	private int addPvName(String pvName){
		Timestamp time = new Timestamp(System.currentTimeMillis());
		Map<String, Object> params = new HashMap<>(3);
		params.put("name", pvName);
		params.put("created", time);
		params.put("last_modified", time);

		return pvInsert.executeAndReturnKey(params).intValue();
	}

	private void updatePvName(String oldName, String newName){
		int pvId = getPvId(oldName);
		if(pvId == NO_ID){
			throw new IllegalArgumentException(String.format("Cannot update PV name %s as no such PV exists", oldName));
		}
		jdbcTemplate.update("update pv set name=? where id=?", new Object[]{newName, pvId});
	}


	private void deleteOrphanedPVs(Collection<Integer> pvList) {
		for (Integer pvId : pvList) {
			int count = jdbcTemplate.queryForObject("select count(*) from config_pv_relation where config_pv_id=?",
					new Object[] { pvId }, Integer.class);

			if (count == 0) {
				jdbcTemplate.update("delete from config_pv where id=?", pvId);
			}
		}
	}

	@Override
	@Transactional
	public Node moveNode(String uniqueNodeId, String targetUniqueNodeId, String userName) {

		Node sourceNode = getNode(uniqueNodeId);

		if (sourceNode == null) {
			throw new NodeNotFoundException(String.format("Source node with unqiue id=%s not found", uniqueNodeId));
		}

		if(!sourceNode.getNodeType().equals(NodeType.FOLDER) && !sourceNode.getNodeType().equals(NodeType.CONFIGURATION)) {
			throw new IllegalArgumentException(String.format("Moving node of type %s not supported", NodeType.SNAPSHOT.toString()));
		}

		Node targetNode = getNode(targetUniqueNodeId);
		if(targetNode == null || !targetNode.getNodeType().equals(NodeType.FOLDER)) {
			throw new IllegalArgumentException("Target node does not exist or is not a folder");
		}

		List<Node> parentsChildNodes = getChildNodes(targetNode.getId());
		if (!isNodeNameValid(sourceNode, parentsChildNodes)) {
			throw new IllegalArgumentException("Node of same name and type already exists in target node.");
		}

		jdbcTemplate.update("delete from node_closure where "
						+ "descendant in (select descendant from node_closure where ancestor=?) "
						+ "and ancestor in (select ancestor from node_closure where descendant=? and ancestor != descendant)",
				sourceNode.getId(), sourceNode.getId());

		jdbcTemplate.update("insert into node_closure (ancestor, descendant, depth) "
				+ "select supertree.ancestor, subtree.descendant, supertree.depth + subtree.depth + 1 AS depth "
				+ "from node_closure as supertree " + "cross join node_closure as subtree "
				+ "where supertree.descendant=? and subtree.ancestor=?", targetNode.getId(), sourceNode.getId());

		// Update the last modified date of the source and target folder.
		jdbcTemplate.update("update node set last_modified=?, username=? where id=? or id=?",
				Timestamp.from(Instant.now()), userName, targetNode.getId(), sourceNode.getId());

		return getNode(targetNode.getId());
	}

	@Override
	@Transactional
	public Node updateConfiguration(Node configToUpdate, List<ConfigPv> updatedConfigPvList) {

		Node configNode = getNode(configToUpdate.getUniqueId());

		if(configNode == null || !configNode.getNodeType().equals(NodeType.CONFIGURATION)) {
			throw new IllegalArgumentException(String.format("Config node with unique id=%s not found or is wrong type", configToUpdate.getUniqueId()));
		}

		List<ConfigPv> existingConfigPvList = getConfigPvs(configToUpdate.getUniqueId());

		Collection<ConfigPv> pvsToRemove = CollectionUtils.removeAll(existingConfigPvList,
				updatedConfigPvList);
		Collection<Integer> pvIdsToRemove = CollectionUtils.collect(pvsToRemove, ConfigPv::getId);

		// Remove PVs from relation table
		pvIdsToRemove.stream().forEach(id -> jdbcTemplate.update(
				"delete from config_pv_relation where config_id=? and config_pv_id=?",
				configNode.getId(), id));

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

	@Transactional
	@Override
	public Node updateNode(Node nodeToUpdate, boolean customTimeForMigration) {

		Node persistedNode = getNode(nodeToUpdate.getUniqueId());

		if(persistedNode == null) {
			throw new IllegalArgumentException(String.format("Node with unique id=%s not found", nodeToUpdate.getUniqueId()));
		}

		if (persistedNode.getId() == Node.ROOT_NODE_ID) {
			throw new IllegalArgumentException("Cannot update root node");
		}

		if(!persistedNode.getNodeType().equals(nodeToUpdate.getNodeType())) {
			throw new IllegalArgumentException("Changing node type is not supported");
		}

		Node parentNode = getParentNode(persistedNode.getId());

		if(parentNode == null) {
			throw new IllegalArgumentException(
					String.format("Cannot update node id=%d as its parent node is not found. Should not happen!", persistedNode.getId()));
		}

		List<Node> parentsChildNodes = getChildNodes(parentNode.getId());

		if(!nodeToUpdate.getName().equals(persistedNode.getName()) && !isNodeNameValid(nodeToUpdate, parentsChildNodes)) {
			throw new IllegalArgumentException(String.format("A node with same type and name (%s) already exists in the same folder", nodeToUpdate.getName()));
		}

		if (customTimeForMigration) {
			jdbcTemplate.update("update node set name=?, created=?, last_modified=?, username=? where id=?",
					nodeToUpdate.getName(), nodeToUpdate.getCreated(), Timestamp.from(Instant.now()), nodeToUpdate.getUserName(), persistedNode.getId());
		} else {
			jdbcTemplate.update("update node set name=?, last_modified=?, username=? where id=?",
					nodeToUpdate.getName(), Timestamp.from(Instant.now()), nodeToUpdate.getUserName(), persistedNode.getId());
		}

		updateProperties(persistedNode.getId(), nodeToUpdate.getProperties());
		updateTags(persistedNode.getUniqueId(), nodeToUpdate.getTags());

		return getNode(persistedNode.getId());
	}


	@Transactional
	@Override
	public Node saveSnapshot(String parentsUniqueId, List<SnapshotItem> snapshotItems, String snapshotName, String comment, String userName) {
		Node snapshotNode = createNode(parentsUniqueId, Node.builder()
				.name(snapshotName)
				.nodeType(NodeType.SNAPSHOT)
				.build());

		Map<String, Object> params = new HashMap<>(6);
		params.put("snapshot_node_id", snapshotNode.getId());

		for (SnapshotItem snapshotItem : snapshotItems) {
			params.put("config_pv_id", snapshotItem.getConfigPv().getId());
			// Should not happen, but if the snapshot value has not been set, continue...
			if(snapshotItem.getValue() == null){
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
			snapshotPvInsert.execute(params);
		}

		jdbcTemplate.update("update node set name=?, username=?, last_modified=? where unique_id=?", snapshotName, userName, Timestamp.from(Instant.now()), snapshotNode.getUniqueId());
		insertOrUpdateProperty(snapshotNode.getId(), new AbstractMap.SimpleEntry<String, String>("comment", comment));

		return getSnapshot(snapshotNode.getUniqueId());
	}

	/**
	 * Retrieves saved snapshot nodes.
	 * @param uniqueNodeId The unique node id of the snapshot
	 */
	@Override
	public List<Node> getSnapshots(String uniqueNodeId) {

		List<Node> snapshots = jdbcTemplate.query("select n.*, nc.ancestor from node as n " +
						"join node_closure as nc on n.id=nc.descendant " +
						"where n.username is not NULL and nc.ancestor=(select id from node where unique_id=?) and nc.depth=1", new Object[] { uniqueNodeId },
				new NodeRowMapper());

		for(Node snapshot : snapshots) {
			snapshot.setProperties(getProperties(snapshot.getId()));
			snapshot.setTags(getTags(snapshot.getUniqueId()));
		}

		return snapshots;

	}

	@Override
	public List<SnapshotItem> getSnapshotItems(String snapshotUniqueId){

		List<SnapshotItem> snapshotItems = jdbcTemplate.query("select snp.*, pv1.name, pv2.name as readback_name, cp.readonly, cp.id as id from snapshot_node_pv as snp " +
			"join config_pv as cp on snp.config_pv_id=cp.id " +
			"left join pv pv1 on cp.pv_id=pv1.id " +
			"left join pv pv2 on cp.readback_pv_id=pv2.id " +
				"where snapshot_node_id=(select id from node where unique_id=?)",
				new Object[] {snapshotUniqueId},
				new SnapshotItemRowMapper());

		return snapshotItems;
	}

	@Override
	public Node getSnapshot(String uniqueNodeId) {

		Node snapshotNode = getNode(uniqueNodeId);
		if(snapshotNode == null || !snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
			return null;
		}

		return snapshotNode;
	}

	/**
	 * Deletes all properties for the specified node id, and then inserts the properties
	 * as specified in the <code>properties</code> parameter. The client hence must make sure
	 * that any existing properties that should not be deleted are present in the map.
	 *
	 * Keys and values of the map must all be non-null and non-empty in order to
	 * be inserted.
	 *
	 * Specifying a <code>null<code> map of properties will delete all existing.
	 * @param nodeId The id of the {@link Node}
	 * @param properties Map of properties to insert.
	 */
	private void updateProperties(int nodeId, Map<String, String> properties) {

		jdbcTemplate.update("delete from property where node_id=?", nodeId);

		if(properties == null || properties.isEmpty()) {
			return;
		}

		for(Map.Entry<String, String> entry : properties.entrySet()) {
			insertOrUpdateProperty(nodeId, entry);
		}
	}

	/**
	 * This method is intentionally not using "on duplicate key" insert since that
	 * is tricky to set up for the H2 database used in unit testing.
	 * @param nodeId The node id identifying the owning node.
	 * @param entry The map entry (including key and value).
	 */
	private void insertOrUpdateProperty(int nodeId, Map.Entry<String, String> entry) {
		if(entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null || entry.getValue().isEmpty()) {
			return; // Ignore rather than throwing exception in order to not break callee's loop.
		}
		// Disallow setting the "root" property. It is set by Flyway.
		if("root".equals(entry.getKey())) {
			return;
		}
		// First check if there is an existing property for the combination of node id and key
		int numberOfHits = jdbcTemplate.queryForObject("select count(*) from property where node_id=? and property_name=?",
				new Object[] {nodeId, entry.getKey()},
				Integer.class);
		if(numberOfHits == 0) {
			jdbcTemplate.update("insert into property values(?, ?, ?)", nodeId, entry.getKey(), entry.getValue());
		}
		else {
			jdbcTemplate.update("update property set value=? where node_id=? and property_name=?", entry.getValue(), nodeId, entry.getKey());
		}
	}

	private Map<String, String> getProperties(int nodeId){
		return jdbcTemplate.query("select * from property where node_id=?",
				new Object[] {nodeId}, new PropertiesRowMapper());
	}

	public List<Tag> getAllTags() {
		return jdbcTemplate.query("select * from tag", new TagsRowMapper());
	}

	public List<Tag> getTags(String uniqueSnapshotId) {
		return jdbcTemplate.query("select * from tag where snapshot_id=?", new Object[]{uniqueSnapshotId}, new TagsRowMapper());
	}

	private void updateTags(String uniqueSnapshotId, List<Tag> tags) {
		jdbcTemplate.update("delete from tag where snapshot_id=?", uniqueSnapshotId);

		if(tags == null || tags.isEmpty()) {
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


	public List<Node> getFromPath(String path){
		if(path == null || !path.startsWith("/") || path.endsWith("/")){
			return null;
		}
		String[] splitPath = path.split("/");
		Node parentOfLastPathElement = findParentFromPathElements(getRootNode(), splitPath, 1);
		if(parentOfLastPathElement == null){ // Path is "invalid"
			return null;
		}
		List<Node> childNodes = getChildNodes(parentOfLastPathElement.getUniqueId());
		List<Node> foundNodes = childNodes.stream()
				.filter(node -> node.getName().equals(splitPath[splitPath.length - 1])).collect(Collectors.toList());
		if(foundNodes.isEmpty()){
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
	 * @param parentNode The parent node from which to continue search.
	 * @param splitPath An array of path elements assumed to be ordered from top level
	 *                  folder and downwards.
	 * @param index The index in the <code>splitPath</code> to match node names.
	 * @return The {@link Node} corresponding to the last path element, or <code>null</code>.
	 */
	protected Node findParentFromPathElements(Node parentNode, String[] splitPath, int index){
		if(index == splitPath.length - 1) {
			return parentNode;
		}
		String nextPathElement = splitPath[index];
		List<Node> childNodes = getChildNodes(parentNode.getUniqueId());
		for(Node node : childNodes){
			if(node.getName().equals(nextPathElement) && node.getNodeType().equals(NodeType.FOLDER)){
				return findParentFromPathElements(node, splitPath, ++index);
			}
		}
		return null;
	}

	public String getFullPath(String uniqueNodeId){
		if(uniqueNodeId == null || uniqueNodeId.isEmpty()){
			return null;
		}
		Node node = getNode(uniqueNodeId);
		if(node == null){
			return null;
		}
		else if(node.getId() == Node.ROOT_NODE_ID){
			return "/";
		}

		return prependParent("", node);
	}

	/**
	 * Prepends the name of the parent node and - if the parent
	 * node is not the root node - recursively calls this method to continue up
	 * the hierarchy until the root node is reached.
	 * @param path Non-null path to be prepended with the name of the parent.
	 * @param node Non-null {@link Node} object as retrieved from the persistence layer.
	 * @return The name of the specified node, prepended by its parent's name.
	 */
	protected String prependParent(String path, Node node){
		path = "/" + node.getName() + path;
		Node parentNode = getParentNode(node.getUniqueId());
		if(parentNode.getId() == Node.ROOT_NODE_ID){
			return path;
		}
		else{
			return prependParent(path, parentNode);
		}
	}
}
