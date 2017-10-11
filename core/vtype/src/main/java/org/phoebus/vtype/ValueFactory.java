/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.phoebus.util.array.ArrayByte;
import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.ListBoolean;
import org.phoebus.util.array.ListByte;
import org.phoebus.util.array.ListDouble;
import org.phoebus.util.array.ListFloat;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListLong;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.array.ListNumbers;
import org.phoebus.util.array.ListShort;
import org.phoebus.util.text.NumberFormats;
//import org.phoebus.vtype.table.ListNumberProvider;

/**
 * Factory class for all concrete implementation of the types.
 * <p>
 * The factory methods do not do anything in terms of defensive copy and
 * immutability to the objects, which they are passed as they are. It's the
 * client responsibility to prepare them appropriately, which is automatically
 * done anyway for all objects except collections.
 *
 * @author carcassi
 */
public class ValueFactory {

    /**
     * Creates a new VString.
     *
     * @param value the string value
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VString newVString(final String value, final Alarm alarm, final Time time) {
        return new IVString(value, alarm, time);
    }

    /**
     * Creates a new VBoolean.
     *
     * @param value the boolean value
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VBoolean newVBoolean(final boolean value, final Alarm alarm, final Time time) {
        return new IVBoolean(value, alarm, time);
    }


    /**
     * Creates a new VMultiDouble.
     *
     * @param values the values
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VMultiDouble newVMultiDouble(List<VDouble> values, final Alarm alarm, final Time time, final Display display) {
        return new IVMultiDouble(values, alarm, time, display);
    }

    /**
     * Creates a new VLong.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VLong newVLong(final Long value, final Alarm alarm, final Time time, final Display display) {
        return new IVLong(value, alarm, time, display);
    }

    /**
     * Creates a new VInt.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VInt newVInt(final Integer value, final Alarm alarm, final Time time, final Display display) {
        return new IVInt(value, alarm, time, display);
    }

    /**
     * Creates a new VShort.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VShort newVShort(final Short value, final Alarm alarm, final Time time, final Display display) {
        return new IVShort(value, alarm, time, display);
    }

    /**
     * Creates a new VByte.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VByte newVByte(final Byte value, final Alarm alarm, final Time time, final Display display) {
        return new IVByte(value, alarm, time, display);
    }

    /**
     * New alarm with the given severity and status.
     *
     * @param alarmSeverity the alarm severity
     * @param alarmName the alarm name
     * @return the new alarm
     */
    public static Alarm newAlarm(final AlarmSeverity alarmSeverity, final String alarmName) {
        return new Alarm() {

            @Override
            public AlarmSeverity getAlarmSeverity() {
                return alarmSeverity;
            }

            @Override
            public String getAlarmName() {
                return alarmName;
            }

            @Override
            public String toString() {
                return VTypeToString.alarmToString(this);
            }

        };
    }

    private static final Alarm alarmNone = newAlarm(AlarmSeverity.NONE, "NONE");
    private static final Display displayBoolean = newDisplay(0.0, 0.0, 0.0, "", NumberFormats.toStringFormat(),
            1.0, 1.0, 1.0, 0.0, 1.0);

    /**
     * No alarm.
     *
     * @return severity and status NONE
     */
    public static Alarm alarmNone() {
        return alarmNone;
    }

    /**
     * Alarm based on the value and the display ranges.
     *
     * @param value the value
     * @param display the display information
     * @return the new alarm
     */
    public static Alarm newAlarm(Number value, Display display) {
        // Calculate new AlarmSeverity, using display ranges
        AlarmSeverity severity = AlarmSeverity.NONE;
        String status = "NONE";
        if (value.doubleValue() <= display.getLowerAlarmLimit()) {
            status = "LOLO";
            severity = AlarmSeverity.MAJOR;
        } else if (value.doubleValue() >= display.getUpperAlarmLimit()) {
            status = "HIHI";
            severity = AlarmSeverity.MAJOR;
        } else if (value.doubleValue() <= display.getLowerWarningLimit()) {
            status = "LOW";
            severity = AlarmSeverity.MINOR;
        } else if (value.doubleValue() >= display.getUpperWarningLimit()) {
            status = "HIGH";
            severity = AlarmSeverity.MINOR;
        }

        return newAlarm(severity, status);
    }

