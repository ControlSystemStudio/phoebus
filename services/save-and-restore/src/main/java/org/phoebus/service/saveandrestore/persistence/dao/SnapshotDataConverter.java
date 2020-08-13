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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.epics.util.array.*;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.vtype.*;
import org.phoebus.applications.saveandrestore.model.SnapshotItem;
import org.phoebus.service.saveandrestore.epics.exception.PVConversionException;
import org.phoebus.service.saveandrestore.model.internal.SnapshotPv;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Provides static methods to convert between {@link SnapshotPv}, which is the internal representation used to persist 
 * snapshot data, and the {@link VType} types which are the data representation provided by the general
 * purpose EPICS client. 
 * @author georgweiss
 * Created 28 Nov 2018
 */
public class SnapshotDataConverter {
	
	protected static final String SCALAR_AS_JSON = "[1]";
	
	private SnapshotDataConverter() {
		
	}
	
	public static SnapshotItem fromSnapshotPv(SnapshotPv snapshotPv, SnapshotPv readback) {
		if(snapshotPv == null) {
			return null;
		}
		
		SnapshotItem snapshotItem = SnapshotItem.builder()
				.configPv(snapshotPv.getConfigPv())
				.snapshotId(snapshotPv.getSnapshotId())
				.build();
	
		if(snapshotPv.getValue() != null) {
			snapshotItem.setValue(toVType(snapshotPv));
		}
		
		if(readback != null) {
			snapshotItem.setReadbackValue(toVType(readback));
		}
	
		return snapshotItem;
		
	}

	public static SnapshotPv fromVType(VType vType) {
			
		SnapshotPvDataType dataType = getDataType(vType);

		if(vType instanceof VNumber) {
			VNumber vNumber = (VNumber)vType;
			Alarm alarm = vNumber.getAlarm();
			Instant instant = vNumber.getTime().getTimestamp();
			return SnapshotPv.builder()
					.alarmSeverity(alarm.getSeverity())
					.alarmName(alarm.getName())
					.alarmStatus(alarm.getStatus())
					.time(instant.getEpochSecond())
					.timens(instant.getNano())
					.value(getScalarValueString(vNumber.getValue()))
					.dataType(dataType)
					.sizes(SCALAR_AS_JSON) // Hard coded as scalar is a 1x1 "matrix"
					.build();
		}
		else if(vType instanceof VNumberArray) {
			VNumberArray vNumberArray = (VNumberArray)vType;
			Alarm alarm = vNumberArray.getAlarm();
			Instant instant = vNumberArray.getTime().getTimestamp();
			return SnapshotPv.builder()
					.alarmSeverity(alarm.getSeverity())
					.alarmName(alarm.getName())
					.alarmStatus(alarm.getStatus())
					.time(instant.getEpochSecond())
					.timens(instant.getNano())
					.value(getNumberArrayValueString(vNumberArray))
					.dataType(dataType)
					.sizes(getDimensionString(vNumberArray))
					.build();
		}
		else if(vType instanceof VString){
			VString vString = (VString)vType;
			Alarm alarm = vString.getAlarm();
			Instant instant = vString.getTime().getTimestamp();
			return SnapshotPv.builder()
					.alarmSeverity(alarm.getSeverity())
					.alarmName(alarm.getName())
					.alarmStatus(alarm.getStatus())
					.time(instant.getEpochSecond())
					.timens(instant.getNano())
					.value(getScalarValueString(vString.getValue()))
					.dataType(dataType)
					.sizes(SCALAR_AS_JSON)
					.build();
		}
		else if(vType instanceof VStringArray){
			VStringArray vStringArray = (VStringArray) vType;
			Alarm alarm = vStringArray.getAlarm();
			Instant instant = vStringArray.getTime().getTimestamp();
			return SnapshotPv.builder()
					.alarmSeverity(alarm.getSeverity())
					.alarmName(alarm.getName())
					.alarmStatus(alarm.getStatus())
					.time(instant.getEpochSecond())
					.timens(instant.getNano())
					.value(getStringArrayValueString(vStringArray))
					.dataType(dataType)
					.sizes(getDimensionString(vStringArray))
					.build();
		}
		else if(vType instanceof VEnum){
			VEnum vEnum = (VEnum)vType;
			Alarm alarm = vEnum.getAlarm();
			Instant instant = vEnum.getTime().getTimestamp();
			return SnapshotPv.builder()
					.alarmSeverity(alarm.getSeverity())
					.alarmName(alarm.getName())
					.alarmStatus(alarm.getStatus())
					.time(instant.getEpochSecond())
					.timens(instant.getNano())
					.value(getEnumValueString(vEnum))
					.dataType(dataType)
					.sizes(SCALAR_AS_JSON)
					.build();
		}

		throw new PVConversionException(String.format("VType \"%s\" not supported", vType.getClass().getCanonicalName()));
	}
	
