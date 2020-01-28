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

package org.phoebus.service.saveandrestore.persistence.dao.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;

import org.junit.Test;
import org.mockito.Mockito;

import org.phoebus.service.saveandrestore.model.internal.SnapshotPv;

/**
 * @author georgweiss
 * Created 27 Mar 2019
 */
public class SnapshotPvRowMapperTest {

	@Test
	public void testSnapshotPvRowMapperNullReadback() throws Exception{

		ResultSet resultSet = Mockito.mock(ResultSet.class);
	
		when(resultSet.getInt("snapshot_node_id")).thenReturn(1);
		when(resultSet.getBoolean("fetch_status")).thenReturn(true);
		when(resultSet.getString("severity")).thenReturn("NONE");
		when(resultSet.getString("status")).thenReturn("NONE");
		when(resultSet.getLong("time")).thenReturn(777L);
		when(resultSet.getInt("timens")).thenReturn(1);
		when(resultSet.getString("value")).thenReturn("[7]");
		when(resultSet.getString("sizes")).thenReturn("[1]");
		when(resultSet.getString("data_type")).thenReturn("INTEGER");
		when(resultSet.getString("name")).thenReturn("pvname");
		when(resultSet.getString("readback_name")).thenReturn("pvname");
		when(resultSet.getString("provider")).thenReturn("ca");
		
		
		assertTrue(new SnapshotPvRowMapper().mapRow(resultSet, 0) instanceof SnapshotPv);
	}
	
	@Test
	public void testSnapshotPvRowMapper() throws Exception{

		ResultSet resultSet = Mockito.mock(ResultSet.class);
	
		when(resultSet.getInt("snapshot_node_id")).thenReturn(1);
		when(resultSet.getBoolean("fetch_status")).thenReturn(true);
		when(resultSet.getString("severity")).thenReturn("NONE");
		when(resultSet.getString("status")).thenReturn("NONE");
		when(resultSet.getLong("time")).thenReturn(777L);
		when(resultSet.getInt("timens")).thenReturn(1);
		when(resultSet.getString("value")).thenReturn("[7]");
		when(resultSet.getString("sizes")).thenReturn("[1]");
		when(resultSet.getString("data_type")).thenReturn("INTEGER");
		when(resultSet.getString("name")).thenReturn("pvname");
		when(resultSet.getString("readback_name")).thenReturn("pvname");
		when(resultSet.getString("provider")).thenReturn("ca");
	}
}
