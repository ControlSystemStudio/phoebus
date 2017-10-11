/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.vtype;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.phoebus.util.array.ArrayDouble;
import org.phoebus.util.array.ArrayInt;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.text.NumberFormats;

/**
 * Various utility methods for runtime handling of the types defined in
 * this package.
 *
 * @author carcassi
 */
public class ValueUtil {

    private ValueUtil() {
        // Can't instantiate
    }

    private static Collection<Class<?>> types = Arrays.<Class<?>>asList(VByte.class, VByteArray.class, VDouble.class,
            VDoubleArray.class, VEnum.class, VEnumArray.class, VFloat.class, VFloatArray.class,
            VLong.class, VLongArray.class, VInt.class, VIntArray.class, VMultiDouble.class, VMultiEnum.class,
            VMultiInt.class, VMultiString.class, VShort.class, VShortArray.class,
            VStatistics.class, VString.class, VStringArray.class, VBoolean.class, VBooleanArray.class, VTable.class,
            VImage.class);

    /**
     * Returns the type of the object by returning the class object of one
     * of the VXxx interfaces. The getClass() methods returns the
     * concrete implementation type, which is of little use. If no
     * super-interface is found, Object.class is used.
     *
     * @param obj an object implementing a standard type
     * @return the type is implementing
     */
    public static Class<?> typeOf(Object obj) {
        if (obj == null)
            return null;

        return typeOf(obj.getClass());
    }

    private static Class<?> typeOf(Class<?> clazz) {
        if (clazz.equals(Object.class))
            return Object.class;

        for (int i = 0; i < clazz.getInterfaces().length; i++) {
            Class<?> interf = clazz.getInterfaces()[i];
            if (types.contains(interf))
                return interf;
        }

        return typeOf(clazz.getSuperclass());
    }

    /**
     * Extracts the alarm information if present.
     *
     * @param obj an object implementing a standard type
     * @return the alarm information for the object
     */
    public static Alarm alarmOf(Object obj) {
        if (obj == null) {
            return ValueFactory.alarmNone();
        }
        if (obj instanceof Alarm)
            return (Alarm) obj;
        return null;
    }