	public static VType toVType(SnapshotPv snapshotPv) {
		
		if(snapshotPv.getValue() == null) {
			return null;
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		boolean isScalar = SCALAR_AS_JSON.equals(snapshotPv.getSizes());
		ListInteger sizes = toSizes(snapshotPv);
		
		Alarm alarm = toAlarm(snapshotPv);
		Time time = toTime(snapshotPv);
		Display display = Display.none();
		
		try {
			switch(snapshotPv.getDataType()) {
				case BYTE:{
					byte[] values = objectMapper.readValue(snapshotPv.getValue(), byte[].class);
					if(isScalar) {
						return VByte.of(values[0], alarm, time, display);
					}
					else {
						return VByteArray.of(CollectionNumbers.toListByte(values), sizes, alarm, time, display);
					}
				}
				case UBYTE:{
					byte[] values = objectMapper.readValue(snapshotPv.getValue(), byte[].class);
					if(isScalar) {
						return VUByte.of(values[0], alarm, time, display);
					}
					else {
						return VByteArray.of(CollectionNumbers.toListUByte(values), sizes, alarm, time, display);
					}
				}
				case SHORT:{
					short[] values = objectMapper.readValue(snapshotPv.getValue(), short[].class);
					if(isScalar) {
						return VShort.of(values[0], alarm, time, display);
					}
					else {
						return VShortArray.of(CollectionNumbers.toListShort(values), sizes, alarm, time, display);
					}
				}
				case USHORT:{
					short[] values = objectMapper.readValue(snapshotPv.getValue(), short[].class);
					if(isScalar) {
						return VUShort.of(values[0], alarm, time, display);
					}
					else {
						return VUShortArray.of(CollectionNumbers.toListUShort(values), sizes, alarm, time, display);
					}
				}
				case INTEGER:{
					int[] values = objectMapper.readValue(snapshotPv.getValue(), int[].class);
					if(isScalar) {
						return VInt.of(values[0], alarm, time, display);
					}
					else {
						return VIntArray.of(CollectionNumbers.toListInt(values), sizes, alarm, time, display);
					}
				}
				case UINTEGER:{
					int[] values = objectMapper.readValue(snapshotPv.getValue(), int[].class);
					if(isScalar) {
						return VUInt.of(values[0], alarm, time, display);
					}
					else {
						return VUIntArray.of(CollectionNumbers.toListUInt(values), sizes, alarm, time, display);
					}
				}
				case LONG:{
					long[] values = objectMapper.readValue(snapshotPv.getValue(), long[].class);
					if(isScalar) {
						return VLong.of(values[0], alarm, time, display);
					}
					else {
						return VLongArray.of(CollectionNumbers.toListLong(values), sizes, alarm, time, display);
					}
				}
				case ULONG:{
					long[] values = objectMapper.readValue(snapshotPv.getValue(), long[].class);
					if(isScalar) {
						return VULong.of(values[0], alarm, time, display);
					}
					else {
						return VULongArray.of(CollectionNumbers.toListULong(values), sizes, alarm, time, display);
					}
				}
				case FLOAT:{
					float[] values = objectMapper.readValue(snapshotPv.getValue(), float[].class);
					if(isScalar) { 
						return VFloat.of(values[0], alarm, time, display);
					}
					else {
						return VFloatArray.of(CollectionNumbers.toListFloat(values), sizes, alarm, time, display);
					}
				}
				case DOUBLE:{
					double[] values = objectMapper.readValue(snapshotPv.getValue(), double[].class);
					if(isScalar) { 
						return VDouble.of(values[0], alarm, time, display);
					}
					else {
						return VDoubleArray.of(CollectionNumbers.toListDouble(values), sizes, alarm, time, display);
					}
				}
				case STRING:{
					String[] values = objectMapper.readValue(snapshotPv.getValue(), String[].class);
					if(isScalar) { 
						return VString.of(values[0], alarm, time);
					}
					else {
						return VStringArray.of(Arrays.asList(values), sizes, alarm, time);
					}
				}
				case ENUM:{
					Object[] values = objectMapper.readValue(snapshotPv.getValue(), Object[].class);

					if (values.length == 2) {
						int index = (int) values[0];
						List<String> choices = (List<String>) values[1];
						EnumDisplay enumDisplay = EnumDisplay.of(choices);

						if (isScalar) {
							return VEnum.of(index, enumDisplay, alarm, time);
						} else {
							throw new PVConversionException("VEnumArray not supported");
						}
					}
					// The following else if statement is for backward compatibility.
					else if (values.length == 1) {
						if (isScalar) {
							return VEnum.of(0, EnumDisplay.of((String) values[0]), alarm, time);
						} else {
							throw new PVConversionException("VEnumArray not supported");
						}
					}
					else {
						throw new PVConversionException("Wrong data size! VEnum DB data has been corrupted!");
					}
				}
			}
		} catch (Exception e) {
			throw new PVConversionException(String.format("Unable to convert to VType, cause: %s", e.getMessage()));
		} 
		throw new PVConversionException(String.format("Cannot convert to PVType from internal type %s", snapshotPv.getDataType()));
	}
	
	private static Alarm toAlarm(SnapshotPv snapshotPv) {
		return Alarm.of(snapshotPv.getAlarmSeverity(), snapshotPv.getAlarmStatus(), snapshotPv.getAlarmName());
	}
	
	private static Time toTime(SnapshotPv snapshotPv) {
		return Time.of(Instant.ofEpochSecond(snapshotPv.getTime(), snapshotPv.getTimens()));
	}
	
	
	protected static SnapshotPvDataType getDataType(VType vType) {
		if(vType instanceof VNumber) {
			VNumber vNumber = (VNumber)vType;
			Class<?> clazz = vNumber.getValue().getClass();
			if(clazz.equals(Byte.class)) {
				return SnapshotPvDataType.BYTE;
			}
			else if(clazz.equals(UByte.class)) {
				return SnapshotPvDataType.UBYTE;
			}
			else if(clazz.equals(Short.class)) {
				return SnapshotPvDataType.SHORT;
			}
			else if(clazz.equals(UShort.class)) {
				return SnapshotPvDataType.USHORT;
			}
			else if(clazz.equals(Integer.class)) {
				return SnapshotPvDataType.INTEGER;
			}
			else if(clazz.equals(UInteger.class)) {
				return SnapshotPvDataType.UINTEGER;
			}
			else if(clazz.equals(Long.class)) {
				return SnapshotPvDataType.LONG;
			}
			else if(clazz.equals(ULong.class)) {
				return SnapshotPvDataType.ULONG;
			}
			else if(clazz.equals(Float.class)) {
				return SnapshotPvDataType.FLOAT;
			}
			else if(clazz.equals(Double.class)){
				return SnapshotPvDataType.DOUBLE;
			}
			throw new PVConversionException("Data class " + vNumber.getValue().getClass().getCanonicalName() + " not supported");
		}
		else if(vType instanceof VNumberArray) {
			VNumberArray vNumberArray = (VNumberArray)vType;
			
			Class<?> clazz = vNumberArray.getData().getClass();
			if(clazz.equals(ArrayByte.class) || clazz.getSuperclass().equals(ListByte.class)) {
				return SnapshotPvDataType.BYTE;
			}
			else if(clazz.equals(ArrayUByte.class) || clazz.getSuperclass().equals(ListUByte.class)) {
				return SnapshotPvDataType.UBYTE;
			}
			else if(clazz.equals(ArrayShort.class) || clazz.getSuperclass().equals(ListShort.class)) {
				return SnapshotPvDataType.SHORT;
			}
			else if(clazz.equals(ArrayUShort.class) || clazz.getSuperclass().equals(ListUShort.class)) {
				return SnapshotPvDataType.USHORT;
			}
			else if(clazz.equals(ArrayInteger.class) || clazz.getSuperclass().equals(ListInteger.class)) {
				return SnapshotPvDataType.INTEGER;
			}
			else if(clazz.equals(ArrayUInteger.class) || clazz.getSuperclass().equals(ListUInteger.class)) {
				return SnapshotPvDataType.UINTEGER;
			}
			else if(clazz.equals(ArrayLong.class) || clazz.getSuperclass().equals(ListLong.class)) {
				return SnapshotPvDataType.LONG;
			}
			else if(clazz.equals(ArrayULong.class) || clazz.getSuperclass().equals(ListULong.class)) {
				return SnapshotPvDataType.ULONG;
			}
			else if(clazz.equals(ArrayFloat.class) || clazz.getSuperclass().equals(ListFloat.class)) {
				return SnapshotPvDataType.FLOAT;
			}
			else if(clazz.equals(ArrayDouble.class) || clazz.getSuperclass().equals(ListDouble.class)) {
				return SnapshotPvDataType.DOUBLE;
			}
			throw new PVConversionException("Data class " + vNumberArray.getData().getClass().getCanonicalName() + " not supported");
		}
		else if(vType instanceof VString) {
			return SnapshotPvDataType.STRING;
		}
		else if(vType instanceof VEnum){
			return SnapshotPvDataType.ENUM;
		}
		else if(vType instanceof VStringArray){
			return SnapshotPvDataType.STRING;
		}
		
		throw new PVConversionException(String.format("Unable to perform data conversion on type %s", vType.getClass().getCanonicalName()));
	}
	
	protected static String getScalarValueString(Object value) {
		ObjectMapper objectMapper = new ObjectMapper();
	
		Object[] valueArray = {value};
		
		try {
			return objectMapper.writeValueAsString(valueArray);
		} catch (JsonProcessingException e) {
			throw new PVConversionException(String.format("Unable to write scalar value \"%s\" as JSON string", value.toString()));
		}
	}

	protected static String getEnumValueString(VEnum value) {
		ObjectMapper objectMapper = new ObjectMapper();

		List<Object> valueList = new ArrayList<>();

		valueList.add(value.getIndex());
		valueList.add(value.getDisplay().getChoices());

		try {
			return objectMapper.writeValueAsString(valueList);
		} catch (JsonProcessingException e) {
			throw new PVConversionException("Unable to write VEnum values as JSON string");
		}
	}

	protected static String getStringArrayValueString(VStringArray vStringArray){
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.writeValueAsString(vStringArray.getData());
		} catch (JsonProcessingException e) {
			throw new PVConversionException("Unable to write string array values as JSON string");
		}
	}
	
