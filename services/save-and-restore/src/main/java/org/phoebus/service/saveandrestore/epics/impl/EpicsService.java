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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.epics.gpclient.GPClient;
import org.epics.vtype.VType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.phoebus.service.saveandrestore.epics.IEpicsService;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

public class EpicsService implements IEpicsService {

	@Autowired
	private ExecutorService executorPool;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EpicsService.class);
	
	@Override
	public List<SnapshotItem> readPvs(List<ConfigPv> configPvs) {
		
		LOGGER.info("Reading {} PVs", configPvs.size());
		ExecutorCompletionService<SnapshotItem> ecs = new ExecutorCompletionService<>(executorPool);
		for (ConfigPv configPv : configPvs) {
			ecs.submit(new SnapshotPvCallable(configPv));
		}

		List<SnapshotItem> snapshotPvs = new ArrayList<>();
		for (int i = 0; i < configPvs.size(); ++i) {
			try {
				SnapshotItem item = ecs.take().get();
				if (item != null) {
					snapshotPvs.add(item);
				}
			} catch (Exception e) {
				LOGGER.error(String.format("Encountered exception when collecting PVs: %s", e.getMessage()));
			}
		}
				
		return snapshotPvs;
	}
	

	private class SnapshotPvCallable implements Callable<SnapshotItem> {

		private ConfigPv configPv;

		public SnapshotPvCallable(ConfigPv configPv) {
			this.configPv = configPv;
		}

		@Override
		public SnapshotItem call() {
				
			Future<VType> value = GPClient.readOnce(configPv.getPvName());
			VType pvValue;
			VType readbackPvValue = null;
			try {
				pvValue = value.get(5L, TimeUnit.SECONDS);
			} catch (Exception ex) {
				LOGGER.error(String.format("Read of PV %s has failed", configPv.getPvName()));
				return SnapshotItem.builder().configPv(configPv).build();
			}
			
			if(configPv.getReadbackPvName() != null) {
				value = GPClient.readOnce(configPv.getReadbackPvName());
				
				try {
					readbackPvValue = value.get(5L, TimeUnit.SECONDS);
				} catch (Exception e) {
					LOGGER.error(String.format("Read of read-back PV %s has failed", configPv.getReadbackPvName()));
				} 
			}
			
			return SnapshotItem.builder().configPv(configPv).value(pvValue).readbackValue(readbackPvValue).build();
		}
	}
}
