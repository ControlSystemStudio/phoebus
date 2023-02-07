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

import org.elasticsearch.common.recycler.Recycler.C;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties
@ContextHierarchy({@ContextConfiguration(classes = {ElasticTestConfig.class})})
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class})
@Profile("IT")
public class ElasticsearchDAOTest {

    @Autowired
    private ElasticsearchDAO elasticsearchDAO;

    private static Node node1;

    private static Node configNode1;
    private static Node configNode2;

    private static Node folderNode1;

    @BeforeAll
    public static void init(){
        node1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("node1").build();
        configNode1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("node1").build();
        configNode2 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId(UUID.randomUUID().toString()).name("configNode2").build();
        folderNode1 = Node.builder().nodeType(NodeType.FOLDER).uniqueId(UUID.randomUUID().toString()).name("node1").build();
    }

    @Test
    public void testIsNodeNameValid(){
        Node newNode = Node.builder().name("node7").uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertTrue(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testIsNodeNameInvalid(){
        Node newNode = Node.builder().name("node1").uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdEqual(){
        Node newNode = Node.builder().name("node1").uniqueId(node1.getUniqueId()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdEqual2(){
        Node newNode = Node.builder().name("node1").uniqueId(node1.getUniqueId()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1, configNode1)));
    }

    @Test
    public void testNodeNameAndUniqueIdNull() {
        Node newNode = Node.builder().name("node1").uniqueId(null).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(elasticsearchDAO.isNodeNameValid(newNode, Arrays.asList(node1, configNode2, folderNode1)));
    }

    @Test
    public void testRemoveDuplicateConfigPvs(){
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
    public void testRemoveDuplicateConfigSnapshotItems(){
        ConfigPv configPv1 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv2 = ConfigPv.builder().pvName("a").build();
        ConfigPv configPv3 = ConfigPv.builder().pvName("b").build();
        ConfigPv configPv4 = ConfigPv.builder().pvName("c").build();

        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnasphotItems(Arrays.asList(SnapshotItem.builder().configPv(configPv1).build(),
                SnapshotItem.builder().configPv(configPv2).build(),
                SnapshotItem.builder().configPv(configPv3).build()));

        snapshotData = elasticsearchDAO.removeDuplicateSnapshotItems(snapshotData);

        assertEquals(2, snapshotData.getSnapshotItems().size());
        assertEquals("a", snapshotData.getSnapshotItems().get(0).getConfigPv().getPvName());
        assertEquals("b", snapshotData.getSnapshotItems().get(1).getConfigPv().getPvName());

        snapshotData.setSnasphotItems(Arrays.asList(SnapshotItem.builder().configPv(configPv1).build(),
                SnapshotItem.builder().configPv(configPv3).build(),
                SnapshotItem.builder().configPv(configPv4).build()));

        snapshotData = elasticsearchDAO.removeDuplicateSnapshotItems(snapshotData);

        assertEquals(3, snapshotData.getSnapshotItems().size());

        //This should not throw a NPE
        assertNull(elasticsearchDAO.removeDuplicateSnapshotItems(null));
    }
}
