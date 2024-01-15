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
 *
 */

package org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchDAO.NodeNameComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties
@ContextHierarchy({@ContextConfiguration(classes = {ElasticTestConfig.class})})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@TestPropertySource(locations = "classpath:test_application.properties")
@Profile("IT")
public class ElasticsearchDAOTest {

    @Autowired
    private ElasticsearchDAO elasticsearchDAO;

    @Autowired
    private ConfigurationDataRepository configurationDataRepository;

    @SuppressWarnings("unused")
    @Autowired
    private SnapshotDataRepository snapshotDataRepository;

    @Autowired
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @Autowired


    private static Node node1;

    private static Node configNode1;
    private static Node configNode2;

    private static Node folderNode1;

    @BeforeAll
    public static void init() {
        node1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("node1").build();
        configNode1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("node1").build();
        configNode2 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("configNode2").build();
        folderNode1 = Node.builder().nodeType(NodeType.FOLDER).uniqueId(UUID.randomUUID().toString()).name("node1").build();
    }

    @Test
    public void testIsNodeNameValid() {
        Node newNode = Node.builder().name("node7").uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertTrue(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testIsNodeNameInvalid() {
        Node newNode = Node.builder().name("node1").uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdEqual() {
        Node newNode = Node.builder().name("node1").uniqueId(node1.getUniqueId()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdEqual2() {
        Node newNode = Node.builder().name("node1").uniqueId(node1.getUniqueId()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1, configNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdNull() {
        Node newNode = Node.builder().name("node1").uniqueId(null).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testRemoveDuplicateConfigPvs() {
        ConfigPv configPv1 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv3 = ConfigPv.builder().pvName("b").build();
        ConfigPv configPv4 = ConfigPv.builder().pvName("c").build();

        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(configPv1, configPv2, configPv3));

        configurationData = elasticsearchDAO.removeDuplicatePVNames(configurationData);

        assertEquals(2, configurationData.getPvList().size());
        assertEquals("a", configurationData.getPvList().get(0).getPvName());
        assertEquals("b", configurationData.getPvList().get(1).getPvName());

        configurationData.setPvList(Arrays.asList(configPv1, configPv3, configPv4));

        configurationData = elasticsearchDAO.removeDuplicatePVNames(configurationData);

        assertEquals(3, configurationData.getPvList().size());

        //This should not throw a NPE
        assertNull(elasticsearchDAO.removeDuplicatePVNames(null));
    }

    @Test
    public void testRemoveDuplicateConfigSnapshotItems() {
        ConfigPv configPv1 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv3 = ConfigPv.builder().pvName("b").build();
        ConfigPv configPv4 = ConfigPv.builder().pvName("c").build();

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(SnapshotItem.builder().configPv(configPv1).build(),
                SnapshotItem.builder().configPv(configPv2).build(),
                SnapshotItem.builder().configPv(configPv3).build()));

        snapshotData = elasticsearchDAO.removeDuplicateSnapshotItems(snapshotData);

        assertEquals(2, snapshotData.getSnapshotItems().size());
        assertEquals("a", snapshotData.getSnapshotItems().get(0).getConfigPv().getPvName());
        assertEquals("b", snapshotData.getSnapshotItems().get(1).getConfigPv().getPvName());

        snapshotData.setSnapshotItems(Arrays.asList(SnapshotItem.builder().configPv(configPv1).build(),
                SnapshotItem.builder().configPv(configPv3).build(),
                SnapshotItem.builder().configPv(configPv4).build()));

        snapshotData = elasticsearchDAO.removeDuplicateSnapshotItems(snapshotData);

        assertEquals(3, snapshotData.getSnapshotItems().size());

        //This should not throw a NPE
        assertNull(elasticsearchDAO.removeDuplicateSnapshotItems(null));
    }

    @Test
    public void testMayCopySnapshot() {
        ConfigPv configPva = ConfigPv.builder().pvName("a").build();
        ConfigPv configPvb = ConfigPv.builder().pvName("b").build();
        ConfigPv configPvc = ConfigPv.builder().pvName("c").build();
        ConfigPv configPvd = ConfigPv.builder().pvName("d").build();
        ConfigPv configPve = ConfigPv.builder().pvName("e").build();

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(Arrays.asList(SnapshotItem.builder().configPv(configPva).build(),
                SnapshotItem.builder().configPv(configPvb).build(),
                SnapshotItem.builder().configPv(configPvc).build(),
                SnapshotItem.builder().configPv(configPvd).build(),
                SnapshotItem.builder().configPv(configPve).build()));

        when(snapshotDataRepository.findById("snapshot")).thenReturn(Optional.of(snapshotData));

        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Arrays.asList(configPva, configPvb, configPvc, configPvd, configPve));

        when(configurationDataRepository.findById("configuration")).thenReturn(Optional.of(configurationData));

        Node snapshotNode = Node.builder().nodeType(NodeType.SNAPSHOT).uniqueId("snapshot").build();
        Node configurationNode = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("configuration").build();

        assertTrue(elasticsearchDAO.mayMoveOrCopySnapshot(snapshotNode, configurationNode));

        configurationData.setPvList(Arrays.asList(configPva, configPvb, configPvc, configPvd));

        assertFalse(elasticsearchDAO.mayMoveOrCopySnapshot(snapshotNode, configurationNode));

        configurationData.setPvList(Arrays.asList(configPva, configPvb, configPvc, configPvd, configPve));

        snapshotData.setSnapshotItems(Arrays.asList(SnapshotItem.builder().configPv(configPva).build(),
                SnapshotItem.builder().configPv(configPvb).build(),
                SnapshotItem.builder().configPv(configPvc).build(),
                SnapshotItem.builder().configPv(configPvd).build()));

        assertFalse(elasticsearchDAO.mayMoveOrCopySnapshot(snapshotNode, configurationNode));

        snapshotData.setSnapshotItems(Arrays.asList(SnapshotItem.builder().configPv(configPva).build(),
                SnapshotItem.builder().configPv(configPvb).build(),
                SnapshotItem.builder().configPv(configPvc).build(),
                SnapshotItem.builder().configPv(configPvd).build(),
                SnapshotItem.builder().configPv(configPve).build()));

        assertTrue(elasticsearchDAO.mayMoveOrCopySnapshot(snapshotNode, configurationNode));
    }

    @Test
    public void testDetermineNewNodeName() {
        Node n1 = Node.builder().uniqueId("abc").name("abc").build();
        Node n2 = Node.builder().uniqueId("def").name("def").build();
        Node n3 = Node.builder().uniqueId("ABC copy").name("ABC copy").build();
        Node n4 = Node.builder().uniqueId("DEF copy 2").name("DEF copy 2").build();
        Node n5 = Node.builder().uniqueId("DEF copy 777").name("DEF copy 777").build();
        Node n6 = Node.builder().uniqueId("XYZ copy 1").name("XYZ copy 1").build();
        Node n7 = Node.builder().uniqueId("XYZ copy abc").name("XYZ copy abc").build();

        ESTreeNode es1 = new ESTreeNode();
        es1.setNode(n1);
        ESTreeNode es2 = new ESTreeNode();
        es2.setNode(n2);
        ESTreeNode es3 = new ESTreeNode();
        es3.setNode(n3);
        ESTreeNode es4 = new ESTreeNode();
        es4.setNode(n4);
        ESTreeNode es5 = new ESTreeNode();
        es5.setNode(n5);
        ESTreeNode es6 = new ESTreeNode();
        es6.setNode(n6);
        ESTreeNode es7 = new ESTreeNode();
        es7.setNode(n7);

        List<String> nodeIds =
                Arrays.asList(n1.getUniqueId(), n2.getUniqueId(), n3.getUniqueId(), n4.getUniqueId(), n5.getUniqueId(), n6.getUniqueId(), n7.getUniqueId());

        ESTreeNode targetTreeNode = new ESTreeNode();
        Node parentNode =
                Node.builder().uniqueId("parent").build();
        targetTreeNode.setChildNodes(nodeIds);
        targetTreeNode.setNode(parentNode);
        when(elasticsearchTreeRepository.findById("parent")).thenReturn(Optional.of(targetTreeNode));
        when(elasticsearchTreeRepository.findAllById(nodeIds))
                .thenReturn(Arrays.asList(es1, es2, es3, es4, es5, es6, es7));

        Node s1 = Node.builder().name("abc").build();
        List<Node> targetChildNodes = Arrays.asList(n1, n2, n3, n4, n5, n6, n7);
        assertEquals("abc copy", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("ABC").build();
        assertEquals("ABC", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("DEF copy 777").build();
        assertEquals("DEF copy 778", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).name("def").build();
        assertEquals("def", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("XYZ copy abc").build();
        assertEquals("XYZ copy abc copy", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));
    }

    @Test
    public void testDetermineNewNodeName2() {
        Node n1 = Node.builder().uniqueId("abc").name("abc").build();
        Node n2 = Node.builder().uniqueId("abc copy").name("abc copy").build();
        Node n3 = Node.builder().uniqueId("abc copy 5").name("abc copy 5").build();
        Node n4 = Node.builder().uniqueId("abc copy 10").name("abc copy 10").build();

        ESTreeNode es1 = new ESTreeNode();
        es1.setNode(n1);
        ESTreeNode es2 = new ESTreeNode();
        es2.setNode(n2);
        ESTreeNode es3 = new ESTreeNode();
        es3.setNode(n3);
        ESTreeNode es4 = new ESTreeNode();
        es4.setNode(n4);

        List<String> nodeIds =
                Arrays.asList(n1.getUniqueId(), n2.getUniqueId(), n3.getUniqueId(), n4.getUniqueId());

        ESTreeNode targetTreeNode = new ESTreeNode();
        Node parentNode =
                Node.builder().uniqueId("parent").build();
        targetTreeNode.setChildNodes(nodeIds);
        targetTreeNode.setNode(parentNode);
        when(elasticsearchTreeRepository.findById("parent")).thenReturn(Optional.of(targetTreeNode));
        when(elasticsearchTreeRepository.findAllById(nodeIds))
                .thenReturn(Arrays.asList(es1, es2, es3, es4));

        Node s1 = Node.builder().name("abc").build();
        List<Node> targetChildNodes = Arrays.asList(n1, n2, n3, n4);
        assertEquals("abc copy 11", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("abc copy").build();
        assertEquals("abc copy 11", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("abc copy 7").build();
        assertEquals("abc copy 7", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        when(elasticsearchTreeRepository.findAllById(nodeIds))
                .thenReturn(Arrays.asList(es1, es4));

        s1 = Node.builder().name("abc copy 7").build();
        assertEquals("abc copy 7", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

        s1 = Node.builder().name("abc").build();
        assertEquals("abc copy 11", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));
    }

    @Test
    public void testDetermineNewNodeName3() {
        Node n1 = Node.builder().uniqueId("abc").name("abc").build();
        Node n2 = Node.builder().uniqueId("abc copy").name("abc copy").build();

        ESTreeNode es1 = new ESTreeNode();
        es1.setNode(n1);
        ESTreeNode es2 = new ESTreeNode();
        es2.setNode(n2);

        List<String> nodeIds =
                Arrays.asList(n1.getUniqueId(), n2.getUniqueId());

        ESTreeNode targetTreeNode = new ESTreeNode();
        Node parentNode =
                Node.builder().uniqueId("parent").build();
        targetTreeNode.setChildNodes(nodeIds);
        targetTreeNode.setNode(parentNode);
        when(elasticsearchTreeRepository.findById("parent")).thenReturn(Optional.of(targetTreeNode));
        when(elasticsearchTreeRepository.findAllById(nodeIds))
                .thenReturn(Arrays.asList(es1, es2));

        Node s1 = Node.builder().name("abc copy").build();
        List<Node> targetChildNodes = Arrays.asList(n1, n2);
        assertEquals("abc copy 2", elasticsearchDAO.determineNewNodeName(s1, targetChildNodes));

    }

    @Test
    public void nodeNameComparatorTest() {
        List<String> sorted = Arrays.asList("abc", "abc copy").stream().sorted(new NodeNameComparator()).collect(Collectors.toList());
        assertEquals("abc", sorted.get(0));
        sorted = Arrays.asList("abc copy", "abc").stream().sorted(new NodeNameComparator()).collect(Collectors.toList());
        assertEquals("abc", sorted.get(0));
        sorted = Arrays.asList("abc copy", "abc copy 2").stream().sorted(new NodeNameComparator()).collect(Collectors.toList());
        assertEquals("abc copy", sorted.get(0));
        sorted = Arrays.asList("abc copy 3", "abc copy 2").stream().sorted(new NodeNameComparator()).collect(Collectors.toList());
        assertEquals("abc copy 2", sorted.get(0));
        sorted = Arrays.asList("abc copy", "abc copy").stream().sorted(new NodeNameComparator()).collect(Collectors.toList());
        assertEquals("abc copy", sorted.get(0));
    }
}
