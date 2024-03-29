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

package org.phoebus.applications.saveandrestore.model;

/**
 * Thin wrapper around a {@link Node} of type {@link NodeType#SNAPSHOT} and a
 * {@link SnapshotData} object.
 */
public class Snapshot {

    private Node snapshotNode;
    private SnapshotData snapshotData;

    public Node getSnapshotNode() {
        return snapshotNode;
    }

    public void setSnapshotNode(Node snapshotNode) {
        this.snapshotNode = snapshotNode;
    }

    public SnapshotData getSnapshotData() {
        return snapshotData;
    }

    public void setSnapshotData(SnapshotData snapshotData) {
        this.snapshotData = snapshotData;
    }

    @Override
    public boolean equals(Object other){
        if(!(other instanceof Snapshot)){
            return false;
        }
        return snapshotNode.equals(((Snapshot) other).getSnapshotNode());
    }

    @Override
    public int hashCode(){
        return snapshotNode.hashCode();
    }
}
