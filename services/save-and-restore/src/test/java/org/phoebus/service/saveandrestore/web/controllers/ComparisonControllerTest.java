/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.applications.saveandrestore.model.ComparisonResult;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@TestPropertySource(locations = "classpath:test_application.properties")
@WebMvcTest(NodeController.class)
public class ComparisonControllerTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testCompareNegativeTolerance() throws Exception {
        MockHttpServletRequestBuilder request = get("/compare/nodeId?tolerance=-1");
        mockMvc.perform(request).andExpect(status().isBadRequest());

        request = get("/compare/nodeId?tolerance=notanumber");
        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    public void testNodeNotFound() throws Exception {
        when(nodeDAO.getNode("nodeId")).thenReturn(null);
        MockHttpServletRequestBuilder request = get("/compare/nodeId");
        mockMvc.perform(request).andExpect(status().isNotFound());
        reset(nodeDAO);
    }

    @Test
    public void testBadNodeType() throws Exception {
        when(nodeDAO.getNode("nodeId")).thenReturn(Node.builder().nodeType(NodeType.FOLDER).build());
        MockHttpServletRequestBuilder request = get("/compare/nodeId");
        mockMvc.perform(request).andExpect(status().isBadRequest());

        reset(nodeDAO);
    }

    @Test
    public void testSingleSnapshot() throws Exception {
        when(nodeDAO.getNode("nodeId")).
                thenReturn(Node.builder().uniqueId("nodeId").nodeType(NodeType.SNAPSHOT).build());

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://x(42.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://y(771.0)");

        when(nodeDAO.getParentNode("nodeId")).thenReturn(Node.builder().nodeType(NodeType.CONFIGURATION).uniqueId("configId").build());

        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setUniqueId("configId");
        configurationData.setPvList(List.of(configPv1, configPv2));

        when(nodeDAO.getConfigurationData("configId")).thenReturn(configurationData);

        SnapshotData snapshotData = new SnapshotData();
        //snapshotData.setUniqueId("uniqueId");
        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(42.0, Alarm.none(),
                Time.now(), Display.none()));
        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(),
                Time.now(), Display.none()));
        snapshotData.setSnapshotItems(List.of(snapshotItem1, snapshotItem2));

        when(nodeDAO.getSnapshotData("nodeId")).thenReturn(snapshotData);

        MockHttpServletRequestBuilder request = get("/compare/nodeId?skipReadback=TRUE");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();
        List<ComparisonResult> compareResults =
                objectMapper.readValue(result.getResponse().getContentAsString(),
                        new TypeReference<>() {
                        });
        assertEquals(2, compareResults.size());
        compareResults.forEach(cr -> assertTrue(cr.isEqual()));

        reset(nodeDAO);

    }

    @Test
    public void testCompositeSnapshot() throws Exception{
        when(nodeDAO.getNode("nodeId")).
                thenReturn(Node.builder().uniqueId("nodeId").nodeType(NodeType.COMPOSITE_SNAPSHOT).build());
        CompositeSnapshotData compositeSnapshotData = new CompositeSnapshotData();
        compositeSnapshotData.setReferencedSnapshotNodes(List.of("id1", "id2"));
        when(nodeDAO.getCompositeSnapshotData("nodeId")).thenReturn(compositeSnapshotData);

        ConfigPv configPv1 = new ConfigPv();
        configPv1.setPvName("loc://x(42.0)");
        ConfigPv configPv2 = new ConfigPv();
        configPv2.setPvName("loc://y(771.0)");

        SnapshotData snapshotData1 = new SnapshotData();
        SnapshotData snapshotData2 = new SnapshotData();
        SnapshotItem snapshotItem1 = new SnapshotItem();
        snapshotItem1.setConfigPv(configPv1);
        snapshotItem1.setValue(VDouble.of(42.0, Alarm.none(),
                Time.now(), Display.none()));
        SnapshotItem snapshotItem2 = new SnapshotItem();
        snapshotItem2.setConfigPv(configPv2);
        snapshotItem2.setValue(VDouble.of(771.0, Alarm.none(),
                Time.now(), Display.none()));
        snapshotData1.setSnapshotItems(List.of(snapshotItem1));
        snapshotData2.setSnapshotItems(List.of(snapshotItem2));

        when(nodeDAO.getSnapshotData("id1")).thenReturn(snapshotData1);
        when(nodeDAO.getSnapshotData("id2")).thenReturn(snapshotData2);

        when(nodeDAO.getNode("id1")).thenReturn(Node.builder().name("id1").uniqueId("id1").nodeType(NodeType.SNAPSHOT).build());
        when(nodeDAO.getNode("id2")).thenReturn(Node.builder().name("id2").uniqueId("id2").nodeType(NodeType.SNAPSHOT).build());
        when(nodeDAO.getParentNode("id1")).thenReturn(Node.builder().nodeType(NodeType.CONFIGURATION).name("id1parent").uniqueId("id1parent").build());
        when(nodeDAO.getParentNode("id2")).thenReturn(Node.builder().nodeType(NodeType.CONFIGURATION).name("id2parent").uniqueId("id2parent").build());

        ConfigurationData configurationData1 = new ConfigurationData();
        configurationData1.setPvList(List.of(configPv1, configPv2));

        when(nodeDAO.getConfigurationData("id1parent")).thenReturn(configurationData1);

        ConfigurationData configurationData2 = new ConfigurationData();
        configurationData2.setPvList(List.of(configPv1, configPv2));

        when(nodeDAO.getConfigurationData("id2parent")).thenReturn(configurationData2);

        MockHttpServletRequestBuilder request = get("/compare/nodeId");

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();
        List<ComparisonResult> compareResults =
                objectMapper.readValue(result.getResponse().getContentAsString(),
                        new TypeReference<>() {
                        });
        assertEquals(2, compareResults.size());
        compareResults.forEach(cr -> assertTrue(cr.isEqual()));

        reset(nodeDAO);
    }

}
