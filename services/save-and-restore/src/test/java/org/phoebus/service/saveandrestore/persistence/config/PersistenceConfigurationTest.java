/**
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

package org.phoebus.service.saveandrestore.persistence.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { PersistenceConfiguration.class }) })
@TestPropertySource(properties = {"dbengine = h2"})
public class PersistenceConfigurationTest {
	
	@Autowired
	private DataSource dataSource;
	
	@Autowired
	private SimpleJdbcInsert configurationEntryInsert;
	
	@Autowired
	private SimpleJdbcInsert configurationEntryRelationInsert;
	
	@Autowired
	private SimpleJdbcInsert snapshotPvInsert;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private ObjectMapper objectMapper;
	

	@Test
	public void test() {
		assertNotNull(dataSource);
		assertNotNull(configurationEntryInsert);
		assertEquals("config_pv", configurationEntryInsert.getTableName());
		assertNotNull(configurationEntryRelationInsert);
		assertEquals("config_pv_relation", configurationEntryRelationInsert.getTableName());
		assertNotNull(snapshotPvInsert);
		assertEquals("snapshot_node_pv", snapshotPvInsert.getTableName());
		assertNotNull(jdbcTemplate);
		assertNotNull(objectMapper);
	}
}
