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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
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
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
@WebMvcTest(NodeController.class)
/**
 * Main purpose of the tests in this class is to verify that REST end points are
 * maintained, i.e. that URLs are not changed and that they return the correct
 * data.
 *
 * @author Georg Weiss, European Spallation Source
 *
 */
public class NodeControllerPermitAllTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    private static Node folderFromClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private String demoUser;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @BeforeAll
    public static void setUp() {
        folderFromClient = Node.builder().name("SomeFolder").userName("myusername").uniqueId("11").build();
    }

    @Test
    public void testCreateFolder() throws Exception {

        when(nodeDAO.createNode(Mockito.any(String.class), Mockito.any(Node.class))).thenReturn(folderFromClient);

        String content = objectMapper.writeValueAsString(folderFromClient);

        MockHttpServletRequestBuilder request = put("/node?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(content);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Node.class);

        request = put("/node?parentNodeId=a")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(content);
        mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON));

        request = put("/node?parentNodeId=a")
                .contentType(JSON)
                .content(content);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteFolder() throws Exception {

        MockHttpServletRequestBuilder request =
                post("/node");

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        when(nodeDAO.getNode("a")).thenReturn(Node.builder().uniqueId("a").userName(demoUser).build());

        request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, userAuthorization);
        mockMvc.perform(request).andExpect(status().isOk());

        when(nodeDAO.getNode("a")).thenReturn(Node.builder().uniqueId("a").userName(demoUser).build());

        request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization);
        mockMvc.perform(request).andExpect(status().isOk());

        when(nodeDAO.getNode("a")).thenReturn(Node.builder().uniqueId("a").userName(demoUser).build());

        request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")))
                        .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization);
        mockMvc.perform(request).andExpect(status().isOk());

        request =
                delete("/node")
                        .contentType(JSON).content(objectMapper.writeValueAsString(List.of("a")));
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testGetFolderIllegalArgument() throws Exception {
        when(nodeDAO.getNode("a")).thenThrow(IllegalArgumentException.class);

        MockHttpServletRequestBuilder request = get("/node/a");

        mockMvc.perform(request).andExpect(status().isBadRequest());

    }

    @Test
    public void testUpdateNode() throws Exception {

        reset(nodeDAO);

        Node node = Node.builder().name("foo").uniqueId("a").userName(demoUser).build();

        when(nodeDAO.getNode("a")).thenReturn(node);
        when(nodeDAO.updateNode(Mockito.any(Node.class), Mockito.anyBoolean())).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/node")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .param("customTimeForMigration", "false")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(node));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        objectMapper.readValue(result.getResponse().getContentAsString(), Node.class);

        node = Node.builder().name("foo").uniqueId("a").userName("notDemoUser").build();

        when(nodeDAO.getNode("a")).thenReturn(node);
        when(nodeDAO.updateNode(Mockito.any(Node.class), Mockito.anyBoolean())).thenReturn(node);


        request = post("/node")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .param("customTimeForMigration", "false")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(node));
        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/node")
                .param("customTimeForMigration", "false")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(node));
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }
}
