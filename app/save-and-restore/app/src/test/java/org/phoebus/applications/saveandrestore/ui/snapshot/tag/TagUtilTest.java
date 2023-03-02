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

package org.phoebus.applications.saveandrestore.ui.snapshot.tag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.Tag;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TagUtilTest {

    private Node node1;
    private Node node2;
    private Node node3;

    private Tag tag1 = Tag.builder().name("name1").build();
    private Tag tag2 = Tag.builder().name("name2").build();
    private Tag tag3 = Tag.builder().name("name3").build();
    private Tag golden = Tag.goldenTag("");

    @BeforeEach
    public void init(){
        node1 = Node.builder().build();
        node2 = Node.builder().build();
        node3 = Node.builder().build();
    }

    @Test
    public void testSingleNode(){
        List<Tag> commonTags = TagUtil.getCommonTags(List.of(node1));
        assertTrue(commonTags.isEmpty());

        node1.setTags(Arrays.asList(tag1, tag2));
        commonTags = TagUtil.getCommonTags(List.of(node1));
        assertEquals(2, commonTags.size());
    }

    @Test
    public void testMultipleNodes(){
        List<Tag> commonTags = TagUtil.getCommonTags(Arrays.asList(node1, node2));
        assertTrue(commonTags.isEmpty());

        node1.setTags(Arrays.asList(tag1, tag2));
        commonTags = TagUtil.getCommonTags(Arrays.asList(node1, node2));
        assertTrue(commonTags.isEmpty());

        node1.setTags(Arrays.asList(tag1, tag2));
        node2.setTags(Arrays.asList(tag1));

        commonTags = TagUtil.getCommonTags(Arrays.asList(node1, node2));
        assertEquals(1, commonTags.size());

        node1.setTags(Arrays.asList(tag1, tag2, tag3));
        node2.setTags(Arrays.asList(tag1, tag2));
        node3.setTags(Arrays.asList(tag2, tag3));
        commonTags = TagUtil.getCommonTags(Arrays.asList(node1, node2, node3));

        assertEquals(1, commonTags.size());

        node1.setTags(Arrays.asList(tag1, tag2, tag3));
        node2.setTags(Arrays.asList(tag1, tag2, tag3));
        node3.setTags(Arrays.asList(tag1, tag2, tag3));
        commonTags = TagUtil.getCommonTags(Arrays.asList(node1, node2, node3));

        assertEquals(3, commonTags.size());
    }
}
