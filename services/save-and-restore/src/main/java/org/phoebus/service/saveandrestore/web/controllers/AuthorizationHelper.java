/*
 * Copyright (C) 2024 European Spallation Source ERIC.
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
import org.phoebus.applications.saveandrestore.model.search.Filter;
import org.phoebus.service.saveandrestore.persistence.dao.NodeDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * {@link Service} class implementing domain specific authorization rules in order to
 * grant or deny access to certain REST endpoints.
 */
@Service("authorizationHelper")
@SuppressWarnings("unused")
public class AuthorizationHelper {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private NodeDAO nodeDAO;

    @Autowired
    private String roleAdmin;

    @Autowired
    private String roleUser;

    @Value("${authorization.permitall:true}")
    public boolean permitAll;

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Checks if all the nodes provided to this method can be deleted by the user. User with admin privileges is always
     * permitted to delete, while a user not having required role may never delete.
     *
     * @param nodeIds                            The list of {@link Node} ids subject to the check.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>true</code> only if <b>all</b> if the nodes can be deleted by the user.
     */
    public boolean mayDelete(List<String> nodeIds, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        for (String nodeId : nodeIds) {
            if (!mayDelete(nodeId, ((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername())) {
                return false;
            }
        }
        return true;
    }

    /**
     * An authenticated user may delete id user identity is same as the target {@link Node}'s user unique id and:
     * <ul>
     *     <li>Target {@link Node} is a snapshot.</li>
     *     <li>Target {@link Node} is not a snapshot, but has no child nodes.</li>
     * </ul>
     *
     * @param nodeId   The target {@link Node}'s unique node id.
     * @param userName {@link MethodSecurityExpressionOperations} Username of authenticated user.
     * @return <code>false</code> if user may not delete the {@link Node}.
     */
    private boolean mayDelete(String nodeId, String userName) {
        Node node = nodeDAO.getNode(nodeId);
        if (!node.getUserName().equals(userName)) {
            return false;
        }
        if (node.getNodeType().equals(NodeType.CONFIGURATION) || node.getNodeType().equals(NodeType.FOLDER)) {
            return nodeDAO.getChildNodes(node.getUniqueId()).isEmpty();
        }
        return true;
    }

    /**
     * An authenticated user may update a node if user has admin privileges, or
     * if user identity is same as the target {@link Node}'s user id.
     *
     * @param node                               {@link Node} identifying the target of the user's update operation.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not update the {@link Node}.
     */
    public boolean mayUpdate(Node node, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        return nodeDAO.getNode(node.getUniqueId()).getUserName()
                .equals(((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername());
    }

    /**
     * An authenticated user may update a composite snapshot if user has admin privileges, or
     * if user identity is same as the target {@link Node}'s user id.
     *
     * @param compositeSnapshot                  {@link CompositeSnapshot} identifying the target of the user's update operation.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not update the {@link CompositeSnapshot}.
     */
    public boolean mayUpdate(CompositeSnapshot compositeSnapshot, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        return nodeDAO.getNode(compositeSnapshot.getCompositeSnapshotNode().getUniqueId()).getUserName()
                .equals(((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername());
    }

    /**
     * An authenticated user may update a configuration if user has admin privileges, or
     * if user identity is same as the target {@link Node}'s user id.
     *
     * @param configuration                      {@link Configuration} identifying the target of the user's update operation.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not update the {@link Configuration}.
     */
    public boolean mayUpdate(Configuration configuration, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        return nodeDAO.getNode(configuration.getConfigurationNode().getUniqueId()).getUserName()
                .equals(((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername());
    }

    /**
     * An authenticated user may update a snapshot if user has admin privileges, or
     * if user identity is same as the target {@link Node}'s user id.
     *
     * @param snapshot                           {@link Snapshot} identifying the target of the user's update operation.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not update the {@link Snapshot}.
     */
    public boolean mayUpdate(Snapshot snapshot, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        // If snapshot's node has null id, then this is a
        return nodeDAO.getNode(snapshot.getSnapshotNode().getUniqueId()).getUserName()
                .equals(((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername());
    }

    /**
     * An authenticated user may add or delete {@link Tag}s if user identity is same as the target's
     * snapshot {@link Node}. However, to add or delete golden tag user must have admin privileges.
     *
     * @param tagData                            {@link TagData} containing {@link Node} ids and {@link Tag} name.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>true</code> if {@link Tag} can be added or deleted.
     */
    public boolean mayAddOrDeleteTag(TagData tagData, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (tagData.getTag() == null ||
                tagData.getTag().getName() == null ||
                tagData.getTag().getName().isEmpty() ||
                tagData.getUniqueNodeIds() == null) {
            throw new IllegalArgumentException("Cannot add tag, data invalid");
        }
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        Tag tag = tagData.getTag();
        if (tag.getName().equals(Tag.GOLDEN)) {
            return methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin);
        }
        String username = ((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername();
        for (String nodeId : tagData.getUniqueNodeIds()) {
            Node node = nodeDAO.getNode(nodeId);
            if (!node.getUserName().equals(username)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * An authenticated user may save a filter, and update/delete if user identity is same as the target's
     * name field.
     * </p>
     *
     * @param filterName                         Unique name identifying the target of the user's update operation.
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not update the {@link Filter}.
     */
    public boolean maySaveOrDeleteFilter(String filterName, MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        if (permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin)) {
            return true;
        }
        if (!methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser)) {
            return false;
        }
        Optional<Filter> filter1 =
                nodeDAO.getAllFilters().stream().filter(f ->
                        f.getName().equals(filterName)).findFirst();
        // If the filter does not (yet) exist, save is OK
        if (filter1.isEmpty()) {
            return true;
        }
        String username = ((UserDetails) methodSecurityExpressionOperations.getAuthentication().getPrincipal()).getUsername();
        return filter1.map(value -> value.getUser().equals(username)).orElse(true);
    }

    /**
     * Checks if user is allowed to create an object (node, snapshot...). This is the case if authorization
     * is disabled or if user has (basic) user role.
     *
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not create the object, otherwise <code>true</code>.
     */
    public boolean mayCreate(MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        return permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleUser);
    }

    /**
     * Checks if user is allowed to move or copy objects. This is the case if authorization
     * is disabled or if user has admin role.
     *
     * @param methodSecurityExpressionOperations {@link MethodSecurityExpressionOperations} Spring managed object
     *                                           queried for authorization.
     * @return <code>false</code> if user may not create the object, otherwise <code>true</code>.
     */
    public boolean mayMoveOrCopy(MethodSecurityExpressionOperations methodSecurityExpressionOperations) {
        return permitAll || methodSecurityExpressionOperations.hasAuthority(ROLE_PREFIX + roleAdmin);
    }
}
