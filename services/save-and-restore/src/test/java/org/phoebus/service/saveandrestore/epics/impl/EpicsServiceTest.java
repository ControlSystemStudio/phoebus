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

package org.phoebus.service.saveandrestore.epics.impl;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.phoebus.service.saveandrestore.epics.IEpicsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.phoebus.service.saveandrestore.epics.config.EpicsServiceTestConfig;
import org.phoebus.service.saveandrestore.epics.exception.PVReadException;
import org.phoebus.applications.saveandrestore.model.ConfigPv;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({ @ContextConfiguration(classes = { EpicsServiceTestConfig.class }) })
@Ignore
public class EpicsServiceTest {

	@Autowired
	private IEpicsService epicsService;

	@Test
	public void testTakeSnapshotOk() throws PVReadException {
			
		ConfigPv configPv = ConfigPv.builder()
				.pvName("channelName")
				.build();
		
		epicsService.readPvs(Arrays.asList(configPv));
	}

	@Test
	public void testTakeSnapshotReadPVFailure() {
		ConfigPv configPv = ConfigPv.builder()
				.pvName("badChannelName")
				.build();
				
		epicsService.readPvs(Arrays.asList(configPv));

	}
	
	@Test
	public void testTakeSnapshotMultiplePVs() {
		ConfigPv configPv1 = ConfigPv.builder()
				.pvName("multi1")
				.build();
		
		ConfigPv configPv2 = ConfigPv.builder()
				.pvName("multi2")
				.build();
		
		ConfigPv configPv3 = ConfigPv.builder()
				.pvName("badChannelName")
				.build();
			
		epicsService.readPvs(Arrays.asList(configPv1, configPv2, configPv3));
	}
}
