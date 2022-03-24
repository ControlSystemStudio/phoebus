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

package org.phoebus.service.saveandrestore.persistence.dao.impl;

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
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.persistence.config.PersistenceConfiguration;
import org.phoebus.service.saveandrestore.persistence.config.PersistenceTestConfig;
import org.phoebus.service.saveandrestore.services.exception.NodeNotFoundException;
import org.phoebus.service.saveandrestore.services.exception.SnapshotNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableConfigurationProperties
@ContextHierarchy({@ContextConfiguration(classes = {PersistenceConfiguration.class, PersistenceTestConfig.class})})
@TestPropertySource(properties = {"dbengine = h2"})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, FlywayTestExecutionListener.class})
public class DAOTest {

    @Autowired
    @SuppressWarnings("unused")
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

    @Test
    @FlywayTest
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

    @Test
    @FlywayTest
    public void testNewFolderNoDuplicateName() {

        Node rootNode = nodeDAO.getRootNode();

        Node folder1 = Node.builder().name("Folder 1").build();

        Node folder2 = Node.builder().name("Folder 2").build();

        // Create a new folder
        assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder1));

        // Try to create a new folder with a different name in the same parent directory
        assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder2));

    }

    @Test
    @FlywayTest
    public void testNewConfigNoConfigPvs() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").build();

        Node newConfig = nodeDAO.createNode(rootNode.getUniqueId(), config);

        assertTrue(nodeDAO.getConfigPvs(newConfig.getUniqueId()).isEmpty());
    }

    @Test
    @FlywayTest
    public void testDeleteConfiguration() {

        Node rootNode = nodeDAO.getRootNode();
        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        Node snapshot = nodeDAO.createNode(config.getUniqueId(), Node.builder().name("node").nodeType(NodeType.SNAPSHOT).build());

        nodeDAO.deleteNode(config.getUniqueId());

        try {
            nodeDAO.getNode(config.getUniqueId());
            fail("NodeNotFeoundException expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            nodeDAO.getNode(snapshot.getUniqueId());
            fail("NodeNotFeoundExcewption expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    @Test
    @FlywayTest
    public void testDeleteConfigurationAndPvs() {

        Node rootNode = nodeDAO.getRootNode();

        ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).build();

        config.setName("My config");

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        config = nodeDAO.updateConfiguration(config, List.of(configPv));

        nodeDAO.deleteNode(config.getUniqueId());

        // TODO: Check that PVs have been deleted. Imposed by foreign key constraint on
        // config_pv table

    }


    @Test
    @FlywayTest
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
        try {
            nodeDAO.getNode(config.getUniqueId());
            fail("NodeNotFeoundException expected");
        } catch (NodeNotFoundException exception) {
            exception.printStackTrace();
        }
        try {
            nodeDAO.getNode(folder2.getUniqueId());
            fail("NodeNotFeoundException expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testDeleteRootFolder() {
        nodeDAO.deleteNode(nodeDAO.getRootNode().getUniqueId());
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testDeleteNodes() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.deleteNodes(List.of(folderNode.getUniqueId()));
        // This will throw NodeNotFoundException
        nodeDAO.getNode(folderNode.getUniqueId());
    }

    @Test
    @FlywayTest
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
        nodeDAO.updateConfiguration(config2, List.of(configPv2));
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
    @FlywayTest
    public void testGetConfigForSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        List<ConfigPv> configPvs = nodeDAO.getConfigPvs(config.getUniqueId());
        SnapshotItem item1 = SnapshotItem.builder().configPv(configPvs.get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshot name", "user", "comment");

        config = nodeDAO.getParentNode(newSnapshot.getUniqueId());

        assertNotNull(config);
    }

    @Test
    @FlywayTest
    public void testGetParentNodeDataAccessException() {
        assertNull(nodeDAO.getParentNode("nonexisting"));
    }

    @Test
    @FlywayTest
    public void testTakeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").readbackPvName("readback_whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(8.8, alarm, time, display))
                .build();

        Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshot name", "user", "comment");
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

    @Test
    @FlywayTest
    public void testGetSnapshotsNoSnapshots() {

        assertTrue(nodeDAO.getSnapshots("a").isEmpty());
    }

    @Test
    @FlywayTest
    public void testGetSnapshotItemsWithNullPvValues() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display))
                .build();
        Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "name", "comment", "user");
        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
        assertNull(snapshotItems.get(0).getReadbackValue());

        item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .build();
        newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "name2", "comment", "user");
        snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
        assertTrue(snapshotItems.isEmpty());
    }

    @Test
    @FlywayTest
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
    @FlywayTest
    public void testGetRootsParent() {
        Node rootNode = nodeDAO.getRootNode();
        Node parent = nodeDAO.getParentNode(rootNode.getUniqueId());

        assertEquals(rootNode.getUniqueId(), parent.getUniqueId());
    }


    @Test
    @FlywayTest
    public void testGetChildNodes() {
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
    @FlywayTest
    public void testGetChildNodesOfNonExistingNode() {
        nodeDAO.getChildNodes("non-existing");
    }

    @Test
    @FlywayTest
    public void testUpdateConfig() throws Exception {

        Node rootNode = nodeDAO.getRootNode();

        nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

        ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).name("name").build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2));

        Date lastModified;

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

        nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2, configPv3));

        // Verify the list of PVs
        assertEquals(3, nodeDAO.getConfigPvs(config.getUniqueId()).size());

    }

    @Test(expected = NodeNotFoundException.class)
    public void testUpdateNonExistinConfiguration() {

        nodeDAO.updateConfiguration(Node.builder().id(-1).build(), null);
    }

    @Test(expected = NodeNotFoundException.class)
    public void testUpdateNodethatIsNotConfiguration() {
        nodeDAO.updateConfiguration(Node.builder().id(Node.ROOT_NODE_ID).build(), null);
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testUpdateNonExistingNode() {
        nodeDAO.updateNode(new Node(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testUpdateRootNode() {
        nodeDAO.updateNode(nodeDAO.getRootNode(), false);
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testUpdateNodeType() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        folderNode.setNodeType(NodeType.CONFIGURATION);

        nodeDAO.updateNode(folderNode, false);
    }

    @Test
    @FlywayTest
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

    @Test
    @FlywayTest
    public void testGetFolderThatIsNotAFolder() {

        Node rootNode = nodeDAO.getRootNode();

        Node folder1 = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

        ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(folder1.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(configPv1));

        assertNotEquals(NodeType.FOLDER, nodeDAO.getNode(config.getUniqueId()).getNodeType());

    }

    @Test
    @FlywayTest
    public void testNoNameClash1() {

        Node rootNode = nodeDAO.getRootNode();

        Node n1 = Node.builder().id(1).name("n1").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n1);
        Node n2 = Node.builder().nodeType(NodeType.CONFIGURATION).id(2).name("n1").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n2);
    }

    @Test
    @FlywayTest
    public void testNoNameClash() {

        Node rootNode = nodeDAO.getRootNode();

        Node n1 = Node.builder().id(1).name("n1").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n1);
        Node n2 = Node.builder().id(2).name("n2").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n2);
    }

    @Test
    @FlywayTest
    public void testUpdateNode() {
        Node rootNode = nodeDAO.getRootNode();

        Map<String, String> props = new HashMap<>();
        props.put("a", "b");

        Node childNode2 = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("b").userName("u1").properties(props).build());

        childNode2.setName("c");
        Map<String, String> props2 = new HashMap<>(props);
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

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testUpdateNodeNewNameInvalid() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("node1");
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("node2");
        nodeDAO.createNode(rootNode.getUniqueId(), node2);

        node1.setName("node2");

        nodeDAO.updateNode(node1, false);
    }


    @Test(expected = SnapshotNotFoundException.class)
    @FlywayTest
    public void testGetSnapshotThatIsNotSnapshot() {
        Node root = nodeDAO.getRootNode();
        Node node = nodeDAO.createNode(root.getUniqueId(), Node.builder().name("dsa").build());
        nodeDAO.getSnapshot(node.getUniqueId());
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testGetNonExistingSnapshot() {
        nodeDAO.getSnapshot("nonExisting");
    }

    @Test
    @FlywayTest
    public void testSetRootNodeProperty() {

        Node rootNode = nodeDAO.getRootNode();
        Map<String, String> props = new HashMap<>();
        props.put("root", "true");
        Node node = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().properties(props).name("a").build());
        assertNull(node.getProperty("root"));
    }

    @Test
    @FlywayTest
    public void testSaveSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");
        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(snapshotNode.getUniqueId());
        assertEquals(1, snapshotItems.size());

    }

    @Test
    @FlywayTest
    public void testCreateNodeWithNonNullUniqueId() {
        Node rootNode = nodeDAO.getRootNode();
        Node folder = Node.builder().name("Folder").nodeType(NodeType.FOLDER).uniqueId("uniqueid").build();
        folder = nodeDAO.createNode(rootNode.getUniqueId(), folder);
        assertNotEquals("uniqueid", folder.getUniqueId());
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testCreateNodeWithNonNullParentNode() {
        nodeDAO.createNode("invalid unique node id", new Node());
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCreateFolderInConfigNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(configNode.getUniqueId(), folderNode);
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCreateConfigInConfigNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        Node anotherConfig = new Node();
        anotherConfig.setName("Another Config");
        anotherConfig.setNodeType(NodeType.CONFIGURATION);
        nodeDAO.createNode(configNode.getUniqueId(), configNode);
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCreateSnapshotInFolderNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node snapshotNode = new Node();
        snapshotNode.setName("Snapshot");
        snapshotNode.setNodeType(NodeType.SNAPSHOT);
        nodeDAO.createNode(rootNode.getUniqueId(), snapshotNode);
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCreateNameClash() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        Node anotherConfig = new Node();
        anotherConfig.setName("Config");
        anotherConfig.setNodeType(NodeType.CONFIGURATION);
        nodeDAO.createNode(rootNode.getUniqueId(), anotherConfig);
    }

    @Test
    public void testGetFromPathInvalidPath() {
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
    @FlywayTest
    public void testFindParentFromPathElements() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

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
    @FlywayTest
    public void testGetFromPathTwoNodes() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.CONFIGURATION).name("c").build());

        List<Node> nodes = nodeDAO.getFromPath("/a/b/c");
        assertEquals(2, nodes.size());
    }

    @Test
    @FlywayTest
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
    @FlywayTest
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

    @Test(expected = NodeNotFoundException.class)
    public void testGetFullPathNullNodeId() {
        nodeDAO.getFullPath(null);
    }

    @Test(expected = NodeNotFoundException.class)
    public void testGetFullPathInvalidNodeId() {
        nodeDAO.getFullPath("invalid");
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testGetFullPathNonExistingNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
        // This will throw NodeNotFoundException
        nodeDAO.getFullPath("nonExisting");
    }

    @Test
    @FlywayTest
    public void testGetFullPathRootNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/", nodeDAO.getFullPath(rootNode.getUniqueId()));
    }

    @Test
    @FlywayTest
    public void testGetFullPath() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/a/b/c", nodeDAO.getFullPath(c.getUniqueId()));
    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedRootNode() {
        Node rootNode = nodeDAO.getRootNode();
        assertFalse(nodeDAO.isMoveOrCopyAllowed(List.of(rootNode), rootNode));
    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedSnapshotNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Snapshot node");
        node1.setNodeType(NodeType.SNAPSHOT);

        Node node2 = new Node();
        node2.setName("Configuration node");
        node2.setNodeType(NodeType.CONFIGURATION);
        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), rootNode));
    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedSameType() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Snapshot node");
        node1.setNodeType(NodeType.CONFIGURATION);
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("Configuration node");
        node2.setNodeType(NodeType.FOLDER);
        node2 = nodeDAO.createNode(rootNode.getUniqueId(), node2);

        Node targetNode = new Node();
        targetNode.setName("Target node");
        targetNode.setNodeType(NodeType.FOLDER);

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), targetNode));
    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedSameParentFolder() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Snapshot node");
        node1.setNodeType(NodeType.CONFIGURATION);
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node folderNode = new Node();
        folderNode.setName("Folder node");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node node2 = new Node();
        node2.setName("Configuration node");
        node2.setNodeType(NodeType.CONFIGURATION);
        node2 = nodeDAO.createNode(folderNode.getUniqueId(), node2);

        Node targetNode = new Node();
        targetNode.setName("Target node");
        targetNode.setNodeType(NodeType.FOLDER);

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), targetNode));
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testMoveNodesInvalidId() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Node1");
        node1.setNodeType(NodeType.FOLDER);
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("Node2");
        node2.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(rootNode.getUniqueId(), node2);

        List<String> nodeIds = Arrays.asList(node1.getUniqueId(), "non-existing");

        nodeDAO.moveNodes(nodeIds, rootNode.getUniqueId(), "userName");
    }

    @Test
    @FlywayTest
    public void testMoveNodesNameAndTypeClash() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Node1");
        node1.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("Node2");
        node2.setNodeType(NodeType.FOLDER);
        node2 = nodeDAO.createNode(rootNode.getUniqueId(), node2);

        Node node3 = new Node();
        node3.setName("Node1");
        node3.setNodeType(NodeType.FOLDER);
        node3 = nodeDAO.createNode(node2.getUniqueId(), node3);

        Node node4 = new Node();
        node4.setName("Node4");
        node4.setNodeType(NodeType.FOLDER);
        node4 = nodeDAO.createNode(node2.getUniqueId(), node4);

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node3, node4), rootNode));
    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedTargetNotInSelectionTree() {
        Node rootNode = nodeDAO.getRootNode();

        Node firstLevelFolder1 = new Node();
        firstLevelFolder1.setName("First level folder 1");
        firstLevelFolder1.setNodeType(NodeType.FOLDER);
        firstLevelFolder1 = nodeDAO.createNode(rootNode.getUniqueId(), firstLevelFolder1);

        Node firstLevelFolder2 = new Node();
        firstLevelFolder2.setName("First Level folder 2");
        firstLevelFolder2.setNodeType(NodeType.FOLDER);
        firstLevelFolder2 = nodeDAO.createNode(rootNode.getUniqueId(), firstLevelFolder2);

        Node secondLevelFolder1 = new Node();
        secondLevelFolder1.setName("Second level folder 1");
        secondLevelFolder1.setNodeType(NodeType.FOLDER);
        secondLevelFolder1 = nodeDAO.createNode(firstLevelFolder1.getUniqueId(), secondLevelFolder1);

        Node secondLevelFolder2 = new Node();
        secondLevelFolder2.setName("Second level folder 2");
        secondLevelFolder2.setNodeType(NodeType.FOLDER);
        secondLevelFolder2 = nodeDAO.createNode(firstLevelFolder1.getUniqueId(), secondLevelFolder2);

        assertTrue(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(secondLevelFolder1, secondLevelFolder2), rootNode));

    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedTargetInSelectionTree() {
        Node rootNode = nodeDAO.getRootNode();

        Node firstLevelFolder1 = new Node();
        firstLevelFolder1.setName("First level folder 1");
        firstLevelFolder1.setNodeType(NodeType.FOLDER);
        firstLevelFolder1 = nodeDAO.createNode(rootNode.getUniqueId(), firstLevelFolder1);

        Node firstLevelFolder2 = new Node();
        firstLevelFolder2.setName("First Level folder 2");
        firstLevelFolder2.setNodeType(NodeType.FOLDER);
        firstLevelFolder2 = nodeDAO.createNode(rootNode.getUniqueId(), firstLevelFolder2);

        Node secondLevelFolder1 = new Node();
        secondLevelFolder1.setName("Second level folder 1");
        secondLevelFolder1.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(firstLevelFolder1.getUniqueId(), secondLevelFolder1);

        Node secondLevelFolder2 = new Node();
        secondLevelFolder2.setName("Second level folder 2");
        secondLevelFolder2.setNodeType(NodeType.FOLDER);
        secondLevelFolder2 = nodeDAO.createNode(firstLevelFolder1.getUniqueId(), secondLevelFolder2);

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(firstLevelFolder2, firstLevelFolder1), secondLevelFolder2));

    }

    @Test
    @FlywayTest
    public void testIsMoveAllowedMoveSaveSetToRoot() {
        Node rootNode = nodeDAO.getRootNode();

        Node saveSetNode = new Node();
        saveSetNode.setNodeType(NodeType.CONFIGURATION);
        saveSetNode.setName("Save Set");

        assertFalse(nodeDAO.isMoveOrCopyAllowed(List.of(saveSetNode), rootNode));

    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testMoveNodesToNonExistingTarget() {
        nodeDAO.moveNodes(Collections.emptyList(), "non existing", "user");
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testMoveNodesToNonFolder() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        nodeDAO.moveNodes(List.of("someId"), configNode.getUniqueId(), "user");
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testMoveSnasphot() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        Node snapshotNode = new Node();
        snapshotNode.setName("Snapshot");
        snapshotNode.setNodeType(NodeType.SNAPSHOT);
        snapshotNode = nodeDAO.createNode(configNode.getUniqueId(), snapshotNode);

        nodeDAO.moveNodes(List.of(snapshotNode.getUniqueId()), rootNode.getUniqueId(), "user");
    }

    @Test
    @FlywayTest
    public void testMoveNodes() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node folderNode1 = new Node();
        folderNode1.setName("Fodler 1");
        folderNode1.setNodeType(NodeType.FOLDER);
        folderNode1 = nodeDAO.createNode(folderNode.getUniqueId(), folderNode1);

        Node folderNode2 = new Node();
        folderNode2.setName("Folder 2");
        folderNode2.setNodeType(NodeType.FOLDER);
        folderNode2 = nodeDAO.createNode(folderNode.getUniqueId(), folderNode2);

        assertEquals(1, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());

        rootNode = nodeDAO.moveNodes(Arrays.asList(folderNode1.getUniqueId(), folderNode2.getUniqueId()), rootNode.getUniqueId(), "user");

        assertEquals(3, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCopyFolderToSameParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.copyNodes(List.of(folderNode.getUniqueId()), rootNode.getUniqueId(), "username");
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCopyConfigToSameParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(rootNode.getUniqueId(), configNode);

        nodeDAO.copyNodes(List.of(configNode.getUniqueId()), rootNode.getUniqueId(), "username");
    }

    @Test
    @FlywayTest
    public void testCopyFolderToOtherParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolderNode = new Node();
        childFolderNode.setName("Child Folder");
        childFolderNode.setNodeType(NodeType.FOLDER);
        childFolderNode = nodeDAO.createNode(folderNode.getUniqueId(), childFolderNode);

        nodeDAO.copyNodes(List.of(childFolderNode.getUniqueId()), rootNode.getUniqueId(), "username");

        List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());
        assertEquals(2, childNodes.size());
    }

    @Test
    @FlywayTest
    public void testCopyConfigToOtherParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node folderNode2 = new Node();
        folderNode2.setName("Folder2");
        folderNode2.setNodeType(NodeType.FOLDER);
        folderNode2 = nodeDAO.createNode(rootNode.getUniqueId(), folderNode2);

        Node config = new Node();
        config.setName("Config");
        config.setNodeType(NodeType.CONFIGURATION);
        config = nodeDAO.createNode(folderNode.getUniqueId(), config);

        nodeDAO.copyNodes(List.of(config.getUniqueId()), folderNode2.getUniqueId(), "username");

        List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());
        assertEquals(2, childNodes.size());
    }

    @Test
    @FlywayTest
    public void testCopyMultipleFolders() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolder1 = new Node();
        childFolder1.setName("Child Folder 1");
        childFolder1.setNodeType(NodeType.FOLDER);
        childFolder1 = nodeDAO.createNode(folderNode.getUniqueId(), childFolder1);

        Node childFolder2 = new Node();
        childFolder2.setName("Child Folder 2");
        childFolder2.setNodeType(NodeType.FOLDER);
        childFolder2 = nodeDAO.createNode(folderNode.getUniqueId(), childFolder2);

        nodeDAO.copyNodes(Arrays.asList(childFolder1.getUniqueId(), childFolder2.getUniqueId()), rootNode.getUniqueId(), "username");

        List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());
        assertEquals(3, childNodes.size());
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCopyFolderAndConfig() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolder1 = new Node();
        childFolder1.setName("Child Folder 1");
        childFolder1.setNodeType(NodeType.FOLDER);
        childFolder1 = nodeDAO.createNode(folderNode.getUniqueId(), childFolder1);

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(folderNode.getUniqueId(), configNode);

        nodeDAO.copyNodes(Arrays.asList(childFolder1.getUniqueId(), configNode.getUniqueId()), rootNode.getUniqueId(), "username");
    }

    @Test
    @FlywayTest
    public void testCopyFolderWithConfigAndSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolder1 = new Node();
        childFolder1.setName("Child Folder 1");
        childFolder1.setNodeType(NodeType.FOLDER);
        childFolder1 = nodeDAO.createNode(folderNode.getUniqueId(), childFolder1);

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(childFolder1.getUniqueId(), configNode);

        nodeDAO.updateConfiguration(configNode, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(configNode.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        nodeDAO.saveSnapshot(configNode.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");

        Node parent = nodeDAO.copyNodes(List.of(childFolder1.getUniqueId()), rootNode.getUniqueId(), "username");
        List<Node> childNodes = nodeDAO.getChildNodes(parent.getUniqueId());
        assertEquals(2, childNodes.size());

        Node copiedFolder = childNodes.stream().filter(node -> node.getName().equals("Child Folder 1")).findFirst().get();
        Node copiedSaveSet = nodeDAO.getChildNodes(copiedFolder.getUniqueId()).get(0);
        Node copiedSnapshot = nodeDAO.getSnapshots(copiedSaveSet.getUniqueId()).get(0);
        assertEquals("snapshotName", copiedSnapshot.getName());
        assertEquals(1, nodeDAO.getConfigPvs(copiedSaveSet.getUniqueId()).size());

    }

    @Test
    @FlywayTest
    public void testCopySubtree() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolder1 = new Node();
        childFolder1.setName("Child Folder 1");
        childFolder1.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(folderNode.getUniqueId(), childFolder1);

        Node targetNode = new Node();
        targetNode.setName("Target Folder");
        targetNode.setNodeType(NodeType.FOLDER);
        targetNode = nodeDAO.createNode(rootNode.getUniqueId(), targetNode);

        nodeDAO.copyNodes(List.of(folderNode.getUniqueId()), targetNode.getUniqueId(), "username");

        assertEquals(2, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());
        assertEquals(1, nodeDAO.getChildNodes(targetNode.getUniqueId()).size());
    }

    @Test(expected = IllegalArgumentException.class)
    @FlywayTest
    public void testCopySnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.copyNodes(List.of(snapshotNode.getUniqueId()), folderNode.getUniqueId(), "username");
    }

    @Test
    @FlywayTest
    public void testCopyConfigWithSnapshots() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");
        Tag tag = new Tag();
        tag.setUserName("username");
        tag.setSnapshotId(snapshotNode.getUniqueId());
        tag.setName("tagname");
        tag.setCreated(new Date());
        tag.setComment("tagcomment");
        snapshotNode.addTag(tag);
        nodeDAO.updateNode(snapshotNode, false);

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.copyNodes(List.of(config.getUniqueId()), folderNode.getUniqueId(), "username");

        Node copiedConfig = nodeDAO.getChildNodes(folderNode.getUniqueId()).get(0);
        Node copiedSnapshot = nodeDAO.getChildNodes(copiedConfig.getUniqueId()).get(0);
        assertEquals("snapshotName", copiedSnapshot.getName());
        assertEquals("comment", copiedSnapshot.getProperties().get("comment"));
        assertEquals("username", copiedSnapshot.getUserName());
        List<Tag> tags = nodeDAO.getTags(copiedSnapshot.getUniqueId());
        assertEquals("username", tags.get(0).getUserName());
        assertEquals("tagname", tags.get(0).getName());
        assertEquals("tagcomment", tags.get(0).getComment());
    }

    @Test
    @FlywayTest
    public void testDeleteNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.deleteNodes(List.of(folderNode.getUniqueId()));
        assertTrue(nodeDAO.getChildNodes(rootNode.getUniqueId()).isEmpty());
    }

    @Test(expected = NodeNotFoundException.class)
    @FlywayTest
    public void testDeleteNodeInvalidList() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.deleteNodes(Arrays.asList(folderNode.getUniqueId(), "invalid"));
    }

    @Test
    @FlywayTest
    public void testDeleteMultiple() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node folderNode2 = new Node();
        folderNode2.setName("Folder2");
        folderNode2.setNodeType(NodeType.FOLDER);
        folderNode2 = nodeDAO.createNode(rootNode.getUniqueId(), folderNode2);

        nodeDAO.deleteNodes(Arrays.asList(folderNode.getUniqueId(), folderNode2.getUniqueId()));
        assertTrue(nodeDAO.getChildNodes(rootNode.getUniqueId()).isEmpty());
    }

    @Test
    @FlywayTest
    public void testDeleteTree() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node folderNode2 = new Node();
        folderNode2.setName("Folder2");
        folderNode2.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(folderNode.getUniqueId(), folderNode2);

        nodeDAO.deleteNodes(List.of(folderNode.getUniqueId()));
        assertTrue(nodeDAO.getChildNodes(rootNode.getUniqueId()).isEmpty());
    }
}
