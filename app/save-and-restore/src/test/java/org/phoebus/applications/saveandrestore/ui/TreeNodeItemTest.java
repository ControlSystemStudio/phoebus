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

package org.phoebus.applications.saveandrestore.ui;

import org.junit.Test;
import static org.junit.Assert.*;

import se.esss.ics.masar.model.Node;

public class TreeNodeItemTest {
	
	@Test
	public void testComparison() {
			
		TreeNodeItem i1 = new TreeNodeItem(Node.builder().name("A").build());
		TreeNodeItem i2 = new TreeNodeItem(Node.builder().name("A").build());
		TreeNodeItem i3 = new TreeNodeItem(Node.builder().name("B").build());
		
		assertTrue(i1.compareTo(i2) == 0);
		assertTrue(i1.compareTo(i3) < 0);
		assertTrue(i3.compareTo(i1) > 0);
		
	}
}
