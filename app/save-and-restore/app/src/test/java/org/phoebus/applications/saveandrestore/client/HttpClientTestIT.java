/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.applications.saveandrestore.client;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VInt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.authentication.SaveAndRestoreAuthenticationScope;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.security.PhoebusSecurity;
import org.phoebus.security.store.SecureStore;
import org.phoebus.security.store.SecureStoreTarget;
import org.phoebus.security.tokens.AuthenticationScope;
import org.phoebus.security.tokens.ScopedAuthenticationToken;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.net.ConnectException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test to verify that save&restore client supports all
 * calls to the service.
 *
 * To run it, the save-and-restore service must be up and running on localhost port 8080,
 * and must apply the in-memory (hard-coded) authentication scheme.
 *
 * Disable by default, to enable:
 *
 * mvn -DskipITs=false ...
 */
public class HttpClientTestIT {

    private static SaveAndRestoreClient saveAndRestoreClient;
    private static Node topLevelTestNode;

    @BeforeAll
    public static void init() {
        PhoebusSecurity.secure_store_target = SecureStoreTarget.IN_MEMORY;
        try {
            SecureStore store = new SecureStore();
            ScopedAuthenticationToken scopedAuthenticationToken =
                    new ScopedAuthenticationToken(new SaveAndRestoreAuthenticationScope(), "admin", "adminPass");
            store.setScopedAuthentication(scopedAuthenticationToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        saveAndRestoreClient = new SaveAndRestoreClientImpl();
        Node node = Node.builder()
                .name("IT test folder")
                .build();
        Node rootNode = saveAndRestoreClient.getRoot();
        List<Node> nodes = saveAndRestoreClient.getChildNodes(rootNode.getUniqueId());

        nodes.stream().forEach(n -> {
            if (n.getName().equals("IT test folder")) {
                saveAndRestoreClient.deleteNodes(List.of(n.getUniqueId()));
            }
        });

        topLevelTestNode = saveAndRestoreClient.createNewNode(rootNode.getUniqueId(), node);
    }

    @AfterAll
    public static void cleanUp() {
        saveAndRestoreClient.deleteNodes(List.of(topLevelTestNode.getUniqueId()));
    }

    @Test
    public void testAuthenticate() throws Exception {
        saveAndRestoreClient.authenticate("admin", "adminPass");
    }

    @Test
    public void testCreateFoldersAndMove() {
        Node node = Node.builder()
                .name("Folder 1")
                .build();
        node = saveAndRestoreClient.createNewNode(topLevelTestNode.getUniqueId(), node);

        Node node2 = Node.builder()
                .name("Folder 2")
                .build();
        node2 = saveAndRestoreClient.createNewNode(topLevelTestNode.getUniqueId(), node2);

        saveAndRestoreClient.moveNodes(List.of(node2.getUniqueId()), node.getUniqueId());
        saveAndRestoreClient.deleteNodes(List.of(node.getUniqueId()));
    }

    @Test
    public void testCreateAndUpdateConfiguration() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        node = configuration.getConfigurationNode();
        node.setName("another");
        node.setDescription("Foo");

        configuration = saveAndRestoreClient.updateConfiguration(configuration);
        assertEquals("another", configuration.getConfigurationNode().getName());

        saveAndRestoreClient.deleteNodes(List.of(configuration.getConfigurationNode().getUniqueId()));
    }

    @Test
    public void testCreateTagAndUpdateSnapshot() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        SnapshotItem snapshotItem = new SnapshotItem();
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(777, Alarm.none(), Time.now(), Display.none()));
        snapshotItem.setReadbackValue(VInt.of(42, Alarm.none(), Time.now(), Display.none()));

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(snapshotItem));

        Node snapshotNode = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .name("snapshot")
                .description("description")
                .build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotData(snapshotData);
        snapshot.setSnapshotNode(snapshotNode);

        snapshot = saveAndRestoreClient.createSnapshot(configuration.getConfigurationNode().getUniqueId(),
                snapshot);

        snapshotNode = snapshot.getSnapshotNode();
        snapshotNode.setName("another");

        snapshot = saveAndRestoreClient.updateSnapshot(snapshot);

        assertEquals("another", snapshot.getSnapshotNode().getName());

        saveAndRestoreClient.deleteNodes(List.of(configuration.getConfigurationNode().getUniqueId()));
    }

    @Test
    public void testTaggingAndDeleteTags() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        SnapshotItem snapshotItem = new SnapshotItem();
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(777, Alarm.none(), Time.now(), Display.none()));
        snapshotItem.setReadbackValue(VInt.of(42, Alarm.none(), Time.now(), Display.none()));

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(snapshotItem));

        Node snapshotNode = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .name("snapshot")
                .description("description")
                .build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotData(snapshotData);
        snapshot.setSnapshotNode(snapshotNode);

        snapshot = saveAndRestoreClient.createSnapshot(configuration.getConfigurationNode().getUniqueId(),
                snapshot);

        List<Tag> tags = saveAndRestoreClient.getAllTags();
        int countBefore = tags.size();

        TagData tagData = new TagData();
        tagData.setUniqueNodeIds(List.of(snapshot.getSnapshotNode().getUniqueId()));
        tagData.setTag(Tag.goldenTag("admin"));

        saveAndRestoreClient.addTag(tagData);

        tags = saveAndRestoreClient.getAllTags();
        assertEquals(1, tags.size() - countBefore);

        saveAndRestoreClient.deleteTag(tagData);

        tags = saveAndRestoreClient.getAllTags();
        assertEquals(countBefore, tags.size());

        saveAndRestoreClient.deleteNodes(List.of(configuration.getConfigurationNode().getUniqueId()));
    }

    @Test
    public void testCreateAndUpdateCompositeSnapshot() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        SnapshotItem snapshotItem = new SnapshotItem();
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(777, Alarm.none(), Time.now(), Display.none()));
        snapshotItem.setReadbackValue(VInt.of(42, Alarm.none(), Time.now(), Display.none()));

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(snapshotItem));

        Node snapshotNode = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .name("snapshot")
                .description("description")
                .build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotData(snapshotData);
        snapshot.setSnapshotNode(snapshotNode);

        snapshot = saveAndRestoreClient.createSnapshot(configuration.getConfigurationNode().getUniqueId(),
                snapshot);

        Node compositeSnapshotNode = Node.builder()
                .nodeType(NodeType.COMPOSITE_SNAPSHOT)
                .name("composite")
                .description("description")
                .build();
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of(snapshot.getSnapshotNode().getUniqueId()));
        CompositeSnapshot compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);
        compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);

        compositeSnapshot = saveAndRestoreClient.createCompositeSnapshot(topLevelTestNode.getUniqueId(), compositeSnapshot);

        compositeSnapshot.getCompositeSnapshotNode().setName("another");
        compositeSnapshot = saveAndRestoreClient.updateCompositeSnapshot(compositeSnapshot);

        assertEquals("another", compositeSnapshot.getCompositeSnapshotNode().getName());

        saveAndRestoreClient.deleteNodes(List.of(compositeSnapshot.getCompositeSnapshotNode().getUniqueId()));
    }

    @Test
    public void testCreateFilter() {

        List<Filter> filters = saveAndRestoreClient.getAllFilters();
        int countBefore = filters.size();

        Filter filter = new Filter();
        filter.setName("filter");
        filter.setQueryString("name=foo");

        filter = saveAndRestoreClient.saveFilter(filter);
        filters = saveAndRestoreClient.getAllFilters();

        assertEquals(1, filters.size() - countBefore);

        saveAndRestoreClient.deleteFilter(filter.getName());

        filters = saveAndRestoreClient.getAllFilters();

        assertEquals(countBefore, filters.size());
    }

    @Test
    public void testSearch() {
        MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
        map.put("type", List.of("FOLDER"));
        SearchResult searchResult = saveAndRestoreClient.search(map);
        assertTrue(searchResult.getHitCount() > 0);
    }

    @Test
    public void testRestore() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        SnapshotItem snapshotItem = new SnapshotItem();
        snapshotItem.setConfigPv(configPv);
        snapshotItem.setValue(VInt.of(777, Alarm.none(), Time.now(), Display.none()));
        snapshotItem.setReadbackValue(VInt.of(42, Alarm.none(), Time.now(), Display.none()));

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(List.of(snapshotItem));

        Node snapshotNode = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .name("snapshot")
                .description("description")
                .build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotData(snapshotData);
        snapshot.setSnapshotNode(snapshotNode);

        snapshot = saveAndRestoreClient.createSnapshot(configuration.getConfigurationNode().getUniqueId(),
                snapshot);

        List<RestoreResult> results = saveAndRestoreClient.restore(snapshot.getSnapshotNode().getUniqueId());
        assertFalse(results.get(0).getErrorMsg().isEmpty());

        results = saveAndRestoreClient.restore(List.of(snapshotItem));
        assertFalse(results.get(0).getErrorMsg().isEmpty());

        saveAndRestoreClient.deleteNodes(List.of(configuration.getConfigurationNode().getUniqueId()));
    }

    @Test
    public void testTakeSnapshot() {
        Node node = Node.builder()
                .nodeType(NodeType.CONFIGURATION)
                .name("config")
                .description("description")
                .build();

        ConfigurationData configurationData = new ConfigurationData();
        ConfigPv configPv = ConfigPv.builder()
                .pvName("pv")
                .readbackPvName("readback")
                .build();
        configurationData.setPvList(List.of(configPv));
        Configuration configuration = new Configuration();
        configuration.setConfigurationData(configurationData);
        configuration.setConfigurationNode(node);

        configuration = saveAndRestoreClient.createConfiguration(topLevelTestNode.getUniqueId(), configuration);

        List<SnapshotItem> snapshotItems = saveAndRestoreClient.takeSnapshot(configuration.getConfigurationNode().getUniqueId());
        assertEquals(1, snapshotItems.size());
        assertNull(snapshotItems.get(0).getValue());

        saveAndRestoreClient.deleteNodes(List.of(configuration.getConfigurationNode().getUniqueId()));

    }

    /*
    @Test
    public void tetsFoo() throws Exception{
        FileInputStream fileInputStream = new FileInputStream(new File("/Users/georgweiss/tmp/tags"));
        ObjectMapper objectMapper = new ObjectMapper();
        List<Tag> tags = objectMapper.readValue(fileInputStream, new TypeReference<>() {
        });
        System.out.println(tags.size());
        int withComments = 0;
        int nonGolden = 0;
        for(Tag t : tags){
            if(!t.getName().equals("golden")){
                nonGolden++;
            }
            if(t.getComment() != null && !t.getComment().isEmpty()){
                withComments++;
                System.out.println(t.getName() + " " + t.getComment());
            }
        }
        System.out.println(nonGolden);
        System.out.println(withComments);
        fileInputStream.close();
    }

     */
}
