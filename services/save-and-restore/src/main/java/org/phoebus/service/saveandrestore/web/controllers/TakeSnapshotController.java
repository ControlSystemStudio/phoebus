/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.service.saveandrestore.web.controllers;

import org.phoebus.applications.saveandrestore.model.ConfigurationData;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.applications.saveandrestore.model.Snapshot;
import org.phoebus.applications.saveandrestore.model.SnapshotData;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.saveandrestore.util.SnapshotUtil;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class TakeSnapshotController extends BaseController {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private SnapshotUtil snapshotUtil;

    private final SimpleDateFormat simpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Logger LOG = Logger.getLogger(TakeSnapshotController.class.getName());

    /**
     * Take a snapshot based on the {@link org.phoebus.applications.saveandrestore.model.Configuration}'s unique id.
     *
     * @param configNodeId Unique id of a {@link org.phoebus.applications.saveandrestore.model.Configuration}
     * @return A {@link List} of {@link SnapshotItem}s, one for each {@link org.phoebus.applications.saveandrestore.model.ConfigPv}
     * in the {@link org.phoebus.applications.saveandrestore.model.Configuration}.
     */
    @SuppressWarnings("unused")
    @GetMapping(value = "/take-snapshot/{configNodeId}", produces = JSON)
    public List<SnapshotItem> takeSnapshot(@PathVariable String configNodeId) {
        Node configNode = nodeDAO.getNode(configNodeId);
        LOG.log(Level.INFO, "Take snapshot for configuration '" + configNode.getName() + "'");
        ConfigurationData configurationData = nodeDAO.getConfigurationData(configNodeId);
        List<SnapshotItem> snapshotItems;
        try {
            snapshotItems = snapshotUtil.takeSnapshot(configurationData, connectionTimeout);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return snapshotItems;
    }

    /**
     * Take a snapshot based on the {@link org.phoebus.applications.saveandrestore.model.Configuration}'s unique id,
     * and attempts to save it. If a snapshot name is provided and if the parent configuration node already
     * contains a snapshot with the same name, an exception is thrown.
     *
     * @param configNodeId Unique id of a {@link org.phoebus.applications.saveandrestore.model.Configuration}
     * @param snapshotName Optional name of the new snapshot. If not provided or empty, current date and time will
     *                     define the name.
     * @param comment      Optional comment of the new snapshot. If not provided or empty, current date and time will
     *                     define the comment.
     * @return A {@link Snapshot} representing the new snapshot node.
     */
    @SuppressWarnings("unused")
    @PutMapping(value = "/take-snapshot/{configNodeId}", produces = JSON)
    public Snapshot takeSnapshotAndSave(@PathVariable String configNodeId,
                                        @RequestParam(name = "name", required = false) String snapshotName,
                                        @RequestParam(name = "comment", required = false) String comment) {
        if (snapshotName != null) {
            String _snapshotName = snapshotName;
            List<Node> childNodes = nodeDAO.getChildNodes(configNodeId);
            if (childNodes.stream().anyMatch(n -> n.getName().equals(_snapshotName) &&
                    n.getNodeType().equals(NodeType.SNAPSHOT))) {
                throw new IllegalArgumentException("Snapshot named " + _snapshotName
                        + " already exists");
            }
        }

        Date now = new Date();
        snapshotName = snapshotName == null || snapshotName.isEmpty() ?
                simpleDateFormat.format(now) :
                snapshotName;
        comment = comment == null || comment.isEmpty() ?
                simpleDateFormat.format(now) :
                comment;
        List<SnapshotItem> snapshotItems = takeSnapshot(configNodeId);

        Node node = Node.builder().nodeType(NodeType.SNAPSHOT).name(snapshotName).description(comment).build();
        SnapshotData snapshotData = new SnapshotData();
        snapshotData.setSnapshotItems(snapshotItems);
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotNode(node);
        snapshot.setSnapshotData(snapshotData);

        return nodeDAO.createSnapshot(configNodeId, snapshot);
    }
}
