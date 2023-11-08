/*
 * Copyright (C) 2023 European Spallation Source ERIC.
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
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

/**
 * {@link Service} class implementing domain specific authorization rules in order to
 * grant or deny access to certain REST endpoints.
 */
@Service("authorizationHelper")
public class AuthorizationHelper {

    @Autowired
    private NodeDAO nodeDAO;

    /**
     * Checks if all the nodes provided to this method can be deleted by the user.
     *
     * @param nodeIds   The list of {@link Node} ids subject to the check.
     * @param principal {@link Principal} of authenticated user.
     * @return <code>true</code> only if <b>all</b> if the nodes can be deleted by the user.
     */
    @SuppressWarnings("unused")
    public boolean mayDelete(List<String> nodeIds, Principal principal) {
        for (String nodeId : nodeIds) {
            if (!mayDelete(nodeId, principal)) {
                return false;
            }
        }
        return true;
    }

    /**
     * An authenticated user may delete a node if User identity is same as the target {@link Node}'s user id and:
     * <ul>
     *     <li>Target {@link Node} is a snapshot.</li>
     *     <li>Target {@link Node} is not a snapshot, but has no child nodes.</li>
     * </ul>
     *
     * @param nodeId    Unique node id identifying the target of the user's delete operation.
     * @param principal Identifies user.
     * @return <code>false</code> if user may not delete the {@link Node}.
     */
    @SuppressWarnings("unused")
    public boolean mayDelete(String nodeId, Principal principal) {
        Node node = nodeDAO.getNode(nodeId);
        if (!node.getUserName().equals(principal.getName())) {
            return false;
        }
        if (node.getNodeType().equals(NodeType.CONFIGURATION) || node.getNodeType().equals(NodeType.FOLDER)) {
            return nodeDAO.getChildNodes(node.getUniqueId()).isEmpty();
        }
        return true;
    }

    /**
     * An authenticated user may update a node if user identity is same as the target {@link Node}'s user id.
     *
     * @param node      {@link Node} identifying the target of the user's update operation.
     * @param principal Identifies user.
     * @return <code>false</code> if user may not update the {@link Node}.
     */
    @SuppressWarnings("unused")
    public boolean mayUpdate(Node node, Principal principal) {
        return nodeDAO.getNode(node.getUniqueId()).getUserName().equals(principal.getName());
    }

    /**
     * <p>
     * An authenticated user may save a composite snapshot, and update if user identity is same as the target's
     * composite snapshot {@link Node}.
     * </p>
     *
     * @param compositeSnapshot {@link CompositeSnapshot} identifying the target of the user's update operation.
     * @param principal         Identifies user.
     * @return <code>false</code> if user may not update the {@link CompositeSnapshot}.
     */
    @SuppressWarnings("unused")
    public boolean mayUpdate(CompositeSnapshot compositeSnapshot, Principal principal) {
        Node node = nodeDAO.getNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId());
        return node.getUserName().equals(principal.getName());
    }

}
