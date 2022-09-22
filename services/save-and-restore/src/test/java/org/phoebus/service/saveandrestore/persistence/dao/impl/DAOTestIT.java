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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VInt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.SnapshotNotFoundException;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test to be executed against a running Elasticsearch 8.x instance.
 * It must be run with application property spring.profiles.active=IT.
 */
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@Profile("IT")
@SuppressWarnings("unused")
public class DAOTestIT {

    @Autowired
    private ElasticsearchDAO nodeDAO;

    @Autowired
    private ConfigurationDataRepository configurationDataRepository;

    @Autowired
    private ElasticsearchClient client;

    @Value("${elasticsearch.tree_node.index:test_saveandrestore_configuration}")
    private String ES_CONFIGURATION_INDEX;

    @Value("${elasticsearch.tree_node.index:test_saveandrestore_tree}")
    private String ES_TREE_INDEX;

    private static Alarm alarm;
    private static Time time;
    private static Display display;

    @BeforeAll
    public static void init() {
        time = Time.of(Instant.now());
        alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
        display = Display.none();
    }

    @Test
    public void testNewNode() {
        Node root = nodeDAO.getRootNode();
        Date lastModified = root.getLastModified();
        Node folder = Node.builder().name("SomeFolder").userName("username").build();
        Node newNode = nodeDAO.createNode(root.getUniqueId(), folder);
        root = nodeDAO.getRootNode();
        assertNotNull(newNode);
        assertEquals(NodeType.FOLDER, newNode.getNodeType());
        // Check that the parent folder's last modified date is updated
        assertTrue(root.getLastModified().getTime() > lastModified.getTime());

        clearAllData();
    }

