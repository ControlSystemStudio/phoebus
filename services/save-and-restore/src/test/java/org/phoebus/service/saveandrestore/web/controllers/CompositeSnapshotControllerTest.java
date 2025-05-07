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

package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(CompositeSnapshotController.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class CompositeSnapshotControllerTest {

    @Autowired
    private String userAuthorization;

    @Autowired
    private String adminAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String demoUser;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static CompositeSnapshot compositeSnapshot;

    @BeforeAll
    public static void init() {
        compositeSnapshot = new CompositeSnapshot();
        compositeSnapshot.setCompositeSnapshotNode(Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT)
                .name("name").uniqueId("id").build());
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of("ref"));
        compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);
    }

    @AfterEach
    public void resetMocks(){
        reset(nodeDAO, webSocketHandler);
    }

    @Test
    public void testCreateCompositeSnapshot1() throws Exception {

        when(nodeDAO.createCompositeSnapshot(Mockito.any(String.class), Mockito.any(CompositeSnapshot.class))).thenReturn(compositeSnapshot);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/composite-snapshot?parentNodeId=id")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, CompositeSnapshot.class);

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateCompositeSnapshot2() throws Exception {

        when(nodeDAO.createCompositeSnapshot(Mockito.any(String.class), Mockito.any(CompositeSnapshot.class))).thenReturn(compositeSnapshot);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/composite-snapshot?parentNodeId=id")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateCompositeSnapshot3() throws Exception {

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/composite-snapshot?parentNodeId=id")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateCompositeSnapshot4() throws Exception {

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/composite-snapshot?parentNodeId=id")
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));

    }

    @Test
    public void testCreateCompositeSnapshotWrongNodeType() throws Exception{
        Node node = Node.builder().uniqueId("c").nodeType(NodeType.SNAPSHOT).build();
        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        MockHttpServletRequestBuilder request = put("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(compositeSnapshot1));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateCompositeSnapshot1() throws Exception {

        Node node = Node.builder().uniqueId("c").nodeType(NodeType.COMPOSITE_SNAPSHOT).userName(demoUser).build();
        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot1);

        when(nodeDAO.updateCompositeSnapshot(compositeSnapshot1)).thenReturn(compositeSnapshot1);
        when(nodeDAO.getNode("c")).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, CompositeSnapshot.class);

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateCompositeSnapshot2() throws Exception {

        Node node = Node.builder().uniqueId("c").nodeType(NodeType.COMPOSITE_SNAPSHOT).userName(demoUser).build();
        when(nodeDAO.getNode("c")).thenReturn(Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).uniqueId("c").userName("notUser").build());

        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot1);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateCompositeSnapshot3() throws Exception {

        Node node = Node.builder().uniqueId("c").nodeType(NodeType.COMPOSITE_SNAPSHOT).userName(demoUser).build();
        when(nodeDAO.getNode("c")).thenReturn(Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).uniqueId("c").userName("notUser").build());

        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot1);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isForbidden());

        request = post("/composite-snapshot")
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateCompositeSnapshot4() throws Exception {

        Node node = Node.builder().uniqueId("c").nodeType(NodeType.COMPOSITE_SNAPSHOT).userName(demoUser).build();
        when(nodeDAO.getNode("c")).thenReturn(Node.builder().nodeType(NodeType.COMPOSITE_SNAPSHOT).uniqueId("c").userName("notUser").build());

        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot1);

        when(nodeDAO.updateCompositeSnapshot(compositeSnapshot1)).thenReturn(compositeSnapshot1);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateCompositeSnapshotWrongNodeType() throws Exception{
        Node node = Node.builder().uniqueId("c").userName(demoUser).nodeType(NodeType.SNAPSHOT).build();
        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        when(nodeDAO.getNode("c")).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(compositeSnapshot1));
        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testGetCompositeSnapshotData() throws Exception {
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of("ref"));

        when(nodeDAO.getCompositeSnapshotData("id")).thenReturn(compositeSnapshotData);

        MockHttpServletRequestBuilder request = get("/composite-snapshot/id").contentType(JSON)
                .content(objectMapper.writeValueAsString(compositeSnapshotData));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, CompositeSnapshotData.class);

    }

    @Test
    public void testGetCompositeSnapshotNodes() throws Exception {
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of("ref"));

        when(nodeDAO.getCompositeSnapshotData("id")).thenReturn(compositeSnapshotData);
        when(nodeDAO.getNodes(List.of("id"))).thenReturn(List.of(Node.builder().nodeType(NodeType.SNAPSHOT)
                .name("name").uniqueId("ref").build()));

        MockHttpServletRequestBuilder request = get("/composite-snapshot/id/nodes");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, new TypeReference<List<Node>>() {
        });
    }

    @Test
    public void testGetCompositeSnapshotItems() throws Exception {

        when(nodeDAO.getSnapshotItemsFromCompositeSnapshot("id"))
                .thenReturn(List.of(new SnapshotItem()));

        MockHttpServletRequestBuilder request = get("/composite-snapshot/id/items");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, new TypeReference<List<SnapshotItem>>() {
        });
    }

    @Test
    public void testGetCompositeSnapshotConsistency() throws Exception {

        when(nodeDAO.checkForPVNameDuplicates(Mockito.any(List.class))).thenReturn(List.of("ref"));

        MockHttpServletRequestBuilder request = post("/composite-snapshot-consistency-check")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("id")));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, new TypeReference<List<String>>() {
        });

        request = post("/composite-snapshot-consistency-check")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("id")));
        mockMvc.perform(request).andExpect(status().isUnauthorized());


        request = post("/composite-snapshot-consistency-check")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("id")));

        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/composite-snapshot-consistency-check")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("id")));

        mockMvc.perform(request).andExpect(status().isOk());
    }
}