    /**
     * Extracts the alarm information if present, based on value
     * and connection status.
     *
     * @param value a value
     * @param connected the connection status
     * @return the alarm information
     */
    public static Alarm alarmOf(Object value, boolean connected) {
        if (value != null) {
            if (value instanceof Alarm) {
                return (Alarm) value;
            } else {
                return ValueFactory.alarmNone();
            }
        } else if (connected) {
            return ValueFactory.newAlarm(AlarmSeverity.INVALID, "No value");
        } else {
            return ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, "Disconnected");
        }
    }

    /**
     * Returns the alarm with highest severity. null values can either be ignored or
     * treated as UNDEFINED severity.
     *
     * @param args a list of values
     * @param considerNull whether to consider null values
     * @return the highest alarm; can't be null
     */
    public static Alarm highestSeverityOf(final List<Object> args, final boolean considerNull) {
        Alarm finalAlarm = ValueFactory.alarmNone();
        for (Object object : args) {
            Alarm newAlarm;
            if (object == null && considerNull) {
                newAlarm = ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, "No Value");
            } else {
                newAlarm = ValueUtil.alarmOf(object);
                if (newAlarm == null) {
                    newAlarm = ValueFactory.alarmNone();
                }
            }
            if (newAlarm.getAlarmSeverity().compareTo(finalAlarm.getAlarmSeverity()) > 0) {
                finalAlarm = newAlarm;
            }
        }

        return finalAlarm;
    }

    /**
     * Returns the time with latest timestamp.
     *
     * @param args a list of values
     * @return the latest time; can be null
     */
    public static Time latestTimeOf(final List<Object> args) {
        Time finalTime = null;
        for (Object object : args) {
            Time newTime;
            if (object != null)  {
                newTime = ValueUtil.timeOf(object);
                if (newTime != null && (finalTime == null || newTime.getTimestamp().compareTo(finalTime.getTimestamp()) > 0)) {
                    finalTime = newTime;
                }
            }
        }

        return finalTime;
    }

    /**
     * Returns the time with latest valid timestamp or now.
     *
     * @param args a list of values
     * @return the latest time; can't be null
     */
    public static Time latestValidTimeOrNowOf(final List<Object> args) {
        Time finalTime = null;
        for (Object object : args) {
            Time newTime;
            if (object != null)  {
                newTime = ValueUtil.timeOf(object);
                if (newTime != null && newTime.isTimeValid() && (finalTime == null || newTime.getTimestamp().compareTo(finalTime.getTimestamp()) > 0)) {
                    finalTime = newTime;
                }
            }
        }

        if (finalTime == null) {
            finalTime = ValueFactory.timeNow();
        }

        return finalTime;
    }

    /**
     * Extracts the time information if present.
     *
     * @param obj an object implementing a standard type
     * @return the time information for the object
     */
    public static Time timeOf(Object obj) {
        if (obj instanceof Time)
            return (Time) obj;
        return null;
    }

    /**
     * Extracts the display information if present.
     *
     * @param obj an object implementing a standard type
     * @return the display information for the object
     */
    public static Display displayOf(Object obj) {
        if (obj instanceof VBoolean) {
            return ValueFactory.displayBoolean();
        }
        if (!(obj instanceof Display))
            return null;
        Display display = (Display) obj;
        if (display.getLowerAlarmLimit() == null || display.getLowerDisplayLimit() == null)
            return null;
        return display;
    }

    /**
     * Checks whether the display limits are non-null and non-NaN.
     *
     * @param display a display
     * @return true if the display limits have actual values
     */
    public static boolean displayHasValidDisplayLimits(Display display) {
        if (display.getLowerDisplayLimit() == null || display.getLowerDisplayLimit().isNaN()) {
            return false;
        }
        if (display.getUpperDisplayLimit() == null || display.getUpperDisplayLimit().isNaN()) {
            return false;
        }
        return true;
    }

    /**
     * Extracts the numericValueOf the object and normalizes according
     * to the display range.
     *
     * @param obj an object implementing a standard type
     * @return the value normalized in its display range, or null
     *         if no value or display information is present
     */
    public static Double normalizedNumericValueOf(Object obj) {
        return normalize(numericValueOf(obj), displayOf(obj));
    }

    /**
     * Normalizes the given value according to the given display information.
     *
     * @param value a value
     * @param display the display information
     * @return the normalized value, or null of either value or display is null
     */
    public static Double normalize(Number value, Display display) {
        if (value == null || display == null) {
            return null;
        }

        return (value.doubleValue() - display.getLowerDisplayLimit()) / (display.getUpperDisplayLimit() - display.getLowerDisplayLimit());
    }

    /**
     * Normalizes the given value according to the given range;
     *
     * @param value a value
     * @param lowValue the lowest value in the range
     * @param highValue the highest value in the range
     * @return the normalized value, or null if any value is null
     */
    public static Double normalize(Number value, Number lowValue, Number highValue) {
        if (value == null || lowValue == null || highValue == null) {
            return null;
        }

        return (value.doubleValue() - lowValue.doubleValue()) / (highValue.doubleValue() - lowValue.doubleValue());
    }

    /**
     * Extracts a numeric value for the object. If it's a numeric scalar,
     * the value is returned. If it's a numeric array, the first element is
     * returned. If it's a numeric multi array, the value of the first
     * element is returned.
     *
     * @param obj an object implementing a standard type
     * @return the numeric value
     */
    public static Double numericValueOf(Object obj) {
        if (obj instanceof VNumber) {
            Number value = ((VNumber) obj).getValue();
            if (value != null) {
                return value.doubleValue();
            }
        }

        if (obj instanceof VBoolean) {
            return (double) (((VBoolean) obj).getValue() ? 1 : 0);
        }

        if (obj instanceof VEnum) {
            return (double) ((VEnum) obj).getIndex();
        }

        if (obj instanceof VNumberArray) {
            ListNumber data = ((VNumberArray) obj).getData();
            if (data != null && data.size() != 0) {
                return data.getDouble(0);
            }
        }

        if (obj instanceof VEnumArray) {
            ListNumber data = ((VEnumArray) obj).getIndexes();
            if (data != null && data.size() != 0) {
                return data.getDouble(0);
            }
        }

        if (obj instanceof MultiScalar) {
            List values = ((MultiScalar) obj).getValues();
            if (!values.isEmpty())
                return numericValueOf(values.get(0));
        }

        return null;
    }

    /**
     * Converts a VImage to an AWT BufferedImage, so that it can be displayed.
     * The content of the vImage buffer is copied, so further changes
     * to the VImage will not modify the BufferedImage.
     * 
     * 
     * 
     * @param vImage the image to be converted
     * @return a new BufferedImage
     */
    public static BufferedImage toImage(VImage vImage) {
        if (vImage.getVImageType() == VImageType.TYPE_3BYTE_BGR) {
            BufferedImage image = new BufferedImage(vImage.getWidth(), vImage.getHeight(),
                    BufferedImage.TYPE_3BYTE_BGR);
            ListNumber data = vImage.getData();
            for (int i = 0; i < data.size(); i++) {
                ((DataBufferByte) image.getRaster().getDataBuffer()).getData()[i] = data.getByte(i);
            }
            return image;
        } else {
            throw new UnsupportedOperationException(
                    "No support for creating a BufferedImage from Image Type: " + vImage.getVImageType());
        }
    }

    /**
     * Converts an AWT BufferedImage to a VImage.
     * <p>
     * Currently, only TYPE_3BYTE_BGR is supported
     *
     * @param image buffered image
     * @return a new image
     */
    public static VImage toVImage(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            newImage.getGraphics().drawImage(image, 0, 0, null);
            image = newImage;
        }

        byte[] buffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        return ValueFactory.newVImage(image.getHeight(), image.getWidth(), buffer);
    }

    /**
     * Returns true if the two displays contain the same information.
     *
     * @param d1 the first display
     * @param d2 the second display
     * @return true if they match
     */
    public static boolean displayEquals(Display d1, Display d2) {
        if (d1 == d2) {
            return true;
        }

        if (Objects.equals(d1.getFormat(), d2.getFormat()) &&
                Objects.equals(d1.getUnits(), d2.getUnits()) &&
                Objects.equals(d1.getLowerDisplayLimit(), d2.getLowerDisplayLimit()) &&
                Objects.equals(d1.getLowerAlarmLimit(), d2.getLowerAlarmLimit()) &&
                Objects.equals(d1.getLowerWarningLimit(), d2.getLowerWarningLimit()) &&
                Objects.equals(d1.getUpperWarningLimit(), d2.getUpperWarningLimit()) &&
                Objects.equals(d1.getUpperAlarmLimit(), d2.getUpperAlarmLimit()) &&
                Objects.equals(d1.getUpperDisplayLimit(), d2.getUpperDisplayLimit()) &&
                Objects.equals(d1.getLowerCtrlLimit(), d2.getLowerCtrlLimit()) &&
                Objects.equals(d1.getUpperCtrlLimit(), d2.getUpperCtrlLimit())) {
            return true;
        }

        return false;
    }

    private static volatile DateTimeFormatter defaultTimestampFormat = DateTimeFormatter.ISO_DATE_TIME;
    private static volatile NumberFormat defaultNumberFormat = NumberFormats.toStringFormat();
    private static volatile ValueFormat defaultValueFormat = new SimpleValueFormat(3);
    private static volatile Map<AlarmSeverity, Integer> rgbSeverityColor = createDefaultSeverityColorMap();

    private static Map<AlarmSeverity, Integer> createDefaultSeverityColorMap() {
        Map<AlarmSeverity, Integer> colorMap = new EnumMap<>(AlarmSeverity.class);
        colorMap.put(AlarmSeverity.NONE, 0xFF00FF00); // Color.GREEN
        colorMap.put(AlarmSeverity.MINOR, 0xFFFFFF00); // Color.YELLOW
        colorMap.put(AlarmSeverity.MAJOR, 0xFFFF0000); // Color.RED
        colorMap.put(AlarmSeverity.INVALID, 0xFFFF00FF); // Color.MAGENTA
        colorMap.put(AlarmSeverity.UNDEFINED, 0xFF404040); // Color.DARK_GRAY
        return colorMap;
    }

    /**
     * Changes the color map for AlarmSeverity. The new color map must be complete
     * and not null;
     *
     * @param map the new color map
     */
    public static void setAlarmSeverityColorMap(Map<AlarmSeverity, Integer> map) {
        if (map == null) {
            throw new IllegalArgumentException("Alarm severity color map can't be null");
        }

        for (AlarmSeverity alarmSeverity : AlarmSeverity.values()) {
            if (!map.containsKey(alarmSeverity)) {
                throw new IllegalArgumentException("Missing color for AlarmSeverity." + alarmSeverity);
            }
        }

        Map<AlarmSeverity, Integer> colorMap = new EnumMap<>(AlarmSeverity.class);
        colorMap.putAll(map);
        rgbSeverityColor = colorMap;
    }

    /**
     * Returns the rgb value for the given severity.
     *
     * @param severity an alarm severity
     * @return the rgb color
     */
    public static int colorFor(AlarmSeverity severity) {
        return rgbSeverityColor.get(severity);
    }

    /**
     * The default object to format and parse timestamps.
     *
     * @return the default timestamp format
     */
    public static DateTimeFormatter getDefaultTimestampFormat() {
        return defaultTimestampFormat;
    }

    /**
     * Changes the default timestamp format.
     *
     * @param defaultTimestampFormat the new default timestamp format
     */
    public static void setDefaultTimestampFormat(DateTimeFormatter defaultTimestampFormat) {
        ValueUtil.defaultTimestampFormat = defaultTimestampFormat;
    }

    /**
     * The default format for numbers.
     *
     * @return the default number format
     */
    public static NumberFormat getDefaultNumberFormat() {
        return defaultNumberFormat;
    }

    /**
     * Changes the default format for numbers.
     *
     * @param defaultNumberFormat the new default number format
     */
    public static void setDefaultNumberFormat(NumberFormat defaultNumberFormat) {
        ValueUtil.defaultNumberFormat = defaultNumberFormat;
    }

    /**
     * The default format for VTypes.
     *
     * @return the default format
     */
    public static ValueFormat getDefaultValueFormat() {
        return defaultValueFormat;
    }

    /**
     * Changes the default format for VTypes.
     *
     * @param defaultValueFormat the new default format
     */
    public static void setDefaultValueFormat(ValueFormat defaultValueFormat) {
        ValueUtil.defaultValueFormat = defaultValueFormat;
    }

    /**
     * Extracts the values of a column, making sure it contains
     * numeric values.
     *
     * @param table a table
     * @param columnName the name of the column to extract
     * @return the values; null if the columnName is null or is not found
     * @throws IllegalArgumentException if the column is found but does not contain numeric values
     */
    public static ListNumber numericColumnOf(VTable table, String columnName) {
        if (columnName == null) {
            return null;
        }

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (columnName.equals(table.getColumnName(i))) {
                if (table.getColumnType(i).isPrimitive()) {
                    return (ListNumber) table.getColumnData(i);
                } else {
                    throw new IllegalArgumentException("Column '" + columnName +"' is not numeric (contains " + table.getColumnType(i).getSimpleName() + ")");
                }
            }
        }

        throw new IllegalArgumentException("Column '" + columnName +"' was not found");
    }

    /**
     * Extracts the values of a column, making sure it contains
     * strings.
     *
     * @param table a table
     * @param columnName the name of the column to extract
     * @return the values; null if the columnName is null or is not found
     * @throws IllegalArgumentException if the column is found but does not contain string values
     */
    public static List<String> stringColumnOf(VTable table, String columnName) {
        if (columnName == null) {
            return null;
        }

        for (int i = 0; i < table.getColumnCount(); i++) {
            if (columnName.equals(table.getColumnName(i))) {
                if (table.getColumnType(i).equals(String.class)) {
                    @SuppressWarnings("unchecked")
                    List<String> result = (List<String>) table.getColumnData(i);
                    return result;
                } else {
                    throw new IllegalArgumentException("Column '" + columnName +"' is not string (contains " + table.getColumnType(i).getSimpleName() + ")");
                }
            }
        }

        throw new IllegalArgumentException("Column '" + columnName +"' was not found");
    }

    /**
     * Returns the default array dimension display by looking at the size
     * of the n dimensional array and creating cell boundaries based on index.
     *
     * @param array the array
     * @return the array dimension display
     */
    public static List<ArrayDimensionDisplay> defaultArrayDisplay(VNumberArray array) {
        return defaultArrayDisplay(array.getSizes());
    }

    /**
     * Returns the default array dimension display given the size
     * of the n dimensional array and creating cell boundaries based on index.
     *
     * @param sizes the shape of the array
     * @return the array dimension display
     */
    public static List<ArrayDimensionDisplay> defaultArrayDisplay(ListInt sizes) {
        List<ArrayDimensionDisplay> displays = new ArrayList<>();
        for (int i = 0; i < sizes.size(); i++) {
            displays.add(ValueFactory.newDisplay(sizes.getInt(i)));
        }
        return displays;
    }

    /**
     * Filters an element of a one-dimensional array.
     *
     * @param array a 1D array
     * @param index a valid index
     * @return the trimmed array to that one index
     */
    public static VNumberArray subArray(VNumberArray array, int index) {
        if (array.getSizes().size() != 1) {
            throw new IllegalArgumentException("Array was not one-dimensional");
        }
        if (index < 0 || array.getData().size() <= index) {
            throw new IllegalArgumentException("Index not in the array range");
        }

        ArrayDimensionDisplay display = array.getDimensionDisplay().get(0);
        return ValueFactory.newVNumberArray(new ArrayDouble(array.getData().getDouble(index)),
                new ArrayInt(1), Arrays.asList(ValueFactory.newDisplay(new ArrayDouble(display.getCellBoundaries().getDouble(index), display.getCellBoundaries().getDouble(index+1)), display.getUnits())),
                array, array, array);

    }
}
