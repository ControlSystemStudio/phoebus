/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.controllers;


import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.phoebus.service.saveandrestore.web.config.WebSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application.properties")
public class HelpResourceTest{

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    public void testGetSearchHelp() throws  Exception{
        MockHttpServletRequestBuilder request = get("/help/SearchHelp");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testGetSearchHelpAcceptLanguage() throws  Exception{
        MockHttpServletRequestBuilder request = get("/help/SearchHelp")
                .header("Accept-Language", "xx-YY");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testGetSearchHelpAcceptLanguageParameter() throws  Exception{
        MockHttpServletRequestBuilder request = get("/help/SearchHelp?lang=xx");
        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testGetCheatSheetUnsupportedHelpType() throws  Exception{
        MockHttpServletRequestBuilder request = get("/help/unsupported");
        mockMvc.perform(request).andExpect(status().isNotFound());
    }
}
