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
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.Node;
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

import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class StructureControllerPermitAllTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

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


    @Test
    public void testMoveNode() throws Exception {
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

        when(nodeDAO.moveNodes(List.of("a"), "b", demoUser))
                .thenReturn(Node.builder().uniqueId("2").uniqueId("a").userName(demoUser).build());

        request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isOk());


        request = post("/move")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/move")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of
                        ("a")))
                .param("to", "b")
                .param("username", "username");

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testCopyNodes() throws Exception {
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
        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/copy")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/copy")
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of("a")))
                .param("to", "target");
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }
}
