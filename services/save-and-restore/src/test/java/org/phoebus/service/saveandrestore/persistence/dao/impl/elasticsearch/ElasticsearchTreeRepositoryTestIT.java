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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.config.ElasticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class ElasticsearchTreeRepositoryTestIT {

    @Autowired
    private ElasticsearchClient client;

    @Value("${elasticsearch.tree_node.index:test_saveandrestore_tree}")
    private String ES_TREE_INDEX;

    @Autowired
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @Test
    public void testCreateAndFindNode() {
        String uniqueId = UUID.randomUUID().toString();
        Node node =
                Node.builder().uniqueId(uniqueId).nodeType(NodeType.FOLDER).name("node").userName("userName").description("description").build();
        ESTreeNode esTreeNode = new ESTreeNode();
        esTreeNode.setNode(node);

        esTreeNode = elasticsearchTreeRepository.save(esTreeNode);
        assertEquals(uniqueId, esTreeNode.getNode().getUniqueId());
        assertNotNull(esTreeNode.getNode().getCreated());
        assertNotNull(esTreeNode.getNode().getLastModified());
        assertNull(esTreeNode.getChildNodes());
        assertTrue(elasticsearchTreeRepository.findById(uniqueId).isPresent());

        esTreeNode.setChildNodes(Arrays.asList("a", "b", "c"));
        esTreeNode = elasticsearchTreeRepository.save(esTreeNode);
        assertEquals(3, esTreeNode.getChildNodes().size());

        assertTrue(elasticsearchTreeRepository.existsById(esTreeNode.getNode().getUniqueId()));
    }

    @Test
    public void testNoUniqueId() {
        Node node =
                Node.builder().nodeType(NodeType.FOLDER).name("node").userName("userName").description("description").build();
        ESTreeNode esTreeNode = new ESTreeNode();
        esTreeNode.setNode(node);

        esTreeNode = elasticsearchTreeRepository.save(esTreeNode);
        assertNotNull(esTreeNode.getNode().getUniqueId());
    }

    @Test
    public void testCreateAndUpdateNode() {
        String uniqueId = UUID.randomUUID().toString();
        Node node =
                Node.builder().uniqueId(uniqueId).nodeType(NodeType.FOLDER).name("node").userName("userName").description("description").build();
        ESTreeNode esTreeNode = new ESTreeNode();
        esTreeNode.setNode(node);
        esTreeNode = elasticsearchTreeRepository.save(esTreeNode);

        Date lastModified = esTreeNode.getNode().getCreated();

        esTreeNode.getNode().setName("newName");
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        esTreeNode = elasticsearchTreeRepository.save(esTreeNode);
        assertEquals(uniqueId, esTreeNode.getNode().getUniqueId());
        assertEquals("newName", esTreeNode.getNode().getName());
        assertNotEquals(lastModified, esTreeNode.getNode().getLastModified());
    }

    @Test
    public void testFindById() {

        Node node1 =
                Node.builder().nodeType(NodeType.FOLDER).name("node1").userName("userName").description("description").build();
        ESTreeNode esTreeNode1 = new ESTreeNode();
        esTreeNode1.setNode(node1);

        esTreeNode1 = elasticsearchTreeRepository.save(esTreeNode1);

        Node node2 =
                Node.builder().nodeType(NodeType.FOLDER).name("node2").userName("userName").description("description").build();
        ESTreeNode esTreeNode2 = new ESTreeNode();
        esTreeNode2.setNode(node2);

        esTreeNode2 = elasticsearchTreeRepository.save(esTreeNode2);

        assertTrue(elasticsearchTreeRepository.findById(esTreeNode1.getNode().getUniqueId()).isPresent());

        Iterable<ESTreeNode> nodes = elasticsearchTreeRepository
                .findAllById(Arrays.asList(esTreeNode1.getNode().getUniqueId(),
                        esTreeNode2.getNode().getUniqueId()));

        List<ESTreeNode> result =
                StreamSupport.stream(nodes.spliterator(), false)
                        .collect(Collectors.toList());
        assertEquals(2, result.size());

        // Adding an invalid unique id should not trigger an exception
        nodes = elasticsearchTreeRepository
                .findAllById(Arrays.asList(esTreeNode1.getNode().getUniqueId(),
                        esTreeNode2.getNode().getUniqueId(),
                        "invalidUniqueId"));
        result =
                StreamSupport.stream(nodes.spliterator(), false)
                        .collect(Collectors.toList());
        assertEquals(2, result.size());

        nodes = elasticsearchTreeRepository
                .findAllById(List.of("invalidUniqueId"));
        result =
                StreamSupport.stream(nodes.spliterator(), false)
                        .collect(Collectors.toList());
        assertEquals(0, result.size());
    }

    @Test
    public void testDeleteById() {
        Node node1 =
                Node.builder().nodeType(NodeType.FOLDER).name("node1").userName("userName").description("description").build();
        ESTreeNode esTreeNode1 = new ESTreeNode();
        esTreeNode1.setNode(node1);

        esTreeNode1 = elasticsearchTreeRepository.save(esTreeNode1);

        elasticsearchTreeRepository.deleteById(esTreeNode1.getNode().getUniqueId());

        assertFalse(elasticsearchTreeRepository.existsById(esTreeNode1.getNode().getUniqueId()));
    }

    @Test
    public void testGetParentNode() {
        Node node1 =
                Node.builder().nodeType(NodeType.FOLDER).name("node1").userName("userName").description("description").build();
        ESTreeNode esTreeNode1 = new ESTreeNode();
        esTreeNode1.setNode(node1);
        esTreeNode1.setChildNodes(Arrays.asList("aa", "bb", "cc"));

        elasticsearchTreeRepository.save(esTreeNode1);

        ESTreeNode parentNode = elasticsearchTreeRepository.getParentNode("aa");
        assertNotNull(parentNode);

        assertThrows(NodeNotFoundException.class,
                () -> elasticsearchTreeRepository.getParentNode("aaa"));
    }

    @Test
    public void testGetParentNodeMultipleParents() {
        Node node1 =
                Node.builder().nodeType(NodeType.FOLDER).name("node1").userName("userName").description("description").build();
        ESTreeNode esTreeNode1 = new ESTreeNode();
        esTreeNode1.setNode(node1);
        esTreeNode1.setChildNodes(List.of("aaaa"));

        elasticsearchTreeRepository.save(esTreeNode1);

        Node node2 =
                Node.builder().nodeType(NodeType.FOLDER).name("node1").userName("userName").description("description").build();
        ESTreeNode esTreeNode2 = new ESTreeNode();
        esTreeNode2.setNode(node2);
        esTreeNode2.setChildNodes(List.of("aaaa")); // Client code should not do this!

        elasticsearchTreeRepository.save(esTreeNode2);

        assertThrows(RuntimeException.class,
                () -> elasticsearchTreeRepository.getParentNode("aaaa"));
    }

    @AfterAll
    public void dropIndex() {
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
