/*
 * *
 *  * Copyright (C) 2019 European Spallation Source ERIC.
 *  * <p>
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * as published by the Free Software Foundation; either version 2
 *  * of the License, or (at your option) any later version.
 *  * <p>
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  * <p>
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package org.phoebus.applications.saveandrestore;

import org.epics.util.array.ArrayByte;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayFloat;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ArrayLong;
import org.epics.util.array.ArrayShort;
import org.epics.util.array.ArrayUByte;
import org.epics.util.array.ArrayUInteger;
import org.epics.util.array.ArrayULong;
import org.epics.util.array.ArrayUShort;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListNumber;
import org.epics.util.array.ListUInteger;
import org.epics.util.array.ListULong;
import org.epics.util.number.UByte;
import org.epics.util.number.UInteger;
import org.epics.util.number.ULong;
import org.epics.util.number.UShort;
import org.epics.util.number.UnsignedConversions;
import org.epics.util.text.NumberFormats;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Array;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.SimpleValueFormat;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VBooleanArray;
import org.epics.vtype.VByte;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VLong;
import org.epics.vtype.VLongArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShort;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.epics.vtype.VUByte;
import org.epics.vtype.VUByteArray;
import org.epics.vtype.VUInt;
import org.epics.vtype.VUIntArray;
import org.epics.vtype.VULong;
import org.epics.vtype.VULongArray;
import org.epics.vtype.VUShort;
import org.epics.vtype.VUShortArray;
import org.epics.vtype.ValueFormat;
import org.phoebus.applications.saveandrestore.ui.model.Threshold;
import org.phoebus.applications.saveandrestore.ui.model.VDisconnectedData;
import org.phoebus.applications.saveandrestore.ui.model.VNoData;
import org.phoebus.core.vtypes.VTypeHelper;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 *
 * <code>Utilities</code> provides common methods to transform between different data types used by the save and
 * restore. This class also provides methods to transform the timestamps into human readable formats. All methods are
 * thread safe.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 *
 */
public final class Utilities {

    /**
     * <code>VTypeComparison</code> is the result of comparison of two {@link org.epics.vtype.VType} values. The {@link #string} field
     * provides the textual representation of the comparison and the {@link #valuesEqual} provides information whether
     * the values are equal (0), the first value is greater than second (1), or the first value is less than second
     * (-1). This only applies to scalar values. In case of array values the comparison can only result in 0 or 1.
     *
     * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
     *
     */
    public static class VTypeComparison {
        private final String string;
        private final int valuesEqual;
        private final boolean withinThreshold;

        VTypeComparison(String string, int equal, boolean withinThreshold) {
            this.string = string;
            this.valuesEqual = equal;
            this.withinThreshold = withinThreshold;
        }

        /**
         * Returns the string representation of the comparison result.
         *
         * @return the comparison result as a string
         */
        public String getString() {
            return string;
        }

        /**
         * Returns 0 if values are identical, -1 if first value is less than second or 1 otherwise.
         *
         * @return the code describing the values equality
         */
        public int getValuesEqual() {
            return valuesEqual;
        }

        /**
         * Indicates if the values are within the allowed threshold or not.
         *
         * @return true if values are within threshold or false otherwise
         */
        public boolean isWithinThreshold() {
            return withinThreshold;
        }
    }

    /** The character code for the greek delta letter */
    public static final char DELTA_CHAR = '\u0394';
    private static final char SEMI_COLON = ';';
    private static final char COMMA = ',';
    // All formats use thread locals, to avoid problems if any of the static methods are invoked concurrently
    private static final ThreadLocal FORMAT = ThreadLocal.withInitial(() -> {
        ValueFormat vf = new SimpleValueFormat(3);
        vf.setNumberFormat(NumberFormats.toStringFormat());
        return vf;


    });
    private static final ThreadLocal<DateFormat> LE_TIMESTAMP_FORMATTER = ThreadLocal
        .withInitial(() -> new SimpleDateFormat("HH:mm:ss.SSS MMM dd"));
    private static final ThreadLocal<DateFormat> SLE_TIMESTAMP_FORMATTER = ThreadLocal
        .withInitial(() -> new SimpleDateFormat("HH:mm:ss.SSS MMM dd yyyy"));
    private static final ThreadLocal<DateFormat> BE_TIMESTAMP_FORMATTER = ThreadLocal
        .withInitial(() -> new SimpleDateFormat("MMM dd HH:mm:ss"));
    private static final ThreadLocal<DateFormat> SBE_TIMESTAMP_FORMATTER = ThreadLocal
        .withInitial(() -> new SimpleDateFormat("yyyy MMM dd HH:mm:ss"));
    private static final Pattern COMMA_PATTERN = Pattern.compile("\\,");
    private static final ThreadLocal<DecimalFormat> NANO_FORMATTER = ThreadLocal
        .withInitial(() -> new DecimalFormat("000000000"));

    /**
     * Private constructor to prevent instantiation of this class.
     */
    private Utilities() {
    }

