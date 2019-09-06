package org.phoebus.applications.saveandrestore.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class NodeTest {

	@Test
	public void test1() {
		
		Node node = Node.builder().id(1).build();
		Node node2 = Node.builder().id(2).build();
		
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
		node.setId(1);
		node.setName("name");
		node.setUserName("userName");
		
		assertEquals(now, node.getCreated());
		assertEquals(now, node.getLastModified());
		assertEquals(1, node.getId());
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
		
		assertTrue(folder3.compareTo(folder1) == 0);
		assertTrue(folder2.compareTo(folder1) > 0);
		assertTrue(folder1.compareTo(folder2) < 0);
		
		assertTrue(folder1.compareTo(config) < 0);
		assertTrue(config.compareTo(folder1) > 0);
		assertTrue(config.compareTo(config2) < 0);
	}
	
	@Test
	public void testBuilder() {
		Map<String, String> props = new HashMap<>();
		props.put("a", "b");
		Date now = new Date();
		Node node = Node.builder()
				.created(now)
				.id(1)
				.lastModified(now)
				.name("name")
				.nodeType(NodeType.FOLDER)
				.properties(props)
				.uniqueId("uniqueid")
				.build();
		
		assertEquals(1, node.getId());
		assertEquals(now, node.getCreated());
		assertEquals(now, node.getLastModified());
		assertEquals("name", node.getName());
		assertEquals("uniqueid", node.getUniqueId());
		assertEquals(NodeType.FOLDER, node.getNodeType());
	}
	
	
	@Test
	public void testToString() {
		assertNotNull(Node.builder().build().toString());
	}
	
	@Test
	public void testProperties() {
		Node node = Node.builder().build();	
		assertNull(node.getProperty("key1"));
		node.removeProperty("key1");
		assertNull(node.getProperties());
		
		node.putProperty("key1", "value1");
		assertNull(node.getProperty("key2"));
		assertEquals("value1", node.getProperty("key1"));
		
		node.putProperty("key3", "value3");
		assertEquals(2, node.getProperties().size());
		
		node.removeProperty("key3");
		assertEquals(1, node.getProperties().size());	
	}
	
	@Test
	public void testHashCode() {
		Node node1 = Node.builder().uniqueId("unique").nodeType(NodeType.FOLDER).build();
		Node node2 = Node.builder().uniqueId("unique").nodeType(NodeType.CONFIGURATION).build();
		assertNotEquals(node1.hashCode(), node2.hashCode());
	}
	
	@Test
	public void testEquals() {
		Node node1 = Node.builder().uniqueId("unique").nodeType(NodeType.FOLDER).build();
		Node node2 = Node.builder().uniqueId("unique").nodeType(NodeType.CONFIGURATION).build();
		Node node3 = Node.builder().uniqueId("unique").nodeType(NodeType.FOLDER).build();
		
		assertFalse(node1.equals(null));
		assertFalse(node1.equals(node2));
		assertTrue(node1.equals(node3));
		assertFalse(node1.equals(new Object()));
	}
}
