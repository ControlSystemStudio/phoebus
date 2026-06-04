/**
 * Copyright (C) 2026 European Spallation Source ERIC.
 */
package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link RestController} performing server-side restore operation.
 */
@SuppressWarnings("unused")
@RestController
public class SnapshotRestoreController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private SnapshotUtil snapshotUtil;

    private static final Logger LOG = Logger.getLogger(SnapshotRestoreController.class.getName());

    /**
     * Restores a snapshot or composite snapshot
     *
     * @param snapshotItems List of {@link SnapshotItem}s subject to a restore operation. Callee will need to
     *                      retrieve this list from a snapshot or composite snapshot.
     * @return The result of the operation
     */
    @PostMapping(value = "/restore/items", produces = JSON)
    public List<RestoreResult> restoreFromSnapshotItems(
            @RequestBody List<SnapshotItem> snapshotItems) {
        return snapshotUtil.restore(snapshotItems, connectionTimeout);
    }

    /**
     * Restores a snapshot or composite snapshot
     *
     * @param nodeId Unique id of a snapshot or composite snapshot
     * @return The result of the operation
     */
    @PostMapping(value = "/restore/node", produces = JSON)
    public List<RestoreResult> restoreFromSnapshotNode(
            @RequestParam(value = "nodeId") String nodeId) {
        Node snapshotNode = nodeDAO.getNode(nodeId);
        LOG.log(Level.INFO, "Restore requested for snapshot '" + snapshotNode.getName() + "'");
        List<SnapshotItem> snapshotItems;
        if (snapshotNode.getNodeType().equals(NodeType.SNAPSHOT)) {
            snapshotItems = nodeDAO.getSnapshotData(nodeId).getSnapshotItems();
        } else if(snapshotNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)){
            snapshotItems = nodeDAO.getSnapshotItemsFromCompositeSnapshot(nodeId);
        }
        else{
            throw new IllegalArgumentException("Node " + snapshotNode + " is not a snapshot or composite snapshot");
        }
        return snapshotUtil.restore(snapshotItems, connectionTimeout);
    }
}