    /**
     * Creates a new time.
     *
     * @param timestamp the timestamp
     * @param timeUserTag the user tag
     * @param timeValid whether the time is valid
     * @return the new time
     */
    public static Time newTime(final Instant timestamp, final Integer timeUserTag, final boolean timeValid) {
        return new Time() {

            @Override
            public Instant getTimestamp() {
                return timestamp;
            }

            @Override
            public Integer getTimeUserTag() {
                return timeUserTag;
            }

            @Override
            public boolean isTimeValid() {
                return timeValid;
            }
        };
    }

    /**
     * New time, with no user tag and valid data.
     *
     * @param timestamp the timestamp
     * @return the new time
     */
    public static Time newTime(final Instant timestamp) {
        return newTime(timestamp, null, true);
    }

    /**
     * New time with the current timestamp, no user tag and valid data.
     *
     * @return the new time
     */
    public static Time timeNow() {
        return newTime(Instant.now(), null, true);
    }

    /**
     * Creates a new display
     *
     * @param lowerDisplayLimit lower display limit
     * @param lowerAlarmLimit lower alarm limit
     * @param lowerWarningLimit lower warning limit
     * @param units the units
     * @param numberFormat the formatter
     * @param upperWarningLimit the upper warning limit
     * @param upperAlarmLimit the upper alarm limit
     * @param upperDisplayLimit the upper display limit
     * @param lowerCtrlLimit the lower control limit
     * @param upperCtrlLimit the upper control limit
     * @return the new display
     */
    public static Display newDisplay(final Double lowerDisplayLimit, final Double lowerAlarmLimit, final Double lowerWarningLimit,
            final String units, final NumberFormat numberFormat, final Double upperWarningLimit,
            final Double upperAlarmLimit, final Double upperDisplayLimit,
            final Double lowerCtrlLimit, final Double upperCtrlLimit) {
        return new Display() {
            @Override
            public Double getLowerCtrlLimit() {
                return lowerCtrlLimit;
            }

            @Override
            public Double getUpperCtrlLimit() {
                return upperCtrlLimit;
            }

            @Override
            public Double getLowerDisplayLimit() {
                return lowerDisplayLimit;
            }

            @Override
            public Double getLowerAlarmLimit() {
                return lowerAlarmLimit;
            }

            @Override
            public Double getLowerWarningLimit() {
                return lowerWarningLimit;
            }

            @Override
            public String getUnits() {
                return units;
            }

            @Override
            public NumberFormat getFormat() {
                return numberFormat;
            }

            @Override
            public Double getUpperWarningLimit() {
                return upperWarningLimit;
            }

            @Override
            public Double getUpperAlarmLimit() {
                return upperAlarmLimit;
            }

            @Override
            public Double getUpperDisplayLimit() {
                return upperDisplayLimit;
            }

        };
    }

    public static ArrayDimensionDisplay newDisplay(final ListNumber boundaries, final String unit) {
        return newDisplay(boundaries, false, unit);
    }

    public static ArrayDimensionDisplay newDisplay(final ListNumber boundaries, final boolean reversed, final String unit) {
        return new IArrayDimensionDisplay(boundaries, reversed, unit);
    }

//    public static ArrayDimensionDisplay newDisplay(final int size, final ListNumberProvider boundaryProvider, final boolean invert) {
//        return newDisplay(boundaryProvider.createListNumber(size + 1), invert, "");
//    }
//
//    public static ArrayDimensionDisplay newDisplay(final int size, final ListNumberProvider boundaryProvider) {
//        return newDisplay(boundaryProvider.createListNumber(size + 1), false, "");
//    }

    /**
     * Returns an array display where the index is used to calculate the
     * cell boundaries.
     *
     * @param nCells the number of cells along the direction
     * @return a new array display
     */
    public static ArrayDimensionDisplay newDisplay(int nCells) {
        final ListNumber boundaries = ListNumbers.linearList(0, 1, nCells + 1);
        return newDisplay(boundaries, "");
    }

