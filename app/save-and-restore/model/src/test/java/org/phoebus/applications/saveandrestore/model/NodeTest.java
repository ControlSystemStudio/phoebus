package org.phoebus.applications.saveandrestore.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NodeTest {

    @Test
    public void test1() {

        Node node = Node.builder().uniqueId("id1").build();
        Node node2 = Node.builder().uniqueId("id2").build();

        assertNotEquals(node, node2);
        assertEquals(node, node);

        assertEquals(NodeType.FOLDER, node.getNodeType());
        assertEquals(NodeType.CONFIGURATION, Node.builder().nodeType(NodeType.CONFIGURATION).build().getNodeType());
        assertEquals(NodeType.SNAPSHOT, Node.builder().nodeType(NodeType.SNAPSHOT).build().getNodeType());
    }


    @Test
    public void genericTest() {

        Date now = new Date();

        Node node = new Node();

        node.setNodeType(NodeType.CONFIGURATION);
        node.setCreated(now);
        node.setLastModified(now);
        node.setUniqueId("id1");
        node.setName("name");
        node.setUserName("userName");

        assertEquals(now, node.getCreated());
        assertEquals(now, node.getLastModified());
        assertEquals("id1", node.getUniqueId());
        assertEquals("name", node.getName());
        assertEquals(NodeType.CONFIGURATION, node.getNodeType());
        assertEquals("userName", node.getUserName());

    }


    @Test
    public void testCompareTo() {

        Node folder1 = Node.builder().name("a").build();
        Node folder2 = Node.builder().name("b").build();
        Node folder3 = Node.builder().name("a").build();

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("c1").build();
        Node config2 = Node.builder().nodeType(NodeType.CONFIGURATION).name("c2").build();

		assertEquals(0, folder3.compareTo(folder1));
        assertTrue(folder2.compareTo(folder1) > 0);
        assertTrue(folder1.compareTo(folder2) < 0);

        assertTrue(folder1.compareTo(config) < 0);
        assertTrue(config.compareTo(folder1) > 0);
        assertTrue(config.compareTo(config2) < 0);


        Node snapshot = Node.builder().nodeType(NodeType.SNAPSHOT).name("b").build();
        Node snapshot2 = Node.builder().nodeType(NodeType.SNAPSHOT).name("b").build();
        Node compositeSnapshot = Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).name("c").build();

        assertTrue(snapshot.compareTo(compositeSnapshot) < 0);
        assertTrue(snapshot.compareTo(snapshot2) == 0);
    }

    @Test
    public void testBuilder() {
        Date now = new Date();
        Node node = Node.builder()
                .created(now)
                .uniqueId("id1")
                .lastModified(now)
                .name("name")
                .nodeType(NodeType.FOLDER)
                .build();

        assertEquals("id1", node.getUniqueId());
        assertEquals(now, node.getCreated());
        assertEquals(now, node.getLastModified());
        assertEquals("name", node.getName());
        assertEquals(NodeType.FOLDER, node.getNodeType());
    }


    @Test
    public void testToString() {
        assertNotNull(Node.builder().build().toString());
    }

    @Test
    public void testHashCode() {
        Node node1 = Node.builder().uniqueId("unique").nodeType(NodeType.FOLDER).build();
        Node node2 = Node.builder().uniqueId("unique").nodeType(NodeType.CONFIGURATION).build();
        assertEquals(node1.hashCode(), node2.hashCode());

        node2 = Node.builder().uniqueId("unique2").nodeType(NodeType.FOLDER).build();
        assertNotEquals(node1.hashCode(), node2.hashCode());
    }
}
