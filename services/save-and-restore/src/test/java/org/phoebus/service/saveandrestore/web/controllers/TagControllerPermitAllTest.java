/*
 * Copyright (C) 2020 European Spallation Source ERIC.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package org.phoebus.service.saveandrestore.web.controllers;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.phoebus.service.saveandrestore.web.config.WebSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {ControllersTestConfig.class, WebSecurityConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class TagControllerPermitAllTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private String demoUser;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testAddTag() throws Exception{
        Tag tag = new Tag();
        tag.setName("tag");

        Node node = Node.builder().name("name").uniqueId("uniqueId").userName(demoUser).tags(List.of(tag)).build();

        TagData tagData = new TagData();
        tagData.setTag(tag);
        tagData.setUniqueNodeIds(List.of("uniqueId"));

        when(nodeDAO.getNode("uniqueId")).thenReturn(node);
        when(nodeDAO.addTag(tagData)).thenReturn(List.of(node));

        MockHttpServletRequestBuilder request = post("/tags").contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .content(objectMapper.writeValueAsString(tagData));
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, List.class);

        request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testGoldenTag() throws Exception{
        Tag tag = new Tag();
        tag.setName(Tag.GOLDEN);
        tag.setUserName(demoUser);

        TagData tagData = new TagData();
        tagData.setTag(tag);
        tagData.setUniqueNodeIds(List.of("uniqueId"));

        MockHttpServletRequestBuilder request = post("/tags").contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .content(objectMapper.writeValueAsString(tagData));

        mockMvc.perform(request).andExpect(status().isOk());

        request = post("/tags").contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .content(objectMapper.writeValueAsString(tagData));

        mockMvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void testDeleteTag() throws Exception{
        Tag tag = new Tag();
        tag.setName("tag");
        tag.setUserName(demoUser);

        TagData tagData = new TagData();
        tagData.setTag(tag);
        tagData.setUniqueNodeIds(List.of("uniqueId"));

        Node node = Node.builder().name("name").uniqueId("uniqueId").userName("otherUser").tags(List.of(tag)).build();

        when(nodeDAO.getNode("uniqueId")).thenReturn(node);

        MockHttpServletRequestBuilder request = delete("/tags").contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .content(objectMapper.writeValueAsString(tagData));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        request = delete("/tags").contentType(JSON)
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .content(objectMapper.writeValueAsString(tagData));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        request = delete("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());

    }
}
