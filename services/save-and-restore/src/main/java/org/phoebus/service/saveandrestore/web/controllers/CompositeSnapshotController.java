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

import org.phoebus.applications.saveandrestore.model.CompositeSnapshot;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SuppressWarnings("unused")
@RestController
public class CompositeSnapshotController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @PutMapping(value = "/composite-snapshot", produces = JSON)
    public CompositeSnapshot createCompositeSnapshot(@RequestParam(value = "parentNodeId") String parentNodeId,
                                                     @RequestBody CompositeSnapshot compositeSnapshot) {
        return nodeDAO.createCompositeSnapshot(parentNodeId, compositeSnapshot);
    }

    @PostMapping(value = "/composite-snapshot", produces = JSON)
    public CompositeSnapshot updateCompositeSnapshot(@RequestBody CompositeSnapshot compositeSnapshot) {
        return nodeDAO.updateCompositeSnapshot(compositeSnapshot);
    }


    @GetMapping(value = "/composite-snapshot/{uniqueId}", produces = JSON)
    public CompositeSnapshotData getCompositeSnapshotData(@PathVariable String uniqueId) {
        return nodeDAO.getCompositeSnapshotData(uniqueId);
    }

    @GetMapping(value = "/composite-snapshot/{uniqueId}/nodes", produces = JSON)
    public List<Node> getCompositeSnapshotNodes(@PathVariable String uniqueId) {
        CompositeSnapshotData compositeSnapshotData = nodeDAO.getCompositeSnapshotData(uniqueId);
        return nodeDAO.getNodes(compositeSnapshotData.getReferencedSnapshotNodes());
    }

    @GetMapping(value = "/composite-snapshot/{uniqueId}/items", produces = JSON)
    public List<SnapshotItem> getCompositeSnapshotItems(@PathVariable String uniqueId) {
        return nodeDAO.getSnapshotItemsFromCompositeSnapshot(uniqueId);
    }

    /**
     * Utility end-point for the purpose of checking whether a set of snapshots contain duplicate PV names.
     * The input snapshot ids may refer to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     * and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     *
     * @param snapshotNodeIds List of {@link Node} ids corresponding to {@link Node}s of types {@link org.phoebus.applications.saveandrestore.model.NodeType#SNAPSHOT}
     *                        and {@link org.phoebus.applications.saveandrestore.model.NodeType#COMPOSITE_SNAPSHOT}
     * @return A list of PV names that occur more than once across the list of {@link Node}s corresponding
     * to the input. Empty if no duplicates are found.
     */
    @PostMapping(value = "/composite-snapshot-consistency-check", produces = JSON)
    public List<String> checkSnapshotsConsistency(@RequestBody List<String> snapshotNodeIds) {
        return nodeDAO.checkForPVNameDuplicates(snapshotNodeIds);
    }
}
