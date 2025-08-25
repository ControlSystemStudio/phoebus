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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.Comparison;
import org.phoebus.applications.saveandrestore.model.ConfigPv;

import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;

import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;

import org.phoebus.applications.saveandrestore.model.ComparisonMode;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(ConfigurationController.class)
@TestPropertySource(locations = "classpath:test_application.properties")
public class ConfigurationControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String adminAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private String demoUser;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @AfterEach
    public void resetMocks(){
        reset(nodeDAO, webSocketHandler);
    }

    @Test
    public void testCreateConfiguration1() throws Exception {

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("foo").readbackPvName("bar").build()));

        configuration.setConfigurationData(configurationData);

        when(nodeDAO.createConfiguration(Mockito.anyString(), Mockito.any(Configuration.class)))
                .thenReturn(configuration);
        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateConfiguration2() throws Exception {

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Collections.emptyList());
        configuration.setConfigurationData(configurationData);

        when(nodeDAO.createConfiguration(Mockito.anyString(), Mockito.any(Configuration.class)))
                .thenReturn(configuration);

        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateConfiguration3() throws Exception {


        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());

        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testCreateConfiguration4() throws Exception{

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());

        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateConfiguration1() throws Exception {

        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName(demoUser).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(configurationNode);

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);
        when(nodeDAO.updateConfiguration(Mockito.any(Configuration.class)))
                .thenReturn(configuration);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void tesUpdateConfiguration2() throws Exception {

        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName(demoUser).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(configurationNode);

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);
        when(nodeDAO.updateConfiguration(Mockito.any(Configuration.class)))
                .thenReturn(configuration);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateConfiguration3() throws Exception {

        Configuration configuration = new Configuration();
        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName("someUser").build();
        configuration.setConfigurationNode(configurationNode);

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);
        when(nodeDAO.updateConfiguration(Mockito.any(Configuration.class)))
                .thenReturn(configuration);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        verify(webSocketHandler, times(1)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateConfiguration4() throws Exception {

        Configuration configuration = new Configuration();
        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName("someUser").build();
        configuration.setConfigurationNode(configurationNode);

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateConfiguration5() throws Exception {

        Configuration configuration = new Configuration();
        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName("someUser").build();
        configuration.setConfigurationNode(configurationNode);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isForbidden());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
    }

    @Test
    public void testUpdateConfiguration6() throws Exception{

        Configuration configuration = new Configuration();
        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName("someUser").build();
        configuration.setConfigurationNode(configurationNode);

        MockHttpServletRequestBuilder request = post("/config")
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        verify(webSocketHandler, times(0)).sendMessage(Mockito.any(SaveAndRestoreWebSocketMessage.class));
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCreateInvalidConfiguration() throws Exception {

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());
        ConfigurationData configurationData = new ConfigurationData();
        configuration.setConfigurationData(configurationData);
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("foo").build(),
                ConfigPv.builder().pvName("fooo").comparison(new Comparison(null, 0.1)).build()));
        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        configurationData.setPvList(List.of(
                ConfigPv.builder().pvName("fooo").readbackPvName("bar").comparison(new Comparison(null, 0.1)).build()));

        configuration.setConfigurationData(configurationData);

        request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isBadRequest());

        configurationData.setPvList(List.of(
                ConfigPv.builder().pvName("fooo").readbackPvName("bar").comparison(new Comparison(ComparisonMode.RELATIVE, -0.1))
                        .build()));

        configuration.setConfigurationData(configurationData);

        request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, adminAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }
}
