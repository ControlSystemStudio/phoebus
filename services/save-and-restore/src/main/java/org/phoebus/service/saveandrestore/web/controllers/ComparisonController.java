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

import org.phoebus.applications.saveandrestore.model.CompareResult;
import org.phoebus.applications.saveandrestore.model.CompositeSnapshotData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.NodeNotFoundException;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@link RestController} offering API to compare stored and live snapshot values.
 */
@SuppressWarnings("unused")
@RestController
@RequestMapping("/compare")
public class ComparisonController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private SnapshotUtil snapshotUtil;

    private static final Logger LOG = Logger.getLogger(ComparisonController.class.getName());

    /**
     *
     * @param nodeId The unique node id of a snapshot or composite snapshot.
     * @return A list of {@link CompareResult}s, one for each PV item in the snapshot/composite snapshot. The
     * {@link CompareResult#getLiveValue()} and {@link CompareResult#getStoredValue()} will return <code>null</code> if
     * comparison evaluates to &quot;equal&quot; for a PV.
     */
    @GetMapping(value = "/{nodeId}", produces = JSON)
    public List<CompareResult> compare(@PathVariable String nodeId, @RequestParam(value = "tolerance", required = false, defaultValue = "0") double tolerance) {
        if(tolerance < 0){
            throw new IllegalArgumentException("Tolerance must be >=0");
        }
        Node node = nodeDAO.getNode(nodeId);
        if (node == null) {
            throw new NodeNotFoundException("Node " + nodeId + " does not exist");
        }
        switch (node.getNodeType()) {
            case SNAPSHOT:
                return snapshotUtil.comparePvs(getSnapshotItems(node.getUniqueId()), tolerance);
            case COMPOSITE_SNAPSHOT:
                return snapshotUtil.comparePvs(getCompositeSnapshotItems(node.getUniqueId()), tolerance);
            default:
                throw new IllegalArgumentException("Node type" + node.getNodeType() + " cannot be compared");
        }
    }

    private List<SnapshotItem> getSnapshotItems(String nodeId) {
        return nodeDAO.getSnapshotData(nodeId).getSnapshotItems();
    }

    private List<SnapshotItem> getCompositeSnapshotItems(String nodeId) {
        CompositeSnapshotData compositeSnapshotData = nodeDAO.getCompositeSnapshotData(nodeId);
        List<String> referencedSnapshots = compositeSnapshotData.getReferencedSnapshotNodes();
        List<SnapshotItem> snapshotItems = new ArrayList<>();
        referencedSnapshots.forEach(id -> snapshotItems.addAll(getSnapshotItems(id)));
        return snapshotItems;
    }
}

