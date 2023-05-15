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

package org.phoebus.applications.saveandrestore.ui;

import javafx.scene.input.TransferMode;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;

import java.util.List;

public class DragNDropUtil {

    /**
     * Implements business rules for dropping a {@link Node}s selection onto an item in the tree view:
     * <ul>
     *     <li>Target {@link Node} may not be contained in list of source {@link Node}s.</li>
     *     <li>If target {@link Node} is if {@link NodeType#COMPOSITE_SNAPSHOT}, then source node
     *     list items must all be of type {@link NodeType#COMPOSITE_SNAPSHOT} or {@link NodeType#SNAPSHOT}</li>
     *     <li>If target {@link Node} is if {@link NodeType#FOLDER} and source node list
     *     items are all of type {@link NodeType#COMPOSITE_SNAPSHOT} or {@link NodeType#SNAPSHOT}, then
     *     the transfer mode must be {@link TransferMode#LINK}. If not, then source node list must not
     *     contain items of type {@link NodeType#COMPOSITE_SNAPSHOT} or {@link NodeType#SNAPSHOT}.</li>
     * </ul>
     * @param targetNode The drop target
     * @param sourceNodes Selection of {@link Node}s
     * @return <code>true</code> if the selection (source nodes) may be dropped on the target, otherwise <code>false</code>.
     */
    public static boolean mayDrop(TransferMode transferMode, Node targetNode, List<Node> sourceNodes) {
        // Source nodes list may not contain target node.
        if (sourceNodes.contains(targetNode)) {
            return false;
        }
        switch (targetNode.getNodeType()){
            case COMPOSITE_SNAPSHOT:
                return snapshotsOrCompositeSnapshotsOnly(sourceNodes);
            case FOLDER:
                if(sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)).findFirst().isPresent()) {
                    return transferMode.equals(TransferMode.MOVE);
                }
                else{
                    return sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.SNAPSHOT)).findFirst().isEmpty();
                }
            default:
                return false;
        }
        /*
        boolean snapshotsOrCompositeSnapshotsOnly = snapshotsOrCompositeSnapshotsOnly(sourceNodes);

        // A list of snapshots and composite snapshots may be dropped onto a composite snapshot node
        if (targetNode.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT) && snapshotsOrCompositeSnapshotsOnly) {
            return true;
        }
        boolean selectionContainsSnapshot = sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.SNAPSHOT)).findFirst().isPresent();
        if (!selectionContainsSnapshot) {
            // Composite snapshot (temporarily?) supports only move.
            if(sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.COMPOSITE_SNAPSHOT)).findFirst().isPresent()) {
                return transferMode.equals(TransferMode.MOVE);
            }
            else {
                return true;
            }
        }
        else if(targetNode.getNodeType().equals(NodeType.FOLDER)){
            return true;
        }

        return false;

         */
    }

    public static boolean snapshotsOrCompositeSnapshotsOnly(List<Node> sourceNodes){
        return sourceNodes.stream().filter(n -> n.getNodeType().equals(NodeType.FOLDER) ||
                n.getNodeType().equals(NodeType.CONFIGURATION)).findFirst().isEmpty();
    }

}
