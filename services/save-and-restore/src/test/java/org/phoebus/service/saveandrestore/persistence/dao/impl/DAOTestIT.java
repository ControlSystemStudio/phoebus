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
import org.epics.vtype.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ConfigurationDataRepository;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

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

    @Value("${elasticsearch.filter.index:test_saveandrestore_filter}")
    private String ES_FILTER_INDEX;

    private static Alarm alarm;
    private static Time time;
    private static Display display;

    @BeforeAll
    public void init() {
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
    public void testGetConfigForSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("folder").build());

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        Node newSnapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot).getSnapshotNode();

        config = nodeDAO.getParentNode(newSnapshot.getUniqueId());

        assertNotNull(config);

        clearAllData();
    }

    @Test
    public void testGetParentNodeForNonexistingNode() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getParentNode("nonexisting"));
    }

    @Test
    public void testDeleteSnapshotReferencedInCompositeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);
        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot").build();
        Snapshot snapshot = new Snapshot();
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(SnapshotItem.builder().configPv(ConfigPv.builder().pvName("pv1").build())
                .build()));
        snapshot.setSnapshotData(snapshotData);
        snapshot.setSnapshotNode(snapshotNode);

        snapshot = nodeDAO.createSnapshot(configNode.getUniqueId(), snapshot);

        Node compositeSnapshotNode = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());
        compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshot.getSnapshotNode().getUniqueId()));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(topLevelFolderNode.getUniqueId(), compositeSnapshot);

        assertThrows(RuntimeException.class, () -> nodeDAO.deleteNode(snapshotNode.getUniqueId()));

        nodeDAO.deleteNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        clearAllData();
    }

    @Test
    public void testUpdateCompositeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();

        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node compositeSnapshotNode = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        Node configNode = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(configNode);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("pv1").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot1").build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(snapshotNode);
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(SnapshotItem.builder().configPv(ConfigPv.builder().pvName("pv1").build()).build()));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());

        compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshot.getSnapshotNode().getUniqueId()));

        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);

        // Set new values and update
        compositeSnapshotNode = compositeSnapshot.getCompositeSnapshotNode();
        compositeSnapshotNode.setName("Updated name");
        compositeSnapshotNode.setDescription("Updated description");

        Node configNode2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("config2").build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(configNode2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(List.of(ConfigPv.builder().pvName("pv2").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        Node snapshotNode2 = Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot2").build();
        Snapshot snapshot2 = new Snapshot();
        snapshot2.setSnapshotNode(snapshotNode2);
        SnapshotData snapshotData2 = new SnapshotData();
        snapshotData2.setSnapshotItems(List.of(SnapshotItem.builder().configPv(ConfigPv.builder().pvName("pv2").build()).build()));
        snapshot2.setSnapshotData(snapshotData2);

        snapshot2 = nodeDAO.createSnapshot(configuration2.getConfigurationNode().getUniqueId(), snapshot2);

        compositeSnapshotData = compositeSnapshot.getCompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId()));

        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.updateCompositeSnapshot(compositeSnapshot);

        assertEquals(compositeSnapshotNode.getUniqueId(), compositeSnapshot.getCompositeSnapshotNode().getUniqueId());
        assertEquals("Updated name", compositeSnapshot.getCompositeSnapshotNode().getName());
        assertEquals("Updated description", compositeSnapshot.getCompositeSnapshotNode().getDescription());
        assertEquals(2, compositeSnapshot.getCompositeSnapshotData().getReferencedSnapshotNodes().size());

        nodeDAO.deleteNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        clearAllData();
    }

    @Test
    public void testGetAllCompositeSnapshotData() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();

        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node configNode =
                Node.builder().nodeType(NodeType.CONFIGURATION).name("config").build();

        configNode = nodeDAO.createNode(folderNode.getUniqueId(), configNode);

        List<String> compositeSnapshotNodeIds = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            Node compositeSnapshotNode = Node.builder().name("My composite snapshot " + i).nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

            CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
            compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

            CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
            compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());

            Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT).name(i + "_").build();
            SnapshotData snapshotData = new SnapshotData();
            snapshotData.setUniqueId(snapshotNode.getUniqueId());
            snapshotData.setSnapshotItems(List.of(
                    SnapshotItem.builder()
                            .configPv(ConfigPv.builder().pvName("pvName" + i).build())
                            .value(VInt.of(i, Alarm.none(), Time.now(), Display.none()))
                            .build()));
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotNode(snapshotNode);
            snapshot.setSnapshotData(snapshotData);
            nodeDAO.createSnapshot(configNode.getUniqueId(), snapshot);

            compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshotNode.getUniqueId()));
            compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

            compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);

            compositeSnapshotNodeIds.add(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());
        }

        List<CompositeSnapshotData> all = nodeDAO.getAllCompositeSnapshotData();

        assertEquals(20, all.size());

        compositeSnapshotNodeIds.forEach(id -> nodeDAO.deleteNode(id));

        clearAllData();

    }

    @Test
    public void testTakeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();

        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder()
                .pvName("whatever").readbackPvName("readback_whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(8.8, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder()
                .name("snapshot name")
                .userName("user")
                .description("comment")
                .build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(config.getUniqueId(), snapshot);

        List<SnapshotItem> snapshotItems = snapshot.getSnapshotData().getSnapshotItems();
        assertEquals(1, snapshotItems.size());
        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue(), 0.01);
        assertEquals(8.8, ((VDouble) snapshotItems.get(0).getReadbackValue()).getValue(), 0.01);

        List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
        assertEquals(1, snapshots.size());

        nodeDAO.deleteNode(snapshot.getSnapshotNode().getUniqueId());

        snapshots = nodeDAO.getSnapshots(config.getUniqueId());
        assertTrue(snapshots.isEmpty());

        clearAllData();
    }

    @Test
    public void testUpdateSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();

        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder()
                .pvName("whatever").readbackPvName("readback_whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(8.8, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder()
                .name("snapshot name")
                .userName("user")
                .description("comment")
                .build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(config.getUniqueId(), snapshot);

        List<SnapshotItem> snapshotItems = snapshot.getSnapshotData().getSnapshotItems();
        assertEquals(1, snapshotItems.size());
        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue(), 0.01);
        assertEquals(8.8, ((VDouble) snapshotItems.get(0).getReadbackValue()).getValue(), 0.01);

        List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
        assertEquals(1, snapshots.size());

        Node snapshotNode = snapshot.getSnapshotNode();
        snapshotNode.setName("other snapshot name");
        snapshotNode.setDescription("other comment");

        snapshot.setSnapshotNode(snapshotNode);

        snapshot = nodeDAO.updateSnapshot(snapshot);

        snapshotNode = snapshot.getSnapshotNode();
        assertEquals("other snapshot name", snapshotNode.getName());
        assertEquals("other comment", snapshotNode.getDescription());

        clearAllData();
    }

    @Test
    public void testGetSnapshotsNoSnapshots() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getSnapshots("a"));
    }

    @Test
    public void testGetSnapshotItemsWithNullPvValues() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder()
                .name("folder2")
                .build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder()
                .pvName("whatever").readbackPvName("readback_whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder()
                .name("name")
                .userName("user")
                .description("comment")
                .build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(config.getUniqueId(), snapshot);

        assertEquals(7.7, ((VDouble) snapshot.getSnapshotData().getSnapshotItems().get(0).getValue()).getValue(), 0.01);
        assertNull(snapshot.getSnapshotData().getSnapshotItems().get(0).getReadbackValue());

        Snapshot snapshot1 = new Snapshot();
        snapshot1.setSnapshotNode(Node.builder()
                .name("name2")
                .userName("user")
                .description("comment")
                .build());
        SnapshotData snapshotData1 = new SnapshotData();
        snapshot1.setSnapshotData(snapshotData1);

        snapshot1 = nodeDAO.createSnapshot(config.getUniqueId(), snapshot1);

        assertNull(snapshot1.getSnapshotData().getSnapshotItems());

        clearAllData();
    }

    @Test
    public void testSnapshotTag() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("testFolder").nodeType(NodeType.FOLDER).build());

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().name("testConfiguration").nodeType(NodeType.CONFIGURATION).build());
        configuration.setConfigurationData(new ConfigurationData());

        Node configNode = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration).getConfigurationNode();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("testSnapshot").nodeType(NodeType.SNAPSHOT).build());
        snapshot.setSnapshotData(new SnapshotData());

        Node snapshotNode = nodeDAO.createSnapshot(configNode.getUniqueId(), snapshot).getSnapshotNode();

        Tag tag = Tag.builder().name("tag1").comment("comment1").userName("testUser1").build();
        snapshotNode.addTag(tag);

        snapshotNode = nodeDAO.updateNode(snapshotNode, false);
        assertEquals(1, snapshotNode.getTags().size());
        assertEquals("tag1", snapshotNode.getTags().get(0).getName());
        assertEquals("comment1", snapshotNode.getTags().get(0).getComment());
        assertEquals("testUser1", snapshotNode.getTags().get(0).getUserName());

        // Adding the same named tag doesn't affect anything.
        tag = Tag.builder().name("tag1").comment("comment2").userName("testUser2").build();
        snapshotNode.addTag(tag);

        snapshotNode = nodeDAO.updateNode(snapshotNode, false);
        assertEquals(1, snapshotNode.getTags().size());
        assertEquals("tag1", snapshotNode.getTags().get(0).getName());
        assertEquals("comment1", snapshotNode.getTags().get(0).getComment());
        assertEquals("testUser1", snapshotNode.getTags().get(0).getUserName());

        snapshotNode.removeTag(tag);

        snapshotNode = nodeDAO.updateNode(snapshotNode, false);
        assertEquals(0, snapshotNode.getTags().size());

        // Create another snapshot, then test Tag management

        Snapshot snapshot2 = new Snapshot();
        snapshot2.setSnapshotNode(Node.builder().name("testSnapshot2").nodeType(NodeType.SNAPSHOT).build());
        snapshot2.setSnapshotData(new SnapshotData());

        Node snapshotNode2 = nodeDAO.createSnapshot(configNode.getUniqueId(), snapshot2).getSnapshotNode();

        Tag newTag = Tag.builder().name("newtag").comment("comment1").userName("testUser1").build();

        TagData tagData = new TagData();
        tagData.setTag(newTag);
        tagData.setUniqueNodeIds(Arrays.asList(snapshotNode.getUniqueId(), snapshotNode2.getUniqueId(), "non-existing"));

        List<Node> updatedNodes = nodeDAO.addTag(tagData);
        assertEquals(2, updatedNodes.size());
        Node n1 = updatedNodes.get(0);
        List<Tag> tagList1 = n1.getTags();
        assertTrue(tagList1.stream().anyMatch(t -> t.getName().equals(newTag.getName())));
        Node n2 = updatedNodes.get(1);
        List<Tag> tagList2 = n2.getTags();
        assertTrue(tagList2.stream().anyMatch(t -> t.getName().equals(newTag.getName())));

        updatedNodes = nodeDAO.deleteTag(tagData);
        assertEquals(2, updatedNodes.size());
        n1 = updatedNodes.get(0);
        tagList1 = n1.getTags();
        assertFalse(tagList1.stream().anyMatch(t -> t.getName().equals(newTag.getName())));
        n2 = updatedNodes.get(1);
        tagList2 = n2.getTags();
        assertFalse(tagList2.stream().anyMatch(t -> t.getName().equals(newTag.getName())));

        clearAllData();
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
    public void testUpdateNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderA = Node.builder().name("folder A").build();
        folderA = nodeDAO.createNode(rootNode.getUniqueId(), folderA);
        String uniqueId = folderA.getUniqueId();
        folderA.setName("folderB");
        folderA = nodeDAO.updateNode(folderA, false);

        assertEquals("folderB", folderA.getName());
        assertEquals(uniqueId, folderA.getUniqueId());

        clearAllData();

    }

    @Test
    public void testUpdateConfig() throws Exception {

        Node rootNode = nodeDAO.getRootNode();

        Node folder = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("SomeFolder").build());

        Node config = Node.builder().name("My config").nodeType(NodeType.CONFIGURATION).name("name").build();

        ConfigPv configPv1 = ConfigPv.builder().pvName("configPv1").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("configPv2").build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(configPv1, configPv2));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folder.getUniqueId(), configuration);

        Date lastModified;

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        SnapshotItem item2 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(1))
                .value(VInt.of(7, alarm, time, display)).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("name").userName("user").description("comment").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(item1, item2));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(config.getUniqueId(), snapshot);

        // Save another snapshot with same data
        Snapshot snapshot1 = new Snapshot();
        snapshot1.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("name1").userName("user").description("comment").build());
        SnapshotData snapshotData1 = new SnapshotData();
        snapshotData1.setSnapshotItems(Arrays.asList(item1, item2));
        snapshot1.setSnapshotData(snapshotData1);

        nodeDAO.createSnapshot(config.getUniqueId(), snapshot1);

        List<SnapshotItem> snapshotItems = snapshot.getSnapshotData().getSnapshotItems();

        assertEquals(7.7, ((VDouble) snapshotItems.get(0).getValue()).getValue(), 0.01);
        assertEquals(7, ((VInt) snapshotItems.get(1).getValue()).getValue().intValue());

        List<Node> snapshots = nodeDAO.getSnapshots(config.getUniqueId());
        assertFalse(snapshots.isEmpty());
        assertEquals(2, snapshots.size());

        lastModified = config.getLastModified();

        Thread.sleep(100);
        Node updatedConfig = nodeDAO.updateNode(config, false);

        assertNotEquals(lastModified, updatedConfig.getLastModified());

        // Verify that last modified time has been updated
        assertTrue(updatedConfig.getLastModified().getTime() > lastModified.getTime());

        ConfigPv configPv3 = ConfigPv.builder().pvName("configPv3").build();
        configurationData.setPvList(Arrays.asList(configPv1, configPv2, configPv3));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.updateConfiguration(configuration);
        // Verify the list of PVs
        assertEquals(3, configuration.getConfigurationData().getPvList().size());
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
    public void testCreateNodeWithNonNullUniqueId() {
        Node rootNode = nodeDAO.getRootNode();
        Node folder = Node.builder().name("Folder").nodeType(NodeType.FOLDER).uniqueId("uniqueid").build();
        folder = nodeDAO.createNode(rootNode.getUniqueId(), folder);
        assertEquals("uniqueid", folder.getUniqueId());
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
    public void testCreateSnapshotInFolderNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("folder").build());
        Node snapshotNode = new Node();
        snapshotNode.setName("Snapshot");
        snapshotNode.setNodeType(NodeType.SNAPSHOT);

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.createNode(folderNode.getUniqueId(), snapshotNode));
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
    public void testGetFromPathTwoNodes() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.CONFIGURATION).name("c").build());

        List<Node> nodes = nodeDAO.getFromPath("/a/b/c");
        assertEquals(2, nodes.size());

        clearAllData();
    }

    @Test
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

        clearAllData();
    }

    @Test
    public void testGetFromPathZeroNodes() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        List<Node> nodes = nodeDAO.getFromPath("/a/b/d");
        assertNull(nodes);

        nodes = nodeDAO.getFromPath("/a/x/c");
        assertNull(nodes);

        clearAllData();
    }

    @Test
    public void testGetFullPathNullNodeId() {
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.getFullPath(null));
    }

    @Test
    public void testGetFullPathEmptyNodeId() {
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.getFullPath(""));
    }

    @Test
    public void testGetFullPathInvalidNodeId() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getFullPath("invalid"));
    }

    @Test
    public void testGetFullPathNonExistingNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());
        // This will throw NodeNotFoundException
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.getFullPath("nonExisting"));

        clearAllData();
    }

    @Test
    public void testGetFullPathRootNode() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/", nodeDAO.getFullPath(rootNode.getUniqueId()));

        clearAllData();
    }

    @Test
    public void testGetFullPath() {
        Node rootNode = nodeDAO.getRootNode();
        Node a = nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("a").build());
        Node b = nodeDAO.createNode(a.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("b").build());
        Node c = nodeDAO.createNode(b.getUniqueId(), Node.builder().nodeType(NodeType.FOLDER).name("c").build());

        assertEquals("/a/b/c", nodeDAO.getFullPath(c.getUniqueId()));

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
        nodeDAO.createNode(rootNode.getUniqueId(), node2);

        List<String> nodeIds = Arrays.asList(node1.getUniqueId(), "non-existing");

        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.moveNodes(nodeIds, rootNode.getUniqueId(), "userName"));

        clearAllData();
    }

    @Test
    public void testMoveNodesToNonExistingTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.moveNodes(Collections.emptyList(), "non existing", "user"));
    }

    @Test
    public void testMoveConfiguration() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);

        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node topLevelFolderNode2 =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder 2").build());

        topLevelFolderNode2 = nodeDAO.moveNodes(List.of(configNode.getUniqueId()), topLevelFolderNode2.getUniqueId(), "user");

        assertEquals(1, nodeDAO.getChildNodes(topLevelFolderNode2.getUniqueId()).size());
        assertEquals(0, nodeDAO.getChildNodes(topLevelFolderNode.getUniqueId()).size());

        clearAllData();
    }

    @Test
    public void testMoveConfigurationNameClash() {
        Node rootNode = nodeDAO.getRootNode();

        Node topLevelFolderNode =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder").build());

        Node configNode = new Node();
        configNode.setName("Config");
        configNode.setNodeType(NodeType.CONFIGURATION);

        configNode = nodeDAO.createNode(topLevelFolderNode.getUniqueId(), configNode);

        Node configNode2 = new Node();
        configNode2.setName("Config");
        configNode2.setNodeType(NodeType.CONFIGURATION);

        Node topLevelFolderNode2 =
                nodeDAO.createNode(rootNode.getUniqueId(), Node.builder().name("top level folder 2").build());

        nodeDAO.createNode(topLevelFolderNode2.getUniqueId(), configNode2);

        Node _configNode = configNode;

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.moveNodes(List.of(_configNode.getUniqueId()), topLevelFolderNode2.getUniqueId(), "user"));

        clearAllData();
    }

    @Test
    public void testMoveSnasphotToRoot() {
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
    public void testMoveSnasphotToConfiguration() {
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

        Node folderNode3 = new Node();
        folderNode3.setName("Folder 3");
        folderNode3.setNodeType(NodeType.FOLDER);
        // Create node, but do not include in move
        nodeDAO.createNode(folderNode.getUniqueId(), folderNode3);

        assertEquals(1, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());

        rootNode = nodeDAO.moveNodes(Arrays.asList(folderNode1.getUniqueId(), folderNode2.getUniqueId()), rootNode.getUniqueId(), "user");

        // Target node now has 3 child elements
        assertEquals(3, nodeDAO.getChildNodes(rootNode.getUniqueId()).size());

        // After move parent of source nodes should now have only one element
        assertEquals(1, nodeDAO.getChildNodes(folderNode.getUniqueId()).size());

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
    public void testCopyFolderNotSupported() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = new Node();
        folderNode.setName("Folder");
        folderNode.setNodeType(NodeType.FOLDER);
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node childFolderNode = new Node();
        childFolderNode.setName("Child Folder");
        childFolderNode.setNodeType(NodeType.FOLDER);
        childFolderNode = nodeDAO.createNode(folderNode.getUniqueId(), childFolderNode);

        Node _childFolderNode = childFolderNode;

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(_childFolderNode.getUniqueId()), rootNode.getUniqueId(), "username"));

        clearAllData();
    }

    @Test
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

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        nodeDAO.copyNodes(List.of(configuration.getConfigurationNode().getUniqueId()), folderNode2.getUniqueId(), "username");

        List<Node> childNodes = nodeDAO.getChildNodes(rootNode.getUniqueId());
        assertEquals(2, childNodes.size());

        clearAllData();
    }

    @Test
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

        String f1 = childFolder1.getUniqueId();
        String f2 = childFolder2.getUniqueId();

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(Arrays.asList(f1, f2), rootNode.getUniqueId(), "username"));

        clearAllData();
    }

    @Test
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

        clearAllData();
    }

    @Test
    public void testCopySnapshotToFolderNotSupported() {

        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder").build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("snapshotName").description("comment").userName("userName").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        String snapshotId = snapshot.getSnapshotNode().getUniqueId();

        Node folderNode1 = new Node();
        folderNode1.setName("Folder1");
        folderNode1.setNodeType(NodeType.FOLDER);
        folderNode1 = nodeDAO.createNode(rootNode.getUniqueId(), folderNode1);

        String uniqueId = folderNode1.getUniqueId();
        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(snapshotId), uniqueId, "username"));

        clearAllData();
    }

    @Test
    public void testCopySnapshotToConfiguration() {

        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder").build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        Node config2 = Node.builder().name("My config 4").nodeType(NodeType.CONFIGURATION).build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        configuration2.setConfigurationData(configurationData);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("snapshotName").description("comment").userName("userName").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        String snapshotId = snapshot.getSnapshotNode().getUniqueId();

        Node updatedConfigNode =
                nodeDAO.copyNodes(List.of(snapshotId), configuration2.getConfigurationNode().getUniqueId(), "useername");

        assertEquals(1, nodeDAO.getChildNodes(updatedConfigNode.getUniqueId()).size());

        clearAllData();
    }

    @Test
    public void testCopySnapshotToConfigurationPvListMismatch() {

        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder").build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        Node config2 = Node.builder().name("My config 4").nodeType(NodeType.CONFIGURATION).build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(List.of(ConfigPv.builder().pvName("non-matching").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("snapshotName").description("comment").userName("userName").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        String snapshotId = snapshot.getSnapshotNode().getUniqueId();
        String config2Id = configuration2.getConfigurationNode().getUniqueId();

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(snapshotId), config2Id, "userName"));

        clearAllData();
    }

    @Test
    public void testCopyCompositeSnapshot() {

        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder").build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("snapshotName").description("comment").userName("userName").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);


        Node folderNode1 = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder 1").build());

        Node node = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshot.getSnapshotNode().getUniqueId()));

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(node);
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);

        String compositeSnapshotId = compositeSnapshot.getCompositeSnapshotNode().getUniqueId();

        folderNode1 = nodeDAO.copyNodes(List.of(compositeSnapshotId), folderNode1.getUniqueId(), "user");

        List<Node> childNodes = nodeDAO.getChildNodes(folderNode1.getUniqueId());

        assertEquals(1, childNodes.size());
        // Make sure referenced nodes have been copied to copied composite snapshot
        assertEquals(1, nodeDAO.getCompositeSnapshotData(childNodes.get(0).getUniqueId()).getReferencedSnapshotNodes().size());

        nodeDAO.deleteNode(childNodes.get(0).getUniqueId());
        nodeDAO.deleteNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        clearAllData();
    }

    @Test
    public void testCopyCompositeSnapshotToConfiguration() {

        Node rootNode = nodeDAO.getRootNode();
        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder").build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().name("snapshotName").description("comment").userName("userName").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        snapshot = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        Node config2 = Node.builder().name("My config 4").nodeType(NodeType.CONFIGURATION).build();

        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        Node folderNode1 = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder().name("Folder 1").build());

        Node node = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshot.getSnapshotNode().getUniqueId()));

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(node);
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);

        String compositeSnapshotId = compositeSnapshot.getCompositeSnapshotNode().getUniqueId();

        String config2Id = configuration2.getConfigurationNode().getUniqueId();

        assertThrows(IllegalArgumentException.class,
                () -> nodeDAO.copyNodes(List.of(compositeSnapshotId), config2Id, "user"));

        nodeDAO.deleteNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        clearAllData();
    }

    @Test
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
    public void testDeleteNodeInvalid() {
        assertThrows(NodeNotFoundException.class,
                () -> nodeDAO.deleteNode("invalid"));
    }

    @Test
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

    @Test
    public void testIsContainedInSubTree() {
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

        Node L3F1 = new Node();
        L3F1.setName("L3F1");
        L3F1.setNodeType(NodeType.FOLDER);
        L3F1 = nodeDAO.createNode(L2F1.getUniqueId(), L3F1);

        Node L2F2 = new Node();
        L2F2.setName("L2F2");
        L2F2.setNodeType(NodeType.FOLDER);
        L2F2 = nodeDAO.createNode(L1F1.getUniqueId(), L2F2);

        assertTrue(nodeDAO.isContainedInSubtree(rootNode.getUniqueId(), L2F2.getUniqueId()));
        assertTrue(nodeDAO.isContainedInSubtree(rootNode.getUniqueId(), L3F1.getUniqueId()));
        assertTrue(nodeDAO.isContainedInSubtree(L1F1.getUniqueId(), L3F1.getUniqueId()));
        assertFalse(nodeDAO.isContainedInSubtree(L1F1.getUniqueId(), L1F2.getUniqueId()));
        assertFalse(nodeDAO.isContainedInSubtree(L2F2.getUniqueId(), rootNode.getUniqueId()));
        assertFalse(nodeDAO.isContainedInSubtree(L1F1.getUniqueId(), L1F1.getUniqueId()));
        assertFalse(nodeDAO.isContainedInSubtree(L2F1.getUniqueId(), L1F1.getUniqueId()));

        clearAllData();
    }

    @Test
    public void testGetAllTags() {
        Node rootNode = nodeDAO.getRootNode();

        Tag goldenTag = Tag.goldenTag("user");
        Tag tag1 = Tag.builder().name("name1").comment("comment1").userName("user1").build();
        Tag tag2 = Tag.builder().name("name2").comment("comment2").userName("user2").build();

        Node L1F1 = new Node();
        L1F1.setName("L1F1");
        L1F1.setNodeType(NodeType.FOLDER);
        L1F1.setTags(Collections.singletonList(goldenTag));
        L1F1 = nodeDAO.createNode(rootNode.getUniqueId(), L1F1);

        Node L1F2 = new Node();
        L1F2.setName("L1F2");
        L1F2.setNodeType(NodeType.FOLDER);
        L1F2.setTags(Collections.singletonList(tag1));
        nodeDAO.createNode(rootNode.getUniqueId(), L1F2);

        Node L2F1 = new Node();
        L2F1.setName("L2F1");
        L2F1.setNodeType(NodeType.FOLDER);
        L2F1.setTags(Arrays.asList(goldenTag, tag2));
        L2F1 = nodeDAO.createNode(L1F1.getUniqueId(), L2F1);

        Node L3F1 = new Node();
        L3F1.setName("L3F1");
        L3F1.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(L2F1.getUniqueId(), L3F1);

        Node L2F2 = new Node();
        L2F2.setName("L2F2");
        L2F2.setNodeType(NodeType.FOLDER);
        nodeDAO.createNode(L1F1.getUniqueId(), L2F2);

        List<Tag> tags = nodeDAO.getAllTags();

        assertEquals(4, tags.size());

        clearAllData();
    }

    @Test
    public void testGetAllSnapshots() {
        Node rootNode = nodeDAO.getRootNode();

        Node folderNode = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder()
                        .name("Folder")
                        .build());

        Node config = Node.builder().name("My config 3").nodeType(NodeType.CONFIGURATION).build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("whatever").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).readbackValue(VDouble.of(7.7, alarm, time, display))
                .build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .name("snapshotName")
                .userName("userName")
                .description("comment")
                .build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(item1));
        snapshot.setSnapshotData(snapshotData);

        nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot);

        List<Node> snapshotNodes = nodeDAO.getAllSnapshots();
        assertEquals(1, snapshotNodes.size());

        clearAllData();
    }

    @Test
    public void testGetAllNodes() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode1 = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder()
                        .name("Folder1")
                        .build());
        Node folderNode2 = nodeDAO.createNode(rootNode.getUniqueId(),
                Node.builder()
                        .name("Folder2")
                        .build());

        List<Node> nodes = nodeDAO.getNodes(Arrays.asList(folderNode1.getUniqueId(), folderNode2.getUniqueId()));

        assertEquals(2, nodes.size());

        clearAllData();
    }

    /**
     * Deletes all objects in all indices.
     */
    private void clearAllData() {
        List<Node> childNodes = nodeDAO.getChildNodes(Node.ROOT_FOLDER_UNIQUE_ID);
        childNodes.forEach(node -> nodeDAO.deleteNode(node.getUniqueId()));
        nodeDAO.deleteAllFilters();

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

    @Test
    public void testCheckForPVNameDuplicates() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        //************  Create snapshot1 ************/
        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 1").build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv1").build(),
                ConfigPv.builder().pvName("pv2").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item2 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(item1, item2));
        snapshot.setSnapshotData(snapshotData);
        Node newSnapshot1 = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot).getSnapshotNode();
        //************  End create snapshot1 ************/

        //************  Create snapshot2 ************/
        Node config2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 2").build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv1").build(),
                ConfigPv.builder().pvName("pv3").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        SnapshotItem item12 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item22 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot2 = new Snapshot();
        snapshot2.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name 2")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData2 = new SnapshotData();
        snapshotData2.setSnapshotItems(Arrays.asList(item12, item22));
        snapshot2.setSnapshotData(snapshotData2);
        Node newSnapshot2 = nodeDAO.createSnapshot(configuration2.getConfigurationNode().getUniqueId(), snapshot2).getSnapshotNode();
        //************  End create snapshot2 ************/

        List<String> duplicates = nodeDAO.checkForPVNameDuplicates(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId()));

        assertEquals(1, duplicates.size());
        assertEquals("pv1", duplicates.get(0));

        //************  Create snapshot3 ************/
        Node config3 = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 3").build();
        Configuration configuration3 = new Configuration();
        configuration3.setConfigurationNode(config3);
        ConfigurationData configurationData3 = new ConfigurationData();
        configurationData3.setPvList(Collections.singletonList(ConfigPv.builder().pvName("pv4").build()));
        configuration3.setConfigurationData(configurationData3);

        configuration3 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration3);

        SnapshotItem item13 = SnapshotItem.builder().configPv(configuration3.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot3 = new Snapshot();
        snapshot3.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name 3")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData3 = new SnapshotData();
        snapshotData3.setSnapshotItems(Collections.singletonList(item13));
        snapshot3.setSnapshotData(snapshotData3);
        Node newSnapshot3 = nodeDAO.createSnapshot(configuration3.getConfigurationNode().getUniqueId(), snapshot3).getSnapshotNode();
        //************  End create snapshot3 ************/

        duplicates = nodeDAO.checkForPVNameDuplicates(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId(),
                snapshot3.getSnapshotNode().getUniqueId()));

        assertEquals(1, duplicates.size());
        assertEquals("pv1", duplicates.get(0));

        //************  Create composite snapshot ************/
        Node compositeSnapshotNode = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());

        compositeSnapshotData.setReferencedSnapshotNodes(Arrays.asList(snapshot2.getSnapshotNode().getUniqueId(),
                snapshot3.getSnapshotNode().getUniqueId()));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);
        //************  End create composite snapshot ************/

        duplicates = nodeDAO.checkForPVNameDuplicates(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                compositeSnapshot.getCompositeSnapshotNode().getUniqueId()));

        assertEquals(1, duplicates.size());
        assertEquals("pv1", duplicates.get(0));

        nodeDAO.deleteNode(compositeSnapshotNode.getUniqueId());

        clearAllData();
    }

    @Test
    public void testCheckForRejectedReferencedNodesInCompositeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        //************  Create snapshot1 ************/
        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 1").build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv1").build(),
                ConfigPv.builder().pvName("pv2").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item2 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(item1, item2));
        snapshot.setSnapshotData(snapshotData);
        Node newSnapshot1 = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot).getSnapshotNode();
        //************  End create snapshot1 ************/

        //************  Create snapshot2 ************/
        Node config2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 2").build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv11").build(),
                ConfigPv.builder().pvName("pv12").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        SnapshotItem item12 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item22 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot2 = new Snapshot();
        snapshot2.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name 2")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData2 = new SnapshotData();
        snapshotData2.setSnapshotItems(Arrays.asList(item12, item22));
        snapshot2.setSnapshotData(snapshotData2);
        Node newSnapshot2 = nodeDAO.createSnapshot(configuration2.getConfigurationNode().getUniqueId(), snapshot2).getSnapshotNode();
        //************  End create snapshot2 ************/


        //************  Create composite snapshot ************/
        Node compositeSnapshotNode = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());

        compositeSnapshotData.setReferencedSnapshotNodes(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId()));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        //************  End create composite snapshot ************/

        assertTrue(nodeDAO.checkCompositeSnapshotReferencedNodeTypes(compositeSnapshot));

        compositeSnapshotData.setReferencedSnapshotNodes(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId(), config.getUniqueId()));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        assertFalse(nodeDAO.checkCompositeSnapshotReferencedNodeTypes(compositeSnapshot));

        clearAllData();
    }

    @Test
    public void testGetSnapshotItemsFromCompositeSnapshot() {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        //************  Create snapshot1 ************/
        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 1").build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv1").build(),
                ConfigPv.builder().pvName("pv2").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        SnapshotItem item1 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item2 = SnapshotItem.builder().configPv(configuration.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(item1, item2));
        snapshot.setSnapshotData(snapshotData);
        Node newSnapshot1 = nodeDAO.createSnapshot(configuration.getConfigurationNode().getUniqueId(), snapshot).getSnapshotNode();
        //************  End create snapshot1 ************/

        //************  Create snapshot2 ************/
        Node config2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("My config 2").build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv12").build(),
                ConfigPv.builder().pvName("pv22").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        SnapshotItem item12 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(0))
                .value(VDouble.of(7.7, alarm, time, display)).build();
        SnapshotItem item22 = SnapshotItem.builder().configPv(configuration2.getConfigurationData().getPvList().get(1))
                .value(VDouble.of(7.7, alarm, time, display)).build();

        Snapshot snapshot2 = new Snapshot();
        snapshot2.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).name("snapshot name 2")
                .description("comment")
                .userName("user").build());
        SnapshotData snapshotData2 = new SnapshotData();
        snapshotData2.setSnapshotItems(Arrays.asList(item12, item22));
        snapshot2.setSnapshotData(snapshotData2);
        Node newSnapshot2 = nodeDAO.createSnapshot(configuration2.getConfigurationNode().getUniqueId(), snapshot2).getSnapshotNode();
        //************  End create snapshot2 ************/

        //************  Create composite snapshot ************/
        Node compositeSnapshotNode = Node.builder().name("My composite snapshot").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();

        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setUniqueId(compositeSnapshotNode.getUniqueId());

        compositeSnapshotData.setReferencedSnapshotNodes(Arrays.asList(snapshot.getSnapshotNode().getUniqueId(),
                snapshot2.getSnapshotNode().getUniqueId()));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);

        compositeSnapshot = nodeDAO.createCompositeSnapshot(folderNode.getUniqueId(), compositeSnapshot);
        //************  End create composite snapshot ************/

        List<SnapshotItem> snapshotItems = nodeDAO.getSnapshotItemsFromCompositeSnapshot(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());

        assertEquals(4, snapshotItems.size());

        nodeDAO.deleteNode(compositeSnapshotNode.getUniqueId());

        clearAllData();
    }

    @Test
    public void testFilters() {
        Filter filter = new Filter();
        filter.setName("name");
        filter.setQueryString("name=aName");
        filter.setUser("user");

        filter = nodeDAO.saveFilter(filter);
        assertEquals("name", filter.getName());
        assertEquals("name=aName", filter.getQueryString());
        assertEquals("user", filter.getUser());
        assertNotNull(filter.getLastUpdated());

        filter.setQueryString("type=Snapshot");
        filter = nodeDAO.saveFilter(filter);
        assertEquals("type=Snapshot", filter.getQueryString());

        assertEquals(1, nodeDAO.getAllFilters().size());

        nodeDAO.deleteFilter("name");

        assertEquals(0, nodeDAO.getAllFilters().size());

        Filter filter2 = new Filter();
        filter2.setName("name");
        filter2.setQueryString("user=John");

        nodeDAO.saveFilter(filter);
        nodeDAO.saveFilter(filter2);

        assertEquals(1, nodeDAO.getAllFilters().size());

        filter2.setName("name2");

        nodeDAO.saveFilter(filter2);

        assertEquals(2, nodeDAO.getAllFilters().size());

        nodeDAO.deleteAllFilters();

        assertEquals(0, nodeDAO.getAllFilters().size());

        Filter unformattedQueryStringFilter = new Filter();
        unformattedQueryStringFilter.setName("name");
        unformattedQueryStringFilter.setUser("user");
        unformattedQueryStringFilter.setQueryString("type=Folder, Configuration&unsupported=value");

        unformattedQueryStringFilter = nodeDAO.saveFilter(unformattedQueryStringFilter);
        assertTrue(unformattedQueryStringFilter.getQueryString().contains("type=Folder,Configuration"));
        assertFalse(unformattedQueryStringFilter.getQueryString().contains("unsupoorted"));

        clearAllData();
    }

    @Test
    public void testSearchForPvs()  {
        Node rootNode = nodeDAO.getRootNode();
        Node folderNode =
                Node.builder().name("folder").build();
        folderNode = nodeDAO.createNode(rootNode.getUniqueId(), folderNode);

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("Myconfig1").build();
        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv1").build(),
                ConfigPv.builder().pvName("pv2").build()));
        configuration.setConfigurationData(configurationData);

        configuration = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration);

        Node config2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("Myconfig2").build();
        Configuration configuration2 = new Configuration();
        configuration2.setConfigurationNode(config2);
        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(Arrays.asList(ConfigPv.builder().pvName("pv12").build(),
                ConfigPv.builder().pvName("pv22").readbackPvName("readbackpv22").build()));
        configuration2.setConfigurationData(configurationData2);

        configuration2 = nodeDAO.createConfiguration(folderNode.getUniqueId(), configuration2);

        MultiValueMap<String, String> searchParameters = new LinkedMultiValueMap<>();

        searchParameters.put("pvs", List.of("pv1", "pv22", "pv12"));

        SearchResult searchResult = nodeDAO.search(searchParameters);
        assertEquals(2, searchResult.getHitCount());
        assertEquals(configuration.getConfigurationNode().getUniqueId(), searchResult.getNodes().get(0).getUniqueId());
        assertEquals(configuration2.getConfigurationNode().getUniqueId(), searchResult.getNodes().get(1).getUniqueId());

        searchParameters.put("name", List.of("Myconfig2"));
        searchResult = nodeDAO.search(searchParameters);
        assertEquals(1, searchResult.getHitCount());

        searchParameters.clear();

        searchParameters.put("pvs", List.of("pv1", "pv2"));
        searchResult = nodeDAO.search(searchParameters);
        assertEquals(1, searchResult.getHitCount());

        searchParameters.put("pvs", List.of("readbackpv22"));
        searchResult = nodeDAO.search(searchParameters);
        assertEquals(1, searchResult.getHitCount());

        searchParameters.put("pvs", List.of("invalid"));
        searchResult = nodeDAO.search(searchParameters);
        assertEquals(0, searchResult.getHitCount());

        searchParameters.clear();
        searchResult = nodeDAO.search(searchParameters);
        // No pvs specified -> find all nodes.
        assertEquals(4, searchResult.getHitCount());

        clearAllData();

    }
}
