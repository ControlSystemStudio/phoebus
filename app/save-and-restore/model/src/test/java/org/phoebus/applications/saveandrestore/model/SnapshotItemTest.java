/*
 * Copyright (C) 2018 European Spallation Source ERIC.
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

package org.phoebus.applications.saveandrestore.model;

import org.junit.Test;

import static org.junit.Assert.*;

import org.epics.vtype.VType;

/**
 * @author georgweiss
 * Created 14 May 2019
 */
public class SnapshotItemTest {

	@Test
	public void testToString() {
		SnapshotItem item = SnapshotItem.builder()
				.configPv(ConfigPv.builder().pvName("pvname").build())
				.build();
		
		assertNotNull(item.toString());
		
		item.setValue(VType.toVType(1));
		assertNotNull(item.toString());
	
		item.setReadbackValue(VType.toVType(1.1));
		assertNotNull(item.toString());
		assertFalse(item.toString().contains("READ FAILED"));
	}
}
