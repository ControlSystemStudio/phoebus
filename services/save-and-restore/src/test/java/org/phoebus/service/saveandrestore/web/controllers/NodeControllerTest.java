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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.model.ESTreeNode;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.persistence.dao.impl.elasticsearch.ElasticsearchTreeRepository;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
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
@ContextHierarchy({@ContextConfiguration(classes = {ControllersTestConfig.class})})
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
    private ElasticsearchTreeRepository elasticsearchTreeRepository;

    @Autowired
    private MockMvc mockMvc;

    private static Node folderFromClient;

    private static Node rootNode;

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

        rootNode = Node.builder().uniqueId(Node.ROOT_FOLDER_UNIQUE_ID).name("root").build();

    }

    @Test
    @Disabled
    public void testGetRootNode() throws Exception {

        when(nodeDAO.getRootNode()).thenReturn(rootNode);

        MockHttpServletRequestBuilder request = get("/root").contentType(JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Node.class);

    }

    @Test
    @Disabled
    public void testCreateFolder() throws Exception {

        when(nodeDAO.createNode("p", folderFromClient)).thenReturn(folderFromClient);

        MockHttpServletRequestBuilder request = put("/node?parentId=a").contentType(JSON)
                .content(objectMapper.writeValueAsString(folderFromClient));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Node.class);
    }

    @Test
    @Disabled
    public void testCreateFolderNoUsername() throws Exception {

        Node folder = Node.builder().name("SomeFolder").uniqueId("11").build();

        MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON)
                .content(objectMapper.writeValueAsString(folder));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    @Disabled
    public void testCreateFolderParentIdDoesNotExist() throws Exception {

        when(nodeDAO.createNode("p", folderFromClient))
                .thenThrow(new IllegalArgumentException("Parent folder does not exist"));

        MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON)
                .content(objectMapper.writeValueAsString(folderFromClient));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    @Disabled
    public void testCreateConfig() throws Exception {

        reset(nodeDAO);

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
                .userName("user").build();
        when(nodeDAO.createNode("p", config)).thenAnswer(new Answer<Node>() {
            public Node answer(InvocationOnMock invocation) throws Throwable {
                return config1;
            }
        });

        MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
    }

    @Test
    @Disabled
    public void testCreateNodeBadRequests() throws Exception {
        reset(nodeDAO);

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
                .build();
        MockHttpServletRequestBuilder request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.CONFIGURATION).name("config").uniqueId("hhh")
                .userName("").build();

        request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("hhh")
                .userName("valid").build();

        request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        config = Node.builder().nodeType(NodeType.CONFIGURATION).name("").uniqueId("hhh")
                .userName("valid").build();

        request = put("/node/p").contentType(JSON).content(objectMapper.writeValueAsString(config));
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
    public void testGetSnapshots() throws Exception {

        when(nodeDAO.getSnapshots("s")).thenReturn(Arrays.asList(snapshot));

        MockHttpServletRequestBuilder request = get("/config/s/snapshots").contentType(JSON);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Node>>() {
        });

        reset(nodeDAO);
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
    @Disabled
    public void testDeleteNodes() throws Exception {
        MockHttpServletRequestBuilder request = delete("/node")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Arrays.asList("a")));
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
    public void testUpdateConfig() throws Exception {

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).userName("myusername").uniqueId("0").build();
        List<ConfigPv> configPvList = Arrays.asList(ConfigPv.builder().pvName("name").build());

        UpdateConfigHolder holder = UpdateConfigHolder.builder().config(config).configPvList(configPvList).build();

        //when(nodeDAO.updateConfiguration(holder.getConfig(), holder.getConfigPvList())).thenReturn(config);

        MockHttpServletRequestBuilder request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);
    }

    @Test
    public void testUpdateConfigBadConfigPv() throws Exception {

        Node config = Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("0").build();
        UpdateConfigHolder holder = UpdateConfigHolder.builder().build();

        MockHttpServletRequestBuilder request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        holder.setConfig(config);

        request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        List<ConfigPv> configPvList = Arrays.asList(ConfigPv.builder().build());
        holder.setConfigPvList(configPvList);

        //when(nodeDAO.updateConfiguration(holder.getConfig(), holder.getConfigPvList())).thenReturn(config);

        request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        config.setUserName("");

        request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        configPvList = Arrays.asList(ConfigPv.builder().pvName("").build());
        holder.setConfigPvList(configPvList);

        request = post("/config/a/update").contentType(JSON)
                .content(objectMapper.writeValueAsString(holder));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testGetFolderIllegalArgument() throws Exception {
        when(nodeDAO.getNode("a")).thenThrow(IllegalArgumentException.class);

        MockHttpServletRequestBuilder request = get("/node/a");

        mockMvc.perform(request).andExpect(status().isBadRequest());

    }

    @Test
    @Disabled
    public void testUpdateNode() throws Exception {

        Node node = Node.builder().name("foo").uniqueId("a").build();

        when(nodeDAO.updateNode(node, false)).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/node/a/update")
                .param("customTimeForMigration", "false")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(node));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);

    }

    @Test
    public void testGetConfigPvs() throws Exception {

        ConfigPv configPv = ConfigPv.builder()
                .pvName("pvname")
                .build();

        when(nodeDAO.getConfigPvs("cpv")).thenReturn(Arrays.asList(configPv));

        MockHttpServletRequestBuilder request = get("/config/cpv/items");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<ConfigPv>>() {
        });
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
}
