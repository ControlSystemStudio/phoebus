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
import org.phoebus.applications.saveandrestore.model.websocket.MessageType;
import org.phoebus.applications.saveandrestore.model.websocket.SaveAndRestoreWebSocketMessage;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.phoebus.service.saveandrestore.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller class for {@link Snapshot} endppoints
 */
@SuppressWarnings("unused")
@RestController
public class SnapshotController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private WebSocketHandler webSocketHandler;
    /**
     *
     * @param uniqueId Unique {@link Node} id of a snapshot.
     * @return SnapshotData object associated with #uniqueId.
     */
    @GetMapping(value = "/snapshot/{uniqueId}", produces = JSON)
    public SnapshotData getSnapshotData(@PathVariable String uniqueId) {
        return nodeDAO.getSnapshotData(uniqueId);
    }

    /**
     *
     * @return All persisted snapshot {@link Node}s
     */
    @GetMapping(value = "/snapshots", produces = JSON)
    public List<Node> getAllSnapshots() {
        return nodeDAO.getAllSnapshots();
    }

    /**
     * Creates a new {@link Snapshot}
     * @param parentNodeId Unique {@link Node} id of the new {@link Snapshot}.
     * @param snapshot {@link Snapshot} data.
     * @param principal User {@link Principal} as injected by Spring.
     * @return The new {@link Snapshot}.
     */
    @PutMapping(value = "/snapshot", produces = JSON)
    @PreAuthorize("@authorizationHelper.mayCreate(#root)")
    public Snapshot createSnapshot(@RequestParam(value = "parentNodeId") String parentNodeId,
                                 @RequestBody Snapshot snapshot,
                                 Principal principal) {
        if(!snapshot.getSnapshotNode().getNodeType().equals(NodeType.SNAPSHOT)){
            throw new IllegalArgumentException("Snapshot node of wrong type");
        }
        snapshot.getSnapshotNode().setUserName(principal.getName());
        Snapshot newSnapshot = nodeDAO.createSnapshot(parentNodeId, snapshot);
        webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.NODE_ADDED, newSnapshot.getSnapshotNode().getUniqueId()));
        return newSnapshot;
    }

    /**
     * Updates a {@link Snapshot}.
     * @param snapshot The {@link Snapshot} subject to update.
     * @param principal User {@link Principal} as injected by Spring.
     * @return The updated {@link Snapshot}
     */
    @PostMapping(value = "/snapshot", produces = JSON)
    @PreAuthorize("@authorizationHelper.mayUpdate(#snapshot, #root)")
    public Snapshot updateSnapshot(@RequestBody Snapshot snapshot,
                                   Principal principal) {
        if(!snapshot.getSnapshotNode().getNodeType().equals(NodeType.SNAPSHOT)){
            throw new IllegalArgumentException("Snapshot node of wrong type");
        }
        snapshot.getSnapshotNode().setUserName(principal.getName());
        Snapshot updatedSnapshot = nodeDAO.updateSnapshot(snapshot);
        webSocketHandler.sendMessage(new SaveAndRestoreWebSocketMessage(MessageType.NODE_UPDATED, updatedSnapshot.getSnapshotNode()));
        return updatedSnapshot;
    }
}
