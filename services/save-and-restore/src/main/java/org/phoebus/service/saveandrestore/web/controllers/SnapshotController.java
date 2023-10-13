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
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@SuppressWarnings("unused")
@RestController
public class SnapshotController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @GetMapping(value = "/snapshot/{uniqueId}", produces = JSON)
    public SnapshotData getSnapshotData(@PathVariable String uniqueId) {
        return nodeDAO.getSnapshotData(uniqueId);
    }

    @GetMapping(value = "/snapshots", produces = JSON)
    public List<Node> getAllSnapshots() {
        return nodeDAO.getAllSnapshots();
    }

    @PutMapping(value = "/snapshot", produces = JSON)
    @PreAuthorize("hasRole(this.roleAdmin) or (hasRole(this.roleUser) and this.maySave(#snapshot, #principal))")
    public Snapshot saveSnapshot(@RequestParam(value = "parentNodeId") String parentNodeId,
                                 @RequestBody Snapshot snapshot,
                                 Principal principal) {
        if(!snapshot.getSnapshotNode().getNodeType().equals(NodeType.SNAPSHOT)){
            throw new IllegalArgumentException("Snapshot node of wrong type");
        }
        snapshot.getSnapshotNode().setUserName(principal.getName());
        return nodeDAO.saveSnapshot(parentNodeId, snapshot);
    }

    /**
     * NOTE: this method MUST be public!
     *
     * <p>
     * An authenticated user may save a snapshot, and update if user identity is same as the target's
     * snapshot {@link Node}.
     * </p>
     *
     * @param snapshot {@link Snapshot} identifying the target of the user's update operation.
     * @param principal Identifies user.
     * @return <code>false</code> if user may not update the {@link Snapshot}.
     */
    public boolean maySave(Snapshot snapshot, Principal principal){
        if(snapshot.getSnapshotNode().getUniqueId() == null){
            return true;
        }
        Node node = nodeDAO.getNode(snapshot.getSnapshotNode().getUniqueId());
        return node.getUserName().equals(principal.getName());
    }
}
