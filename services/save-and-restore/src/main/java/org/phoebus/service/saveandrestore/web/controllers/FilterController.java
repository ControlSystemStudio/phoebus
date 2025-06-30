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

import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/**
 * Controller class for {@link Filter} endpoints.
 */
@RestController
public class FilterController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private WebSocketHandler webSocketHandler;

    /**
     * Saves a new or updated {@link Filter}.
     *
     * @param filter    The {@link Filter} to save.
     * @param principal The {@link java.security.Principal} of the authenticated user
     * @return The saved {@link Filter}.
     */
    @SuppressWarnings("unused")
    @PutMapping(value = "/filter", produces = JSON)
    @PreAuthorize("@authorizationHelper.maySaveOrDeleteFilter(#filter.getName(), #root)")
    public Filter saveFilter(@RequestBody final Filter filter,
                             Principal principal) {
        filter.setUser(principal.getName());
        Filter savedFilter = nodeDAO.saveFilter(filter);
        webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.FILTER_ADDED_OR_UPDATED, filter));
        return savedFilter;
    }

    /**
     * @return A {@link List} of all persisted {@link Filter} objects. Empty if none are found.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/filters", produces = JSON)
    public List<Filter> getAllFilters() {
        return nodeDAO.getAllFilters();
    }

    /**
     * Deletes a {@link Filter}
     *
     * @param name      Unique name of the {@link Filter}
     * @param principal User {@link Principal} as injected by Spring.
     */
    @SuppressWarnings("unused")
    @DeleteMapping(value = "/filter/{name}")
    @PreAuthorize("@authorizationHelper.maySaveOrDeleteFilter(#name, #root)")
    public void deleteFilter(@PathVariable final String name, Principal principal) {
        nodeDAO.deleteFilter(name);
        webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.FILTER_REMOVED, name));
    }
}
