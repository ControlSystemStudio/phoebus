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
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Tag;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;

/**
 * Controller offering endpoints for CRUD operations on {@link Node}s, which represent
 * objects in the tree structure of the save-and-restore data.
 */
@RestController
public class NodeController extends BaseController {

    @SuppressWarnings("unused")
    @Autowired
    private NodeDAO nodeDAO;

    private final Logger logger = Logger.getLogger(NodeController.class.getName());

    /**
     * Create a new folder in the tree structure.
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if:
     * <ul>
     * <li>The parent node does not exist</li>
     * <li>The parent node is not a {@link Node}</li>
     * <li>A folder with the same name already exists in the parent folder</li>
     * </ul>
     *
     * @param parentsUniqueId The unique id of the parent node for the new node.
     * @param node            A {@link Node} object. The {@link Node#getName()} field must be
     *                        non-null and non-empty.
     * @param principal       Authenticated user's {@link java.security.Principal}
     * @return The new folder in the tree.
     */
    @SuppressWarnings("unused")
    @PutMapping(value = "/node", produces = JSON)
    @PreAuthorize("@authorizationHelper.mayCreate(#root)")
    public Node createNode(@RequestParam(name = "parentNodeId") String parentsUniqueId,
                           @RequestBody final Node node,
                           Principal principal) {
        if (node.getName() == null || node.getName().isEmpty()) {
            throw new IllegalArgumentException("Node name must be non-null and of non-zero length");
        }
        if (!areTagsValid(node)) {
            throw new IllegalArgumentException("Node may not contain golden tag");
        }
        node.setUserName(principal.getName());
        return nodeDAO.createNode(parentsUniqueId, node);
    }

    /**
     * Gets a node.
     * <p>
     * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
     *
     * @param uniqueNodeId The id of the folder
     * @return A {@link Node} object if a node with the specified id exists.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/node/{uniqueNodeId}", produces = JSON)
    public Node getNode(@PathVariable final String uniqueNodeId) {
        return nodeDAO.getNode(uniqueNodeId);
    }

    /**
     * Gets nodes as identified by input string array.
     * <p>
     * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
     *
     * @param uniqueNodeIds The ids of the requested nodes.
     * @return A list of {@link Node} objects.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/nodes", produces = JSON)
    public List<Node> getNodes(@RequestBody List<String> uniqueNodeIds) {
        return nodeDAO.getNodes(uniqueNodeIds);
    }

    @SuppressWarnings("unused")
    @GetMapping(value = "/node/{uniqueNodeId}/parent", produces = JSON)
    public Node getParentNode(@PathVariable String uniqueNodeId) {
        return nodeDAO.getParentNode(uniqueNodeId);
    }

    @SuppressWarnings("unused")
    @GetMapping(value = "/node/{uniqueNodeId}/children", produces = JSON)
    public List<Node> getChildNodes(@PathVariable final String uniqueNodeId) {
        return nodeDAO.getChildNodes(uniqueNodeId);
    }

    /**
     * Recursively deletes a node and all its child nodes, if any. In particular, if the node id points to a configuration,
     * all snapshots associated with that configuration will also be deleted. A client may wish to alert the
     * user of this side effect.
     * <p>
     * A {@link HttpStatus#NOT_FOUND} is returned if the specified unique node id does not exist.
     * </p>
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if the specified unique node id is the tree root node id,
     * see {@link Node#ROOT_FOLDER_UNIQUE_ID}.
     * </p>
     *
     * @param uniqueNodeId The non-zero id of the node to delete
     * @param authentication {@link Authentication} of authenticated user.
     */
    /*
    @SuppressWarnings("unused")
    @DeleteMapping(value = "/node/{uniqueNodeId}", produces = JSON)
    @PreAuthorize("hasRole(this.roleAdmin) or @authorizationHelper.mayDelete(#uniqueNodeId, #authentication)")
    @Deprecated
    public void deleteNode(@PathVariable final String uniqueNodeId, Authentication authentication) {
        logger.info("Deleting node with unique id " + uniqueNodeId);
        nodeDAO.deleteNode(uniqueNodeId);
    }

     */

    /**
     * Deletes all {@link Node}s contained in the provided list.
     * <br>
     * Checks are made to make sure user may delete
     * the {@link Node}s, see {@link AuthorizationHelper}. If the checks fail on any of the {@link Node} ids,
     * checks are aborted and client will receive an HTTP 403 response.
     * <br>
     * Note that the {@link PreAuthorize} annotations calls a helper method in {@link AuthorizationHelper}, using
     * the list of {@link Node} ids and a Spring injected object - <code>root</code> - used to check
     * authorities of the user.
     * <br>
     * Note also that an unauthenticated user (e.g. no basic authentication header in client's request) will
     * receive a HTTP 401 response, i.e. the {@link PreAuthorize} check is not invoked.
     * @param nodeIds
     */
    @SuppressWarnings("unused")
    @DeleteMapping(value = "/node", produces = JSON)
    @PreAuthorize("@authorizationHelper.mayDelete(#nodeIds, #root)")
    public void deleteNodes(@RequestBody List<String> nodeIds) {
        nodeDAO.deleteNodes(nodeIds);
    }

    /**
     * Updates a {@link Node}. The purpose is to support modification of name or comment/description, or both. Modification of
     * node type is not supported.
     *
     * <br>
     * Checks are made to make sure user may update
     * the {@link Node}, see {@link AuthorizationHelper}. If the checks fail client will receive an HTTP 403 response.
     * <br>
     * Note that the {@link PreAuthorize} annotations calls a helper method in {@link AuthorizationHelper}, using
     * the of {@link Node} id and a Spring injected object - <code>root</code> - used to check
     * authorities of the user.
     * <br>
     * Note also that an unauthenticated user (e.g. no basic authentication header in client's request) will
     * receive a HTTP 401 response, i.e. the {@link PreAuthorize} check is not invoked.
     *
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if a node of the same name and type already exists in the parent folder,
     * or if the node in question is the root node (0).
     * </p>
     *
     * @param customTimeForMigration Self-explanatory
     * @param nodeToUpdate           {@link Node} object containing updated data. Only name, description and properties may be changed.
     * @param principal              Authenticated user's {@link java.security.Principal}
     * @return A {@link Node} object representing the updated node.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/node", produces = JSON)
    @PreAuthorize("@authorizationHelper.mayUpdate(#nodeToUpdate, #root)")
    public Node updateNode(@RequestParam(value = "customTimeForMigration", required = false, defaultValue = "false") String customTimeForMigration,
                           @RequestBody Node nodeToUpdate,
                           Principal principal) {
        if (!areTagsValid(nodeToUpdate)) {
            throw new IllegalArgumentException("Node may not contain golden tag");
        }
        nodeToUpdate.setUserName(principal.getName());
        return nodeDAO.updateNode(nodeToUpdate, Boolean.valueOf(customTimeForMigration));
    }

    /**
     * Checks if a {@link Node} has a tag named "golden". If so, it must be of type {@link NodeType#SNAPSHOT}.
     *
     * @param node A {@link Node} with potentially null or empty list of tags.
     * @return <code>true</code> if the {@link Node} in question has a valid list of tags, otherwise <code>false</code>.
     */
    private boolean areTagsValid(Node node) {
        if (node.getTags() == null || node.getTags().isEmpty()) {
            return true;
        }

        if (!node.getNodeType().equals(NodeType.SNAPSHOT) &&
                node.getTags().stream().anyMatch(t -> t.getName().equalsIgnoreCase(Tag.GOLDEN))) {
            return false;
        }

        return true;
    }
}