    private static final Display displayNone = newDisplay(Double.NaN, Double.NaN,
            Double.NaN, "", NumberFormats.toStringFormat(), Double.NaN, Double.NaN,
            Double.NaN, Double.NaN, Double.NaN);

    /**
     * Empty display information.
     *
     * @return no display
     */
    public static Display displayNone() {
        return displayNone;
    }

    /**
     * Returns a display from 0 to 1, suitable for booleans.
     *
     * @return a display for boolean
     */
    public static Display displayBoolean() {
        return displayBoolean;
    }

    /**
     * Creates a new VNumber based on the type of the data
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new number
     */
    public static VNumber newVNumber(Number value, Alarm alarm, Time time, Display display){
        if (value instanceof Double) {
            return newVDouble((Double) value, alarm, time, display);
        } else if (value instanceof Float) {
            return newVFloat((Float) value, alarm, time, display);
        } else if (value instanceof Long) {
            return newVLong((Long) value, alarm, time, display);
        } else if (value instanceof Integer) {
            return newVInt((Integer) value, alarm, time, display);
        } else if (value instanceof Short) {
            return newVShort((Short) value, alarm, time, display);
        } else if (value instanceof Byte) {
            return newVByte((Byte) value, alarm, time, display);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new VDouble.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VDouble newVDouble(final Double value, final Alarm alarm, final Time time, final Display display) {
        return new IVDouble(value, alarm, time, display);
    }

    /**
     * Creates a new VDouble using the given value, time, display and
     * generating the alarm from the value and display information.
     *
     * @param value the new value
     * @param time the time
     * @param display the display information
     * @return the new value
     */
    public static VDouble newVDouble(Double value, Time time, Display display) {
        return newVDouble(value, newAlarm(value, display), time, display);
    }

    /**
     * Creates new immutable VDouble by using metadata from the old value,
     * now as timestamp and computing alarm from the metadata range.
     *
     * @param value new numeric value
     * @param display metadata
     * @return new value
     */
    public static VDouble newVDouble(Double value, Display display) {
        return newVDouble(value, timeNow(), display);
    }

    /**
     * Creates a new VDouble, no alarm, time now, no display.
     *
     * @param value the value
     * @return the new value
     */
    public static VDouble newVDouble(Double value) {
        return newVDouble(value, alarmNone(), timeNow(), displayNone());
    }

    /**
     * Creates a new VDouble, no alarm, no display.
     *
     * @param value the value
     * @param time the time
     * @return the new value
     */
    public static VDouble newVDouble(Double value, Time time) {
        return newVDouble(value, alarmNone(), time, displayNone());
    }

    /**
     * Creates a new VFloat.
     *
     * @param value the value
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VFloat newVFloat(final Float value, final Alarm alarm, final Time time, final Display display) {
        return new IVFloat(value, alarm, time, display);
    }

    /**
     * Create a new VEnum.
     *
     * @param index the index in the label array
     * @param labels the labels
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VEnum newVEnum(int index, List<String> labels, Alarm alarm, Time time) {
        return new IVEnum(index, labels, alarm, time);
    }

    /**
     * Creates a new VStatistics.
     *
     * @param average average
     * @param stdDev standard deviation
     * @param min minimum
     * @param max maximum
     * @param nSamples number of samples
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VStatistics newVStatistics(final double average, final double stdDev,
            final double min, final double max, final int nSamples, final Alarm alarm,
            final Time time, final Display display) {
        return new IVStatistics(average, stdDev, min, max, nSamples,
                alarm, time, display);
    }

    /**
     * Creates a new VNumberArray based on the type of the data.
     *
     * @param data the array data
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return a new value
     */
    public static VNumberArray newVNumberArray(final ListNumber data, final Alarm alarm, final Time time, final Display display){
        ListInt sizes = new ArrayInt(data.size());
        List<ArrayDimensionDisplay> dimensionDisplay = ValueUtil.defaultArrayDisplay(sizes);
        return newVNumberArray(data, sizes, dimensionDisplay, alarm, time, display);
    }

    /**
     * Creates a new VNumberArray based on the type of the data.
     *
     * @param data the array data
     * @param sizes the array shape
     * @param dimensionDisplay the array axis display information
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return a new value
     */
    public static VNumberArray newVNumberArray(final ListNumber data, final ListInt sizes, final List<ArrayDimensionDisplay> dimensionDisplay,
            final Alarm alarm, final Time time, final Display display){
        if (data instanceof ListDouble){
            return new IVDoubleArray((ListDouble) data, sizes, dimensionDisplay, alarm, time, display);
        } else if (data instanceof ListFloat){
            return new IVFloatArray((ListFloat) data, sizes, dimensionDisplay, alarm, time, display);
        } else if (data instanceof ListLong){
            return new IVLongArray((ListLong) data, sizes, dimensionDisplay, alarm, time, display);
        } else if (data instanceof ListInt){
            return new IVIntArray((ListInt) data, sizes, dimensionDisplay, alarm, time, display);
        } else if (data instanceof ListByte){
            return new IVByteArray((ListByte) data, sizes, dimensionDisplay, alarm, time, display);
        } else if (data instanceof ListShort){
            return new IVShortArray((ListShort) data, sizes, dimensionDisplay, alarm, time, display);
        }
        throw new UnsupportedOperationException("Data is of an unsupported type (" + data.getClass() + ")");
    }

    /**
     * Constructs and nd array with the data, time and alarm in the first array and the given
     * dimension information.
     *
     * @param data the array with the data
     * @param dimensions the dimension information
     * @return a new array
     */
    public static VNumberArray ndArray(VNumberArray data, ArrayDimensionDisplay... dimensions) {
        int[] sizes = new int[dimensions.length];
        List<ArrayDimensionDisplay> displays = new ArrayList<>();
        for (int i = 0; i < dimensions.length; i++) {
            ArrayDimensionDisplay dimensionInfo = dimensions[i];
            sizes[i] = dimensionInfo.getCellBoundaries().size() - 1;
            displays.add(dimensionInfo);
        }
        return ValueFactory.newVNumberArray(data.getData(), new ArrayInt(sizes), displays, data, data, data);
    }

    /**
     * Creates a new VDoubleArray.
     *
     * @param data array data
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VDoubleArray newVDoubleArray(ListDouble data, Alarm alarm, Time time, Display display) {
        return new IVDoubleArray(data, new ArrayInt(data.size()), alarm,
                time, display);
    }

    /**
     * Creates a new VFloatArray.
     *
     * @param data array data
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VFloatArray newVFloatArray(ListFloat data, Alarm alarm, Time time, Display display) {
        return new IVFloatArray(data, new ArrayInt(data.size()), alarm,
                time, display);
    }

    
    /**
     * Creates a new VImage given the data and the size.
     *
     * @param height the height
     * @param width the width
     * @param data the data
     * @return a new object
     */
    public static VImage newVImage(int height, int width, byte[] data) {
        return newVImage(height, width, data, alarmNone(), timeNow());
    }

    /**
     * Creates a new VImage given the data and the size.
     *
     * @param height the height
     * @param width the width
     * @param data the data
     * @param alarm the alarm
     * @param time the time
     * @return a new object
     */
    public static VImage newVImage(int height, int width, byte[] data, Alarm alarm, Time time) {
        return new IVImage(height, width, new ArrayByte(data), VImageDataType.pvByte, VImageType.TYPE_3BYTE_BGR, alarm,
                time);
    }
    
    /**
     * Creates a new VImage of type TYPE_3BYTE_BGR given the data and the size.
     *
     * @param height the height
     * @param width the width
     * @param data the data {@link ListNumber}
     * @param imageDataType {@link VImageDataType}
     * @param alarm the alarm
     * @param time the time
     * @return a new object
     */
    public static VImage newVImage(int height, int width, final ListNumber data, VImageDataType imageDataType, Alarm alarm, Time time) {
        return new IVImage(height, width, data, imageDataType, VImageType.TYPE_3BYTE_BGR, alarm, time);
    }

    /**
     * Creates a new VLongArray.
     *
     * @param values array values
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VLongArray newVLongArray(final ListLong values, Alarm alarm, Time time, Display display) {
        ListInt sizes = new ArrayInt(values.size());
        return new IVLongArray(values, sizes, ValueUtil.defaultArrayDisplay(sizes), alarm, time, display);
    }

    /**
     * Creates a new VIntArray.
     *
     * @param values array values
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VIntArray newVIntArray(final ListInt values, Alarm alarm, Time time, Display display) {
        return new IVIntArray(values, new ArrayInt(values.size()), alarm, time, display);
    }

    /**
     * Creates a new VShortArray.
     *
     * @param values array values
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VShortArray newVShortArray(final ListShort values, Alarm alarm, Time time, Display display) {
        ListInt sizes = new ArrayInt(values.size());
        return new IVShortArray(values, sizes, ValueUtil.defaultArrayDisplay(sizes), alarm, time, display);
    }
    /**
     * Creates a new VByteArray.
     *
     * @param values array values
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new value
     */
    public static VByteArray newVByteArray(final ListByte values, Alarm alarm, Time time, Display display) {
        ListInt sizes = new ArrayInt(values.size());
        return new IVByteArray(values, sizes, ValueUtil.defaultArrayDisplay(sizes), alarm, time, display);
    }

    /**
     * Create a new VEnumArray.
     *
     * @param indexes the indexes in the label array
     * @param labels the labels
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VEnumArray newVEnumArray(ListInt indexes, List<String> labels, Alarm alarm, Time time) {
        return new IVEnumArray(indexes, labels, new ArrayInt(indexes.size()), alarm, time);
    }

    /**
     * Creates a new VBooleanArray.
     *
     * @param data the strings
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VBooleanArray newVBooleanArray(ListBoolean data, Alarm alarm, Time time) {
        return new IVBooleanArray(data, new ArrayInt(data.size()), alarm, time);
    }

    /**
     * Creates a new VStringArray.
     *
     * @param data the strings
     * @param alarm the alarm
     * @param time the time
     * @return the new value
     */
    public static VStringArray newVStringArray(List<String> data, Alarm alarm, Time time) {
        return new IVStringArray(data, new ArrayInt(data.size()), alarm, time);
    }

    /**
     * Creates a new VTable - this method is provisional and will change in the future.
     *
     * @param types the types for each column
     * @param names the names for each column
     * @param values the values for each column
     * @return the new value
     */
    public static VTable newVTable(List<Class<?>> types, List<String> names, List<Object> values) {
        //TODO: should check the types
        return new IVTable(types, names, values);
    }

    /**
     * Takes a java objects and wraps it into a vType. All numbers are wrapped
     * as VDouble. String is wrapped as VString. double[] and ListDouble are wrapped as
     * VDoubleArray. A List of String is wrapped to a VStringArray. Alarms
     * are alarmNone(), time are timeNow() and display are displayNone();
     *
     * @param value the value to wrap
     * @return the wrapped value
     * @deprecated use {@link #toVType(java.lang.Object) }
     */
    @Deprecated
    public static VType wrapValue(Object value) {
        return wrapValue(value, alarmNone());
    }

    /**
     * Takes a java objects and wraps it into a vType. All numbers are wrapped
     * as VDouble. String is wrapped as VString. double[] and ListDouble are wrapped as
     * VDoubleArray. A List of String is wrapped to a VStringArray. Alarms
     * are alarm, time are timeNow() and display are displayNone();
     *
     * @param value the value to wrap
     * @param alarm the alarm for the value
     * @return the wrapped value
     * @deprecated use {@link #toVType(java.lang.Object, org.diirt.vtype.Alarm, org.diirt.vtype.Time, org.diirt.vtype.Display) }
     */
    @Deprecated
    public static VType wrapValue(Object value, Alarm alarm) {
        if (value instanceof Number) {
            // Special support for numbers
            return newVDouble(((Number) value).doubleValue(), alarm, timeNow(),
                    displayNone());
        } else if (value instanceof String) {
            // Special support for strings
            return newVString(((String) value),
                    alarm, timeNow());
        } else if (value instanceof double[]) {
            return newVDoubleArray(new ArrayDouble((double[]) value), alarm, timeNow(), displayNone());
        } else if (value instanceof ListDouble) {
            return newVDoubleArray((ListDouble) value, alarm, timeNow(), displayNone());
        } else if (value instanceof List) {
            boolean matches = true;
            List list = (List) value;
            for (Object object : list) {
                if (!(object instanceof String)) {
                    matches = false;
                }
            }
            if (matches) {
                @SuppressWarnings("unchecked")
                List<String> newList = (List<String>) list;
                return newVStringArray(Collections.unmodifiableList(newList), alarm, timeNow());
            } else {
                throw new UnsupportedOperationException("Type " + value.getClass().getName() + " contains non Strings");
            }
        } else {
            // TODO: need to implement all the other arrays
            throw new UnsupportedOperationException("Type " + value.getClass().getName() + "  is not yet supported");
        }
    }

    /**
     * Converts a standard java type to VTypes. Returns null if no conversion
     * is possible. Calls {@link #toVType(java.lang.Object, org.diirt.vtype.Alarm, org.diirt.vtype.Time, org.diirt.vtype.Display)}
     * with no alarm, time now and no display.
     *
     * @param javaObject the value to wrap
     * @return the new VType value
     */
    public static VType toVType(Object javaObject) {
        return toVType(javaObject, alarmNone(), timeNow(), displayNone());
    }

    /**
     * Converts a standard java type to VTypes. Returns null if no conversion
     * is possible.
     * <p>
     * Types are converted as follow:
     * <ul>
     *   <li>Boolean -&gt; VBoolean</li>
     *   <li>Number -&gt; corresponding VNumber</li>
     *   <li>String -&gt; VString</li>
     *   <li>number array -&gt; corresponding VNumberArray</li>
     *   <li>ListNumber -&gt; corresponding VNumberArray</li>
     *   <li>List -&gt; if all elements are String, VStringArray</li>
     * </ul>
     *
     * @param javaObject the value to wrap
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new VType value
     */
    public static VType toVType(Object javaObject, Alarm alarm, Time time, Display display) {
        if (javaObject instanceof Number) {
            return ValueFactory.newVNumber((Number) javaObject, alarm, time, display);
        } else if (javaObject instanceof String) {
            return newVString((String) javaObject, alarm, time);
        } else if (javaObject instanceof Boolean) {
            return newVBoolean((Boolean) javaObject, alarm, time);
        } else if (javaObject instanceof byte[]
                || javaObject instanceof short[]
                || javaObject instanceof int[]
                || javaObject instanceof long[]
                || javaObject instanceof float[]
                || javaObject instanceof double[]) {
            return newVNumberArray(ListNumbers.toListNumber(javaObject), alarm, time, display);
        } else if (javaObject instanceof ListNumber) {
            return newVNumberArray((ListNumber) javaObject, alarm, time, display);
        } else if (javaObject instanceof String[]) {
            return newVStringArray(Arrays.asList((String[]) javaObject), alarm, time);
        } else if (javaObject instanceof List) {
            boolean matches = true;
            List list = (List) javaObject;
            for (Object object : list) {
                if (!(object instanceof String)) {
                    matches = false;
                }
            }
            if (matches) {
                @SuppressWarnings("unchecked")
                List<String> newList = (List<String>) list;
                return newVStringArray(Collections.unmodifiableList(newList), alarm, time);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * As {@link #toVType(java.lang.Object)} but throws an exception
     * if conversion not possible.
     *
     * @param javaObject the value to wrap
     * @return the new VType value
     */
    public static VType toVTypeChecked(Object javaObject) {
        VType value = toVType(javaObject);
        if (value == null) {
            throw new RuntimeException("Value " + value + " cannot be converted to VType.");
        }
        return value;
    }

    /**
     * As {@link #toVType(java.lang.Object, org.diirt.vtype.Alarm, org.diirt.vtype.Time, org.diirt.vtype.Display)} but throws an exception
     * if conversion not possible.
     *
     * @param javaObject the value to wrap
     * @param alarm the alarm
     * @param time the time
     * @param display the display
     * @return the new VType value
     */
    public static VType toVTypeChecked(Object javaObject, Alarm alarm, Time time, Display display) {
        VType value = toVType(javaObject, alarm, time, display);
        if (value == null) {
            throw new RuntimeException("Value " + value + " cannot be converted to VType.");
        }
        return value;
    }
}
