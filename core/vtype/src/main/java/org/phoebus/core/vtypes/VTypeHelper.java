/*******************************************************************************
 * Copyright (c) 2019-2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.core.vtypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.epics.util.array.*;
import org.epics.pva.data.*;
import org.epics.pva.data.nt.PVATable;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Array;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VStatistics;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShortArray;

public class VTypeHelper {
    /**
     * Read number from a {@link VType}
     *
     * @param value Value
     * @return double or NaN
     */
    final public static double toDouble(final VType value) {
        return toDouble(value, 0);
    }


    /**
     * Get VType as double[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return double[]
     */
    public static double[] toDoubles(final VType value) {
        final double[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new double[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getDouble(i);
            }
        } else
            array = new double[0];
        return array;
    }

    /**
     * Get VType as int[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return int[]
     */
    public static int[] toIntegers(final VType value) {
        final int[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new int[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getInt(i);
            }
        } else
            array = new int[0];
        return array;
    }

    /**
     * Get VType as long[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return int[]
     */
    public static long[] toLongs(final VType value) {
        final long[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new long[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getLong(i);
            }
        } else
            array = new long[0];
        return array;
    }

    /**
     * Get VType as short[]; empty array if not possible.
     * Do not be mislead by the method name, this is season-independent.
     *
     * @param value {@link VType}
     * @return short[]
     */
    public static short[] toShorts(final VType value) {
        final short[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new short[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getShort(i);
            }
        } else
            array = new short[0];
        return array;
    }

    /**
     * Get VType as float[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return float[]
     */
    public static float[] toFloats(final VType value) {
        final float[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new float[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getFloat(i);
            }
        } else
            array = new float[0];
        return array;
    }

    /**
     * Get VType as byte[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return byte[]
     */
    public static byte[] toBytes(final VType value) {
        final byte[] array;
        if (value instanceof VNumberArray) {
            final ListNumber list = ((VNumberArray) value).getData();
            array = new byte[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getByte(i);
            }
        } else
            array = new byte[0];
        return array;
    }

    /**
     * Get VType as boolean[]; empty array if not possible
     *
     * @param value {@link VType}
     * @return boolean[]
     */
    public static boolean[] toBooleans(final VType value) {
        final boolean[] array;
        if (value instanceof VBooleanArray) {
            final ListBoolean list = ((VBooleanArray) value).getData();
            array = new boolean[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.getBoolean(i);
            }
        } else
            array = new boolean[0];
        return array;
    }

    /**
     * @param value {@link VType}
     * @return Value as String
     */
    public static String toString(final VType value) {
        if (value == null) {
            return "null";
        }
        if (isDisconnected(value)) {
            return null;
        }
        if (value instanceof VNumber) {
            return ((VNumber) value).getValue().toString();
        }
        if (value instanceof VEnum) {
            return ((VEnum) value).getValue();
        }
        if (value instanceof VString) {
            return ((VString) value).getValue();
        }
        // Else: Hope that value somehow represents itself
        return value.toString();
    }

    /**
     * Read number by array index from array {@link VType}
     *
     * @param value Value
     * @param index Array index, must be >= 0 for array types.
     * @return The double value at the specified index, or NaN if the index is invalid. If the value is not of
     * array type, {@link #toDouble(VType)} is called;
     */
    public static double toDouble(final VType value, final int index) {
        if (value instanceof VNumber) {
            return ((VNumber) value).getValue().doubleValue();
        }
        if (value instanceof VString) {
            try {
                return Double.parseDouble(((VString) value).getValue());
            } catch (NumberFormatException ex) {
                // Ignore
                return Double.NaN;
            }
        }
        if (value instanceof VBoolean) {
            return ((VBoolean) value).getValue() ? 1.0 : 0.0;
        }
        if (value instanceof VEnum) {
            return ((VEnum) value).getIndex();
        }
        if (value instanceof VStatistics) {
            return ((VStatistics) value).getAverage();
        }
        if (value instanceof VNumberArray && index >= 0) {
            final ListNumber data = ((VNumberArray) value).getData();
            if (index < data.size()) {
                return data.getDouble(index);
            }
        }
        if (value instanceof VEnumArray && index >= 0) {
            final ListNumber data = ((VEnumArray) value).getIndexes();
            if (index < data.size()) {
                return data.getDouble(index);
            }
        }
        return Double.NaN;
    }

    /**
     * @param value {@link VType}
     * @return <code>true</code> if value is a numeric array
     */
    public static boolean isNumericArray(final VType value) {
        return value instanceof VNumberArray ||
                value instanceof VEnumArray;
    }

    /**
     * @param value {@link VType}
     * @return Array size. 0 for scalar.
     */
    public static int getArraySize(final VType value) {
        final ListInteger sizes;
        if (value instanceof VNumberArray) {
            sizes = ((VNumberArray) value).getSizes();
        } else if (value instanceof VEnumArray) {
            sizes = ((VEnumArray) value).getSizes();
        } else if (value instanceof VStringArray) {
            sizes = ((VStringArray) value).getSizes();
        } else {
            return 0;
        }
        return sizes.size() > 0 ? sizes.getInt(0) : 0;
    }

    /**
     * @param a {@link VType}
     * @param b {@link VType}
     * @return Highest alarm of the two values
     */
    public static Alarm highestAlarmOf(final VType a, VType b) {
        return Alarm.highestAlarmOf(java.util.List.of(a, b), false);
    }

    /**
     * @param a {@link VType}
     * @param b {@link VType}
     * @return Latest time stamp of the two values
     */
    public static Time lastestTimeOf(final VType a, final VType b) {
        final Time ta = Time.timeOf(a);
        final Time tb = Time.timeOf(b);
        if (ta.getTimestamp().isAfter(tb.getTimestamp())) {
            return ta;
        }
        return tb;
    }

    /**
     * Decode a {@link VType}'s time stamp
     *
     * @param value Value to decode
     * @return {@link Instant}
     */
    final public static Instant getTimestamp(final VType value) {
        final Time time = Time.timeOf(value);
        if (time != null && time.isValid()) {
            return time.getTimestamp();
        }
        return Instant.now();
    }

    /**
     * @return Copy of given value with updated timestamp,
     * or <code>null</code> if value is not handled
     */
    public static VType transformTimestamp(final VType value,
                                           final Instant time) {
        if (value instanceof VDouble) {
            final VDouble number = (VDouble) value;
            return VDouble.of(number.getValue().doubleValue(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VNumber) {
            final VNumber number = (VNumber) value;
            return VInt.of(number.getValue().intValue(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VString) {
            final VString string = (VString) value;
            return VString.of(string.getValue(), string.getAlarm(), Time.of(time));
        }
        if (value instanceof VDoubleArray) {
            final VDoubleArray number = (VDoubleArray) value;
            return VDoubleArray.of(number.getData(), number.getAlarm(), Time.of(time), number.getDisplay());
        }
        if (value instanceof VEnum) {
            final VEnum labelled = (VEnum) value;
            return VEnum.of(labelled.getIndex(), labelled.getDisplay(), labelled.getAlarm(), Time.of(time));
        }
        return null;
    }

    /**
     * @return Copy of given value with timestamp set to 'now',
     * or <code>null</code> if value is not handled
     */
    public static VType transformTimestampToNow(final VType value) {
        return transformTimestamp(value, Instant.now());
    }

    public static boolean isDisconnected(final VType value) {
        if (value == null) {
            return true;
        }

        // VTable does not implement alarm,
        // but receiving a table means we're not disconnected
        if (value instanceof VTable) {
            return false;
        }
        final Alarm alarm = Alarm.alarmOf(value);
        return Alarm.disconnected().equals(alarm);
    }

    public static AlarmSeverity getSeverity(final VType value) {
        final Alarm alarm = Alarm.alarmOf(value);
        if (isDisconnected(value))
            return AlarmSeverity.UNDEFINED;
        return alarm.getSeverity();
    }

    /**
     * Formats an {@link Array}, optionally limiting the number of elements included in the
     * output string. Useful if the array data can be very large.
     * @param array The raw data, must not be <code>null</code>.
     * @param maxElements The maximum number of elements to include in the output string. If set to zero, or if the
     *                    specified array has zero elements,
     *                    only the data type string (VIntArray, VStringArray...) is returned. If set to a negative
     *                    number, the formatting will use the <code>toString()</code> method of the array type
     *                    to format the data elements.
     * @return A string prefixed with the data type (VIntArray, VStringArray...), followed by element data.
     */
    public static String formatArray(Array array, int maxElements) {
        StringBuilder builder = new StringBuilder();
        builder.append(VType.typeOf(array).getSimpleName());
        if (maxElements < 0) {
            builder.append(array.getData().toString());
            return builder.toString();
        } else if (maxElements == 0) {
            return builder.toString();
        }
        ListInteger sizes = array.getSizes();
        int sizesSize = sizes.size();
        int totalElements = 1;
        for (int i = 0; i < sizesSize; i++) {
            totalElements *= sizes.getInt(i);
        }
        if (totalElements == 0) { // Zero elements in array
            return builder.toString();
        }
        int numberOfElementsToFormat = Math.min(totalElements, maxElements);
        builder.append("[");
        if ((array instanceof VIntArray) || (array instanceof VUIntArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getInt(i)).append(", ");
            }
            builder.append(listNumber.getInt(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VLongArray) || (array instanceof VULongArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getLong(i)).append(", ");
            }
            builder.append(listNumber.getLong(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VShortArray) || (array instanceof VUShortArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getShort(i)).append(", ");
            }
            builder.append(listNumber.getShort(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VByteArray) || (array instanceof VUByteArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getByte(i)).append(", ");
            }
            builder.append(listNumber.getByte(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VDoubleArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getDouble(i)).append(", ");
            }
            builder.append(listNumber.getDouble(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VFloatArray)) {
            VNumberArray numberArray = (VNumberArray) array;
            ListNumber listNumber = numberArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listNumber.getFloat(i)).append(", ");
            }
            builder.append(listNumber.getFloat(numberOfElementsToFormat - 1));
        }
        else if ((array instanceof VBooleanArray)) {
            VBooleanArray booleanArray = (VBooleanArray) array;
            ListBoolean listBoolean = booleanArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listBoolean.getBoolean(i)).append(", ");
            }
            builder.append(listBoolean.getBoolean(numberOfElementsToFormat - 1));
        }
        else if (array instanceof VStringArray) {
            VStringArray stringArray = (VStringArray) array;
            List<String> listString = stringArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listString.get(i)).append(", ");
            }
            builder.append(listString.get(numberOfElementsToFormat - 1));
        }
        else if (array instanceof VEnumArray) {
            VEnumArray enumArray = (VEnumArray) array;
            List<String> listString = enumArray.getData();
            for (int i = 0; i < numberOfElementsToFormat - 1; i++) {
                builder.append(listString.get(i)).append(", ");
            }
            builder.append(listString.get(numberOfElementsToFormat - 1));
        }

        if (numberOfElementsToFormat < totalElements) {
            builder.append(",...");
        } else {
            builder.append("]");
        }
        return builder.toString();
    }

    /**
     * Read a boolean from a {@link VType}
     * This helper method accepts VString and VNumber and tries to parse a boolean value from them
     *
     * @param vtype
     * @return boolean
     */
    public static boolean toBoolean(VType vtype) {
        if (vtype instanceof VBoolean) {
            return ((VBoolean) vtype).getValue();
        } else if (vtype instanceof VNumber) {
            return ((VNumber) vtype).getValue().doubleValue() == 0 ? false : true;
        } else if (vtype instanceof VString) {
            return Boolean.parseBoolean(((VString) vtype).getValue());
        }
        return false;
    }
    /**
     * @param name Data item name
     * @param object The object subject to conversion
     * @return Converted object
     */
    public static Object toPVArrayType(String name, Object object) {
        if (object instanceof ListBoolean) {
            ListBoolean listBoolean = (ListBoolean) object;
            boolean[] booleans = new boolean[listBoolean.size()];
            for (int i = 0; i < listBoolean.size(); i++) {
                booleans[i] = listBoolean.getBoolean(i);
            }
            return new PVABoolArray(name, booleans);
        } else if (object instanceof ListNumber) {
            ListNumber listNumber = (ListNumber) object;
            if (object instanceof ArrayByte || object instanceof ArrayUByte) {
                byte[] bytes = new byte[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    bytes[i] = listNumber.getByte(i);
                }
                return new PVAByteArray(name, object instanceof ArrayUByte, bytes);
            } else if (object instanceof ArrayShort || object instanceof ArrayUShort) {
                short[] shorts = new short[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    shorts[i] = listNumber.getShort(i);
                }
                return new PVAShortArray(name, object instanceof ArrayUShort, shorts);
            } else if (object instanceof ArrayInteger || object instanceof ArrayUInteger) {
                int[] ints = new int[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    ints[i] = listNumber.getInt(i);
                }
                return new PVAIntArray(name, object instanceof ArrayUInteger, ints);
            } else if (object instanceof ArrayLong || object instanceof ArrayULong) {
                long[] longs = new long[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    longs[i] = listNumber.getLong(i);
                }
                return new PVALongArray(name, object instanceof ArrayULong, longs);
            } else if (object instanceof ArrayFloat) {
                float[] floats = new float[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    floats[i] = listNumber.getFloat(i);
                }
                return new PVAFloatArray(name, floats);
            } else if (object instanceof ArrayDouble) {
                double[] doubles = new double[listNumber.size()];
                for (int i = 0; i < listNumber.size(); i++) {
                    doubles[i] = listNumber.getDouble(i);
                }
                return new PVADoubleArray(name, doubles);
            } else {
                throw new IllegalArgumentException("Conversion of type " + object.getClass().getCanonicalName() + " not supported");
            }
        } else { // Assume this always is for string arrays
            Collection<String> list = (Collection<String>) object;
            String[] strings = new String[list.size()];
            strings = list.toArray(strings);
            return new PVAStringArray(name, strings);
        }
    }

    /**
     * Extracts the raw value from the given data object. The raw value is either one of the primitive wrappers or some
     * kind of a list type if the value is an {@link Array}.
     *
     * @param type the value to extract the raw data from
     * @return the raw data
     */
    public static Object toObject(VType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof VNumberArray) {
            if (type instanceof VIntArray || type instanceof VUIntArray) {
                return VTypeHelper.toIntegers(type);
            } else if (type instanceof VDoubleArray) {
                return VTypeHelper.toDoubles(type);
            } else if (type instanceof VFloatArray) {
                return VTypeHelper.toFloats(type);
            } else if (type instanceof VLongArray || type instanceof VULongArray) {
                return VTypeHelper.toLongs(type);
            } else if (type instanceof VShortArray || type instanceof VUShortArray) {
                return VTypeHelper.toShorts(type);
            } else if (type instanceof VByteArray || type instanceof VUByteArray) {
                return VTypeHelper.toBytes(type);
            }
        } else if (type instanceof VEnumArray) {
            var indexes = ((VEnumArray) type).getIndexes();
            var array = new int[indexes.size()];
            return indexes.toArray(array);
        } else if (type instanceof VStringArray) {
            List<String> data = ((VStringArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VBooleanArray) {
            return VTypeHelper.toBooleans(type);
        } else if (type instanceof VNumber) {
            return ((VNumber) type).getValue();
        } else if (type instanceof VEnum) {
            return ((VEnum) type).getIndex();
        } else if (type instanceof VString) {
            return ((VString) type).getValue();
        } else if (type instanceof VBoolean) {
            return ((VBoolean) type).getValue();
        } else if (type instanceof VTable) {
            VTable vTable = (VTable) type;
            int columnCount = vTable.getColumnCount();
            List dataArrays = new ArrayList();
            for (int i = 0; i < columnCount; i++) {
                dataArrays.add(toPVArrayType("Col " + i, vTable.getColumnData(i)));
            }
            return new PVAStructure(PVATable.STRUCT_NAME, "", dataArrays);
        }
        return null;
    }
}
