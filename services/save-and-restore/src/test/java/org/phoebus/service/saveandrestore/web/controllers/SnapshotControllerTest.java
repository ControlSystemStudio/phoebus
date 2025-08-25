/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
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
import java.util.Set;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@WebMvcTest(SnapshotController.class)
public class SnapshotControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String adminAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private String demoUser;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @AfterEach
    public void resetMocks(){
        reset(webSocketHandler, nodeDAO);
    }


    @Test
    public void testSaveSnapshotWrongNodeType() throws Exception {

        Node node = Node.builder().uniqueId("uniqueId").userName(demoUser).nodeType(NodeType.FOLDER).build();

        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        when(nodeDAO.getNode("uniqueId")).thenReturn(node);

        MockHttpServletRequestBuilder request = put("/snapshot?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(snapshot));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testSaveSnapshotNoParentNodeId() throws Exception {
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(Node.builder().nodeType(NodeType.SNAPSHOT).build());

        MockHttpServletRequestBuilder request = put("/snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(snapshot));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateSnapshot1() throws Exception {
        Node node = Node.builder().uniqueId("uniqueId").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        when(nodeDAO.getNode("uniqueId")).thenReturn(node);
        when(nodeDAO.createSnapshot(Mockito.any(String.class), Mockito.any(Snapshot.class)))
                .thenAnswer((Answer<Snapshot>) invocation -> snapshot);

        MockHttpServletRequestBuilder request = put("/snapshot?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(snapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Snapshot.class);

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateSnapshot2() throws Exception {

        Node node = Node.builder().uniqueId("uniqueId").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        MockHttpServletRequestBuilder request = put("/snapshot?parentNodeId=a")
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isUnauthorized());

        request = put("/snapshot?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateSnapshot3() throws Exception{

        Node node = Node.builder().uniqueId("uniqueId").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        when(nodeDAO.createSnapshot(Mockito.anyString(), Mockito.any(Snapshot.class)))
                .thenReturn(snapshot);

        MockHttpServletRequestBuilder request = put("/snapshot?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateSnapshot1() throws Exception {
        Node node = Node.builder().uniqueId("s").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        when(nodeDAO.getNode("s")).thenReturn(node);
        when(nodeDAO.updateSnapshot(Mockito.any(Snapshot.class)))
                .thenAnswer((Answer<Snapshot>) invocation -> snapshot);

        MockHttpServletRequestBuilder request = post("/snapshot")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(snapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Snapshot.class);

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateSnapshot2() throws Exception {

        Node node = Node.builder().uniqueId("s").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        MockHttpServletRequestBuilder request = put("/snapshot")
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateSnapshot3() throws Exception {

        Node node = Node.builder().uniqueId("s").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        MockHttpServletRequestBuilder request = post("/snapshot")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateSnapshot4() throws Exception{

        Node node = Node.builder().uniqueId("s").nodeType(NodeType.SNAPSHOT).userName(demoUser).build();
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);

        String snapshotString = objectMapper.writeValueAsString(snapshot);

        when(nodeDAO.updateSnapshot(Mockito.any(Snapshot.class)))
                .thenReturn(snapshot);

        MockHttpServletRequestBuilder request = post("/snapshot")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(snapshotString);
        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testDeleteSnapshot1() throws Exception {

        Node node = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .uniqueId("a").userName(demoUser).build();

        when(nodeDAO.getNode("a")).thenReturn(node);

        //when(nodeDAO.deleteNodes(List.of("a"))).thenReturn(Set.of("a"));

        MockHttpServletRequestBuilder request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization);

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testDeleteSnapshot2() throws Exception {

        MockHttpServletRequestBuilder request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization);

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testDeleteSnapshot3() throws Exception {

        Node node = Node.builder()
                .nodeType(NodeType.SNAPSHOT)
                .uniqueId("a").userName("otherUser").build();

        when(nodeDAO.getNode("a")).thenReturn(node);
        //when(nodeDAO.deleteNodes(List.of("a"))).thenReturn(Set.of("a"));

        MockHttpServletRequestBuilder request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, adminAuthorization);

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testDeleteSnapshot4() throws Exception{

        MockHttpServletRequestBuilder request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")));

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }
}
