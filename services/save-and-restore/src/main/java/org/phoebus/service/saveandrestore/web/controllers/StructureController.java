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

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controller offering endpoints for manipulating the tree structure of the
 * {@link Node} objects in the save-and-restore data.
 */
@RestController
public class StructureController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    private Logger logger = Logger.getLogger(StructureController.class.getName());


    /**
     * Moves a list of source nodes to a new target (parent) node.
     *
     * @param to       The unique id of the new parent, which must be a folder. If empty or if
     *                 target node does not exist, {@link HttpStatus#BAD_REQUEST} is returned.
     * @param userName Identity of the user performing the action on the client.
     *                 If empty, {@link HttpStatus#BAD_REQUEST} is returned.
     * @param nodes    List of source nodes to move. If empty, or if any of the listed source nodes does not exist,
     *                 {@link HttpStatus#BAD_REQUEST} is returned.
     * @return The (updated) target node.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/move", produces = JSON)
    public Node moveNodes(@RequestParam(value = "to") String to,
                          @RequestParam(value = "username") String userName,
                          @RequestBody List<String> nodes) {
        if (to.isEmpty() || userName.isEmpty() || nodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username, target node and list of source nodes must all be non-empty.");
        }
        Logger.getLogger(StructureController.class.getName()).info(Thread.currentThread().getName() + " " + (new Date()) + " move");
        return nodeDAO.moveNodes(nodes, to, userName);
    }

    /**
     * Copies a list of source nodes to a target (parent) node. Since the source nodes may contain sub-trees at
     * any depth, the copy operation needs to do a deep copy, which may take some time to complete.
     *
     * @param to       The unique id of the target parent node, which must be a folder. If empty or if
     *                 target node does not exist, {@link HttpStatus#BAD_REQUEST} is returned.
     * @param userName Identity of the user performing the action on the client.
     *                 If empty, {@link HttpStatus#BAD_REQUEST} is returned.
     * @param nodes    List of source nodes to copy. If empty, or if any of the listed source nodes does not exist,
     *                 {@link HttpStatus#BAD_REQUEST} is returned.
     * @return The (updated) target node.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/copy", produces = JSON)
    public Node copyNodes(@RequestParam(value = "to") String to,
                          @RequestParam(value = "username") String userName,
                          @RequestBody List<String> nodes) {
        if (to.isEmpty() || userName.isEmpty() || nodes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username, target node and list of source nodes must all be non-empty.");
        }
        Logger.getLogger(StructureController.class.getName()).info(Thread.currentThread().getName() + " " + (new Date()) + " move");
        return nodeDAO.copyNodes(nodes, to, userName);
    }

    /**
     * Retrieves the "full path" of the specified node, e.g. /topLevelFolder/folder/nodeName,
     * where nodeName is the name of the node uniquely identified by <code>unqiueNodeId</code>,
     * and any preceding path elements are the names of parent folders all the way up to the root.
     * The root folder corresponds to a single "/".
     *
     * @param uniqueNodeId Non-null unique node id of the node for which the client wishes to get the
     *                     full path.
     * @return A string like /topLevelFolder/folder/nodeName if the node exists, otherwise HTTP 404
     * is returned.
     */
    @SuppressWarnings("unused")
    @GetMapping("/path/{uniqueNodeId}")
    public String getFullPath(@PathVariable String uniqueNodeId) {
        String fullPath = nodeDAO.getFullPath(uniqueNodeId);
        if (fullPath == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return fullPath;
    }

    /**
     * Retrieves the node(s) corresponding to the specified "full path". Since a folder node may
     * contain a folder node and a configuration (configuration) node with the same name, this end point will - as long
     * as the specified path is valid - return a list with one or two node objects.
     *
     * @param path Non-null path that must start with a forward slash and not end in a forward slash.
     * @return A {@link List} containing one or two {@link Node}s. If the specified path is invalid or
     * cannot be resolved to an existing node, HTTP 404 is returned.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/path", produces = JSON)
    public List<Node> getFromPath(@RequestParam(value = "path") String path) {
        List<Node> nodes = nodeDAO.getFromPath(path);
        if (nodes == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return nodes;
    }
}
