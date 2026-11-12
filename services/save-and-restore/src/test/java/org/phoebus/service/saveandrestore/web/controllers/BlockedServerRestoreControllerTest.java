package org.phoebus.service.saveandrestore.web.controllers;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VFloat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ControllersTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.main.allow-bean-definition-overriding=true")
@TestPropertySource(locations = "classpath:test_application_block_server_restore.properties")
public class BlockedServerRestoreControllerTest {

    @Autowired
    private String userAuthorization;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    public void testServerRestoreBlocked() throws Exception{
        MockHttpServletRequestBuilder request = post("/restore/node?nodeId=uniqueId")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization);
        mockMvc.perform(request).andExpect(status().isNotFound());
    }
}
