/** 
 * Copyright (C) 2018 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.service.saveandrestore.web.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.UpdateConfigHolder;
import org.phoebus.service.saveandrestore.services.IServices;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ConfigurationController extends BaseController {

	@Autowired
	private IServices services;

	/**
	 * Create a new folder in the tree structure.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if:
	 * <ul>
	 * <li>The parent node does not exist</li>
	 * <li>The parent node is not a {@link Node}</li>
	 * <li>A folder with the same name already exists in the parent folder</li>
	 * </ul>
	 * 
	 * @param parentsUniqueId The unique id of the parent node for the new node.
	 * @param node
	 *            A {@link Node} object. The {@link Node#getName()} field must be
	 *            non-null.
	 * @return The new folder in the tree.
	 */
	@PutMapping("/node/{parentsUniqueId}")
	public Node createNode(@PathVariable String parentsUniqueId, @RequestBody final Node node) {
		if(node.getUserName() == null || node.getUserName().isEmpty()) {
			throw new IllegalArgumentException("User name must be non-null and of non-zero length");
		}
		if(node.getName() == null || node.getName().isEmpty()) {
			throw new IllegalArgumentException("Node name must be non-null and of non-zero length");
		}
		return services.createNode(parentsUniqueId, node);
	}

	/**
	 * Gets a node.
	 *
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * @param uniqueNodeId The id of the folder
	 * @return A {@link Node} object if a node with the specified id exists.
	 */	
	@GetMapping("/node/{uniqueNodeId}")
	public Node getNode(@PathVariable final String uniqueNodeId) {
		return services.getNode(uniqueNodeId);
	}
	
	@GetMapping("/root")
	public Node getRootNode() {
		return services.getRootNode();
	}
	
	@GetMapping("/node/{uniqueNodeId}/parent")
	public Node getParentNode(@PathVariable String uniqueNodeId) {
		return services.getParentNode(uniqueNodeId);
	}
	
	@GetMapping("/node/{uniqueNodeId}/children")
	public List<Node> getChildNodes(@PathVariable final String uniqueNodeId) {
		return services.getChildNodes(uniqueNodeId);
	}

	/**
	 * Updates a configuration. For instance, user may change the name of the
	 * configuration or modify the list of PVs. NOTE: in case PVs are removed from
	 * the configuration, the corresponding snapshot values are also deleted.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is a configuration node, or if a user name has not
	 * been specified in the config data.
	 * 
	 * @param uniqueNodeId The unique id of the configuration.
	 * @param updateConfigHolder Wrapper of a {@link Node} object representing the config node and a list of {@link ConfigPv}s
	 * @return The updated configuration {@link Node} object.
	 */
	@PostMapping("/config/{uniqueNodeId}/update")
	public ResponseEntity<Node> updateConfiguration(@PathVariable String uniqueNodeId, 
			@RequestBody UpdateConfigHolder updateConfigHolder) {
		
		if(updateConfigHolder.getConfig() == null) {
			throw new IllegalArgumentException("Cannot update a null configuration");
		}
		else if(updateConfigHolder.getConfigPvList() == null) {
			throw new IllegalArgumentException("Cannot update a configration with a null config PV list");
		}
		else if(updateConfigHolder.getConfig().getUserName() == null || 
				updateConfigHolder.getConfig().getUserName().isEmpty()) {
			throw new IllegalArgumentException("Will not update a configuration where user name is null or empty");
		}
		
		for(ConfigPv configPv : updateConfigHolder.getConfigPvList()) {
			if(configPv.getPvName() == null || configPv.getPvName().isEmpty()) {
				throw new IllegalArgumentException("Cannot update configuration, encountered a null or empty PV name");
			}
		}
		
		return new ResponseEntity<>(services.updateConfiguration(updateConfigHolder.getConfig(), updateConfigHolder.getConfigPvList()), HttpStatus.OK);
	}

	/**
	 * Recursively deletes a node and all its child nodes, if any. In particular, if the node id points to a configuration,
	 * all snapshots associated with that configuration will also be deleted. A client may wish to alert the
	 * user of this side-effect.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified node id does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is the tree root id (0).
	 * 
	 * @param uniqueNodeId The non-zero id of the node to delete
	 */
	@DeleteMapping("/node/{uniqueNodeId}")
	public void deleteNode(@PathVariable final String uniqueNodeId) {
		services.deleteNode(uniqueNodeId);
	}

	/**
	 * Returns a potentially empty list of {@link Node}s associated with the specified configuration node id.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified configuration does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if the specified node id is not a configuration.
	 * 
	 * @param uniqueNodeId The id of the configuration
	 * @return A potentially empty list of {@link Node}s for the specified configuration.
	 */
	@GetMapping("/config/{uniqueNodeId}/snapshots")
	public List<Node> getSnapshots(@PathVariable String uniqueNodeId) {
		return services.getSnapshots(uniqueNodeId);
	}

	/**
	 * Moves a node and its sub-tree to a another parent (target) folder.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified source node does not exist.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if:
	 * <ol>
	 * <li>The specified target node does not exist</li>
	 * <li>The specified target node is not a folder node</li>
	 * <li>The specified target node already contains a child node with same name and type (i.e. configuration or folder)</li>
	 * </ol>
	 * 
	 * @param uniqueNodeId The id of the source node
	 * @param to The new parent (target) node id
	 * @param userName The (account) name of the user performing the operation.
	 * @return A {@link Node} object representing the parent (target) folder.
	 */
	@PostMapping("/node/{uniqueNodeId}")
	public Node moveNode(@PathVariable String uniqueNodeId, 
			@RequestParam(value = "to", required = true) String to, 
			@RequestParam(value = "username", required = true) String userName) {
		return services.moveNode(uniqueNodeId, to, userName);
	}

	/**
	 * Renames a node.
	 * 
	 * A {@link HttpStatus#BAD_REQUEST} is returned if a node of the same name and type already exists in the parent folder,
	 * or if the node in question is the root node (0).
	 * 
	 * @param uniqueNodeId Node id of the node to rename. Must be non-zero.
	 * @param customTimeForMigration Self-explanatory
	 * @param nodeToUpdate {@link Node} object containing updated data. Only name and properties may be changed. The user name
	 * should be set by the client in an automated fashion and will be updated by the persistence layer.
	 * @return A {@link Node} object representing the updated node.
	 */
	@PostMapping("/node/{uniqueNodeId}/update")
	public Node updateNode(@PathVariable String uniqueNodeId,
			@RequestParam(value = "customTimeForMigration", required = true) String customTimeForMigration,
			@RequestBody Node nodeToUpdate) {
		return services.updateNode(nodeToUpdate, Boolean.valueOf(customTimeForMigration));
	}
	
	@GetMapping("/config/{uniqueNodeId}/items")
	public List<ConfigPv> getConfigPvs(@PathVariable String uniqueNodeId) {
		return services.getConfigPvs(uniqueNodeId);
	}

	/**
	 * Retrieves the "full path" of the specified node, e.g. /topLevelFolder/folder/nodeName,
	 * where nodeName is the name of the node uniquely identified by <code>unqiueNodeId</code>,
	 * and any preceding path elements are the names of parent folders all the way up to the root.
	 * The root folder corresponds to a single "/".
	 * @param uniqueNodeId Non-null unique node id of the node for which the client wishes to get the
	 *                     full path.
	 * @return A string like /topLevelFolder/folder/nodeName if the node exists, otherwise HTTP 404
	 * is returned.
	 *
	 */
	@GetMapping("/path/{uniqueNodeId}")
	public String getFullPath(@PathVariable String uniqueNodeId){
		String fullPath = services.getFullPath(uniqueNodeId);
		if(fullPath == null){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return fullPath;
	}

	/**
	 * Retrieves the node(s) corresponding to the specified "full path". Since a folder node may
	 * contain a folder node and a save set (configuration) node with the same name, this end point will - as long
	 * as the specified path is valid - return a list with one or two node objects.
	 * @param path Non-null path that must start with a forward slash and not end in a forward slash.
	 * @return A {@link List} containing one or two {@link Node}s. If the specified path is invalid or
	 * cannot be resolved to an existing node, HTTP 404 is returned.
	 */
	@GetMapping("/path")
	public List<Node> getFromPath(@RequestParam(value = "path") String path){
		List<Node> nodes = services.getFromPath(path);
		if(nodes == null){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return nodes;
	}
}
