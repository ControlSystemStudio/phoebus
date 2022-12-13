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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    private Logger logger = Logger.getLogger(NodeController.class.getName());

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
     * @param node            A {@link Node} object. The {@link Node#getName()} and {@link Node#getUserName()} ()} fields must be
     *                        non-null and non-empty.
     * @return The new folder in the tree.
     */
    @SuppressWarnings("unused")
    @PutMapping(value = "/node", produces = JSON)
    public Node createNode(@RequestParam(name = "parentNodeId") String parentsUniqueId, @RequestBody final Node node) {
        if (node.getUserName() == null || node.getUserName().isEmpty()) {
            throw new IllegalArgumentException("User name must be non-null and of non-zero length");
        }
        if (node.getName() == null || node.getName().isEmpty()) {
            throw new IllegalArgumentException("Node name must be non-null and of non-zero length");
        }
        if(!areTagsValid(node)){
            throw new IllegalArgumentException("Node may not contain golden tag");
        }
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
     */
    @SuppressWarnings("unused")
    @DeleteMapping(value = "/node/{uniqueNodeId}", produces = JSON)
    public void deleteNode(@PathVariable final String uniqueNodeId) {
        logger.info("Deleting node with unique id " + uniqueNodeId);
        nodeDAO.deleteNode(uniqueNodeId);
    }

    /**
     * Updates a {@link Node}. The purpose is to support modification of name or comment/description, or both. Modification of
     * node type is not supported.
     *
     * <p>
     * A {@link HttpStatus#BAD_REQUEST} is returned if a node of the same name and type already exists in the parent folder,
     * or if the node in question is the root node (0).
     * </p>
     *
     * @param customTimeForMigration Self-explanatory
     * @param nodeToUpdate           {@link Node} object containing updated data. Only name, description and properties may be changed. The user name
     *                               should be set by the client in an automated fashion and will be updated by the persistence layer.
     * @return A {@link Node} object representing the updated node.
     */
    @SuppressWarnings("unused")
    @PostMapping(value = "/node", produces = JSON)
    public Node updateNode(@RequestParam(value = "customTimeForMigration", required = false, defaultValue = "false") String customTimeForMigration,
                           @RequestBody Node nodeToUpdate) {
        if(!areTagsValid(nodeToUpdate)){
            throw new IllegalArgumentException("Node may not contain golden tag");
        }
        return nodeDAO.updateNode(nodeToUpdate, Boolean.valueOf(customTimeForMigration));
    }

    /**
     * Checks if a {@link Node} has a tag named "golden". If so, it must be of type {@link NodeType#SNAPSHOT}.
     * @param node A {@link Node} with potentially null or empty list of tags.
     * @return <code>true</code> if the {@link Node} in question has a valid list of tags, otherwise <code>false</code>.
     */
    private boolean areTagsValid(Node node){
        if(node.getTags() == null || node.getTags().isEmpty()){
            return true;
        }

        if(!node.getNodeType().equals(NodeType.SNAPSHOT) &&
                node.getTags().stream().filter(t -> t.getName().equalsIgnoreCase(Tag.GOLDEN)).findFirst().isPresent()){
            return false;
        }

        return true;
    }
}
