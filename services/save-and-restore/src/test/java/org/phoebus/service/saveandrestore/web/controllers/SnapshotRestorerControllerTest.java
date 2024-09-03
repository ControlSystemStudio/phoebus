package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VFloat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
@WebMvcTest(SnapshotRestoreController.class)
public class SnapshotRestorerControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String userAuthorization;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;


    @Test
    public void testRestoreFromSnapshotNode() throws Exception {

        SnapshotData snapshotData = new SnapshotData();
        SnapshotItem item = new SnapshotItem();
        ConfigPv configPv = new ConfigPv();
        configPv.setPvName("loc://x");
        item.setValue(VFloat.of(1.0, Alarm.none(), Time.now(), Display.none()));
        item.setConfigPv(configPv);
        snapshotData.setSnapshotItems(List.of(item));

        when(nodeDAO.getNode("uniqueId")).thenReturn(Node.builder().name("name").uniqueId("uniqueId").build());
        when(nodeDAO.getSnapshotData("uniqueId")).thenReturn(snapshotData);

        MockHttpServletRequestBuilder request = post("/restore/node?nodeId=uniqueId")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response is in the Restore Result json format
        objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<RestoreResult>>() {
                });
    }

    @Test
    public void testRestoreFromSnapshotItems() throws Exception {

        SnapshotItem item = new SnapshotItem();
        ConfigPv configPv = new ConfigPv();
        configPv.setPvName("loc://x");
        item.setValue(VFloat.of(1.0, Alarm.none(), Time.now(), Display.none()));
        item.setConfigPv(configPv);

        MockHttpServletRequestBuilder request = post("/restore/items")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(objectMapper.writeValueAsString(List.of(item)));

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        // Make sure response is in the Restore Result json format
        objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<RestoreResult>>() {
                });
    }
}
