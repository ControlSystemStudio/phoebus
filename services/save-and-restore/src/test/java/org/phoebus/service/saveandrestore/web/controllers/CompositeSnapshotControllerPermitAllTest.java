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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(CompositeSnapshotController.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class CompositeSnapshotControllerPermitAllTest {

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String demoUser;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    public void testCreateCompositeSnapshot() throws Exception {

        when(nodeDAO.createCompositeSnapshot(Mockito.any(String.class), Mockito.any(CompositeSnapshot.class))).thenReturn(compositeSnapshot);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot);

        MockHttpServletRequestBuilder request = put("/composite-snapshot?parentNodeId=id")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, CompositeSnapshot.class);

        request = put("/composite-snapshot?parentNodeId=id")
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
        reset(nodeDAO);
    }

    @Test
    public void testUpdateCompositeSnapshot() throws Exception {

        Node node = Node.builder().uniqueId("c").nodeType(NodeType.COMPOSITE_SNAPSHOT).userName(demoUser).build();
        CompositeSnapshot compositeSnapshot1 = new CompositeSnapshot();
        compositeSnapshot1.setCompositeSnapshotNode(node);

        String compositeSnapshotString = objectMapper.writeValueAsString(compositeSnapshot1);

        when(nodeDAO.updateCompositeSnapshot(compositeSnapshot1)).thenReturn(compositeSnapshot1);
        when(nodeDAO.getNode("c")).thenReturn(node);

        MockHttpServletRequestBuilder request = post("/composite-snapshot")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(compositeSnapshotString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, CompositeSnapshot.class);

        request = post("/composite-snapshot")
                .contentType(JSON)
                .content(compositeSnapshotString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());

        reset(nodeDAO);
    }
}
