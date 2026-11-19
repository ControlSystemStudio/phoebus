/*
 * Copyright (C) 2026 European Spallation Source ERIC.
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.LoginCredentials;
import org.phoebus.applications.saveandrestore.model.UserData;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {ControllersTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application.properties")
public class AuthenticationControllerTest extends BaseController{

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String serverRestoreAuthorization;

    @Autowired
    public String roleUser;

    @Value("${demo.user:user}")
    private String demoUser;

    @Value("${demo.user.password:userPass}")
    private String demoUserPassword;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    public void testCheckServerRestoreAuthorizationRegularUser() throws Exception{
        MockHttpServletRequestBuilder request = post("/server-restore-authorized")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        assertEquals("false", result.getResponse().getContentAsString());
    }

    @Test
    public void testCheckServerRestoreAuthorizationServerRestoreUser() throws Exception{
        MockHttpServletRequestBuilder request = post("/server-restore-authorized")
                .header(HttpHeaders.AUTHORIZATION, serverRestoreAuthorization);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk())
                .andReturn();

        assertEquals("true", result.getResponse().getContentAsString());
    }

    @Test
    public void testCheckServerRestoreUnauthenticated() throws Exception{
        MockHttpServletRequestBuilder request = post("/server-restore-authorized");

        mockMvc.perform(request).andExpect(status().isUnauthorized());

    }

    @Test
    public void testSuccessfulLogin() throws Exception{

        LoginCredentials loginCredentials = new LoginCredentials(demoUser, demoUserPassword);
        MockHttpServletRequestBuilder request = post("/login")
                .contentType(JSON).content(objectMapper.writeValueAsString(loginCredentials));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response contains expected data
        UserData userData = objectMapper.readValue(result.getResponse().getContentAsString(), UserData.class);

        assertEquals(demoUser, userData.getUserName());
        assertTrue(userData.getRoles().size() > 1); // Spring Security adds role related to authentication procedure
        assertTrue(userData.getRoles().contains("ROLE_" + roleUser));
    }

    @Test
    public void testLoginBadCredentials() throws Exception{

        LoginCredentials loginCredentials = new LoginCredentials("foo", "bar");
        MockHttpServletRequestBuilder request = post("/login")
                .contentType(JSON).content(objectMapper.writeValueAsString(loginCredentials));

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginMissingCredentials() throws Exception{

         MockHttpServletRequestBuilder request = post("/login")
                .contentType(JSON);

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }
}
