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
import org.phoebus.applications.saveandrestore.model.RestoreResult;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
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

    @PostMapping(value = "/restore/items", produces = JSON)
    public List<RestoreResult> restoreFromSnapshotItems(
            @RequestBody List<SnapshotItem> snapshotItems) {
        return snapshotUtil.restore(snapshotItems, connectionTimeout);
    }

    @PostMapping(value = "/restore/node", produces = JSON)
    public List<RestoreResult> restoreFromSnapshotNode(
            @RequestParam(value = "nodeId") String nodeId){
        Node snapshotNode = nodeDAO.getNode(nodeId);
        LOG.log(Level.INFO, "Restore requested for snapshot '" + snapshotNode.getName() + "'");
        var snapshot = nodeDAO.getSnapshotData(nodeId);
        return snapshotUtil.restore(snapshot.getSnapshotItems(), connectionTimeout);
    }
}

