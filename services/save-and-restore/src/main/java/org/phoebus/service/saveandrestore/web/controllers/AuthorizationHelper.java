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

import org.phoebus.applications.saveandrestore.model.*;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link Service} class implementing domain specific authorization rules in order to
 * grant or deny access to certain REST endpoints.
 */
@Service("authorizationHelper")
@SuppressWarnings("unused")
public class AuthorizationHelper {

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String roleAdmin;

    /**
     * Checks if all the nodes provided to this method can be deleted by the user.
     *
     * @param nodeIds   The list of {@link Node} ids subject to the check.
     * @param principal {@link Principal} of authenticated user.
     * @return <code>true</code> only if <b>all</b> if the nodes can be deleted by the user.
     */
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

    public boolean mayUpdate(CompositeSnapshot compositeSnapshot, Principal principal) {
        return isOwner(compositeSnapshot.getCompositeSnapshotNode().getUniqueId(), principal.getName());
    }

    public boolean mayAddOrDeleteTag(TagData tagData, Authentication authentication){
        Tag tag = tagData.getTag();
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        if(tag.getName().equals(Tag.GOLDEN)){
            return roles.contains(roleAdmin);
        }
        for(String nodeId : tagData.getUniqueNodeIds()){
            if(!isOwner(nodeId, authentication.getName())){
                return false;
            }
        }
        return true;
    }

    private boolean isOwner(String nodeId, String username){
        Node node = nodeDAO.getNode(nodeId);
        return node.getUserName().equals(username);
    }
}
