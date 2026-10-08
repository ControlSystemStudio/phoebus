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
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.search.Filter;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class FilterControllerPermitAllTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private String demoUser;

    @Test
    public void testSaveFilter() throws Exception {

        reset(nodeDAO);

        Filter filter = new Filter();
        filter.setName("name");
        filter.setQueryString("query");
        filter.setUser("user");

        String filterString = objectMapper.writeValueAsString(filter);

        when(nodeDAO.saveFilter(Mockito.any(Filter.class))).thenReturn(filter);

        MockHttpServletRequestBuilder request = put("/filter")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(filterString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Filter.class);

        request = put("/filter")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(filterString);

        mockMvc.perform(request).andExpect(status().isOk());

        request = put("/filter")
                .contentType(JSON)
                .content(filterString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteFilter() throws Exception {
        Filter filter = new Filter();
        filter.setName("name");
        filter.setQueryString("query");
        filter.setUser(demoUser);

        when(nodeDAO.getAllFilters()).thenReturn(List.of(filter));

        MockHttpServletRequestBuilder request = delete("/filter/name")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        request = delete("/filter/name")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        request = delete("/filter/name")
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

}
