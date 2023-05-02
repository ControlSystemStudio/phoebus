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
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DragNDropUtilTest {

    private final Node folderTargetNode = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.FOLDER).build();
    private final Node compositeSnapshotTargetNode = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.COMPOSITE_SNAPSHOT).build();
    private final Node snapshotTargetNode = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.SNAPSHOT).build();
    private final Node configurationTargetNode = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();


    @Test
    public void testMayDropFoldersOnFolder(){
        Node f1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.FOLDER).build();
        Node f2 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.FOLDER).build();

        assertTrue(DragNDropUtil.mayDrop(TransferMode.MOVE, folderTargetNode, Arrays.asList(f1, f2)));
        assertTrue(DragNDropUtil.mayDrop(TransferMode.COPY, folderTargetNode, Arrays.asList(f1, f2)));
    }

    @Test
    public void testMayNotDropFoldersOnNonFolder(){
        Node f1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.FOLDER).build();
        Node f2 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.FOLDER).build();

        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, compositeSnapshotTargetNode, Arrays.asList(f1, f2)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, compositeSnapshotTargetNode, Arrays.asList(f1, f2)));

        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, configurationTargetNode, Arrays.asList(f1, f2)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, configurationTargetNode, Arrays.asList(f1, f2)));

        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, snapshotTargetNode, Arrays.asList(f1, f2)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, snapshotTargetNode, Arrays.asList(f1, f2)));
    }

    @Test
    public void testMayDropConfigurationOnFolder(){
        Node c1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertTrue(DragNDropUtil.mayDrop(TransferMode.MOVE, folderTargetNode, Collections.singletonList(c1)));
        assertTrue(DragNDropUtil.mayDrop(TransferMode.COPY, folderTargetNode, Collections.singletonList(c1)));
    }

    @Test
    public void testMayNotDropConfigurationOnNonFolder(){
        Node c1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.CONFIGURATION).build();
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, configurationTargetNode, Collections.singletonList(c1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, configurationTargetNode, Collections.singletonList(c1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, snapshotTargetNode, Collections.singletonList(c1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, snapshotTargetNode, Collections.singletonList(c1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, compositeSnapshotTargetNode, Collections.singletonList(c1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, compositeSnapshotTargetNode, Collections.singletonList(c1)));
    }

    @Test
    public void testMayDropSnapshotsAndCompositeSnapshotsOnCompositeSnapshot(){
        Node cs1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.COMPOSITE_SNAPSHOT).build();
        Node s1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.SNAPSHOT).build();
        assertTrue(DragNDropUtil.mayDrop(TransferMode.MOVE, compositeSnapshotTargetNode, Arrays.asList(cs1, s1)));
        assertTrue(DragNDropUtil.mayDrop(TransferMode.COPY, compositeSnapshotTargetNode, Arrays.asList(cs1, s1)));
    }

    @Test
    public void testMayNotDropSnapshotOnNonCompositeSnapshot(){
        Node s1 = Node.builder().uniqueId(UUID.randomUUID().toString()).nodeType(NodeType.SNAPSHOT).build();
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, configurationTargetNode, Collections.singletonList(s1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, configurationTargetNode, Collections.singletonList(s1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, folderTargetNode, Collections.singletonList(s1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, folderTargetNode, Collections.singletonList(s1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.MOVE, snapshotTargetNode, Collections.singletonList(s1)));
        assertFalse(DragNDropUtil.mayDrop(TransferMode.COPY, snapshotTargetNode, Collections.singletonList(s1)));
    }
}