	protected static String getNumberArrayValueString(VNumberArray vNumberArray) {
		
		List<Object> valueList = new ArrayList<>();
		
		if(vNumberArray instanceof VByteArray || 
				vNumberArray instanceof VUByteArray ||
				vNumberArray instanceof VShortArray ||
				vNumberArray instanceof VUShortArray ||
				vNumberArray instanceof VIntArray ||
				vNumberArray instanceof VUIntArray ||
				vNumberArray instanceof VLongArray ||
				vNumberArray instanceof VULongArray) {
			IteratorNumber iterator = vNumberArray.getData().iterator();
			while(iterator.hasNext()) {
				valueList.add(iterator.nextLong());
			}
		}
		else if(vNumberArray instanceof VFloatArray) {
			IteratorFloat iterator = ((VFloatArray)vNumberArray).getData().iterator();
			while(iterator.hasNext()) {
				valueList.add(iterator.nextFloat());
			}
		}
		else if(vNumberArray instanceof VDoubleArray) {
			IteratorDouble iterator = ((VDoubleArray)vNumberArray).getData().iterator();
			while(iterator.hasNext()) {
				valueList.add(iterator.nextDouble());
			}
		}
		else {
			throw new PVConversionException(String.format("Unable to create JSON string for array type %s", vNumberArray.getClass().getCanonicalName()));
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
	
		try {
			return objectMapper.writeValueAsString(valueList);
		} catch (JsonProcessingException e) {
			throw new PVConversionException("Unable to write array values as JSON string");
		}
	}
	
	protected static String getDimensionString(VNumberArray vNumberArray) {
		ListInteger sizes = vNumberArray.getSizes();
		
		List<Integer> sizesAsIntList = new ArrayList<>();
		for(int i = 0; i < sizes.size(); i++) {
			sizesAsIntList.add(sizes.getInt(i));
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			return objectMapper.writeValueAsString(sizesAsIntList);
		} catch (JsonProcessingException e) {
			throw new PVConversionException("Unable to write sizes of number array as JSON string");
		}
	}

	protected static String getDimensionString(VStringArray vStringArray) {
		ListInteger sizes = vStringArray.getSizes();

		List<Integer> sizesAsIntList = new ArrayList<>();
		for(int i = 0; i < sizes.size(); i++) {
			sizesAsIntList.add(sizes.getInt(i));
		}

		ObjectMapper objectMapper = new ObjectMapper();

		try {
			return objectMapper.writeValueAsString(sizesAsIntList);
		} catch (JsonProcessingException e) {
			throw new PVConversionException("Unable to write sizes of number array as JSON string");
		}
	}
	
	protected static ListInteger toSizes(SnapshotPv snapshotPv) {
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			int[] sizes = objectMapper.readValue(snapshotPv.getSizes(), int[].class);
			return CollectionNumbers.toListInt(sizes);
		} catch (Exception e) {
			throw new PVConversionException(String.format("Unable to convert string %s to int array", snapshotPv.getSizes()));
		}
		
	}
}
