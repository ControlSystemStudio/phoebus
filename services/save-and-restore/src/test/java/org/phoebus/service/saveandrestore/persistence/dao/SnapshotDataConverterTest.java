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

package org.phoebus.service.saveandrestore.persistence.dao;

import org.epics.util.array.*;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.vtype.*;
import org.epics.vtype.json.VTypeToJson;
import org.junit.Before;
import org.junit.Test;
import org.phoebus.applications.saveandrestore.model.ConfigPv;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.epics.exception.PVConversionException;
import org.phoebus.service.saveandrestore.model.internal.SnapshotPv;

import javax.json.Json;
import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.junit.Assert.*;

/**
 * @author georgweiss Created 28 Nov 2018
 */
public class SnapshotDataConverterTest {

	private Alarm alarm;
	private Time time;
	private Display display;

	@Before
	public void init() {
		time = Time.of(Instant.ofEpochSecond(1000, 7000));
		alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "name");
		display = Display.none();
	}

	@Test
	public void testFromVString(){
		VString vString = VString.of("someString", alarm, time);
		SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(vString);
		assertEquals("[\"someString\"]", snapshotPv.getValue());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(1000, snapshotPv.getTime());
		assertEquals(7000, snapshotPv.getTimens());
		assertEquals(SnapshotPvDataType.STRING, snapshotPv.getDataType());
	}

	@Test
	public void testFromVEnum(){
		VEnum vEnum = VEnum.of(1, EnumDisplay.of("a", "b", "c"), alarm, time);
		SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(vEnum);
		assertEquals("[1,[\"a\",\"b\",\"c\"]]", snapshotPv.getValue());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(1000, snapshotPv.getTime());
		assertEquals(7000, snapshotPv.getTimens());
		assertEquals(SnapshotPvDataType.ENUM, snapshotPv.getDataType());
	}

	@Test
	public void testFromVNumber() {

		VDouble vDouble = VDouble.of(7.7, alarm, time, Display.none());

		SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(vDouble);

		assertEquals("[7.7]", snapshotPv.getValue());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(1000, snapshotPv.getTime());
		assertEquals(7000, snapshotPv.getTimens());
		assertEquals(SnapshotPvDataType.DOUBLE, snapshotPv.getDataType());

		VByte vByte = VByte.of(new Byte((byte) 1), alarm, time, display);

		snapshotPv = SnapshotDataConverter.fromVType(vByte);

		assertEquals("[1]", snapshotPv.getValue());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(1000, snapshotPv.getTime());
		assertEquals(7000, snapshotPv.getTimens());
		assertEquals(SnapshotPvDataType.BYTE, snapshotPv.getDataType());

		VShort vShort = VShort.of(new Short((short) 1), alarm, time, display);

		snapshotPv = SnapshotDataConverter.fromVType(vShort);

		assertEquals("[1]", snapshotPv.getValue());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(1000, snapshotPv.getTime());
		assertEquals(7000, snapshotPv.getTimens());
		assertEquals(SnapshotPvDataType.SHORT, snapshotPv.getDataType());
	}

	@Test
	public void fromVNumberArray() {
		VIntArray vIntArray = VIntArray.of(CollectionNumbers.toListInt(1, 2, 3, 4, 5, 6),
				CollectionNumbers.toListInt(1, 2, 3), alarm, time, display);
		SnapshotPv snapshotPv = SnapshotDataConverter.fromVType(vIntArray);

		assertEquals(SnapshotPvDataType.INTEGER, snapshotPv.getDataType());
		assertEquals("name", snapshotPv.getAlarmName());
		assertEquals(AlarmSeverity.NONE, snapshotPv.getAlarmSeverity());
		assertEquals(AlarmStatus.NONE, snapshotPv.getAlarmStatus());
		assertEquals("[1,2,3]", snapshotPv.getSizes());
		assertEquals(1000L, snapshotPv.getTime());
		assertEquals(7000L, snapshotPv.getTimens());
		assertEquals("[1,2,3,4,5,6]", snapshotPv.getValue());
	}

	@Test
	public void testGetDataType() {

		VByte vByte = VByte.of(new Byte((byte) 1), alarm, time, display);
		assertEquals(SnapshotPvDataType.BYTE, SnapshotDataConverter.getDataType(vByte));

		VUByte vUByte = VUByte.of(new UByte((byte) 1), alarm, time, display);
		assertEquals(SnapshotPvDataType.UBYTE, SnapshotDataConverter.getDataType(vUByte));

		VShort vShort = VShort.of(new Short((short) 1), alarm, time, display);
		assertEquals(SnapshotPvDataType.SHORT, SnapshotDataConverter.getDataType(vShort));

		VUShort vUShort = VUShort.of(new UShort((short) 1), alarm, time, display);
		assertEquals(SnapshotPvDataType.USHORT, SnapshotDataConverter.getDataType(vUShort));

		VInt vInt = VInt.of(new Integer(1), alarm, time, display);
		assertEquals(SnapshotPvDataType.INTEGER, SnapshotDataConverter.getDataType(vInt));

		VUInt vUInt = VUInt.of(new UInteger(1), alarm, time, display);
		assertEquals(SnapshotPvDataType.UINTEGER, SnapshotDataConverter.getDataType(vUInt));

		VLong vLong = VLong.of(new Long(1), alarm, time, display);
		assertEquals(SnapshotPvDataType.LONG, SnapshotDataConverter.getDataType(vLong));

		VULong vULong = VULong.of(new ULong(1), alarm, time, display);
		assertEquals(SnapshotPvDataType.ULONG, SnapshotDataConverter.getDataType(vULong));

		VFloat vFloat = VFloat.of(new Float(1.1), alarm, time, display);
		assertEquals(SnapshotPvDataType.FLOAT, SnapshotDataConverter.getDataType(vFloat));

		VDouble vDouble = VDouble.of(new Double(1), alarm, time, display);
		assertEquals(SnapshotPvDataType.DOUBLE, SnapshotDataConverter.getDataType(vDouble));

		VString vString = VString.of("string", alarm, time);
		assertEquals(SnapshotPvDataType.STRING, SnapshotDataConverter.getDataType(vString));

		VEnum vEnum = VEnum.of(0, EnumDisplay.of("choice1"), alarm, time);
		assertEquals(SnapshotPvDataType.ENUM, SnapshotDataConverter.getDataType(vEnum));

		VByteArray vByteArray = VByteArray.of(new ArrayByte(CollectionNumbers.toListByte((byte) 1)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.BYTE, SnapshotDataConverter.getDataType(vByteArray));

		VNumberArray vNumberArray = VNumberArray.of(CollectionNumbers.toList(new byte[] { (byte) 1 }), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.BYTE, SnapshotDataConverter.getDataType(vNumberArray));

		ListByte listByte = new ListByte() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public byte getByte(int index) {
				return (byte) 1;
			}
		};

		vByteArray = VByteArray.of(listByte, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.BYTE, SnapshotDataConverter.getDataType(vByteArray));

		VUByteArray vUByteArray = VUByteArray.of(new ArrayUByte(CollectionNumbers.toListUByte((byte) 1)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.UBYTE, SnapshotDataConverter.getDataType(vUByteArray));

		ListUByte listUByte = new ListUByte() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public byte getByte(int index) {
				return (byte) 1;
			}
		};

		vUByteArray = VUByteArray.of(listUByte, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.UBYTE, SnapshotDataConverter.getDataType(vUByteArray));

		VShortArray vShortArray = VShortArray.of(new ArrayShort(CollectionNumbers.toListShort((short) 1)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.SHORT, SnapshotDataConverter.getDataType(vShortArray));

		vNumberArray = VNumberArray.of(CollectionNumbers.toList(new short[] { (short) 1 }), alarm, time, display);
		assertEquals(SnapshotPvDataType.SHORT, SnapshotDataConverter.getDataType(vNumberArray));

		ListShort listShort = new ListShort() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public short getShort(int index) {
				return (short) 1;
			}
		};

		vShortArray = VShortArray.of(listShort, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.SHORT, SnapshotDataConverter.getDataType(vShortArray));

		VUShortArray vUShortArray = VUShortArray.of(new ArrayUShort(CollectionNumbers.toListUShort((short) 1)), alarm,
				time, display);
		assertEquals(SnapshotPvDataType.USHORT, SnapshotDataConverter.getDataType(vUShortArray));

		ListUShort listUShort = new ListUShort() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public short getShort(int index) {
				return (short) 1;
			}
		};

		vUShortArray = VUShortArray.of(listUShort, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.USHORT, SnapshotDataConverter.getDataType(vUShortArray));

		VIntArray vIntArray = VIntArray.of(new ArrayInteger(CollectionNumbers.toListInt(1)), alarm, time, display);
		assertEquals(SnapshotPvDataType.INTEGER, SnapshotDataConverter.getDataType(vIntArray));

		vNumberArray = VNumberArray.of(CollectionNumbers.toList(new int[] { 1 }), alarm, time, display);
		assertEquals(SnapshotPvDataType.INTEGER, SnapshotDataConverter.getDataType(vNumberArray));

		ListInteger listInteger = new ListInteger() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public int getInt(int index) {
				return 1;
			}
		};

		vIntArray = VIntArray.of(listInteger, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.INTEGER, SnapshotDataConverter.getDataType(vIntArray));

		VUIntArray vUIntArray = VUIntArray.of(new ArrayUInteger(CollectionNumbers.toListUInt(1)), alarm, time, display);
		assertEquals(SnapshotPvDataType.UINTEGER, SnapshotDataConverter.getDataType(vUIntArray));

		ListUInteger listUInteger = new ListUInteger() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public int getInt(int index) {
				return 1;
			}
		};

		vUIntArray = VUIntArray.of(listUInteger, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.UINTEGER, SnapshotDataConverter.getDataType(vUIntArray));

		VLongArray vLongArray = VLongArray.of(new ArrayLong(CollectionNumbers.toListLong(1L)), alarm, time, display);
		assertEquals(SnapshotPvDataType.LONG, SnapshotDataConverter.getDataType(vLongArray));

		vNumberArray = VNumberArray.of(CollectionNumbers.toList(new long[] { 1 }), alarm, time, display);
		assertEquals(SnapshotPvDataType.LONG, SnapshotDataConverter.getDataType(vNumberArray));

		ListLong listLong = new ListLong() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public long getLong(int index) {
				return 1L;
			}
		};

		vLongArray = VLongArray.of(listLong, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.LONG, SnapshotDataConverter.getDataType(vLongArray));

		VULongArray vULongArray = VULongArray.of(new ArrayULong(CollectionNumbers.toListULong(1L)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.ULONG, SnapshotDataConverter.getDataType(vULongArray));

		ListULong listULong = new ListULong() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public long getLong(int index) {
				return 1L;
			}
		};

		vULongArray = VULongArray.of(listULong, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.ULONG, SnapshotDataConverter.getDataType(vULongArray));

		VFloatArray vFloatArray = VFloatArray.of(new ArrayFloat(CollectionNumbers.toListFloat(1.1f)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.FLOAT, SnapshotDataConverter.getDataType(vFloatArray));

		vNumberArray = VNumberArray.of(CollectionNumbers.toList(new float[] { 1.1f }), alarm, time, display);
		assertEquals(SnapshotPvDataType.FLOAT, SnapshotDataConverter.getDataType(vNumberArray));

		ListFloat listFloat = new ListFloat() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public float getFloat(int index) {
				return 1.1f;
			}
		};

		vFloatArray = VFloatArray.of(listFloat, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.FLOAT, SnapshotDataConverter.getDataType(vFloatArray));

		VDoubleArray vDoubleArray = VDoubleArray.of(new ArrayDouble(CollectionNumbers.toListDouble(1.1)), alarm, time,
				display);
		assertEquals(SnapshotPvDataType.DOUBLE, SnapshotDataConverter.getDataType(vDoubleArray));

		vNumberArray = VNumberArray.of(CollectionNumbers.toList(new double[] { 1.1 }), alarm, time, display);
		assertEquals(SnapshotPvDataType.DOUBLE, SnapshotDataConverter.getDataType(vNumberArray));

		ListDouble listDouble = new ListDouble() {

			@Override
			public int size() {
				return 1;
			}

			@Override
			public double getDouble(int index) {
				return 1.1;
			}
		};

		vDoubleArray = VDoubleArray.of(listDouble, alarm, time, Display.none());
		assertEquals(SnapshotPvDataType.DOUBLE, SnapshotDataConverter.getDataType(vDoubleArray));
	}

	@Test(expected = RuntimeException.class)
	public void testUnsupportedType() {

		VEnumArray vEnumArray = VEnumArray.of(ArrayInteger.of(1, 2, 3), EnumDisplay.of("a", "b", "c"), Alarm.none(), Time.now());
		SnapshotDataConverter.fromVType(vEnumArray);
	}

	@Test
	public void testGetScalarValueString() {

		assertEquals("[1]", SnapshotDataConverter.getScalarValueString(Integer.valueOf(1)));
		assertEquals("[1.1]", SnapshotDataConverter.getScalarValueString(Double.valueOf(1.1)));
		String string = SnapshotDataConverter.getScalarValueString("string");
		assertEquals("[\"string\"]", string);
	}

	@Test
	public void testGetArrayValueString() {

		VNumberArray vNumberArray = VByteArray.of(CollectionNumbers.toListByte((byte) 1, (byte) 2), alarm, time,
				display);
		assertEquals("[1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VUByteArray.of(CollectionNumbers.toListUByte((byte) 1, (byte) 2), alarm, time, display);
		assertEquals("[1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VShortArray.of(CollectionNumbers.toListShort((short) -1, (short) 2), alarm, time, display);
		assertEquals("[-1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VUShortArray.of(CollectionNumbers.toListUShort((short) 1, (short) 2), alarm, time, display);
		assertEquals("[1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VIntArray.of(CollectionNumbers.toListInt(-1, 2), alarm, time, display);
		assertEquals("[-1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VUIntArray.of(CollectionNumbers.toListUInt(1, 2), alarm, time, display);
		assertEquals("[1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VLongArray.of(CollectionNumbers.toListLong(-1L, 2L), alarm, time, display);
		assertEquals("[-1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VULongArray.of(CollectionNumbers.toListULong(1L, 2L), alarm, time, display);
		assertEquals("[1,2]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VFloatArray.of(CollectionNumbers.toListFloat(1.2f, 2.1f), alarm, time, display);
		assertEquals("[1.2,2.1]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

		vNumberArray = VDoubleArray.of(CollectionNumbers.toListDouble(1.2, 2.1), alarm, time, display);
		assertEquals("[1.2,2.1]", SnapshotDataConverter.getNumberArrayValueString(vNumberArray));

	}

	@Test
	public void testGetDimensionString() {
		VNumberArray vNumberArray = VIntArray.of(CollectionNumbers.toListInt(1, 2, 3, 4, 5, 6),
				CollectionNumbers.toListInt(1, 2, 3), alarm, time, display);
		vNumberArray.getSizes();
		assertEquals("[1,2,3]", SnapshotDataConverter.getDimensionString(vNumberArray));

		vNumberArray = VIntArray.of(CollectionNumbers.toListInt(1, 2, 3), CollectionNumbers.toListInt(1, 2), alarm,
				time, display);
		vNumberArray.getSizes();
		assertEquals("[1,2]", SnapshotDataConverter.getDimensionString(vNumberArray));

		vNumberArray = VIntArray.of(CollectionNumbers.toListInt(1), CollectionNumbers.toListInt(1), alarm, time,
				display);
		vNumberArray.getSizes();
		assertEquals("[1]", SnapshotDataConverter.getDimensionString(vNumberArray));
	}

	@Test
	public void testDesrializeSizes() {

		ListInteger sizes = SnapshotDataConverter.toSizes(SnapshotPv.builder().sizes("[1,2]").build());
		assertEquals(2, sizes.size());
		assertEquals(1, sizes.getInt(0));
	}

	@Test(expected = PVConversionException.class)
	public void testDeserializeBadSizes() {
		SnapshotDataConverter.toSizes(SnapshotPv.builder().sizes("[1,2").build());
	}

	@Test
	public void testToVType() {

		SnapshotPv snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.BYTE)
				.sizes(SnapshotDataConverter.SCALAR_AS_JSON).time(1000L).timens(7000).value("[1]").build();

		VByte vByte = (VByte) SnapshotDataConverter.toVType(snapshotPv);

		assertEquals(1, vByte.getValue().byteValue());
		assertEquals(1000L, vByte.getTime().getTimestamp().getEpochSecond());
		assertEquals(7000, vByte.getTime().getTimestamp().getNano());
		assertEquals(AlarmSeverity.NONE, vByte.getAlarm().getSeverity());
		assertEquals(AlarmStatus.NONE, vByte.getAlarm().getStatus());
		assertEquals("name", vByte.getAlarm().getName());

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.UBYTE)
				.sizes(SnapshotDataConverter.SCALAR_AS_JSON).time(1000L).timens(7000).value("[1]").build();

		VUByte vUByte = (VUByte) SnapshotDataConverter.toVType(snapshotPv);

		assertEquals(1, vUByte.getValue().byteValue());
		assertEquals(1000L, vUByte.getTime().getTimestamp().getEpochSecond());
		assertEquals(7000, vUByte.getTime().getTimestamp().getNano());
		assertEquals(AlarmSeverity.NONE, vUByte.getAlarm().getSeverity());
		assertEquals(AlarmStatus.NONE, vUByte.getAlarm().getStatus());
		assertEquals("name", vUByte.getAlarm().getName());

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.BYTE).sizes("[1,2,3]").time(1000L)
				.timens(7000).value("[1,2,3,4,5,6]").build();

		VByteArray vByteArray = (VByteArray) SnapshotDataConverter.toVType(snapshotPv);

		assertEquals(6, vByteArray.getData().size());

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.UBYTE).sizes("[1,2,3]").time(1000L)
				.timens(7000).value("[1,2,3,4,5,6]").build();

		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VUByteArray);

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.INTEGER)
				.sizes(SnapshotDataConverter.SCALAR_AS_JSON).time(1000L).timens(7000).value("[1]").build();

		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VInt);

		snapshotPv.setDataType(SnapshotPvDataType.UINTEGER);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VUInt);

		snapshotPv.setDataType(SnapshotPvDataType.SHORT);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VShort);

		snapshotPv.setDataType(SnapshotPvDataType.USHORT);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VUShort);

		snapshotPv.setDataType(SnapshotPvDataType.LONG);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VLong);

		snapshotPv.setDataType(SnapshotPvDataType.ULONG);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VULong);

		snapshotPv.setDataType(SnapshotPvDataType.FLOAT);
		snapshotPv.setValue("[1.1]");
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VFloat);

		snapshotPv.setDataType(SnapshotPvDataType.DOUBLE);
		snapshotPv.setValue("[1.1]");
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VDouble);

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.SHORT).sizes("[1,2,3]").time(1000L)
				.timens(7000).value("[1,2,3,4,5,6]").build();
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VShortArray);

		snapshotPv.setDataType(SnapshotPvDataType.USHORT);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VUShortArray);

		snapshotPv.setDataType(SnapshotPvDataType.INTEGER);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VIntArray);

		snapshotPv.setDataType(SnapshotPvDataType.UINTEGER);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VUIntArray);

		snapshotPv.setDataType(SnapshotPvDataType.LONG);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VLongArray);

		snapshotPv.setDataType(SnapshotPvDataType.ULONG);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VULongArray);

		snapshotPv.setDataType(SnapshotPvDataType.FLOAT);
		snapshotPv.setValue("[1.1, 2.2]");
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VFloatArray);

		snapshotPv.setDataType(SnapshotPvDataType.DOUBLE);
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VDoubleArray);

		snapshotPv.setDataType(SnapshotPvDataType.STRING);
		snapshotPv.setSizes(SnapshotDataConverter.SCALAR_AS_JSON);
		snapshotPv.setValue("[\"string\"]");
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VString);

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.STRING).sizes("[1,2,3]").time(1000L)
				.timens(7000).value("[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"]").build();
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VStringArray);

		snapshotPv = SnapshotPv.builder().alarmName("name").alarmStatus(AlarmStatus.NONE)
				.alarmSeverity(AlarmSeverity.NONE).dataType(SnapshotPvDataType.ENUM).sizes("[1]").time(1000L)
				.timens(7000).value("[1,[\"a\",\"b\",\"c\"]]").build();
		assertTrue(SnapshotDataConverter.toVType(snapshotPv) instanceof VEnum);
	}

	@Test
	public void jsonTest() throws Exception {
		VLongArray vIntArray = VLongArray.of(new ArrayLong(CollectionNumbers.toListLong(-1, 2, 3)), alarm, time,
				display);
		String json1 = org.epics.vtype.json.VTypeToJson.toJson(vIntArray).toString();

		VULongArray vULongArray = VULongArray.of(new ArrayULong(CollectionNumbers.toListULong(1, 2, 3)), alarm, time,
				display);

		VTypeToJson.toJson(vULongArray).toString();

		VLongArray deserialized1 = (VLongArray) VTypeToJson
				.toVType(Json.createReader(new ByteArrayInputStream(json1.getBytes())).readObject());

		assertEquals(-1, deserialized1.getData().getLong(0));
	}

	@Test
	public void testFromSnapshotPv() {

		SnapshotPv snapshotPv = SnapshotPv.builder().alarmName("name").alarmSeverity(AlarmSeverity.NONE).alarmStatus(AlarmStatus.NONE)
				.snapshotId(2).dataType(SnapshotPvDataType.LONG).time(1000L).timens(7000).value("[1]").sizes("[1]").configPv(ConfigPv.builder().id(1).build()).build();
		SnapshotPv readback = SnapshotPv.builder().alarmName("name").alarmSeverity(AlarmSeverity.NONE).alarmStatus(AlarmStatus.NONE)
				.snapshotId(2).dataType(SnapshotPvDataType.LONG).time(1000L).timens(7000).value("[1]").sizes("[1]").configPv(ConfigPv.builder().id(1).build()).build();
		SnapshotItem snapshotItem = SnapshotDataConverter.fromSnapshotPv(snapshotPv, readback);
		
		assertEquals(2, snapshotItem.getSnapshotId());
		assertEquals(1, snapshotItem.getConfigPv().getId());
		assertNotNull(snapshotItem.getReadbackValue());
		
		snapshotPv = SnapshotPv.builder().snapshotId(1).configPv(ConfigPv.builder().id(1).build()).build();
		
		snapshotItem = SnapshotDataConverter.fromSnapshotPv(snapshotPv, readback);
		
		assertNull(snapshotItem.getValue());
		
		snapshotItem = SnapshotDataConverter.fromSnapshotPv(snapshotPv, null);
		assertNull(snapshotItem.getReadbackValue());

	}

}