    /**
     * Transform the string <code>data</code> to a {@link VType} which is of identical type as the parameter
     * <code>type</code>. The data is expected to be in a proper format so that it can be parsed into the requested
     * type. The alarm of the returned object is none, with message USER DEFINED and the timestamp of the object is now.
     * If the given type is an array type, the number of elements in the new value has to match the number of elements
     * in the type. Individual elements in the input data are separated by comma. This method is the inverse of the
     * {@link #valueToString(VType)}.
     *
     * @param indata the data to parse and transform into VType
     * @param type the type of the destination object
     * @return VType representing the data#
     * @throws IllegalArgumentException if the numbers of array elements do not match
     */
    public static VType valueFromString(String indata, VType type) throws IllegalArgumentException {
        String data = indata.trim();
        if (data.isEmpty()) {
            return type;
        }
        if (data.charAt(0) == '[') {
            data = data.substring(1);
        }
        if (data.charAt(data.length() - 1) == ']') {
            data = data.substring(0, data.length() - 1);
        }
        Alarm alarm = Alarm.of(AlarmSeverity.NONE, AlarmStatus.NONE, "USER DEFINED");

        Time time = Time.now();
        if (type instanceof VNumberArray) {
            ListNumber list = null;
            String[] elements = data.split("\\,");
            if (((VNumberArray) type).getData().size() != elements.length) {
                throw new IllegalArgumentException("The number of array elements is different from the original.");
            }
            if (type instanceof VDoubleArray) {
                double[] array = new double[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Double.parseDouble(elements[i].trim());
                }
                list = ArrayDouble.of(array);
            } else if (type instanceof VFloatArray) {
                float[] array = new float[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Float.parseFloat(elements[i].trim());
                }
                list = ArrayFloat.of(array);
            } else if (type instanceof VULongArray) {
                long[] array = new long[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = new BigInteger(elements[i].trim()).longValue();
                }
                list = ArrayULong.of(array);
            } else if (type instanceof VLongArray) {
                long[] array = new long[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Long.parseLong(elements[i].trim());
                }
                list = ArrayLong.of(array);
            } else if (type instanceof VUIntArray) {
                int[] array = new int[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Long.valueOf(Integer.parseInt(elements[i].trim())).intValue();
                }
                list = ArrayUInteger.of(array);
            } else if (type instanceof VIntArray) {
                int[] array = new int[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Integer.parseInt(elements[i].trim());
                }
                list = ArrayInteger.of(array);
            } else if (type instanceof VUShortArray) {
                short[] array = new short[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Integer.valueOf(Integer.parseInt(elements[i].trim())).shortValue();
                }
                list = ArrayUShort.of(array);
            } else if (type instanceof VShortArray) {
                short[] array = new short[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Short.parseShort(elements[i].trim());
                }
                list = ArrayShort.of(array);
            } else if (type instanceof VUByteArray) {
                byte[] array = new byte[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Integer.valueOf(Integer.parseInt(elements[i].trim())).byteValue();
                }
                list = ArrayUByte.of(array);
            } else if (type instanceof VByteArray) {
                byte[] array = new byte[elements.length];
                for (int i = 0; i < elements.length; i++) {
                    array[i] = Byte.parseByte(elements[i].trim());
                }
                list = ArrayByte.of(array);
            }

            return VNumberArray.of(list, alarm, time, Display.none());
        } else if (type instanceof VDouble) {
            return VDouble.of(Double.parseDouble(data), alarm, time, Display.none());
        } else if (type instanceof VFloat) {
            return VFloat.of(Float.parseFloat(data), alarm, time, Display.none());
        } else if (type instanceof VULong) {
            return VULong.of(ULong.valueOf(new BigInteger(data).longValue()), alarm, time, Display.none());
        } else if (type instanceof VLong) {
            return VLong.of(Long.parseLong(data), alarm, time, Display.none());
        } else if (type instanceof VUInt) {
            return VUInt.of(UInteger.valueOf(Long.valueOf(Long.parseLong(data)).intValue()), alarm, time, Display.none());
        } else if (type instanceof VInt) {
            return VInt.of(Integer.parseInt(data), alarm, time, Display.none());
        } else if (type instanceof VUShort) {
            return VUShort.of(UShort.valueOf(Integer.valueOf(Integer.parseInt(data)).shortValue()), alarm, time, Display.none());
        } else if (type instanceof VShort) {
            return VShort.of(Short.parseShort(data), alarm, time, Display.none());
        } else if (type instanceof VUByte) {
            return VUByte.of(UByte.valueOf(Integer.valueOf(Integer.parseInt(data)).byteValue()), alarm, time, Display.none());
        } else if (type instanceof VByte) {
            return VByte.of(Byte.parseByte(data), alarm, time, Display.none());
        } else if (type instanceof VEnum) {
            List<String> labels = new ArrayList<>(((VEnum) type).getDisplay().getChoices());
            int idx = labels.indexOf(data);
            if (idx < 0) {
                try {
                    idx = Integer.parseInt(data);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(String.format("'%s' is not a valid enum value.",data));
                }
            }
            return VEnum.of(idx, EnumDisplay.of(labels), alarm, time);
        } else if (type instanceof VString) {
            return VString.of(data, alarm, time);
        } else if (type instanceof VBoolean) {
            return VBoolean.of(Boolean.parseBoolean(data), alarm, time);
        } else if (type == VDisconnectedData.INSTANCE || type == VNoData.INSTANCE) {
            try {
                long v = Long.parseLong(indata);
                return VLong.of(v, alarm, time, Display.none());
            } catch (NumberFormatException e) {
                // ignore
            }
            try {
                double v = Double.parseDouble(indata);
                return VDouble.of(v, alarm, time, Display.none());
            } catch (NumberFormatException e) {
                // ignore
            }
            return VString.of(indata, alarm, time);
        }
        return type;
    }

