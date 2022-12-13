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

package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(NodeController.class)
/**
 * Main purpose of the tests in this class is to verify that REST end points are
 * maintained, i.e. that URLs are not changed and that they return the correct
 * data.
 *
 * @author Georg Weiss, European Spallation Source
 *
 */
public class NodeControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    private static Node folderFromClient;

    private static Node config1;

    private static Node snapshot;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void setUp() {

        config1 = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("a")
                .userName("myusername").build();

        folderFromClient = Node.builder().name("SomeFolder").userName("myusername").uniqueId("11").build();

        snapshot = Node.builder().nodeType(NodeType.SNAPSHOT).nodeType(NodeType.SNAPSHOT).name("name")
                .build();

    }

    @Test
    public void testCreateFolder() throws Exception {

        when(nodeDAO.createNode(Mockito.any(String.class), Mockito.any(Node.class))).thenReturn(folderFromClient);

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=a").contentType(JSON)
                .content(objectMapper.writeValueAsString(folderFromClient));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Node.class);
    }

    @Test
    public void testCreateFolderNoUsername() throws Exception {

        Node folder = Node.builder().name("SomeFolder").uniqueId("11").build();

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON)
                .content(objectMapper.writeValueAsString(folder));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateFolderParentIdDoesNotExist() throws Exception {

        when(nodeDAO.createNode(Mockito.anyString(), Mockito.any(Node.class)))
                .thenThrow(new IllegalArgumentException("Parent folder does not exist"));

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON)
                .content(objectMapper.writeValueAsString(folderFromClient));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateConfig() throws Exception {

        reset(nodeDAO);

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
                .userName("user").build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(config);

        when(nodeDAO.createConfiguration(Mockito.any(String.class), Mockito.any(Configuration.class))).thenAnswer(new Answer<Configuration>() {
            public Configuration answer(InvocationOnMock invocation) {
                return configuration;
            }
        });

        MockHttpServletRequestBuilder request = put("/config?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Configuration.class);
    }

    @Test
    public void testCreateNodeBadRequests() throws Exception {
        reset(nodeDAO);

        Node config = Node.builder().nodeType(NodeType.FOLDER).name("config").uniqueId("hhh")
                .build();
        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.FOLDER).name("config").uniqueId("hhh")
                .userName("").build();

        request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.FOLDER).uniqueId("hhh")
                .userName("valid").build();

        request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.FOLDER).name("").uniqueId("hhh")
                .userName("valid").build();

        request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetChildNodes() throws Exception {
        reset(nodeDAO);

        when(nodeDAO.getChildNodes("p")).thenAnswer(new Answer<List<Node>>() {
            public List<Node> answer(InvocationOnMock invocation) throws Throwable {
                return Arrays.asList(config1);
            }
        });

        MockHttpServletRequestBuilder request = get("/node/p/children").contentType(JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        List<Node> childNodes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Node>>() {
        });

        assertEquals(1, childNodes.size());
    }

    @Test
    public void testGetChildNodesNonExistingNode() throws Exception {
        reset(nodeDAO);

        when(nodeDAO.getChildNodes("non-existing")).thenThrow(NodeNotFoundException.class);
        MockHttpServletRequestBuilder request = get("/node/non-existing/children").contentType(JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void testGetNonExistingConfig() throws Exception {

        when(nodeDAO.getNode("x")).thenThrow(new NodeNotFoundException("lasdfk"));

        MockHttpServletRequestBuilder request = get("/node/x").contentType(JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void testGetSnapshotsForNonExistingConfig() throws Exception {

        when(nodeDAO.getSnapshots("x")).thenThrow(new NodeNotFoundException("lasdfk"));

        MockHttpServletRequestBuilder request = get("/config/x/snapshots").contentType(JSON);

        mockMvc.perform(request).andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteFolder() throws Exception {
        MockHttpServletRequestBuilder request = delete("/node/a");

        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testDeleteNode() throws Exception {
        MockHttpServletRequestBuilder request = delete("/node/s");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testGetFolder() throws Exception {
        when(nodeDAO.getNode("q")).thenReturn(Node.builder().uniqueId("1").uniqueId("q").build());

        MockHttpServletRequestBuilder request = get("/node/q");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
    }

    @Test
    public void testGetConfiguration() throws Exception {

        Mockito.reset(nodeDAO);

        when(nodeDAO.getNode("a")).thenReturn(Node.builder().build());

        MockHttpServletRequestBuilder request = get("/node/a");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
    }

    @Test
    public void testGetNonExistingConfiguration() throws Exception {
        Mockito.reset(nodeDAO);
        when(nodeDAO.getNode("a")).thenThrow(NodeNotFoundException.class);

        MockHttpServletRequestBuilder request = get("/node/a");

        mockMvc.perform(request).andExpect(status().isNotFound());


    }

    @Test
    public void testGetNonExistingFolder() throws Exception {

        Mockito.reset(nodeDAO);
        when(nodeDAO.getNode("a")).thenThrow(NodeNotFoundException.class);

        MockHttpServletRequestBuilder request = get("/node/a");

        mockMvc.perform(request).andExpect(status().isNotFound());

        when(nodeDAO.getNode("b")).thenThrow(IllegalArgumentException.class);

        request = get("/node/b");

        mockMvc.perform(request).andExpect(status().isBadRequest());


    }

    @Test
    public void testMoveNode() throws Exception {
        when(nodeDAO.moveNodes(Arrays.asList("a"), "b", "username")).thenReturn(Node.builder().uniqueId("2").uniqueId("a").build());

        MockHttpServletRequestBuilder request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList("a")))
                .param("to", "b")
                .param("username", "username");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
    }

    @Test
    public void testMoveNodeUsernameEmpty() throws Exception {
        MockHttpServletRequestBuilder request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList("a")))
                .param("to", "b")
                .param("username", "");

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testMoveNodeTargetIdEmpty() throws Exception {
        MockHttpServletRequestBuilder request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList("a")))
                .param("to", "")
                .param("username", "user");

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testMoveNodeSourceNodeListEmpty() throws Exception {
        MockHttpServletRequestBuilder request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Collections.emptyList()))
                .param("to", "targetId")
                .param("username", "user");

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testMoveNodeNoUsername() throws Exception {
        MockHttpServletRequestBuilder request = post("/move").param("to", "b");

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetFolderIllegalArgument() throws Exception {
        when(nodeDAO.getNode("a")).thenThrow(IllegalArgumentException.class);

        MockHttpServletRequestBuilder request = get("/node/a");

        mockMvc.perform(request).andExpect(status().isBadRequest());

    }

    @Test
    public void testUpdateNode() throws Exception {

        Node node = Node.builder().name("foo").uniqueId("a").build();

        when(nodeDAO.updateNode(Mockito.any(Node.class), Mockito.anyBoolean())).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/node")
                .param("customTimeForMigration", "false")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(node));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);

    }

    @Test
    public void testGetFromPath() throws Exception {
        when(nodeDAO.getFromPath("/a/b/c")).thenReturn(null);
        MockHttpServletRequestBuilder request = get("/path?path=/a/b/c");
        mockMvc.perform(request).andExpect(status().isNotFound());

        request = get("/path");
        mockMvc.perform(request).andExpect(status().isBadRequest());

        Node node = Node.builder().name("name").uniqueId("uniqueId").build();
        when(nodeDAO.getFromPath("/a/b/c")).thenReturn(Arrays.asList(node));
        request = get("/path?path=/a/b/c");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        List<Node> nodes = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Node>>() {
        });

        assertEquals(1, nodes.size());
    }

    @Test
    public void testGetFullPath() throws Exception {
        when(nodeDAO.getFullPath("nonexisting")).thenReturn(null);
        MockHttpServletRequestBuilder request = get("/path/nonexsiting");
        mockMvc.perform(request).andExpect(status().isNotFound());

        when(nodeDAO.getFullPath("existing")).thenReturn("/a/b/c");
        request = get("/path/existing");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertEquals("/a/b/c", result.getResponse().getContentAsString());

    }

    @Test
    public void testCreateNodeWithInvalidTags() throws Exception {

        Node compositeSnapshot = Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).name("composite snapshot").uniqueId("hhh")
                .userName("user").build();
        Tag tag1 = new Tag();
        tag1.setName("a");

        Tag tag2 = new Tag();
        tag2.setName("goLDeN");
        compositeSnapshot.setTags(Arrays.asList(tag1, tag2));

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(compositeSnapshot));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateNodeWithInvalidTags() throws Exception {

        Node compositeSnapshot = Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).name("composite snapshot").uniqueId("hhh")
                .userName("user").build();
        Tag tag1 = new Tag();
        tag1.setName("a");

        Tag tag2 = new Tag();
        tag2.setName("goLDeN");
        compositeSnapshot.setTags(Arrays.asList(tag1, tag2));

        MockHttpServletRequestBuilder request = post("/node").contentType(JSON).content(objectMapper.writeValueAsString(compositeSnapshot));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testCreateNodeWithValidTags1() throws Exception {

        Node compositeSnapshot = Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).name("composite snapshot").uniqueId("hhh")
                .userName("user").build();
        Tag tag1 = new Tag();
        tag1.setName("a");

        Tag tag2 = new Tag();
        tag2.setName("other");
        compositeSnapshot.setTags(Arrays.asList(tag1, tag2));

        when(nodeDAO.createNode(Mockito.any(String.class), Mockito.any(Node.class))).thenReturn(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(compositeSnapshot));

        mockMvc.perform(request).andExpect(status().isOk());

        reset(nodeDAO);
    }

    @Test
    public void testCreateNodeWithValidTags2() throws Exception {

        Node compositeSnapshot = Node.builder().nodeType(NodeType.SNAPSHOT).name("composite snapshot").uniqueId("hhh")
                .userName("user").build();
        Tag tag1 = new Tag();
        tag1.setName("a");

        Tag tag2 = new Tag();
        tag2.setName("golden");
        compositeSnapshot.setTags(Arrays.asList(tag1, tag2));

        when(nodeDAO.createNode(Mockito.any(String.class), Mockito.any(Node.class))).thenReturn(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=p").contentType(JSON).content(objectMapper.writeValueAsString(compositeSnapshot));

        mockMvc.perform(request).andExpect(status().isOk());

        reset(nodeDAO);
    }
}
