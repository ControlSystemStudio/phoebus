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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.applications.saveandrestore.model.TagData;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.MockMvc.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(NodeController.class)
public class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NodeDAO nodeDAO;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGetAllTags() throws Exception{
        Tag tag = new Tag();
        tag.setName("tag");
        List<Tag> tags = List.of(tag);
        when(nodeDAO.getAllTags()).thenReturn(tags);

        MockHttpServletRequestBuilder request = get("/tags");
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, List.class);
    }

    @Test
    public void testAddTag() throws Exception{
        Tag tag = new Tag();
        tag.setName("tag");

        Node node = Node.builder().name("name").uniqueId("uniqueId").tags(List.of(tag)).build();

        TagData tagData = new TagData();
        tagData.setTag(tag);
        tagData.setUniqueNodeIds(List.of("uniqueId"));
        when(nodeDAO.addTag(tagData)).thenReturn(List.of(node));

        MockHttpServletRequestBuilder request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, List.class);
    }

    @Test
    public void testAddTagBadData() throws Exception{

        TagData tagData = new TagData();
        tagData.setUniqueNodeIds(List.of("uniqueId"));
        MockHttpServletRequestBuilder request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        Tag tag = new Tag();
        tag.setName(null);
        tagData.setTag(tag);
        request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        tag.setName("");
        tagData.setTag(tag);
        request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());

        tag.setName("name");
        tagData.setTag(tag);
        tagData.setUniqueNodeIds(null);
        request = post("/tags").contentType(JSON)
                .content(objectMapper.writeValueAsString(tagData));
        mockMvc.perform(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDeleteTag() throws Exception{

    }
}
