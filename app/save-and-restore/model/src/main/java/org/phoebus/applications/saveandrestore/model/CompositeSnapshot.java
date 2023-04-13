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
 * Thin wrapper around a {@link Node} of type {@link NodeType#COMPOSITE_SNAPSHOT} and a
 * {@link CompositeSnapshotData} object representing all referenced {@link Node}s.
 */
public class CompositeSnapshot {

    private Node compositeSnapshotNode;

    private CompositeSnapshotData compositeSnapshotData;

    public Node getCompositeSnapshotNode() {
        return compositeSnapshotNode;
    }

    public void setCompositeSnapshotNode(Node compositeSnapshotNode) {
        this.compositeSnapshotNode = compositeSnapshotNode;
    }

    public CompositeSnapshotData getCompositeSnapshotData() {
        return compositeSnapshotData;
    }

    public void setCompositeSnapshotData(CompositeSnapshotData compositeSnapshotData) {
        this.compositeSnapshotData = compositeSnapshotData;
    }

    public static CompositeSnapshot.Builder builder(){
        return new CompositeSnapshot.Builder();
    }

    public static class Builder{

        private CompositeSnapshot compositeSnapshot;

        private Builder(){
            compositeSnapshot = new CompositeSnapshot();
        }

        public CompositeSnapshot.Builder compositeSnapshotData(CompositeSnapshotData compositeSnapshotData){
            compositeSnapshot.setCompositeSnapshotData(compositeSnapshotData);
            return this;
        }

        public CompositeSnapshot.Builder compositeSnapshotNode(Node compositeSnapshotNode){
            compositeSnapshot.setCompositeSnapshotNode(compositeSnapshotNode);
            return this;
        }

        public CompositeSnapshot build(){
            return compositeSnapshot;
        }
    }
}
