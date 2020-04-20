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

package org.phoebus.applications.saveandrestore.model.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.epics.util.array.CollectionNumbers;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VType;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;

/**
 * @author georgweiss Created 30 Nov 2018
 */
public class JsonSerializationTest {

	private VDouble vDouble;
	private VDoubleArray vDoubleArray;
	private ObjectMapper objectMapper;

	@Before
	public void init() {

		Alarm alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
		Time time = Time.of(Instant.ofEpochSecond(1000L, 7000L));
		vDouble = VDouble.of(new Double(7.7), alarm, time, Display.none());

		vDoubleArray = VDoubleArray.of(CollectionNumbers.toListDouble(1.1, 7.7), alarm, time, Display.none());

		objectMapper = new ObjectMapper();
	}

	@Test
	public void testSerializationAndDeserialzation() throws Exception {
		SnapshotItem snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a").build())
				.snapshotId(2)
				.value(vDouble)
				.readbackValue(vDouble)
				.build();
		
		// Should not throw any exceptions...
		String json = objectMapper.writeValueAsString(snapshotItem);
		// Try to deserialize...
		SnapshotItem item = objectMapper.readValue(json, SnapshotItem.class);
		VType vType = item.getValue();
		assertTrue(vType instanceof VDouble);
		assertEquals(7.7, ((VDouble)vType).getValue().doubleValue(), 0.01);
		vType = item.getReadbackValue();
		assertEquals(7.7, ((VDouble)vType).getValue().doubleValue(), 0.01);

		snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a")
				.build())
				.snapshotId(2)
				.value(vDoubleArray)
				.readbackValue(vDoubleArray)
				.build();
		json = objectMapper.writeValueAsString(snapshotItem);
		item = objectMapper.readValue(json, SnapshotItem.class);
		vType = item.getValue();
		assertTrue(vType instanceof VDoubleArray);
		VDoubleArray vDoubleArray = (VDoubleArray)vType;
		assertEquals(1.1, vDoubleArray.getData().getDouble(0), 0.01);
		assertTrue(item.getConfigPv().getPvName().equals("a"));
		
		item.getReadbackValue();
		assertTrue(vType instanceof VDoubleArray);
		vDoubleArray = (VDoubleArray)vType;
		assertEquals(1.1, vDoubleArray.getData().getDouble(0), 0.01);
		assertTrue(item.getConfigPv().getPvName().equals("a"));
		
	}

	@Test
	public void testNaN() throws Exception{
		SnapshotItem snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a").build())
				.snapshotId(2)
				.value(VDouble.of(Double.NaN, Alarm.none(), Time.now(), Display.none()))
				.build();
		String json = objectMapper.writeValueAsString(snapshotItem);
		SnapshotItem item = objectMapper.readValue(json, SnapshotItem.class);
		VDouble vType = (VDouble)item.getValue();
		assertEquals(vType.getValue(), Double.NaN, 0);
	}

	@Test
	public void testPositiveInfinity() throws Exception{
		SnapshotItem snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a").build())
				.snapshotId(2)
				.value(VDouble.of(Double.POSITIVE_INFINITY, Alarm.none(), Time.now(), Display.none()))
				.build();
		String json = objectMapper.writeValueAsString(snapshotItem);
		SnapshotItem item = objectMapper.readValue(json, SnapshotItem.class);
		VDouble vType = (VDouble)item.getValue();
		assertEquals(vType.getValue(), Double.POSITIVE_INFINITY, 0);
	}

	@Test
	public void testNegativeInfinity() throws Exception{
		SnapshotItem snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a").build())
				.snapshotId(2)
				.value(VDouble.of(Double.NEGATIVE_INFINITY, Alarm.none(), Time.now(), Display.none()))
				.build();
		String json = objectMapper.writeValueAsString(snapshotItem);
		SnapshotItem item = objectMapper.readValue(json, SnapshotItem.class);
		VDouble vType = (VDouble)item.getValue();
		assertEquals(vType.getValue(), Double.NEGATIVE_INFINITY, 0);
	}

	@Test
	public void testEnum() throws Exception{
		SnapshotItem snapshotItem = SnapshotItem.builder().configPv(ConfigPv.builder().pvName("a").build())
				.snapshotId(2)
				.value(VEnum.of(0, EnumDisplay.of("a", "b"), Alarm.none(), Time.now()))
				.build();
		String json = objectMapper.writeValueAsString(snapshotItem);
		SnapshotItem item = objectMapper.readValue(json, SnapshotItem.class);
		VEnum vType = (VEnum) item.getValue();
		assertEquals("a", vType.getValue());
	}
}
