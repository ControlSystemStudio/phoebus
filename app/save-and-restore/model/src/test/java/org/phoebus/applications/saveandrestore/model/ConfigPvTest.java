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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * @author georgweiss
 * Created 27 Mar 2019
 */
public class ConfigPvTest {

	@Test
	public void testConfigPv() {
		assertNull(ConfigPv.builder().pvName("a").build().getReadbackPvName());
		assertFalse(ConfigPv.builder().pvName("a").build().isReadOnly());
		
		ConfigPv configPV = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		
		assertEquals("b", configPV.getReadbackPvName());
		assertTrue(configPV.isReadOnly());
	}
	
	@Test
	public void testEquals() {
	
		
		ConfigPv configPV1 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		assertFalse(configPV1.equals(new Object()));
		assertFalse(configPV1.equals(null));
		ConfigPv configPV2 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		ConfigPv configPV3 = ConfigPv.builder().pvName("a").readbackPvName("c").readOnly(true).build();
		
		assertEquals(configPV1, configPV2);
		assertNotEquals(configPV1, configPV3);
		
		configPV1 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("b").readbackPvName("b").readOnly(true).build();
		
		assertNotEquals(configPV1, configPV2);
		
		configPV1 = ConfigPv.builder().pvName("a").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("b").readOnly(true).build();
		
		assertNotEquals(configPV1, configPV2);
		
		configPV1 = ConfigPv.builder().pvName("a").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("a").readOnly(true).build();
		
		assertEquals(configPV1, configPV2);
	}
	
	@Test
	public void testHashCode() {
		ConfigPv configPV1 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		ConfigPv configPV2 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		
		assertEquals(configPV1.hashCode(), configPV2.hashCode());
		
		configPV1 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("b").readbackPvName("b").readOnly(true).build();
		
		assertNotEquals(configPV1.hashCode(), configPV2.hashCode());
		
		configPV1 = ConfigPv.builder().pvName("a").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("b").readOnly(true).build();
		
		assertNotEquals(configPV1.hashCode(), configPV2.hashCode());
		
		configPV1 = ConfigPv.builder().pvName("a").readOnly(true).build();
		configPV2 = ConfigPv.builder().pvName("a").readOnly(true).build();
		
		assertEquals(configPV1.hashCode(), configPV2.hashCode());
	}
	
	@Test
	public void testToString() {
		assertNotNull(ConfigPv.builder().build().toString());
		assertNotNull(ConfigPv.builder().readbackPvName("a").build().toString());
		assertNotNull(ConfigPv.builder().readbackPvName("").build().toString());
	}
	
	@Test
	public void testCompareTo(){
		ConfigPv configPV1 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		ConfigPv configPV2 = ConfigPv.builder().pvName("a").readbackPvName("b").readOnly(true).build();
		ConfigPv configPV3 = ConfigPv.builder().pvName("b").readbackPvName("b").readOnly(true).build();
		
		assertTrue(configPV1.compareTo(configPV2) == 0);
		assertTrue(configPV1.compareTo(configPV3) < 0);
		assertTrue(configPV3.compareTo(configPV1) > 0);
	}
	
}
