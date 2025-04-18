/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@ContextConfiguration(classes = ElasticConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@Profile("IT")
public class ComparisonControllerTestIT {

    @Autowired
    private ComparisonController comparisonController;

    @Autowired
    private NodeDAO nodeDAO;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetSnapshotItemsAndConfig() throws Exception{
        Node topLevelFolder = Node.builder().nodeType(NodeType.FOLDER).name(UUID.randomUUID().toString())
                        .build();
        topLevelFolder = nodeDAO.createNode(Node.ROOT_FOLDER_UNIQUE_ID, topLevelFolder);

        CompositeSnapshot compositeSnapshot1 = null;
        CompositeSnapshot compositeSnapshot2 = null;

        try {

            Configuration configuration1 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test1-config.json"), Configuration.class);
            configuration1 = nodeDAO.createConfiguration(topLevelFolder.getUniqueId(), configuration1);

            Snapshot snapshot1 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test1-snapshot.json"), Snapshot.class);
            snapshot1 = nodeDAO.createSnapshot(configuration1.getConfigurationNode().getUniqueId(), snapshot1);

            Configuration configuration2 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test2-config.json"), Configuration.class);
            configuration2 = nodeDAO.createConfiguration(topLevelFolder.getUniqueId(), configuration2);

            Snapshot snapshot2 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test2-snapshot.json"), Snapshot.class);
            snapshot2 = nodeDAO.createSnapshot(configuration2.getConfigurationNode().getUniqueId(), snapshot2);

            Configuration configuration3 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test3-config.json"), Configuration.class);
            configuration3 = nodeDAO.createConfiguration(topLevelFolder.getUniqueId(), configuration3);

            Snapshot snapshot3 = objectMapper.readValue(getClass().getResourceAsStream("/IT-test3-snapshot.json"), Snapshot.class);
            snapshot3 = nodeDAO.createSnapshot(configuration3.getConfigurationNode().getUniqueId(), snapshot3);

            Node compositeSnapshotSimple = Node.builder().name("CompositeSimple").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();
            CompositeSnapshotData compositeSnapshotData1 = new CompositeSnapshotData();
            compositeSnapshotData1.setReferencedSnapshotNodes(List.of(snapshot1.getSnapshotNode().getUniqueId(), snapshot2.getSnapshotNode().getUniqueId()));
            compositeSnapshot1 = new CompositeSnapshot();
            compositeSnapshot1.setCompositeSnapshotNode(compositeSnapshotSimple);
            compositeSnapshot1.setCompositeSnapshotData(compositeSnapshotData1);

            compositeSnapshot1 = nodeDAO.createCompositeSnapshot(topLevelFolder.getUniqueId(), compositeSnapshot1);

            Node compositeSnapshotComposite = Node.builder().name("CompositeComposite").nodeType(NodeType.COMPOSITE_SNAPSHOT).build();
            CompositeSnapshotData compositeSnapshotData2 = new CompositeSnapshotData();
            compositeSnapshotData2.setReferencedSnapshotNodes(List.of(compositeSnapshot1.getCompositeSnapshotNode().getUniqueId(), snapshot3.getSnapshotNode().getUniqueId()));
            compositeSnapshot2 = new CompositeSnapshot();
            compositeSnapshot2.setCompositeSnapshotNode(compositeSnapshotComposite);
            compositeSnapshot2.setCompositeSnapshotData(compositeSnapshotData2);

            compositeSnapshot2 = nodeDAO.createCompositeSnapshot(topLevelFolder.getUniqueId(), compositeSnapshot2);

            List<SnapshotItem> snapshotItems = new ArrayList<>();
            List<ConfigPv> configPvs = new ArrayList<>();

            comparisonController.getCompositeSnapshotItemsAndConfig(compositeSnapshot1.getCompositeSnapshotNode().getUniqueId(), snapshotItems, configPvs);

            assertEquals(snapshotItems.size(), configPvs.size());

            snapshotItems.clear();
            configPvs.clear();

            comparisonController.getCompositeSnapshotItemsAndConfig(compositeSnapshot2.getCompositeSnapshotNode().getUniqueId(), snapshotItems, configPvs);

            assertEquals(snapshotItems.size(), configPvs.size());

        } finally {
            if(compositeSnapshot1 != null && compositeSnapshot1.getCompositeSnapshotNode() != null && compositeSnapshot1.getCompositeSnapshotNode().getUniqueId() != null){
                nodeDAO.deleteNode(compositeSnapshot1.getCompositeSnapshotNode().getUniqueId());
            }
            if(compositeSnapshot2 != null && compositeSnapshot2.getCompositeSnapshotNode() != null && compositeSnapshot2.getCompositeSnapshotNode().getUniqueId() != null){
                nodeDAO.deleteNode(compositeSnapshot2.getCompositeSnapshotNode().getUniqueId());
            }
            nodeDAO.deleteNode(topLevelFolder.getUniqueId());
        }
    }
}
