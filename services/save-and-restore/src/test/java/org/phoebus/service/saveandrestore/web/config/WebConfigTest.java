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

package org.phoebus.service.saveandrestore.web.config;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.phoebus.service.saveandrestore.persistence.config.PersistenceConfiguration;
import org.phoebus.service.saveandrestore.services.IServices;
import org.phoebus.service.saveandrestore.services.config.ServicesConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextHierarchy({ @ContextConfiguration(classes = {
		PersistenceConfiguration.class, 
		ServicesConfiguration.class, 
		WebConfiguration.class})})
@SuppressWarnings("unused")
public class WebConfigTest {
	
	@Autowired
	private IServices services;
	
	@BeforeAll
	public static void init() {
		System.setProperty("dbengine", "h2");
	}

	@Test
	public void testWbeConfig() {
		assertNotNull(services);
	}
}
