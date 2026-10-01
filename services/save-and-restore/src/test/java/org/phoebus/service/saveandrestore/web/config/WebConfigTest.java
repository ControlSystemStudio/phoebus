/**
 * Copyright (C) 2026 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {WebConfiguration.class, ControllersTestConfig.class, WebSecurityConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:test_application.properties",
        properties = "spring.main.allow-bean-definition-overriding=true")
@SuppressWarnings("unused")
public class WebConfigTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Test
    public void testWebConfig() {
        Assertions.assertNotNull(nodeDAO);
    }
}
