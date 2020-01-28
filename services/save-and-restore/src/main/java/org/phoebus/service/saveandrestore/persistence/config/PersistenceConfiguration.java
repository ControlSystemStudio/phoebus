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

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
@PropertySource("classpath:/${dbengine}.properties")
public class PersistenceConfiguration {

	@Bean
	@Primary
	@ConfigurationProperties(prefix = "spring.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	@Bean
	public SimpleJdbcInsert configurationEntryInsert() {
		DataSource dataSource = dataSource();

		return new SimpleJdbcInsert(dataSource).withTableName("config_pv").usingGeneratedKeyColumns("id");
	}

	@Bean
	public SimpleJdbcInsert configurationEntryRelationInsert() {
		DataSource dataSource = dataSource();

		return new SimpleJdbcInsert(dataSource).withTableName("config_pv_relation");
	}

	@Bean
	public SimpleJdbcInsert snapshotPvInsert() {
		DataSource dataSource = dataSource();

		return new SimpleJdbcInsert(dataSource).withTableName("snapshot_node_pv");
	}

	@Bean
	public JdbcTemplate jdbcTemplate() {
		DataSource dataSource = dataSource();
		return new JdbcTemplate(dataSource);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	public SimpleJdbcInsert nodeInsert() {
		return new SimpleJdbcInsert(dataSource()).withTableName("node").usingGeneratedKeyColumns("id");
	}

	@Bean
	public SimpleJdbcInsert nodeClosureInsert() {
		return new SimpleJdbcInsert(dataSource()).withTableName("node_closure");
	}

	@Bean
	public SimpleJdbcInsert pvInsert() {
		return new SimpleJdbcInsert(dataSource()).withTableName("pv").usingGeneratedKeyColumns("id");
	}
}