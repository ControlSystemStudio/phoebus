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

package org.phoebus.service.saveandrestore.model;

import org.phoebus.applications.saveandrestore.model.Node;

import java.util.List;

/**
 * Pojo class representing a tree node and a list of child nodes.
 */
public class ESTreeNode {

    private Node node;
    private List<String> childNodes;

    /**
     * @return The {@link Node} object represented by this class.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Setter
     * @param node A {@link Node} object
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     *
     * @return List of child {@link Node}s for the {@link Node} object represented by this class. May be <code>null</code>
     * or empty.
     */
    public List<String> getChildNodes() {
        return childNodes;
    }

    /**
     *
     * @param childNodes {@link List} of child {@link Node}s
     */
    public void setChildNodes(List<String> childNodes) {
        this.childNodes = childNodes;
    }
}
