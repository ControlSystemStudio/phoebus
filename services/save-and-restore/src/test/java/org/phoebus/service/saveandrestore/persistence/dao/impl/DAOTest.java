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

package org.phoebus.service.saveandrestore.persistence.dao.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VInt;
import org.flywaydb.test.FlywayTestExecutionListener;
import org.flywaydb.test.annotation.FlywayTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

//import org.phoebus.applications.saveandrestore.model.Config;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
//import org.phoebus.applications.saveandrestore.model.Folder;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
//import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.persistence.config.PersistenceConfiguration;
import org.phoebus.service.saveandrestore.persistence.config.PersistenceTestConfig;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableConfigurationProperties
@ContextHierarchy({ @ContextConfiguration(classes = { PersistenceConfiguration.class, PersistenceTestConfig.class }) })
@TestPropertySource(properties = { "dbengine = h2" })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class })
public class DAOTest {

	@Autowired
	private NodeJdbcDAO nodeDAO;

	private Alarm alarm;
	private Time time;
	private Display display;

	@Before
	public void init() {
		time = Time.of(Instant.now());
		alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
		display = Display.none();
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testCreateConfigNoParentFound() {

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).build();

		// The parent node does not exist in the database, so this throws an exception
		nodeDAO.createNode(UUID.randomUUID().toString(), config);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewNode() {

		Node root = nodeDAO.getRootNode();

		Date lastModified = root.getLastModified();

		Map<String, String> props = new HashMap<>();
		props.put("a", "b");

		Node folder = Node.builder().name("SomeFolder").userName("username").properties(props).build();

		Node newNode = nodeDAO.createNode(root.getUniqueId(), folder);

		root = nodeDAO.getRootNode();

		assertNotNull(newNode);
		assertEquals(NodeType.FOLDER, newNode.getNodeType());
		assertNotNull(newNode.getProperty("a"));
		assertNull(newNode.getProperty("x"));

		// Check that the parent folder's last modified date is updated
		assertTrue(root.getLastModified().getTime() > lastModified.getTime());
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderWithDuplicateName() {

		Node rootNode = nodeDAO.getRootNode();

		Node node = Node.builder().name("SomeFolder").build();

		// Create a new folder
		nodeDAO.createNode(rootNode.getUniqueId(), node);

		// Try to create a new folder with the same name in the same parent directory
		nodeDAO.createNode(rootNode.getUniqueId(), node);

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderNoDuplicateName() {

		Node rootNode = nodeDAO.getRootNode();

		Node folder1 = Node.builder().name("Folder 1").build();

		Node folder2 = Node.builder().name("Folder 2").build();

		// Create a new folder
		assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder1));

		// Try to create a new folder with a different name in the same parent directory
		assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder2));

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderParentIsConfiguration() {

		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").build();

		Node newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);

		Node folder1 = Node.builder().name("Folder 1").build();

		nodeDAO.createNode(newConfig.getUniqueId(), folder1);

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNewFolderParentDoesNotExist() {
		Node folder1 = Node.builder().name("Folder 1").build();
		nodeDAO.createNode(null, folder1);

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfig() {

		Node rootNode = nodeDAO.getRootNode();

		ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").userName("username").build();

		Node newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(newConfig, Arrays.asList(configPv));

		int configPvId = nodeDAO.getConfigPvs(newConfig.getUniqueId()).get(0).getId();

		config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 2").build();

		newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);
		assertEquals(NodeType.CONFIGURATION, newConfig.getNodeType());

		nodeDAO.updateConfiguration(newConfig, Arrays.asList(configPv));
		// Verify that a new ConfigPv has NOT been created

		assertEquals(configPvId, nodeDAO.getConfigPvs(newConfig.getUniqueId()).get(0).getId());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNewConfigNoConfigPvs() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").build();

		Node newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);

		assertTrue(nodeDAO.getConfigPvs(newConfig.getUniqueId()).isEmpty());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfiguration() {

		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		Node snapshot = nodeDAO.createNode(config.getUniqueId(), Node.builder().nodeType(NodeType.SNAPSHOT).build());

		nodeDAO.deleteNode(config.getUniqueId());

		assertNull(nodeDAO.getNode(config.getUniqueId()));
		assertNull(nodeDAO.getNode(snapshot.getUniqueId()));
	}

