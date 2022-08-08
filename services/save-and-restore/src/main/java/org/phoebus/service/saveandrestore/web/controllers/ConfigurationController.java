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
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/config")
public class ConfigurationController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    private Logger logger = Logger.getLogger(ConfigurationController.class.getName());

    /**
     * Updates a configuration. For instance, user may change the name of the
     * configuration or modify the list of PVs. NOTE: in case PVs are removed from
     * the configuration, the corresponding snapshot values are also deleted.
     * <p>
     * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is a configuration node, or if a user name has not
     * been specified in the config data.
     *
     * @param uniqueNodeId       The unique id of the configuration.
     * @param updateConfigHolder Wrapper of a {@link Node} object representing the config node and a list of {@link ConfigPv}s
     * @return The updated configuration {@link Node} object.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/{uniqueNodeId}/update", produces = JSON)
    public ResponseEntity<Node> updateConfiguration(@PathVariable String uniqueNodeId,
                                                    @RequestBody UpdateConfigHolder updateConfigHolder) {

        if (updateConfigHolder.getConfig() == null) {
            throw new IllegalArgumentException("Cannot update a null configuration");
        } else if (updateConfigHolder.getConfigPvList() == null) {
            throw new IllegalArgumentException("Cannot update a configration with a null config PV list");
        } else if (updateConfigHolder.getConfig().getUserName() == null ||
                updateConfigHolder.getConfig().getUserName().isEmpty()) {
            throw new IllegalArgumentException("Will not update a configuration where user name is null or empty");
        }

        for (ConfigPv configPv : updateConfigHolder.getConfigPvList()) {
            if (configPv.getPvName() == null || configPv.getPvName().isEmpty()) {
                throw new IllegalArgumentException("Cannot update configuration, encountered a null or empty PV name");
            }
        }

        return new ResponseEntity<>(nodeDAO.updateConfiguration(updateConfigHolder.getConfig(), updateConfigHolder.getConfigPvList()), HttpStatus.OK);
    }

    /**
     * Returns a potentially empty list of {@link Node}s associated with the specified configuration node id.
     * <p>
     * A {@link HttpStatus#NOT_FOUND} is returned if the specified configuration does not exist.
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is not a configuration.
     *
     * @param uniqueNodeId The id of the configuration
     * @return A potentially empty list of {@link Node}s for the specified configuration.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/{uniqueNodeId}/snapshots", produces = JSON)
    public List<Node> getSnapshots(@PathVariable String uniqueNodeId) {
        return nodeDAO.getSnapshots(uniqueNodeId);
    }


    @SuppressWarnings("unused")
    @GetMapping(value = "/{uniqueNodeId}/items", produces = JSON)
    public List<ConfigPv> getConfigPvs(@PathVariable String uniqueNodeId) {
        return nodeDAO.getConfigPvs(uniqueNodeId);
    }

    @SuppressWarnings("unused")
    @PutMapping(produces = JSON)
    public Configuration saveConfiguration(@RequestBody Configuration configuration){
        return nodeDAO.saveConfiguration(configuration);
    }

    @SuppressWarnings("unused")
    @GetMapping(value = "/{uniqueId}", produces = JSON)
    public Configuration getConfiguration(@PathVariable String uniqueId){
        return nodeDAO.getConfiguration(uniqueId);
    }

    @SuppressWarnings("unused")
    @PostMapping(produces = JSON)
    public Configuration updateConfiguration(@RequestBody Configuration configuration){
        return nodeDAO.updateConfiguration(configuration);
    }
}
