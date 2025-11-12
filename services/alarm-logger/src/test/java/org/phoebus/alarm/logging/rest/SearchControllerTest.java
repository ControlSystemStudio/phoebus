/*
 * Copyright (C) 2025 European Spallation Source ERIC.
 */

package org.phoebus.alarm.logging.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(SearchController.class)
public class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testRedirectSwagger() throws Exception {
        MockHttpServletRequestBuilder request = get("/swagger-ui");
        ResultActions resultActions = mockMvc.perform(request).andExpect(status().isMovedPermanently());
        assertEquals("/swagger-ui/index.html", resultActions.andReturn().getResponse().getHeader("Location"));
    }
}
