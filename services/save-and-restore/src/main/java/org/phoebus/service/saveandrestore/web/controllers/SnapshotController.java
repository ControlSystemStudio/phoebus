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

import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotWrapper;
import org.phoebus.applications.saveandrestore.model.ThinWrapper;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

@RestController
public class SnapshotController extends BaseController {

	@Autowired
	private NodeDAO nodeDAO;

	/**
	 * Retrieves a snapshot {@link Node}.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified snapshot does not exist or if
	 * a a snapshot with the specified unique id exists but is uncommitted.
	 * 
	 * @param uniqueNodeId The unique id of the snapshot
	 * @return A {@link Node} object.
	 */

	@GetMapping(value = "/snapshot/{uniqueNodeId}", produces = JSON)
	public Node getSnapshot(@PathVariable String uniqueNodeId) {

		return nodeDAO.getSnapshotNode(uniqueNodeId);
	}

	@GetMapping(value = "/snapshot/{uniqueNodeId}/items", produces = JSON)
	public List<SnapshotItem> getSnapshotItems(@PathVariable String uniqueNodeId) {

		return nodeDAO.getSnapshotItems(uniqueNodeId);
	}

	@GetMapping(value = "/snapshots", produces = JSON)
	public List<Node> getAllSnapshots() {
		return nodeDAO.getAllSnapshots();
	}

	@PutMapping(value = "/snapshot/{configUniqueId}", produces = JSON)
	public Node saveSnapshot(@PathVariable String configUniqueId, 
			@RequestParam String snapshotName,
			@RequestParam String userName,
			@RequestParam String comment,
			@RequestBody List<SnapshotItem> snapshotItems) {
		
		if(snapshotName.length() == 0 || userName.length() == 0 || comment.length() == 0) {
			throw new IllegalArgumentException("SnapshotData name, user name and comment must be of non-zero length");
		}

		return nodeDAO.saveSnapshot(configUniqueId, snapshotItems, snapshotName, userName, comment);
	}

	@SuppressWarnings("unused")
	@PutMapping(value = "/snapshot", produces = JSON)
	public SnapshotWrapper saveSnapshot(@RequestParam(name = "parentId") String parentsUniqueId, @RequestBody SnapshotWrapper snapshotWrapper) {
		if(snapshotWrapper.getSnapshotNode() == null || snapshotWrapper.getSnapshotNode().getName().length() == 0 ||
				snapshotWrapper.getSnapshotData().getComment() == null || snapshotWrapper.getSnapshotData().getComment() .length() == 0) {
			throw new IllegalArgumentException("Request body does not meet requirements");
		}

		Node savedSnapshotNode = nodeDAO.createNode(parentsUniqueId, snapshotWrapper.getSnapshotNode());
		SnapshotData snapshotData = snapshotWrapper.getSnapshotData();
		snapshotData.setUniqueId(savedSnapshotNode.getUniqueId());
		SnapshotData savedSnapshotData = nodeDAO.saveSnapshot(snapshotData);

		SnapshotWrapper newSnapshotWrapper = new SnapshotWrapper();
		newSnapshotWrapper.setSnapshotNode(savedSnapshotNode);
		newSnapshotWrapper.setSnapshotData(savedSnapshotData);

		return newSnapshotWrapper;
	}

	@PutMapping(value = "/vtype")
	public void receiveVType(@RequestBody ThinWrapper vType){
		System.out.println(vType.getValue().toString());
	}
}
