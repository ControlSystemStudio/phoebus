package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
@WebMvcTest(TakeSnapshotController.class)
public class TakeSnapshotControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    private SnapshotUtil snapshotUtil = Mockito.mock(SnapshotUtil.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;


    @Test
    public void testTakeSnapshot() throws Exception {
        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setPvList(List.of(ConfigPv.builder().pvName("pvName").build()));

        when(nodeDAO.getNode("uniqueId")).thenReturn(Node.builder().name("name").uniqueId("uniqueId").build());
        when(nodeDAO.getConfigurationData("uniqueId")).thenReturn(configurationData);
        when(snapshotUtil.takeSnapshot(configurationData)).thenReturn(Collections.emptyList());

        MockHttpServletRequestBuilder request = get("/take-snapshot/uniqueId");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response is in the Restore Result json format
        objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<SnapshotItem>>() {
                });
    }

    @Test
    public void testTakeSnapshotBadConfigId() throws Exception {

        when(nodeDAO.getNode("uniqueId")).thenReturn(Node.builder().name("name").uniqueId("uniqueId").build());
        when(nodeDAO.getConfigurationData("uniqueId")).thenThrow(new NodeNotFoundException(""));

        MockHttpServletRequestBuilder request = get("/take-snapshot/uniqueId");

        mockMvc.perform(request).andExpect(status().isNotFound());

    }
}