	@Test
	public void testDeleteNodeInvalidNodeId() {
		try {
			nodeDAO.deleteNode(null);
			fail("IllegalArgumentException expected here");
		} catch (Exception e) {
		}

		try {
			nodeDAO.deleteNode("");
			fail("IllegalArgumentException expected here");
		} catch (Exception e) {
		}
	}



	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationAndPvs() {

		Node rootNode = nodeDAO.getRootNode();

		ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).build();

		config.setName("My config");

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		config = nodeDAO.updateConfiguration(config, Arrays.asList(configPv));

		nodeDAO.deleteNode(config.getUniqueId());

		// TODO: Check that PVs have been deleted. Imposed by foreign key constraint on
		// config_pv table

	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteNonExistingFolder() {

		nodeDAO.deleteNode("a");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteNodeNullId() {

		nodeDAO.deleteNode(null);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteFolder() {

		Node rootNode = nodeDAO.getRootNode();

		Node root = nodeDAO.getNode(rootNode.getUniqueId());

		Date rootLastModified = root.getLastModified();

		Node folder1 = Node.builder().name("SomeFolder").build();

		folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folder1);

		Node folder2 = Node.builder().name("SomeFolder").build();

		folder2 = nodeDAO.createNode(folder1.getUniqueId(), folder2);

		Node config = nodeDAO.createNode(folder1.getUniqueId(),
				Node.builder().nodeType(NodeType.CONFIGURATION).name("Config").build());

		nodeDAO.deleteNode(folder1.getUniqueId());

		root = nodeDAO.getNode(rootNode.getUniqueId());

		assertTrue(root.getLastModified().getTime() > rootLastModified.getTime());

		assertNull(nodeDAO.getNode(config.getUniqueId()));

		assertNull(nodeDAO.getNode(folder2.getUniqueId()));

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteConfigurationLeaveReferencedPVs() {

		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.getNode(rootNode.getUniqueId());

		ConfigPv configPv1 = ConfigPv.builder().pvName("pvName").build();

		ConfigPv configPv2 = ConfigPv.builder().pvName("pvName2").build();

		Node config1 = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").build();

		config1 = nodeDAO.createNode(rootNode.getUniqueId(), config1);
		nodeDAO.updateConfiguration(config1, Arrays.asList(configPv1, configPv2));

		Node config2 = Node.builder().name("My config 2").nodeType(NodeType.CONFIGURATION).build();

		config2 = nodeDAO.createNode(rootNode.getUniqueId(), config2);
		nodeDAO.updateConfiguration(config2, Arrays.asList(configPv2));

		nodeDAO.deleteNode(config1.getUniqueId());

		assertEquals(1, nodeDAO.getConfigPvs(config2.getUniqueId()).size());

	}

	@Test
	@FlywayTest(invokeCleanDB = false)
	public void testGetNodeAsConfig() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();

		Node newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);

		Node configFromDB = nodeDAO.getNode(newConfig.getUniqueId());

		assertEquals(newConfig, configFromDB);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetConfigForSnapshot() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(ConfigPv.builder().pvName("whatever").build()));

		List<ConfigPv> configPvs = nodeDAO.getConfigPvs(config.getUniqueId());
		SnapshotItem item1 = SnapshotItem.builder().configPv(configPvs.get(0))
				.value(VDouble.of(7.7, alarm, time, display)).build();

		Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshot name", "user", "comment");

		config = nodeDAO.getParentNode(newSnapshot.getUniqueId());

		assertNotNull(config);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetParentNodeDataAccessException() {
		assertNull(nodeDAO.getParentNode("nonexisting"));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testTakeSnapshot() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(ConfigPv.builder().pvName("whatever").readbackPvName("readback_whatever").build()));

		SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(8.8, alarm, time, display))
				.build();

		Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshot name", "user", "comment");
		List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
		assertEquals(1, snapshotItems.size());
		assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
		assertEquals(8.8, ((VDouble) snapshotItems.get(0).getReadbackValue()).getValue().doubleValue(), 0.01);

		Node fullSnapshot = nodeDAO.getSnapshot(newSnapshot.getUniqueId());
		assertNotNull(fullSnapshot);
		snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
		assertEquals(1, snapshotItems.size());

		List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
		assertEquals(1, snapshots.size());

		nodeDAO.deleteNode(newSnapshot.getUniqueId());

		snapshots = nodeDAO.getSnapshots(config.getUniqueId());
		assertTrue(snapshots.isEmpty());
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testCommitSnapshotNameClashesWithExisting() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(ConfigPv.builder().pvName("whatever").build()));

		SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
				.build();

		nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshot name", "user", "comment");

		nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshot name 2", "user", "comment");

		nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshot name", "user", "comment");
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetSnapshotsNoSnapshots() {

		assertTrue(nodeDAO.getSnapshots("a").isEmpty());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetSnapshotItemsWithNullPvValues() {
		Node rootNode = nodeDAO.getRootNode();

		Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(ConfigPv.builder().pvName("whatever").build()));

		SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VDouble.of(7.7, alarm, time, display))
				.build();
		Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "name", "comment", "user");
		List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
		assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
		assertNull(snapshotItems.get(0).getReadbackValue());

		item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.build();
		newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "name2", "comment", "user");
		snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
		assertTrue(snapshotItems.isEmpty());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testSnapshotTag() {
		Node rootNode = nodeDAO.getRootNode();
		Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("testFolder").nodeType(NodeType.FOLDER).build());
		Node savesetNode = nodeDAO.createNode(folderNode.getUniqueId(), Node.builder().name("testSaveset").nodeType(NodeType.CONFIGURATION).build());
		Node snapshot = nodeDAO.createNode(savesetNode.getUniqueId(), Node.builder().name("testSnapshot").nodeType(NodeType.SNAPSHOT).build());

		Tag tag = Tag.builder().snapshotId(snapshot.getUniqueId()).name("tag1").comment("comment1").userName("testUser1").build();
		snapshot.addTag(tag);

		snapshot = nodeDAO.updateNode(snapshot, false);
		assertEquals(1, snapshot.getTags().size());
		assertEquals(snapshot.getUniqueId(), snapshot.getTags().get(0).getSnapshotId());
		assertEquals("tag1", snapshot.getTags().get(0).getName());
		assertEquals("comment1", snapshot.getTags().get(0).getComment());
		assertEquals("testUser1", snapshot.getTags().get(0).getUserName());

		// Adding the same named tag doesn't affect anything.
		tag = Tag.builder().name("tag1").comment("comment2").userName("testUser2").build();
		snapshot.addTag(tag);

		snapshot = nodeDAO.updateNode(snapshot, false);
		assertEquals(1, snapshot.getTags().size());
		assertEquals(snapshot.getUniqueId(), snapshot.getTags().get(0).getSnapshotId());
		assertEquals("tag1", snapshot.getTags().get(0).getName());
		assertEquals("comment1", snapshot.getTags().get(0).getComment());
		assertEquals("testUser1", snapshot.getTags().get(0).getUserName());

		snapshot.removeTag(tag);

		snapshot = nodeDAO.updateNode(snapshot, false);
		assertEquals(0, snapshot.getTags().size());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetRootsParent() {
		Node rootNode = nodeDAO.getRootNode();
		Node parent = nodeDAO.getParentNode(rootNode.getUniqueId());

		assertEquals(rootNode.getUniqueId(), parent.getUniqueId());
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalSource() {

		Node rootNode = nodeDAO.getRootNode();

		Node folderFromClient = Node.builder().name("SomeFolder").build();

		Node folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folderFromClient);

		nodeDAO.moveNode("a", folder1.getUniqueId(), "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeSourceWrongType() {

		Node rootNode = nodeDAO.getRootNode();
		Node folder = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("name").build());
		Node config = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("confg").nodeType(NodeType.CONFIGURATION).build());
		Node snapshot = nodeDAO.createNode(config.getUniqueId(), Node.builder().nodeType(NodeType.SNAPSHOT).build());

		nodeDAO.moveNode(snapshot.getUniqueId(), folder.getUniqueId(), "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeTargetNodeInvalidId() {

		Node rootNode = nodeDAO.getRootNode();
		Node config = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("confg").nodeType(NodeType.CONFIGURATION).build());
		Node snapshot = nodeDAO.createNode(config.getUniqueId(), Node.builder().nodeType(NodeType.SNAPSHOT).build());

		nodeDAO.moveNode(snapshot.getUniqueId(),"invalidNodeId", "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeIllegalTarget() {

		Node rootNode = nodeDAO.getRootNode();

		Node folderFromClient = Node.builder().name("SomeFolder").build();

		Node folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folderFromClient);

		nodeDAO.moveNode(folder1.getUniqueId(), "a", "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash1() {

		Node rootNode = nodeDAO.getRootNode();

		Node folder1 = Node.builder().name("SomeFolder").build();

		folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folder1);

		Node folder2 = Node.builder().name("SomeFolder").build();

		folder2 = nodeDAO.createNode(rootNode.getUniqueId(), folder2);

		nodeDAO.createNode(rootNode.getUniqueId(),
				Node.builder().nodeType(NodeType.CONFIGURATION).name("Config").build());

		nodeDAO.moveNode(folder2.getUniqueId(), rootNode.getUniqueId(), "username");
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNodeNameClash2() {

		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.createNode(rootNode.getUniqueId(),
				Node.builder().nodeType(NodeType.CONFIGURATION).name("Config").build());

		Node folder = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

		Node config2 = nodeDAO.createNode(folder.getUniqueId(),
				Node.builder().nodeType(NodeType.CONFIGURATION).name("Config").build());

		nodeDAO.moveNode(config2.getUniqueId(), rootNode.getUniqueId(), "username");

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetChildNodes() throws Exception {
		Node rootNode = nodeDAO.getRootNode();

		Map<String, String> props = new HashMap<>();
		props.put("a", "b");

		Node folder1 = Node.builder().name("SomeFolder").properties(props).build();

		// Create folder1 in the root folder
		folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folder1);

		List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());

		assertEquals("b", childNodes.get(0).getProperty("a"));
		assertTrue(nodeDAO.getChildNodes(folder1.getUniqueId()).isEmpty());
	}

	@Test(expected = NodeNotFoundException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testGetChildNodesOfNonExistingNode() throws Exception {
		nodeDAO.getChildNodes("non-existing");
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testMoveNode() throws Exception {

		Node rootNode = nodeDAO.getRootNode();

		Node folder1 = Node.builder().name("SomeFolder").build();

		// Create folder1 in the root folder
		folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folder1);

		// Root node has one child node
		assertEquals(1, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());

		Node folder2 = Node.builder().name("SomeFolder2").userName("dummy").build();

		// Create folder2 in the folder1 folder
		folder2 = nodeDAO.createNode(folder1.getUniqueId(), folder2);

		// folder1 has one child node
		assertEquals(1, nodeDAO.getChildNodes(folder1.getUniqueId()).size());

		// Create a configuration node in folder2
		nodeDAO.createNode(folder2.getUniqueId(),
				Node.builder().nodeType(NodeType.CONFIGURATION).name("Config").build());

		Date lastModifiedOfSource = folder1.getLastModified();
		Date lastModifiedOfTarget = rootNode.getLastModified();

		Thread.sleep(100);

		// Move folder2 from folder1 to root folder
		rootNode = nodeDAO.moveNode(folder2.getUniqueId(), rootNode.getUniqueId(), "username");

		folder2 = nodeDAO.getNode(folder2.getUniqueId());

		// After move the target's last_modified should have been updated
		assertTrue(rootNode.getLastModified().getTime() > lastModifiedOfTarget.getTime());

		// After move the source's last_modified should have been updated
		assertTrue(folder2.getLastModified().getTime() > lastModifiedOfSource.getTime());

		// After the move, the target's (root folder) username should have been updated
		assertEquals("username", rootNode.getUserName());

		// After move root node has two child nodes
		assertEquals(2, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());

		folder1 = nodeDAO.getNode(folder1.getUniqueId());
		// After move folder1 has no child nodes
		assertTrue(nodeDAO.getChildNodes(folder1.getUniqueId()).isEmpty());
	}



	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateConfig() throws Exception {

		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
		ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

		Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).name("name").build();

		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2));

		Date lastModified = config.getLastModified();

		SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VDouble.of(7.7, alarm, time, display)).build();

		SnapshotItem item2 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VInt.of(7, alarm, time, display)).build();

		Node snapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1, item2), "name", "comment", "user");

		List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(snapshot.getUniqueId());

		assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
		assertEquals(7, ((VInt) snapshotItems.get(1).getValue()).getValue().intValue());

		Node fullSnapshot = nodeDAO.getSnapshot(snapshot.getUniqueId());

		assertNotNull(fullSnapshot);

		List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
		assertFalse(snapshots.isEmpty());
		assertEquals(2, nodeDAO.getSnapshotItems(fullSnapshot.getUniqueId()).size());

		config.putProperty("description", "Updated description");

		lastModified = config.getLastModified();

		Thread.sleep(100);
		Node updatedConfig = nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2));

		assertNotEquals(lastModified, updatedConfig.getLastModified());

		// Verify that last modified time has been updated
		assertTrue(updatedConfig.getLastModified().getTime() > lastModified.getTime());

		// Verify the list of PVs
		assertEquals(2, nodeDAO.getConfigPvs(config.getUniqueId()).size());

		ConfigPv configPv3 = ConfigPv.builder().pvName("configPv3").build();

		updatedConfig = nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2, configPv3));

		// Verify the list of PVs
		assertEquals(3, nodeDAO.getConfigPvs(config.getUniqueId()).size());

	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpdateNonExistinConfiguration() {

		nodeDAO.updateConfiguration(Node.builder().id(-1).build(), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpdateNodethatIsNotConfiguration() {
		nodeDAO.updateConfiguration(Node.builder().id(Node.ROOT_NODE_ID).build(), null);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateNodeBadProperties() {
		Node rootNode = nodeDAO.getRootNode();

		Map<String, String> props = new HashMap<>();
		props.put("a", "b");

		Node childNode2 = nodeDAO.createNode(rootNode.getUniqueId(),
				Node.builder().name("b").userName("u1").properties(props).build());

		props = new HashMap<>();
		props.put("a", null);
		childNode2.setProperties(props);
		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertNull("b", childNode2.getProperty("a"));

		props = new HashMap<>();
		props.put("a", "b");
		childNode2.setProperties(props);
		nodeDAO.updateNode(childNode2, false);

		props = new HashMap<>();
		props.put("a", "");
		childNode2.setProperties(props);
		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertNull("b", childNode2.getProperty("a"));

		props = new HashMap<>();
		props.put("a", "b");
		childNode2.setProperties(props);
		nodeDAO.updateNode(childNode2, false);

		props = new HashMap<>();
		props.put(null, "c");
		childNode2.setProperties(props);
		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertNull("b", childNode2.getProperty("a"));

		props = new HashMap<>();
		props.put("a", "b");
		childNode2.setProperties(props);
		nodeDAO.updateNode(childNode2, false);

		props = new HashMap<>();
		props.put("", "c");
		childNode2.setProperties(props);
		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertNull("b", childNode2.getProperty("a"));

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testDeleteRootNode() {

		Node rootNode = nodeDAO.getRootNode();

		// Try to delete root folder (id = 0)
		nodeDAO.deleteNode(rootNode.getUniqueId());

	}

	@Test
	public void testNonExistingFolder() {
		assertNull(nodeDAO.getNode("a"));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFolderThatIsNotAFolder() {

		Node rootNode = nodeDAO.getRootNode();

		Node folder1 = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

		ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();

		Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

		config = nodeDAO.createNode(folder1.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(configPv1));

		assertNotEquals(NodeType.FOLDER, nodeDAO.getNode(config.getUniqueId()).getNodeType());

	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNoNameClash1() {

		Node rootNode = nodeDAO.getRootNode();

		Node n1 = Node.builder().id(1).name("n1").build();
		n1 = nodeDAO.createNode(rootNode.getUniqueId(), n1);
		Node n2 = Node.builder().nodeType(NodeType.CONFIGURATION).id(2).name("n1").build();
		n2 = nodeDAO.createNode(rootNode.getUniqueId(), n2);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testNoNameClash() {

		Node rootNode = nodeDAO.getRootNode();

		Node n1 = Node.builder().id(1).name("n1").build();
		n1 = nodeDAO.createNode(rootNode.getUniqueId(), n1);
		Node n2 = Node.builder().id(2).name("n2").build();
		n2 = nodeDAO.createNode(rootNode.getUniqueId(), n2);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testNameClash() {

		Node rootNode = nodeDAO.getRootNode();

		Node n1 = Node.builder().id(1).name("n1").build();
		n1 = nodeDAO.createNode(rootNode.getUniqueId(), n1);
		Node n2 = Node.builder().id(2).name("n1").build();
		n2 = nodeDAO.createNode(rootNode.getUniqueId(), n2);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateRootNode() {
		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.updateNode(rootNode, false);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateNonExistingNode() {
		nodeDAO.updateNode(Node.builder().uniqueId("invalidUniqueId").build(), false);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateNodeInvalidNewName() {

		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("a").build());
		Node childNode2 = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("b").build());

		childNode2.setName("a");

		nodeDAO.updateNode(childNode2, false);
	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateNodeChangeNodeType() {

		Node rootNode = nodeDAO.getRootNode();

		nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("a").build());
		Node childNode2 = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("b").build());

		childNode2.setNodeType(NodeType.CONFIGURATION);

		nodeDAO.updateNode(childNode2, false);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testUpdateNode() {
		Node rootNode = nodeDAO.getRootNode();

		Map<String, String> props = new HashMap<>();
		props.put("a", "b");

		Node childNode2 = nodeDAO.createNode(rootNode.getUniqueId(),
				Node.builder().name("b").userName("u1").properties(props).build());

		childNode2.setName("c");
		Map<String, String> props2 = new HashMap<>();
		props2.putAll(props);
		props2.put("aa", "bb");

		childNode2.setProperties(props2);

		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertEquals(2, childNode2.getProperties().size());
		assertNotNull(childNode2.getProperty("aa"));
		assertNotNull(childNode2.getProperty("a"));

		Map<String, String> props3 = new HashMap<>();
		props3.put("x", "z");

		childNode2.setProperties(props3);

		childNode2 = nodeDAO.updateNode(childNode2, false);

		assertEquals(1, childNode2.getProperties().size());
		assertNull(childNode2.getProperty("aa"));
		assertNull(childNode2.getProperty("a"));
		assertNotNull(childNode2.getProperty("x"));
	}



	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetSnapshotThatIsNotSnapshot() {
		Node root = nodeDAO.getRootNode();
		Node node = nodeDAO.createNode(root.getUniqueId(), Node.builder().name("dsa").build());
		assertNull(nodeDAO.getSnapshot(node.getUniqueId()));
	}

	@Test(expected = RuntimeException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testUniqueKeyOnNode() {

		Node root = nodeDAO.getRootNode();
		nodeDAO.createNode(root.getUniqueId(), Node.builder().uniqueId("b").name("n1").userName("g").build());
		nodeDAO.createNode(root.getUniqueId(), Node.builder().uniqueId("b").name("n2").userName("g").build());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testSetRootNodeProperty() {

		Node rootNode = nodeDAO.getRootNode();
		Map<String, String> props = new HashMap<>();
		props.put("root", "true");
		Node node = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().properties(props).name("a").build());
		assertNull(node.getProperty("root"));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testSaveSnapshot() {
		Node rootNode = nodeDAO.getRootNode();
		Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
		config = nodeDAO.createNode(rootNode.getUniqueId(), config);
		nodeDAO.updateConfiguration(config, Arrays.asList(ConfigPv.builder().pvName("whatever").build()));

		SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
				.value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
				.build();

		Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1), "snapshotName", "comment", "userName");
		List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(snapshotNode.getUniqueId());
		assertTrue(snapshotItems.size() == 1);

	}

	@Test(expected = IllegalArgumentException.class)
	@FlywayTest(invokeCleanDB = true)
	public void testCreateSnapshotInFolderParent() {
		Node rootNode = nodeDAO.getRootNode();
		Node snapshot = Node.builder().name("Snapshot").nodeType(NodeType.SNAPSHOT).build();
		nodeDAO.createNode(rootNode.getUniqueId(), snapshot);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testCreateNodeWithNonNullUNiqueId() {
		Node rootNode = nodeDAO.getRootNode();
		Node folder = Node.builder().name("Folder").nodeType(NodeType.FOLDER).uniqueId("uniqueid").build();
		folder = nodeDAO.createNode(rootNode.getUniqueId(), folder);
		assertEquals("uniqueid", folder.getUniqueId());
	}

	@Test
	public void testGetFromPathInvalidPath(){
		List<Node> nodes = nodeDAO.getFromPath(null);
		assertNull(nodes);
		nodes = nodeDAO.getFromPath("doesNotStartWithForwardSlash");
		assertNull(nodes);
		nodes = nodeDAO.getFromPath("/endsInSlash/");
		assertNull(nodes);
		nodes = nodeDAO.getFromPath("/");
		assertNull(nodes);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testFindParentFromPathElements() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		Node found = nodeDAO.findParentFromPathElements(rootNode, "/a/b/c".split("/"), 1);
		assertEquals(found.getUniqueId(), b.getUniqueId());

		found = nodeDAO.findParentFromPathElements(rootNode, "/a/b/d".split("/"), 1);
		assertEquals(found.getUniqueId(), b.getUniqueId());

		found = nodeDAO.findParentFromPathElements(rootNode, "/a/b".split("/"), 1);
		assertEquals(found.getUniqueId(), a.getUniqueId());

		found = nodeDAO.findParentFromPathElements(rootNode, "/a".split("/"), 1);
		assertEquals(found.getUniqueId(), rootNode.getUniqueId());

		found = nodeDAO.findParentFromPathElements(rootNode, "/a/d/c".split("/"), 1);
		assertNull(found);
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFromPathTwoNodes() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
		Node cc = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.CONFIGURATION).name("c").build());

		List<Node> nodes = nodeDAO.getFromPath("/a/b/c");
		assertEquals(2, nodes.size());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFromPathOneNode() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		List<Node> nodes = nodeDAO.getFromPath("/a/b/c");
		assertEquals(1, nodes.size());
		assertEquals(c.getUniqueId(), nodes.get(0).getUniqueId());

		nodes = nodeDAO.getFromPath("/a/b");
		assertEquals(1, nodes.size());
		assertEquals(b.getUniqueId(), nodes.get(0).getUniqueId());

		nodes = nodeDAO.getFromPath("/a");
		assertEquals(1, nodes.size());
		assertEquals(a.getUniqueId(), nodes.get(0).getUniqueId());
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFromPathZeroNodes() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		List<Node> nodes = nodeDAO.getFromPath("/a/b/d");
		assertNull(nodes);

		nodes = nodeDAO.getFromPath("/a/x/c");
		assertNull(nodes);
	}

	@Test
	public void testGetFullPathInvalidNodeId(){
		assertNull(nodeDAO.getFullPath(null));
		assertNull(nodeDAO.getFullPath(""));
		assertNull(nodeDAO.getFullPath("invalid"));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFullPathNonExistingNode() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		assertNull(nodeDAO.getFullPath("nonExisting"));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFullPathRootNode() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		assertEquals("/", nodeDAO.getFullPath(rootNode.getUniqueId()));
	}

	@Test
	@FlywayTest(invokeCleanDB = true)
	public void testGetFullPath() {
		Node rootNode = nodeDAO.getRootNode();
		Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
		Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
		Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

		assertEquals("/a/b/c", nodeDAO.getFullPath(c.getUniqueId()));
	}
}
