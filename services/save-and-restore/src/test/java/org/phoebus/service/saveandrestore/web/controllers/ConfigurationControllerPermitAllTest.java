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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(ConfigurationController.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class ConfigurationControllerPermitAllTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private String demoUser;

    @Test
    public void testCreateConfiguration() throws Exception {

        reset(nodeDAO);

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(Node.builder().build());
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(Collections.emptyList());
        configuration.setConfigurationData(configurationData);
        MockHttpServletRequestBuilder request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        request = put("/config?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        request =  put("/config?parentNodeId=a")
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testUpdateConfiguration() throws Exception {

        Node configurationNode = Node.builder().uniqueId("uniqueId").nodeType(NodeType.CONFIGURATION).userName(demoUser).build();

        Configuration configuration = new Configuration();
        configuration.setConfigurationNode(configurationNode);

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);

        MockHttpServletRequestBuilder request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);

        request = post("/config")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isOk());

        when(nodeDAO.getNode("uniqueId")).thenReturn(configurationNode);

        request = post("/config")
                .contentType(JSON).content(objectMapper.writeValueAsString(configuration));

        mockMvc.perform(request).andExpect(status().isUnauthorized());

    }
}
