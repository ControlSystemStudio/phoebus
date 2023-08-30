/**
 * Copyright (C) 2018 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/config")
public class ConfigurationController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    @SuppressWarnings("unused")
    @PutMapping(produces = JSON)
    public Configuration createConfiguration(@RequestParam(value = "parentNodeId") String parentNodeId,
                                             @RequestBody Configuration configuration) {
        return nodeDAO.createConfiguration(parentNodeId, configuration);
    }

    @SuppressWarnings("unused")
    @GetMapping(value = "/{uniqueId}", produces = JSON)
    public ConfigurationData getConfigurationData(@PathVariable String uniqueId) {
        return nodeDAO.getConfigurationData(uniqueId);
    }

    @SuppressWarnings("unused")
    @PostMapping(produces = JSON)
    public Configuration updateConfiguration(@RequestBody Configuration configuration) {
        return nodeDAO.updateConfiguration(configuration);
    }
}