    /**
     * Extracts the raw value from the given data object. The raw value is either one of the primitive wrappers or some
     * kind of a list type if the value is an {@link Array}.
     *
     * @param type the value to extract the raw data from
     * @return the raw data
     */
    public static Object toRawValue(VType type) {
        if (type == null) {
            return null;
        }
        if (type instanceof VNumberArray) {
            return ((VNumberArray) type).getData();
        } else if (type instanceof VEnumArray) {
            return ((VEnumArray) type).getData();
        } else if (type instanceof VStringArray) {
            List<String> data = ((VStringArray) type).getData();
            return data.toArray(new String[data.size()]);
        } else if (type instanceof VBooleanArray) {
            return ((VBooleanArray) type).getData();
        } else if (type instanceof VNumber) {
            return ((VNumber) type).getValue();
        } else if (type instanceof VEnum) {
            VEnum en = (VEnum) type;
            String val = en.getValue();
            if (val.isEmpty()) {
                // if all labels are empty, return the index as a string, otherwise return the label
                List<String> labels = en.getDisplay().getChoices();
                for (String s : labels) {
                    if (!s.isEmpty()) {
                        return val;
                    }
                }
                return String.valueOf(en.getIndex());
            } else {
                return val;
            }
        } else if (type instanceof VString) {
            return ((VString) type).getValue();
        } else if (type instanceof VBoolean) {
            return ((VBoolean) type).getValue();
        }
        return null;
    }

    /**
     * Transforms the value of the given {@link VType} to a human readable string. This method uses formatting to format
     * all values, which may result in the arrays being truncated.
     *
     * @param type the data to transform
     * @return string representation of the data
     */
    public static String valueToString(VType type) {
        return valueToString(type, 15);
    }

