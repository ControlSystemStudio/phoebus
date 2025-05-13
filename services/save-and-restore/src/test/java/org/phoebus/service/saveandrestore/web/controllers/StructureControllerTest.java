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
import org.phoebus.applications.saveandrestore.model.Node;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@WebMvcTest(StructureController.class)
public class StructureControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private String demoUser;

    @Autowired
    private String demoAdmin;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String adminAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @AfterEach
    public void resetMocks(){
        reset(webSocketHandler, nodeDAO);
    }

    @Test
    public void testMoveNode1() throws Exception {
        when(nodeDAO.moveNodes(List.of("a"), "b", demoAdmin))
                .thenReturn(Node.builder().uniqueId("2").uniqueId("a").userName(demoAdmin).build());

        MockHttpServletRequestBuilder request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);

        verify(webSocketHandler, times(2)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testMoveNode2() throws Exception{

        when(nodeDAO.moveNodes(List.of("a"), "b", demoUser))
                .thenReturn(Node.builder().uniqueId("2").uniqueId("a").userName(demoUser).build());

        MockHttpServletRequestBuilder request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testMoveNode3() throws Exception{

        MockHttpServletRequestBuilder request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isForbidden());

        request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testMoveNodeSourceNodeListEmpty() throws Exception {
        MockHttpServletRequestBuilder request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Collections.emptyList()))
                .param("to", "targetId")
                .param("username", "user");

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testMoveNodeTargetIdEmpty() throws Exception {
        MockHttpServletRequestBuilder request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "")
                .param("username", "user");

        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCopyNodes1() throws Exception {
        MockHttpServletRequestBuilder request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCopyNodes2() throws Exception{

        MockHttpServletRequestBuilder request = post("/copy")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCopyNodesBadRequest1() throws Exception {

        MockHttpServletRequestBuilder request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "");
        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCopyNodesBadRequest2() throws Exception {

        MockHttpServletRequestBuilder request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")));
        mockMvc.perform(request).andExpect(status().isBadRequest());

    }

    @Test
    public void testCopyNodesBadRequest3() throws Exception {

        MockHttpServletRequestBuilder request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(Collections.emptyList()))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isBadRequest());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }
}
