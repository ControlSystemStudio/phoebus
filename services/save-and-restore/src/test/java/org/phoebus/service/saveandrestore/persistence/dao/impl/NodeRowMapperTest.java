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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.junit.Test;
import org.mockito.Mockito;

import org.phoebus.applications.saveandrestore.model.Node;
import org.phoebus.applications.saveandrestore.model.NodeType;

/**
 * @author georgweiss Created 7 Feb 2019
 */
public class NodeRowMapperTest {

	@Test
	public void testRowMapper() throws Exception {

		ResultSet resultSet = Mockito.mock(ResultSet.class);

		when(resultSet.getInt("id")).thenReturn(1);
		when(resultSet.getString("name")).thenReturn("name");
		when(resultSet.getTimestamp("created")).thenReturn(new Timestamp(System.currentTimeMillis()));
		when(resultSet.getTimestamp("last_modified")).thenReturn(new Timestamp(System.currentTimeMillis()));
		when(resultSet.getString("username")).thenReturn("username");
		when(resultSet.getString("type")).thenReturn(NodeType.FOLDER.toString());
		
		assertTrue(new NodeRowMapper().mapRow(resultSet, 0) instanceof Node);
	}
}
