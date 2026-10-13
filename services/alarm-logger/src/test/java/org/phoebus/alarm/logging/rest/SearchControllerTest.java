/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.alarm.logging.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit test for {@link SearchController} using standalone MockMvc setup.
 * Migrated from @WebMvcTest (removed in Spring Boot 4.x) to
 * MockMvcBuilders.standaloneSetup() which requires no Spring context.
 */
public class SearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController()).build();
    }

    @Test
    public void testRedirectSwagger() throws Exception {
        MockHttpServletRequestBuilder request = get("/swagger-ui");
        ResultActions resultActions = mockMvc.perform(request).andExpect(status().isMovedPermanently());
        assertEquals("/swagger-ui/index.html", resultActions.andReturn().getResponse().getHeader("Location"));
    }
}