    @Test
    public void testNewFolderNoDuplicateName() {

        Node rootNode = nodeDAO.getRootNode();
        Node folder1 = Node.builder().name("Folder 1").build();
        Node folder2 = Node.builder().name("Folder 2").build();
        // Create a new folder
        assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder1));
        // Try to create a new folder with a different name in the same parent directory
        assertNotNull(nodeDAO.createNode(rootNode.getUniqueId(), folder2));

        clearAllData();
    }

    @Test
    public void testNewConfigNoConfigPvs() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config").build();
        Node newConfig = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), config);
        assertTrue(nodeDAO.getConfigPvs(newConfig.getUniqueId()).isEmpty());

        clearAllData();
    }

    @Test
    public void testDeleteConfiguration() {

        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), config);
        Node snapshot = nodeDAO.createNode(config.getUniqueId(), Node.builder().name("node").nodeType(NodeType.SNAPSHOT).build());

        nodeDAO.deleteNode(topLevelFolderNode.getUniqueId());

        try {
            nodeDAO.getNode(config.getUniqueId());
            fail("NodeNotFoundException expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        try {
            nodeDAO.getNode(snapshot.getUniqueId());
            fail("NodeNotFoundException expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }


    @Test
    public void testDeleteConfigurationAndPvs() {

        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        ConfigPv configPv = ConfigPv.builder().pvName("pvName").build();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).build();

        config.setName("My config");

        // This should not throw exception
        nodeDAO.createNode(topLevelFolderNode.getUniqueId(), config);

        clearAllData();
    }


    @Test
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
            fail("NodeNotFoundException expected");
        } catch (NodeNotFoundException exception) {
            exception.printStackTrace();
        }
        try {
            nodeDAO.getNode(folder2.getUniqueId());
            fail("NodeNotFoundException expected");
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Test
    public void testDeleteRootFolder() {
        assertThrows(IllegalArgumentException.class, () -> nodeDAO.deleteNode(nodeDAO.getRootNode().getUniqueId()));
    }

    @Test
    public void testGetNodeAsConfig() {
        Node rootNode = nodeDAO.getRootNode();
        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();
        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());
        Node newConfig = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), config);
        Node configFromDB = nodeDAO.getNode(newConfig.getUniqueId());
        assertEquals(newConfig.getUniqueId(), configFromDB.getUniqueId());

        clearAllData();
    }

    @Test
    @Disabled
    public void testGetConfigForSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        List<ConfigPv> configPvs = nodeDAO.getConfigPvs(config.getUniqueId());
        SnapshotItem item1 = SnapshotItem.builder().configPv(configPvs.get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshot name", "user", "comment");

        config = nodeDAO.getParentNode(newSnapshot.getUniqueId());

        assertNotNull(config);
    }

    @Test
    public void testGetParentNodeForNonexistingNode() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getParentNode("nonexisting"));
    }

    @Test
    @Disabled
    public void testTakeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").readbackPvName("readback_whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(8.8, alarm, time, display))
                .build();

        Node newSnapshot = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshot name", "user", "comment");
        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(newSnapshot.getUniqueId());
        assertEquals(1, snapshotItems.size());
        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
        assertEquals(8.8, ((VDouble) snapshotItems.get(0).getReadbackValue()).getValue().doubleValue(), 0.01);

        Node fullSnapshot = nodeDAO.getSnapshotNode(newSnapshot.getUniqueId());
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
    @Disabled
    public void testGetSnapshotsNoSnapshots() {
        assertTrue(nodeDAO.getSnapshots("a").isEmpty());
    }

    @Test
    @Disabled
    public void testGetSnapshotItemsWithNullPvValues() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

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
    @Disabled
    public void testSnapshotTag() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("testFolder").nodeType(NodeType.FOLDER).build());
        Node savesetNode = nodeDAO.createNode(folderNode.getUniqueId(), Node.builder().name("testSaveset").nodeType(NodeType.CONFIGURATION).build());
        Node snapshot = nodeDAO.createNode(savesetNode.getUniqueId(), Node.builder().name("testSnapshot").nodeType(NodeType.SNAPSHOT).build());

        Tag tag = Tag.builder().name("tag1").comment("comment1").userName("testUser1").build();
        snapshot.addTag(tag);

        snapshot = nodeDAO.updateNode(snapshot, false);
        assertEquals(1, snapshot.getTags().size());
        assertEquals("tag1", snapshot.getTags().get(0).getName());
        assertEquals("comment1", snapshot.getTags().get(0).getComment());
        assertEquals("testUser1", snapshot.getTags().get(0).getUserName());

        // Adding the same named tag doesn't affect anything.
        tag = Tag.builder().name("tag1").comment("comment2").userName("testUser2").build();
        snapshot.addTag(tag);

        snapshot = nodeDAO.updateNode(snapshot, false);
        assertEquals(1, snapshot.getTags().size());
        assertEquals("tag1", snapshot.getTags().get(0).getName());
        assertEquals("comment1", snapshot.getTags().get(0).getComment());
        assertEquals("testUser1", snapshot.getTags().get(0).getUserName());

        snapshot.removeTag(tag);

        snapshot = nodeDAO.updateNode(snapshot, false);
        assertEquals(0, snapshot.getTags().size());
    }

    @Test
    public void testGetRootsParent() {
        Node rootNode = nodeDAO.getRootNode();
        Node parent = nodeDAO.getParentNode(rootNode.getUniqueId());
        assertEquals(rootNode.getUniqueId(), parent.getUniqueId());
    }


    @Test
    public void testGetChildNodes() {
        Node rootNode = nodeDAO.getRootNode();

        Node folder1 = Node.builder().name("SomeFolder").build();

        // Create folder1 in the root folder
        folder1 = nodeDAO.createNode(rootNode.getUniqueId(), folder1);

        List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());
        assertTrue(nodeDAO.getChildNodes(folder1.getUniqueId()).isEmpty());

        clearAllData();
    }

    @Test
    public void testGetChildNodesOfNonExistingNode() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getChildNodes("non-existing"));
    }

    @Test
    @Disabled
    public void testUpdateConfig() throws Exception {

        Node rootNode = nodeDAO.getRootNode();

        nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

        ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).name("name").build();

        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2));

        Date lastModified;

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        SnapshotItem item2 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VInt.of(7, alarm, time, display)).build();

        Node snapshot = nodeDAO.saveSnapshot(config.getUniqueId(), Arrays.asList(item1, item2), "name", "comment", "user");

        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(snapshot.getUniqueId());

        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue().doubleValue(), 0.01);
        assertEquals(7, ((VInt) snapshotItems.get(1).getValue()).getValue().intValue());

        Node fullSnapshot = nodeDAO.getSnapshotNode(snapshot.getUniqueId());

        assertNotNull(fullSnapshot);

        List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
        assertFalse(snapshots.isEmpty());
        assertEquals(2, nodeDAO.getSnapshotItems(fullSnapshot.getUniqueId()).size());

        lastModified = config.getLastModified();

        Thread.sleep(100);
        Node updatedConfig = null; // = nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2));

        assertNotEquals(lastModified, updatedConfig.getLastModified());

        // Verify that last modified time has been updated
        assertTrue(updatedConfig.getLastModified().getTime() > lastModified.getTime());

        // Verify the list of PVs
        assertEquals(2, nodeDAO.getConfigPvs(config.getUniqueId()).size());

        ConfigPv configPv3 = ConfigPv.builder().pvName("configPv3").build();

        //nodeDAO.updateConfiguration(config, Arrays.asList(configPv1, configPv2, configPv3));

        // Verify the list of PVs
        assertEquals(3, nodeDAO.getConfigPvs(config.getUniqueId()).size());

    }

    @Test
    public void testUpdateNonExistingNode() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.updateNode(Node.builder().uniqueId("bad").build(), false));
    }

    @Test
    public void testUpdateRootNode() {
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.updateNode(nodeDAO.getRootNode(), false));
    }

    @Test
    public void testUpdateNodeType() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        folderNode.setNodeType(NodeType.CONFIGURATION);
        Node node = folderNode;
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.updateNode(node, false));

        clearAllData();
    }

    @Test
    @Disabled
    public void testGetFolderThatIsNotAFolder() {

        Node rootNode = nodeDAO.getRootNode();

        Node folder1 = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

        ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).build();

        config = nodeDAO.createNode(folder1.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(configPv1));

        assertNotEquals(NodeType.FOLDER, nodeDAO.getNode(config.getUniqueId()).getNodeType());

    }

    @Test
    public void testNoNameClash1() {

        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node n1 = Node.builder().uniqueId("1").name("n1").build();
        nodeDAO.createNode(topLevelFolderNode.getUniqueId(), n1);
        Node n2 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("2").name("n1").build();
        nodeDAO.createNode(topLevelFolderNode.getUniqueId(), n2);

        clearAllData();
    }

    @Test
    public void testNoNameClash() {

        Node rootNode = nodeDAO.getRootNode();

        Node n1 = Node.builder().uniqueId("1").name("n1").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n1);
        Node n2 = Node.builder().uniqueId("2").name("n2").build();
        nodeDAO.createNode(rootNode.getUniqueId(), n2);

        clearAllData();
    }

    @Test
    public void testUpdateNodeNewNameInvalid() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("node1");
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("node2");
        nodeDAO.createNode(rootNode.getUniqueId(), node2);

        node1.setName("node2");

        Node node = node1;

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.updateNode(node, false));

        clearAllData();
    }


    @Test
    @Disabled
    public void testGetSnapshotThatIsNotSnapshot() {
        Node root = nodeDAO.getRootNode();
        Node node = nodeDAO.createNode(root.getUniqueId(), Node.builder().name("dsa").build());
        assertThrows(SnapshotNotFoundException.class,
                () -> nodeDAO.getSnapshotNode(node.getUniqueId()));
    }

    @Test
    @Disabled
    public void testGetNonExistingSnapshot() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getSnapshotNode("nonExisting"));
    }

    @Test
    @Disabled
    public void testSaveSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");
        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItems(snapshotNode.getUniqueId());
        assertEquals(1, snapshotItems.size());

    }

    @Test
    public void testCreateNodeWithNonNullUniqueId() {
        Node rootNode = nodeDAO.getRootNode();
        Node folder = Node.builder().name("Folder").nodeType(NodeType.FOLDER).uniqueId("uniqueid").build();
        folder = nodeDAO.createNode(rootNode.getUniqueId(), folder);
        assertNotEquals("uniqueid", folder.getUniqueId());

        clearAllData();
    }

    @Test
    public void testCreateNodeWithNonNullParentNode() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.createNode("invalid unique node id", new Node()));
    }

    @Test
    public void testCreateFolderInConfigNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);

        Node node = configNode;
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.createNode(node.getUniqueId(), folderNode));

        clearAllData();
    }

    @Test
    public void testCreateConfigInConfigNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node anotherConfig = new Node();
        anotherConfig.setName("Another Config");
        anotherConfig.setNodeType(NodeType.CONFIGURATION);
        Node node = configNode;
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.createNode(node.getUniqueId(), node));

        clearAllData();
    }

    @Test
    @Disabled
    public void testCreateSnapshotInFolderNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node snapshotNode = new Node();
        snapshotNode.setName("Snapshot");
        snapshotNode.setNodeType(NodeType.SNAPSHOT);

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.createNode(rootNode.getUniqueId(), snapshotNode));
    }

    @Test
    public void testCreateNameClash() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node anotherConfig = new Node();
        anotherConfig.setName("Config");
        anotherConfig.setNodeType(NodeType.CONFIGURATION);

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.createNode(rootNode.getUniqueId(), anotherConfig));

        clearAllData();
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
    @Disabled
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

        clearAllData();
    }

    @Test
    @Disabled
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
    @Disabled
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
    @Disabled
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
    @Disabled
    public void testGetFullPathNullNodeId() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getFullPath(null));
    }

    @Test
    @Disabled
    public void testGetFullPathInvalidNodeId() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getFullPath("invalid"));
    }

    @Test
    @Disabled
    public void testGetFullPathNonExistingNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
        // This will throw NodeNotFoundException
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getFullPath("nonExisting"));
    }

    @Test
    @Disabled
    public void testGetFullPathRootNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/", nodeDAO.getFullPath(rootNode.getUniqueId()));
    }

    @Test
    @Disabled
    public void testGetFullPath() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/a/b/c", nodeDAO.getFullPath(c.getUniqueId()));
    }

    @Test
    public void testIsMoveAllowedRootNode() {
        Node rootNode = nodeDAO.getRootNode();
        assertFalse(nodeDAO.isMoveOrCopyAllowed(List.of(rootNode), rootNode));
    }

    @Test
    @Disabled
    public void testIsMoveAllowedSnapshotNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("SnapshotData node");
        node1.setNodeType(NodeType.SNAPSHOT);

        Node node2 = new Node();
        node2.setName("Configuration node");
        node2.setNodeType(NodeType.CONFIGURATION);
        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), rootNode));
    }

    @Test
    public void testIsMoveAllowedSameType() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node node1 = new Node();
        node1.setName("SnapshotData node");
        node1.setNodeType(NodeType.CONFIGURATION);
        node1 = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("Configuration node");
        node2.setNodeType(NodeType.FOLDER);
        node2 = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), node2);

        Node targetNode = new Node();
        targetNode.setUniqueId(Node.ROOT_FOLDER_UNIQUE_ID);
        targetNode.setName("Target node");
        targetNode.setNodeType(NodeType.FOLDER);

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), targetNode));

        clearAllData();
    }

    @Test
    public void testIsMoveAllowedSameParentFolder() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("SnapshotData node");
        node1.setNodeType(NodeType.FOLDER);
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
        targetNode.setUniqueId(node1.getUniqueId());

        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(node1, node2), targetNode));

        clearAllData();
    }

    @Test
    public void testMoveNodesInvalidId() {
        Node rootNode = nodeDAO.getRootNode();

        Node node1 = new Node();
        node1.setName("Node1");
        node1.setNodeType(NodeType.FOLDER);
        node1 = nodeDAO.createNode(rootNode.getUniqueId(), node1);

        Node node2 = new Node();
        node2.setName("Node2");
        node2.setNodeType(NodeType.FOLDER);
        node2 = nodeDAO.createNode(rootNode.getUniqueId(), node2);

        List<String> nodeIds = Arrays.asList(node1.getUniqueId(), "non-existing");

        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.moveNodes(nodeIds, rootNode.getUniqueId(), "userName"));

        clearAllData();
    }

    @Test
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

        clearAllData();

    }

    @Test
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

        clearAllData();

    }

    @Test
    @Disabled
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

        clearAllData();
    }

    @Test
    public void testIsMoveAllowedMoveSaveSetToRoot() {
        Node rootNode = nodeDAO.getRootNode();

        Node saveSetNode = new Node();
        saveSetNode.setNodeType(NodeType.CONFIGURATION);
        saveSetNode.setName("Save Set");

        assertFalse(nodeDAO.isMoveOrCopyAllowed(List.of(saveSetNode), rootNode));

    }

    @Test
    public void testMoveNodesToNonExistingTarget() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.moveNodes(Collections.emptyList(), "non existing", "user"));
    }

    @Test
    public void testMoveNodesToNonFolder() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node node = configNode;
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.moveNodes(List.of("someId"), node.getUniqueId(), "user"));

        clearAllData();
    }

    @Test
    public void testMoveSnasphot() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node snapshotNode = new Node();
        snapshotNode.setName("Snapshot");
        snapshotNode.setNodeType(NodeType.SNAPSHOT);
        snapshotNode = nodeDAO.createNode(configNode.getUniqueId(), snapshotNode);

        String uniqueId = snapshotNode.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.moveNodes(List.of(uniqueId), rootNode.getUniqueId(),
                        "user"));

        clearAllData();
    }

    @Test
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

        clearAllData();

    }

    @Test
    public void testCopyFolderToSameParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);
        String uniqueId = folderNode.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(uniqueId), rootNode.getUniqueId(),
                        "username"));

        clearAllData();
    }

    @Test
    public void testCopyConfigToSameParent() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);
        String uniqueId = configNode.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(uniqueId), rootNode.getUniqueId(),
                        "username"));

        clearAllData();
    }

    @Test
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

        clearAllData();
    }

    @Test
    @Disabled
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
    @Disabled
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

    @Test
    @Disabled
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

        String folderUniqueId = childFolder1.getUniqueId();
        String configUniqueId = configNode.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(Arrays.asList(folderUniqueId,
                        configUniqueId), rootNode.getUniqueId(), "username"));
    }

    @Test
    @Disabled
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

        //nodeDAO.updateConfiguration(configNode, List.of(ConfigPv.builder().pvName("whatever").build()));

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
    @Disabled
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

    @Test
    @Disabled
    public void testCopySnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        String uniqueId = folderNode.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(snapshotNode.getUniqueId()), uniqueId, "username"));
    }

    @Test
    @Disabled
    public void testCopyConfigWithSnapshots() {
        Node rootNode = nodeDAO.getRootNode();

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        config = nodeDAO.createNode(rootNode.getUniqueId(), config);
        //nodeDAO.updateConfiguration(config, List.of(ConfigPv.builder().pvName("whatever").build()));

        SnapshotItem item1 = SnapshotItem.builder().configPv(nodeDAO.getConfigPvs(config.getUniqueId()).get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Node snapshotNode = nodeDAO.saveSnapshot(config.getUniqueId(), List.of(item1), "snapshotName", "comment", "userName");
        Tag tag = new Tag();
        tag.setUserName("username");
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
        assertEquals("username", copiedSnapshot.getUserName());
        List<Tag> tags = nodeDAO.getTags(copiedSnapshot.getUniqueId());
        assertEquals("username", tags.get(0).getUserName());
        assertEquals("tagname", tags.get(0).getName());
        assertEquals("tagcomment", tags.get(0).getComment());
    }

    @Test
    @Disabled
    public void testDeleteNode() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        nodeDAO.deleteNode(folderNode.getUniqueId());
        assertTrue(nodeDAO.getChildNodes(rootNode.getUniqueId()).isEmpty());
    }

    @Test
    @Disabled
    public void testDeleteNodeInvalid() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.deleteNode("invalid"));
    }

    @Test
    @Disabled
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

        nodeDAO.deleteNode(folderNode.getUniqueId());
        assertTrue(nodeDAO.getChildNodes(rootNode.getUniqueId()).isEmpty());
    }

    /**
     * This method should verify that the target {@link Node} is <code>not</code>
     * found in any of the source {@link Node}s subtrees.
     */
    @Test
    public void testContainedInSubTree(){
        Node rootNode = nodeDAO.getRootNode();

        Node L1F1 = new Node();
        L1F1.setName("L1F1");
        L1F1.setNodeType(NodeType.FOLDER);
        L1F1 = nodeDAO.createNode(rootNode.getUniqueId(), L1F1);

        Node L1F2 = new Node();
        L1F2.setName("L1F2");
        L1F2.setNodeType(NodeType.FOLDER);
        L1F2 = nodeDAO.createNode(rootNode.getUniqueId(), L1F2);

        Node L2F1 = new Node();
        L2F1.setName("L2F1");
        L2F1.setNodeType(NodeType.FOLDER);
        L2F1 = nodeDAO.createNode(L1F1.getUniqueId(), L2F1);

        Node L2F2 = new Node();
        L2F2.setName("L2F2");
        L2F2.setNodeType(NodeType.FOLDER);
        L2F2 = nodeDAO.createNode(L1F1.getUniqueId(), L2F2);

        // OK to copy/move level 2 folders to root
        assertTrue(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(L2F1), rootNode));

        // NOT OK to copy/move level 1 folders to root as they are already there
        assertFalse(nodeDAO.isMoveOrCopyAllowed(Arrays.asList(L1F1), rootNode));

        clearAllData();

    }

    /**
     * Deletes all child nodes of the root node, i.e. all data except root node.
     */
    private void clearAllData(){
        List<Node> childNodes = nodeDAO.getChildNodes(Node.ROOT_FOLDER_UNIQUE_ID);
        childNodes.forEach(node -> nodeDAO.deleteNode(node.getUniqueId()));
    }


    @AfterAll
    public void dropIndices() {
        try {
            BooleanResponse exists = client.indices().exists(ExistsRequest.of(e -> e.index(ES_CONFIGURATION_INDEX)));
            if (exists.value()) {
                client.indices().delete(
                        DeleteIndexRequest.of(
                                c -> c.index(ES_CONFIGURATION_INDEX)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BooleanResponse exists = client.indices().exists(ExistsRequest.of(e -> e.index(ES_TREE_INDEX)));
            if (exists.value()) {
                client.indices().delete(
                        DeleteIndexRequest.of(
                                c -> c.index(ES_TREE_INDEX)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
