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

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Configuration;
import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Controller class for {@link Configuration} endpoints.
 */
@RestController
@RequestMapping("/config")
public class ConfigurationController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    /**
     * Creates new {@link Configuration} {@link Node}.
     * @param parentNodeId Valid id of the {@link Node}s intended parent.
     * @param configuration {@link Configuration} data.
     * @param principal User {@link Principal} injected by Spring.
     * @return The new {@link Configuration}.
     */
    @SuppressWarnings("unused")
    @PutMapping(produces = JSON)
    @PreAuthorize("@authorizationHelper.mayCreate(#root)")
    public Configuration createConfiguration(@RequestParam(value = "parentNodeId") String parentNodeId,
                                             @RequestBody Configuration configuration,
                                             Principal principal) {
        for(ConfigPv configPv : configuration.getConfigurationData().getPvList()){
            // Compare mode is set, verify tolerance is non-null
            if(configPv.getComparison() != null && (configPv.getComparison().getComparisonMode() == null || configPv.getComparison().getTolerance() == null)){
                throw new IllegalArgumentException("PV item \"" + configPv.getPvName() + "\" specifies comparison but no comparison or tolerance value");
            }
            // Tolerance is set...
            if(configPv.getComparison() != null && configPv.getComparison().getTolerance() < 0){
                //Tolerance is less than zero, which does not make sense as comparison considers tolerance as upper and lower limit.
                throw new IllegalArgumentException("PV item \"" + configPv.getPvName() + "\" specifies zero tolerance");
             }
        }
        configuration.getConfigurationNode().setUserName(principal.getName());
        return nodeDAO.createConfiguration(parentNodeId, configuration);
    }

    /**
     * Retrieves data associated with a {@link Configuration} {@link Node}-.
     * @param uniqueId unique {@link Node} id of a {@link Configuration}.
     * @return A {@link ConfigurationData} object.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/{uniqueId}", produces = JSON)
    public ConfigurationData getConfigurationData(@PathVariable String uniqueId) {
        return nodeDAO.getConfigurationData(uniqueId);
    }

    /**
     * Updates/overwrites an existing {@link Configuration}
     * @param configuration The {@link Configuration} subject to update.
     * @param principal User {@link Principal} injected by Spring.
     * @return The updated {@link Configuration}.
     */
    @SuppressWarnings("unused")
    @PostMapping(produces = JSON)
    @PreAuthorize("@authorizationHelper.mayUpdate(#configuration, #root)")
    public Configuration updateConfiguration(@RequestBody Configuration configuration,
                                             Principal principal) {
        configuration.getConfigurationNode().setUserName(principal.getName());
        return nodeDAO.updateConfiguration(configuration);
    }
}
