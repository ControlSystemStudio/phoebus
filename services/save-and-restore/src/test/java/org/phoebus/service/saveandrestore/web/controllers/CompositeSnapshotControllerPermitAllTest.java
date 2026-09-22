/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.phoebus.service.saveandrestore.web.config.WebSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
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

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class CompositeSnapshotControllerPermitAllTest {

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String demoUser;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

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
