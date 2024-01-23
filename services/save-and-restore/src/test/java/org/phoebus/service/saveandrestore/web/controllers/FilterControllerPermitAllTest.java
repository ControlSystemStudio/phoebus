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
import org.phoebus.applications.saveandrestore.model.search.Filter;
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

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.phoebus.service.saveandrestore.web.controllers.BaseController.JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
@WebMvcTest(FilterController.class)
@TestPropertySource(locations = "classpath:test_application_permit_all.properties")
public class FilterControllerPermitAllTest {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private String userAuthorization;

    @Autowired
    private String readOnlyAuthorization;

    @Autowired
    private String demoUser;

    @Test
    public void testSaveFilter() throws Exception {

        reset(nodeDAO);

        Filter filter = new Filter();
        filter.setName("name");
        filter.setQueryString("query");
        filter.setUser("user");

        String filterString = objectMapper.writeValueAsString(filter);

        when(nodeDAO.saveFilter(Mockito.any(Filter.class))).thenReturn(filter);

        MockHttpServletRequestBuilder request = put("/filter")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON)
                .content(filterString);

        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andExpect(content().contentType(JSON))
                .andReturn();

        String s = result.getResponse().getContentAsString();
        // Make sure response contains expected data
        objectMapper.readValue(s, Filter.class);

        request = put("/filter")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON)
                .content(filterString);

        mockMvc.perform(request).andExpect(status().isOk());

        request = put("/filter")
                .contentType(JSON)
                .content(filterString);

        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @Test
    public void testDeleteFilter() throws Exception {
        Filter filter = new Filter();
        filter.setName("name");
        filter.setQueryString("query");
        filter.setUser(demoUser);

        when(nodeDAO.getAllFilters()).thenReturn(List.of(filter));

        MockHttpServletRequestBuilder request = delete("/filter/name")
                .header(HttpHeaders.AUTHORIZATION, userAuthorization)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        request = delete("/filter/name")
                .header(HttpHeaders.AUTHORIZATION, readOnlyAuthorization)
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isOk());

        request = delete("/filter/name")
                .contentType(JSON);
        mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

}