    /**
     * Transforms the value of the given {@link VType} to a human readable string. All values are formatted, which means
     * that they may not be exact. If the value is an array type, the maximum number of elements that are included is
     * given by the <code>arrayLimi</code> parameter. This method should only be used for presentation of the value on
     * the screen.
     *
     * @param type the data to transform
     * @param arrayLimit the maximum number of array elements to include
     * @return string representation of the data
     */
    public static String valueToString(VType type, int arrayLimit) {
        if (type == null) {
            return null;
        } else if (type instanceof VNumberArray) {
            ListNumber list = ((VNumberArray) type).getData();
            int size = Math.min(arrayLimit, list.size());
            StringBuilder sb = new StringBuilder(size * 15 + 2);
            sb.append('[');
            Pattern pattern = Pattern.compile("\\,");
            NumberFormat formatter = ((SimpleValueFormat)FORMAT.get()).getNumberFormat();
            if (type instanceof VDoubleArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(pattern.matcher(formatter.format(list.getDouble(i))).replaceAll("\\.")).append(COMMA)
                        .append(' ');
                }
            } else if (type instanceof VFloatArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(pattern.matcher(formatter.format(list.getFloat(i))).replaceAll("\\.")).append(COMMA)
                        .append(' ');
                }
            } else if (type instanceof VULongArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(ULong.valueOf(list.getLong(i))).append(COMMA).append(' ');
                }
            } else if (type instanceof VLongArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(list.getLong(i)).append(COMMA).append(' ');
                }
            } else if (type instanceof VUIntArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(UInteger.valueOf(list.getInt(i))).append(COMMA).append(' ');
                }
            } else if (type instanceof VIntArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(list.getInt(i)).append(COMMA).append(' ');
                }
            } else if (type instanceof VUShortArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(UShort.valueOf(list.getShort(i))).append(COMMA).append(' ');
                }
            } else if (type instanceof VShortArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(list.getShort(i)).append(COMMA).append(' ');
                }
            } else if (type instanceof VUByteArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(UByte.valueOf(list.getByte(i))).append(COMMA).append(' ');
                }
            } else if (type instanceof VByteArray) {
                for (int i = 0; i < size; i++) {
                    sb.append(list.getByte(i)).append(COMMA).append(' ');
                }
            }
            if (size == 0) {
                sb.append(']');
            } else if (size < list.size()) {
                sb.setCharAt(sb.length() - 1, '.');
                sb.append("..]");
            } else {
                sb.setCharAt(sb.length() - 2, ']');
            }
            return sb.toString().trim();
        } else if (type instanceof VNumber) {
            if (type instanceof VDouble) {
                return ((SimpleValueFormat)FORMAT.get()).format(((VDouble) type).getValue());
            } else if (type instanceof VFloat) {
                return ((SimpleValueFormat)FORMAT.get()).format(((VFloat) type).getValue());
            } else {
                return String.valueOf(((VNumber) type).getValue());
            }
        } else if (type instanceof VEnum) {
            VEnum en = (VEnum) type;
            String val = en.getValue();
            if (val.isEmpty()) {
                // if all labels are empty, return the index as a string, otherwise return the label
                List<String> labels = en.getDisplay().getChoices();
                for (String s : labels) {
                    if (!s.isEmpty()) {
                        return val;
                    }
                }
                return String.valueOf(en.getIndex());
            } else {
                return val;
            }
        } else if (type instanceof VString) {
            return ((VString) type).getValue();
        } else if (type instanceof VBoolean) {
            return String.valueOf(((VBoolean) type).getValue());
        }
        // no support for MultiScalars (VMultiDouble, VMultiInt, VMultiString, VMultiEnum), VStatistics, VTable and
        // VImage), no support for VStringArray
        return "Type " + VType.typeOf(type).getSimpleName() + " not supported";
    }

    /**
     * Transforms the value of the given {@link VType} to a string and makes a comparison to the <code>baseValue</code>.
     * If the base value and the transformed value are both of a {@link VNumber} type, the difference of the transformed
     * value to the base value is added to the returned string.
     *
     * @param value the value to compare
     * @param baseValue the base value to compare the value to
     * @param threshold the threshold values to use for comparing the values, if defined and difference is within
     *            threshold limits the values are equal
     * @return string representing the value and the difference from the base value together with the flag indicating
     *         the comparison result
     */
    @SuppressWarnings("unchecked")
    public static VTypeComparison valueToCompareString(VType value, VType baseValue, Optional<Threshold<?>> threshold) {
        if (value == null && baseValue == null
            || value == VDisconnectedData.INSTANCE && baseValue == VDisconnectedData.INSTANCE) {
            return new VTypeComparison(VDisconnectedData.INSTANCE.toString(), 0, true);
        } else if (value == null || baseValue == null) {
            return value == null ? new VTypeComparison(VDisconnectedData.INSTANCE.toString(), -1, false)
                : new VTypeComparison(valueToString(value), 1, false);
        } else if (value == VDisconnectedData.INSTANCE || baseValue == VDisconnectedData.INSTANCE) {
            return value == VDisconnectedData.INSTANCE
                ? new VTypeComparison(VDisconnectedData.INSTANCE.toString(), -1, false)
                : new VTypeComparison(valueToString(value), 1, false);
        }
        if (value instanceof VNumber && baseValue instanceof VNumber) {
            StringBuilder sb = new StringBuilder(20);
            int diff = 0;
            boolean withinThreshold = threshold.isPresent();
            sb.append(((SimpleValueFormat)FORMAT.get()).format(value));
            if (value instanceof VDouble) {
                double data = ((VDouble) value).getValue();
                double base = ((VNumber) baseValue).getValue().doubleValue();
                double newd = data - base;
                diff = Double.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Double>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VFloat) {
                float data = ((VFloat) value).getValue();
                float base = ((VNumber) baseValue).getValue().floatValue();
                float newd = data - base;
                diff = Float.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Float>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VULong) {
                BigInteger data = ((VULong) value).getValue().bigIntegerValue();
                BigInteger base = ((VULong) baseValue).getValue().bigIntegerValue();
                BigInteger newd = data.subtract(base);
                diff = data.compareTo(base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<BigInteger>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd.compareTo(BigInteger.ZERO) > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VLong) {
                long data = ((VLong) value).getValue();
                long base = ((VNumber) baseValue).getValue().longValue();
                long newd = data - base;
                diff = Long.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUInt) {
                long data = ((VUInt) value).getValue().longValue();
                long base = ((VUInt) baseValue).getValue().longValue();
                long newd = data - base;
                diff = Long.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VInt) {
                int data = ((VInt) value).getValue();
                int base = ((VNumber) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUShort) {
                int data = ((VUShort) value).getValue().intValue();
                int base = ((VUShort) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VShort) {
                short data = ((VShort) value).getValue();
                short base = ((VNumber) baseValue).getValue().shortValue();
                short newd = (short) (data - base);
                diff = Short.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Short>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUByte) {
                int data = ((VUByte) value).getValue().intValue();
                int base = ((VUByte) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VByte) {
                byte data = ((VByte) value).getValue();
                byte base = ((VNumber) baseValue).getValue().byteValue();
                byte newd = (byte) (data - base);
                diff = Byte.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Byte>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                sb.append(' ').append(DELTA_CHAR);
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            }
            return new VTypeComparison(sb.toString(), diff, withinThreshold);
        } else if (value instanceof VBoolean && baseValue instanceof VBoolean) {
            String str = valueToString(value);
            boolean b = ((VBoolean) value).getValue();
            boolean c = ((VBoolean) baseValue).getValue();
            return new VTypeComparison(str, Boolean.compare(b, c), b == c);
        } else if (value instanceof VEnum && baseValue instanceof VEnum) {
            String str = valueToString(value);
            String b = ((VEnum) value).getValue();
            String c = ((VEnum) baseValue).getValue();
            int diff = b == null ? (c == null ? 0 : 1) : (c == null ? -1 : b.compareTo(c));
            return new VTypeComparison(str, diff, diff == 0);
        } else if (value instanceof VString && baseValue instanceof VString) {
            String str = valueToString(value);
            String b = ((VString) value).getValue();
            String c = ((VString) baseValue).getValue();
            int diff = b == null ? (c == null ? 0 : 1) : (c == null ? -1 : b.compareTo(c));
            return new VTypeComparison(str, diff, diff == 0);
        } else if (value instanceof VNumberArray && baseValue instanceof VNumberArray) {
            String sb = valueToString(value);
            boolean equal = areValuesEqual(value, baseValue, Optional.empty());
            return new VTypeComparison(sb, equal ? 0 : 1, equal);
        } else {
            String str = valueToString(value);
            boolean valuesEqual = areValuesEqual(value, baseValue, Optional.empty());
            return new VTypeComparison(str, valuesEqual ? 0 : 1, valuesEqual);
        }
    }

    /**
     * Compares the value of the given {@link VType} to the <code>baseValue</code>.
     * If the base value and the transformed value are both of a {@link VNumber} type, the difference of the transformed
     * value to the base value is returned as string.
     *
     * @param value the value to compare
     * @param baseValue the base value to compare the value to
     * @param threshold the threshold values to use for comparing the values, if defined and difference is within
     *            threshold limits the values are equal
     * @return string representing the difference from the base value together with the flag indicating
     *         the comparison result
     */
    @SuppressWarnings("unchecked")
    public static VTypeComparison deltaValueToString(VType value, VType baseValue, Optional<Threshold<?>> threshold) {
        if (value == null && baseValue == null
            || value == VDisconnectedData.INSTANCE && baseValue == VDisconnectedData.INSTANCE) {
            return new VTypeComparison(VDisconnectedData.INSTANCE.toString(), 0, true);
        } else if (value == null || baseValue == null) {
            return value == null ? new VTypeComparison(VDisconnectedData.INSTANCE.toString(), -1, false)
                : new VTypeComparison(valueToString(value), 1, false);
        } else if (value == VDisconnectedData.INSTANCE || baseValue == VDisconnectedData.INSTANCE) {
            return value == VDisconnectedData.INSTANCE
                ? new VTypeComparison(VDisconnectedData.INSTANCE.toString(), -1, false)
                : new VTypeComparison(valueToString(value), 1, false);
        }
        if (value instanceof VNumber && baseValue instanceof VNumber) {
            StringBuilder sb = new StringBuilder(20);
            int diff = 0;
            boolean withinThreshold = threshold.isPresent();
            if (value instanceof VDouble) {
                double data = ((VDouble) value).getValue();
                double base = ((VNumber) baseValue).getValue().doubleValue();
                double newd = data - base;
                diff = Double.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Double>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VFloat) {
                float data = ((VFloat) value).getValue();
                float base = ((VNumber) baseValue).getValue().floatValue();
                float newd = data - base;
                diff = Float.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Float>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VULong) {
                BigInteger data = ((VULong) value).getValue().bigIntegerValue();
                BigInteger base = ((VULong) baseValue).getValue().bigIntegerValue();
                BigInteger newd = data.subtract(base);
                diff = data.compareTo(base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<BigInteger>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd.compareTo(BigInteger.ZERO) > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VLong) {
                long data = ((VLong) value).getValue();
                long base = ((VNumber) baseValue).getValue().longValue();
                long newd = data - base;
                diff = Long.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUInt) {
                long data = ((VUInt) value).getValue().longValue();
                long base = ((VUInt) baseValue).getValue().longValue();
                long newd = data - base;
                diff = Long.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VInt) {
                int data = ((VInt) value).getValue();
                int base = ((VNumber) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUShort) {
                int data = ((VUShort) value).getValue().intValue();
                int base = ((VUShort) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VShort) {
                short data = ((VShort) value).getValue();
                short base = ((VNumber) baseValue).getValue().shortValue();
                short newd = (short) (data - base);
                diff = Short.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Short>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VUByte) {
                int data = ((VUByte) value).getValue().intValue();
                int base = ((VUByte) baseValue).getValue().intValue();
                int newd = data - base;
                diff = Integer.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            } else if (value instanceof VByte) {
                byte data = ((VByte) value).getValue();
                byte base = ((VNumber) baseValue).getValue().byteValue();
                byte newd = (byte) (data - base);
                diff = Byte.compare(data, base);
                if (threshold.isPresent()) {
                    withinThreshold = ((Threshold<Byte>) threshold.get()).isWithinThreshold(data, base);
                } else {
                    withinThreshold = diff == 0;
                }
                if (newd > 0) {
                    sb.append('+');
                }
                sb.append(((SimpleValueFormat)FORMAT.get()).format(newd));
            }
            return new VTypeComparison(sb.toString(), diff, withinThreshold);
        } else if (value instanceof VBoolean && baseValue instanceof VBoolean) {
            String str = valueToString(value);
            boolean b = ((VBoolean) value).getValue();
            boolean c = ((VBoolean) baseValue).getValue();
            return new VTypeComparison(str, Boolean.compare(b, c), b == c);
        } else if (value instanceof VEnum && baseValue instanceof VEnum) {
            String str = valueToString(value);
            String b = ((VEnum) value).getValue();
            String c = ((VEnum) baseValue).getValue();
            int diff = b == null ? (c == null ? 0 : 1) : (c == null ? -1 : b.compareTo(c));
            return new VTypeComparison(str, diff, diff == 0);
        } else if (value instanceof VString && baseValue instanceof VString) {
            String str = valueToString(value);
            String b = ((VString) value).getValue();
            String c = ((VString) baseValue).getValue();
            int diff = b == null ? (c == null ? 0 : 1) : (c == null ? -1 : b.compareTo(c));
            return new VTypeComparison(str, diff, diff == 0);
        } else if (value instanceof VNumberArray && baseValue instanceof VNumberArray) {
            String sb = valueToString(value);
            boolean equal = areValuesEqual(value, baseValue, Optional.empty());
            return new VTypeComparison(sb, equal ? 0 : 1, equal);
        } else {
            String str = valueToString(value);
            boolean valuesEqual = areValuesEqual(value, baseValue, Optional.empty());
            return new VTypeComparison(str, valuesEqual ? 0 : 1, valuesEqual);
        }
    }

    /**
     * Compares the value of the given {@link VType} to the <code>baseValue</code>.
     * If the base va0lue and the transformed value are both of a {@link VNumber} type,
     * the formatted percentage of the transformed value to the base value is returned.
     *
     * @param value the value to compare
     * @param baseValue the base value to compare the value to
     * @return formatted percentage of the difference
     */
    @SuppressWarnings("unchecked")
    public static String deltaValueToPercentage(VType value, VType baseValue) {
        if (value instanceof VNumber && baseValue instanceof VNumber) {
            double data = ((VNumber) value).getValue().doubleValue();
            double base = ((VNumber) baseValue).getValue().doubleValue();
            double newd = data - base;

            double percentage = newd/data*100;

            if (Double.compare(newd, 0) == 0) {
                return "";
            } else if (Double.compare(base, 0) == 0) {
                return "0 Live";
            } else if (Double.compare(data, 0) == 0) {
                return "0 Stored";
            }

            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMaximumFractionDigits(2);

            return format.format(percentage) + "%";
        }

        return "";
    }

    /**
     * Transforms the timestamp to string, using the format HH:mm:ss.SSS MMM dd (yyyy).
     *
     * @param t the timestamp to transform
     * @param includeYear true if the year should included in the format
     * @return string representation of the timestamp using the above format
     */
    public static String timestampToLittleEndianString(Instant t, boolean includeYear) {
        if (t == null) {
            return null;
        }
        return includeYear ? SLE_TIMESTAMP_FORMATTER.get().format(Date.from(t))
            : LE_TIMESTAMP_FORMATTER.get().format(Date.from(t));
    }

    /**
     * Transforms the date to string formatted as yyyy MMM dd HH:mm:ss. Year is only included if the parameter
     * <code>includeYear</code> is true.
     *
     * @param t the date to transform
     * @param includeYear true if the year should be included or false otherwise
     * @return string representation of the date
     */
    public static String timestampToBigEndianString(Instant t, boolean includeYear) {
        if (t == null) {
            return null;
        }
        return includeYear ? SBE_TIMESTAMP_FORMATTER.get().format(Date.from(t))
            : BE_TIMESTAMP_FORMATTER.get().format(Date.from(t));
    }

    /**
     * Checks if the values of the given vtype are equal and returns true if they are or false if they are not.
     * Timestamps, alarms and other parameters are ignored.
     *
     * @param v1 the first value to check
     * @param v2 the second value to check
     * @param threshold the threshold values which define if the difference is within limits or not
     * @return true if the values are equal or false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean areValuesEqual(VType v1, VType v2, Optional<Threshold<?>> threshold) {
        if (v1 == null && v2 == null) {
            return true;
        } else if (v1 == null || v2 == null) {
            return false;
        } else if (v1 == VDisconnectedData.INSTANCE && v2 == VDisconnectedData.INSTANCE) {
            return true;
        } else if (v1 == VDisconnectedData.INSTANCE || v2 == VDisconnectedData.INSTANCE) {
            return false;
        }
        if (v1 instanceof VNumber && v2 instanceof VNumber) {
            if (v1 instanceof VDouble) {
                double data = ((VDouble) v1).getValue();
                double base = ((VNumber) v2).getValue().doubleValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Double>) threshold.get()).isWithinThreshold(data, base);
                }
                return Double.compare(data, base) == 0;
            } else if (v1 instanceof VFloat) {
                float data = ((VFloat) v1).getValue();
                float base = ((VNumber) v2).getValue().floatValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Float>) threshold.get()).isWithinThreshold(data, base);
                }
                return Float.compare(data, base) == 0;
            } else if (v1 instanceof VULong) {
                BigInteger data = ((VULong) v1).getValue().bigIntegerValue();
                BigInteger base = ((VULong) v2).getValue().bigIntegerValue();
                if (threshold.isPresent()) {
                    return ((Threshold<BigInteger>) threshold.get()).isWithinThreshold(data, base);
                }
                return data.compareTo(base) == 0;
            } else if (v1 instanceof VLong) {
                long data = ((VLong) v1).getValue();
                long base = ((VNumber) v2).getValue().longValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                }
                return Long.compare(data, base) == 0;
            } else if (v1 instanceof VUInt) {
                long data = ((VUInt) v1).getValue().longValue();
                long base = ((VUInt) v2).getValue().longValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Long>) threshold.get()).isWithinThreshold(data, base);
                }
                return Long.compare(data, base) == 0;
            } else if (v1 instanceof VInt) {
                int data = ((VInt) v1).getValue();
                int base = ((VNumber) v2).getValue().intValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                }
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VUShort) {
                int data = ((VUShort) v1).getValue().intValue();
                int base = ((VUShort) v2).getValue().intValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                }
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VShort) {
                short data = ((VShort) v1).getValue();
                short base = ((VNumber) v2).getValue().shortValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Short>) threshold.get()).isWithinThreshold(data, base);
                }
                return Short.compare(data, base) == 0;
            } else if (v1 instanceof VUByte) {
                int data = ((VUByte) v1).getValue().intValue();
                int base = ((VNumber) v2).getValue().intValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Integer>) threshold.get()).isWithinThreshold(data, base);
                }
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VByte) {
                byte data = ((VByte) v1).getValue();
                byte base = ((VNumber) v2).getValue().byteValue();
                if (threshold.isPresent()) {
                    return ((Threshold<Byte>) threshold.get()).isWithinThreshold(data, base);
                }
                return Byte.compare(data, base) == 0;
            }
        } else if (v1 instanceof VBoolean && v2 instanceof VBoolean) {
            boolean b = ((VBoolean) v1).getValue();
            boolean c = ((VBoolean) v2).getValue();
            return b == c;
        } else if (v1 instanceof VEnum && v2 instanceof VEnum) {
            String b = ((VEnum) v1).getValue();
            String c = ((VEnum) v2).getValue();
            return b == c || b != null && b.equals(c);
        } else if (v1 instanceof VString && v2 instanceof VString) {
            String b = ((VString) v1).getValue();
            String c = ((VString) v2).getValue();
            return b == c || b != null && b.equals(c);
        } else if (v1 instanceof VNumberArray && v2 instanceof VNumberArray) {
            if ((v1 instanceof VByteArray && v2 instanceof VByteArray)
                || (v1 instanceof VUByteArray && v2 instanceof VUByteArray)
                || (v1 instanceof VShortArray && v2 instanceof VShortArray)
                || (v1 instanceof VUShortArray && v2 instanceof VUShortArray)
                || (v1 instanceof VIntArray && v2 instanceof VIntArray)
                || (v1 instanceof VFloatArray && v2 instanceof VFloatArray)
                || (v1 instanceof VDoubleArray && v2 instanceof VDoubleArray)) {
                ListNumber b = ((VNumberArray) v1).getData();
                ListNumber c = ((VNumberArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (Double.compare(b.getDouble(i), c.getDouble(i)) != 0) {
                        return false;
                    }
                }
                return true;
            } else if (v1 instanceof VUIntArray && v2 instanceof VUIntArray) {
                ListUInteger b = ((VUIntArray) v1).getData();
                ListUInteger c = ((VUIntArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (Long.compare(UInteger.valueOf(b.getInt(i)).longValue(), UInteger.valueOf(c.getInt(i)).longValue()) != 0) {
                        return false;
                    }
                }
                return true;
            } else if (v1 instanceof VLongArray && v2 instanceof VLongArray) {
                ListLong b = ((VLongArray) v1).getData();
                ListLong c = ((VLongArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (Long.compare(b.getLong(i), c.getLong(i)) != 0) {
                        return false;
                    }
                }
                return true;
            } else if (v1 instanceof VULongArray && v2 instanceof VULongArray) {
                ListULong b = ((VULongArray) v1).getData();
                ListULong c = ((VULongArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (BigInteger.valueOf(b.getLong(i)).compareTo(BigInteger.valueOf(c.getLong(i))) != 0) {
                        return false;
                    }
                }
                return true;
            }
        }
        // no support for MultiScalars (VMultiDouble, VMultiInt, VMultiString, VMultiEnum), VStatistics, VTable,
        // VImage and VStringArray)
        return false;
    }

    /**
     * Compares two instances of {@link VType} and returns true if they are identical or false of they are not. Values
     * are identical if their alarm signatures are identical, timestamps are the same, values are the same and in case
     * of enum and enum array also the labels have to be identical.
     *
     * @param v1 the first value
     * @param v2 the second value to compare to the first one
     * @param compareAlarmAndTime true if alarm and time values should be compare or false if no
     * @return true if values are identical or false otherwise
     */
    public static boolean areVTypesIdentical(VType v1, VType v2, boolean compareAlarmAndTime) {
        if (v1 == v2) {
            // this works for no data as well
            return true;
        } else if (v1 == null || v2 == null) {
            return false;
        }
        if (compareAlarmAndTime && !isAlarmAndTimeEqual(v1, v2)) {
            return false;
        }

        if (v1 instanceof VNumber && v2 instanceof VNumber) {
            if (v1 instanceof VDouble && v2 instanceof VDouble) {
                double data = ((VDouble) v1).getValue();
                double base = ((VDouble) v2).getValue();
                return Double.compare(data, base) == 0;
            } else if (v1 instanceof VFloat && v2 instanceof VFloat) {
                float data = ((VFloat) v1).getValue();
                float base = ((VFloat) v2).getValue();
                return Float.compare(data, base) == 0;
            } else if (v1 instanceof VULong && v2 instanceof VULong) {
                BigInteger data = ((VULong) v1).getValue().bigIntegerValue();
                BigInteger base = ((VULong) v2).getValue().bigIntegerValue();
                return data.compareTo(base) == 0;
            } else if (v1 instanceof VLong && v2 instanceof VLong) {
                long data = ((VLong) v1).getValue();
                long base = ((VLong) v2).getValue();
                return Long.compare(data, base) == 0;
            } else if (v1 instanceof VUInt && v2 instanceof VUInt) {
                long data = UnsignedConversions.toLong(((VUInt) v1).getValue().intValue());
                long base = UnsignedConversions.toLong(((VUInt) v2).getValue().intValue());
                return Long.compare(data, base) == 0;
            } else if (v1 instanceof VInt && v2 instanceof VInt) {
                int data = ((VInt) v1).getValue();
                int base = ((VInt) v2).getValue();
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VUShort && v2 instanceof VUShort) {
                int data = UnsignedConversions.toInt(((VUShort) v1).getValue().shortValue());
                int base = UnsignedConversions.toInt(((VUShort) v2).getValue().shortValue());
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VShort && v2 instanceof VShort) {
                short data = ((VShort) v1).getValue();
                short base = ((VShort) v2).getValue();
                return Short.compare(data, base) == 0;
            } else if (v1 instanceof VUByte && v2 instanceof VUByte) {
                int data = UnsignedConversions.toInt(((VUByte) v1).getValue().byteValue());
                int base = UnsignedConversions.toInt(((VUByte) v2).getValue().byteValue());
                return Integer.compare(data, base) == 0;
            } else if (v1 instanceof VByte && v2 instanceof VByte) {
                byte data = ((VByte) v1).getValue();
                byte base = ((VByte) v2).getValue().byteValue();
                return Byte.compare(data, base) == 0;
            }
        } else if (v1 instanceof VBoolean && v2 instanceof VBoolean) {
            boolean b = ((VBoolean) v1).getValue();
            boolean c = ((VBoolean) v2).getValue();
            return b == c;
        } else if (v1 instanceof VEnum && v2 instanceof VEnum) {
            int b = ((VEnum) v1).getIndex();
            int c = ((VEnum) v2).getIndex();
            if (b == c) {
                List<String> l1 = ((VEnum) v1).getDisplay().getChoices();
                List<String> l2 = ((VEnum) v2).getDisplay().getChoices();
                return l1.equals(l2);
            }
            return false;
        } else if (v1 instanceof VNumberArray && v2 instanceof VNumberArray) {
            if ((v1 instanceof VByteArray && v2 instanceof VByteArray)
                || (v1 instanceof VUByteArray && v2 instanceof VUByteArray)
                || (v1 instanceof VShortArray && v2 instanceof VShortArray)
                || (v1 instanceof VUShortArray && v2 instanceof VUShortArray)
                || (v1 instanceof VIntArray && v2 instanceof VIntArray)
                || (v1 instanceof VUIntArray && v2 instanceof VUIntArray)
                || (v1 instanceof VFloatArray && v2 instanceof VFloatArray)
                || (v1 instanceof VDoubleArray && v2 instanceof VDoubleArray)) {
                ListNumber b = ((VNumberArray) v1).getData();
                ListNumber c = ((VNumberArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (Double.compare(b.getDouble(i), c.getDouble(i)) != 0) {
                        return false;
                    }
                }
                return true;
            } else if (v1 instanceof VLongArray && v2 instanceof VLongArray) {
                ListLong b = ((VLongArray) v1).getData();
                ListLong c = ((VLongArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (Long.compare(b.getLong(i), c.getLong(i)) != 0) {
                        return false;
                    }
                }
                return true;
            } else if (v1 instanceof VULongArray && v2 instanceof VULongArray) {
                ListULong b = ((VULongArray) v1).getData();
                ListULong c = ((VULongArray) v2).getData();
                int size = b.size();
                if (size != c.size()) {
                    return false;
                }
                for (int i = 0; i < size; i++) {
                    if (BigInteger.valueOf(b.getLong(i)).compareTo(BigInteger.valueOf(c.getLong(i))) != 0) {
                        return false;
                    }
                }
                return true;
            }
        }
        // no support for MultiScalars (VMultiDouble, VMultiInt, VMultiString, VMultiEnum), VStatistics, VTable and
        // VImage)
        return false;
    }

    private static boolean isAlarmAndTimeEqual(VType a1, VType a2) {

        if (a1 instanceof VNumber && a2 instanceof VNumber) {
            VNumber vn1 = (VNumber)a1;
            VNumber vn2 = (VNumber)a2;
            return vn1.getAlarm().getName().equals(vn2.getAlarm().getName()) &&
                    vn1.getAlarm().getSeverity().equals(vn2.getAlarm().getSeverity()) &&
                    vn1.getTime().getTimestamp().equals(vn2.getTime().getTimestamp());
        }
        else if (a1 instanceof VNumberArray && a2 instanceof VNumberArray) {
            VNumberArray vn1 = (VNumberArray)a1;
            VNumberArray vn2 = (VNumberArray)a2;
            return vn1.getAlarm().getName().equals(vn2.getAlarm().getName()) &&
                    vn1.getAlarm().getSeverity().equals(vn2.getAlarm().getSeverity()) &&
                    vn1.getTime().getTimestamp().equals(vn2.getTime().getTimestamp());
        }
        else if (a1 instanceof VNumber && a2 instanceof VNumberArray) {
            VNumber vn1 = (VNumber)a1;
            VNumberArray vn2 = (VNumberArray)a2;
            return vn1.getAlarm().getName().equals(vn2.getAlarm().getName()) &&
                    vn1.getAlarm().getSeverity().equals(vn2.getAlarm().getSeverity()) &&
                    vn1.getTime().getTimestamp().equals(vn2.getTime().getTimestamp());
        }
        else if (a1 instanceof VNumberArray && a2 instanceof VNumber) {
            VNumberArray vn1 = (VNumberArray)a1;
            VNumber vn2 = (VNumber)a2;
            return vn1.getAlarm().getName().equals(vn2.getAlarm().getName()) &&
                    vn1.getAlarm().getSeverity().equals(vn2.getAlarm().getSeverity()) &&
                    vn1.getTime().getTimestamp().equals(vn2.getTime().getTimestamp());
        }
        else if (a1 instanceof VEnum && a2 instanceof VEnum) {
            VEnum vn1 = (VEnum)a1;
            VEnum vn2 = (VEnum)a2;
            return vn1.getAlarm().getName().equals(vn2.getAlarm().getName()) &&
                    vn1.getAlarm().getSeverity().equals(vn2.getAlarm().getSeverity()) &&
                    vn1.getTime().getTimestamp().equals(vn2.getTime().getTimestamp());
        }
        return false;
    }
}
