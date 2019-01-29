/*
 * Copyright (C) 2019 European Spallation Source ERIC.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.phoebus.applications.saveandrestore.data;

import org.junit.Test;
import org.phoebus.applications.saveandrestore.ui.model.FolderTreeNode;
import org.phoebus.applications.saveandrestore.ui.model.TreeNode;

import static org.junit.Assert.*;

public class TestTreeNode {

	@Test
	public void testEquals() {
		
		TreeNode t1 = FolderTreeNode.builder()
				.id(1)
				.name("name1")
				.build();
		
		TreeNode t2 = FolderTreeNode.builder()
				.id(1)
				.name("name1")
				.build();
		
		assertEquals(t1, t2);
		assertEquals(t1.hashCode(), t2.hashCode());
		
		t1.setId(2);
		
		assertNotEquals(t1,  t2);
		assertNotEquals(t1.hashCode(), t2.hashCode());
		
		t1.setId(1);
		t1.setName("name");
		
		assertNotEquals(t1,  t2);
		assertNotEquals(t1.hashCode(), t2.hashCode());
		
		assertNotEquals(t1, new Object());
		
		assertEquals(t1, t1);
	}
}
