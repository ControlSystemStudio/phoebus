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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.services.IServices;

@RestController
public class SnapshotController extends BaseController {

	@Autowired
	private IServices services;

	/**
	 * Retrieves a snapshot {@link Node}.
	 * 
	 * A {@link HttpStatus#NOT_FOUND} is returned if the specified snapshot does not exist or if
	 * a a snapshot with the specified unique id exists but is uncommitted.
	 * 
	 * @param uniqueNodeId The unique id of the snapshot
	 * @return A {@link Node} object.
	 */

	@GetMapping("/snapshot/{uniqueNodeId}")
	public Node getSnapshot(@PathVariable String uniqueNodeId) {

		return services.getSnapshot(uniqueNodeId);
	}

	@GetMapping("/snapshot/{uniqueNodeId}/items")
	public List<SnapshotItem> getSnapshotItems(@PathVariable String uniqueNodeId) {

		return services.getSnapshotItems(uniqueNodeId);
	}

	
	@PutMapping("/snapshot/{configUniqueId}")
	public Node saveSnapshot(@PathVariable String configUniqueId, 
			@RequestParam(required = true) String snapshotName,
			@RequestParam(required = true) String userName,
			@RequestParam(required = true) String comment,
			@RequestBody(required = true) List<SnapshotItem> snapshotItems) {
		
		if(snapshotName.length() == 0 || userName.length() == 0 || comment.length() == 0) {
			throw new IllegalArgumentException("Snapshot name, user name and comment must be of non-zero length");
		}

		return services.saveSnapshot(configUniqueId, snapshotItems, snapshotName, userName, comment);
	}
}
