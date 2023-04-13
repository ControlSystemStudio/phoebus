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
 */

package org.phoebus.service.saveandrestore.web.controllers;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.service.saveandrestore.web.config.ControllersTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@WebMvcTest(HelpResource.class)
@ContextConfiguration(classes = ControllersTestConfig.class)
public class HelpResourceTest{

    @Autowired
    protected MockMvc mockMvc;

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
