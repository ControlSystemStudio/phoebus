/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.search.SearchResult;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.search.SearchUtil;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.phoebus.service.saveandrestore.web.config.WebSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application.properties")
public class SearchControllerTest {

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
    private SearchUtil searchUtil;

    @Value("${elasticsearch.configuration_node.index:saveandrestore_configuration}")
    public String ES_CONFIGURATION_INDEX;

    @Test
    public void testSearch() throws Exception {
        SearchResult searchResult = new SearchResult();
        searchResult.setHitCount(1);
        searchResult.setNodes(List.of(Node.builder().name("node").build()));

        when(nodeDAO.search(Mockito.any())).thenReturn(searchResult);

        MockHttpServletRequestBuilder request = get("/search").contentType(JSON).params(new LinkedMultiValueMap<>());

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        SearchResult searchResult1 = objectMapper.readValue(s, SearchResult.class);
        assertEquals(1, searchResult1.getHitCount());
    }

    @Test
    public void testSearchForPVs() {
        MultiValueMap<String, List<String>> searchParams = new LinkedMultiValueMap<>();
        searchParams.put("type", List.of(List.of(NodeType.CONFIGURATION.toString())));
        searchParams.put("pvs", List.of(List.of("abc")));

        SearchRequest searchRequest = searchUtil.buildSearchRequestForPvs(List.of("abc"));
        assertEquals(ES_CONFIGURATION_INDEX, searchRequest.index().get(0));
        assertEquals("pvList", searchRequest.query().bool().must().get(0).disMax().queries().get(0).match().field());
        assertEquals("abc", searchRequest.query().bool().must().get(0).disMax().queries().get(0).match().query().stringValue());
    }
}
