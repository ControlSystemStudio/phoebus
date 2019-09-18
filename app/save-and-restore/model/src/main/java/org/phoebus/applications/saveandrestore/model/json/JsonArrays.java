/**
 * Copyright (C) 2010-18 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.phoebus.applications.saveandrestore.model.json;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;
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
import org.epics.util.array.ListByte;
import org.epics.util.array.ListDouble;
import org.epics.util.array.ListFloat;
import org.epics.util.array.ListInteger;
import org.epics.util.array.ListLong;
import org.epics.util.array.ListNumber;
import org.epics.util.array.ListShort;
import org.epics.util.array.ListUByte;
import org.epics.util.array.ListUInteger;
import org.epics.util.array.ListULong;
import org.epics.util.array.ListUShort;
import org.epics.util.number.UnsignedConversions;

/**
 * Utility classes to convert JSON arrays to and from Lists and ListNumbers.
 *
 * @author carcassi
 */
public class JsonArrays {
    /**
     * Checks whether the array contains only numbers.
     *
     * @param array a JSON array
     * @return true if all elements are JSON numbers
     */
    public static boolean isNumericArray(JsonArray array) {
        for (JsonValue value : array) {
            if (!(value instanceof JsonNumber)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the array contains only strings.
     *
     * @param array a JSON array
     * @return true if all elements are JSON strings
     */
    public static boolean isStringArray(JsonArray array) {
        for (JsonValue value : array) {
            if (!(value instanceof JsonString)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts the given numeric JSON array to a ListDouble.
     *
     * @param array an array of numbers
     * @return a new ListDouble
     */
    public static ListDouble toListDouble(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            if (array.isNull(i)) {
                values[i] = Double.NaN;
            } else {
                values[i] = array.getJsonNumber(i).doubleValue();
            }
        }
        return ArrayDouble.of(values);
    }

    /**
     * Converts the given numeric JSON array to a ListFloat.
     *
     * @param array an array of numbers
     * @return a new ListFloat
     */
    public static ListFloat toListFloat(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) {
            if (array.isNull(i)) {
                values[i] = Float.NaN;
            } else {
                values[i] = (float) array.getJsonNumber(i).doubleValue();
            }
        }
        return ArrayFloat.of(values);
    }

    /**
     * Converts the given numeric JSON array to a {@code ListULong}.
     *
     * @param array an array of numbers
     * @return a new {@code ListULong}
     */
    public static ListULong toListULong(JsonArray array) {
        long[] values = new long[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (long) array.getJsonNumber(i).longValue();
        }
        return ArrayULong.of(values);
    }

    /**
     * Converts the given numeric JSON array to a ListLong.
     *
     * @param array an array of numbers
     * @return a new ListLong
     */
    public static ListLong toListLong(JsonArray array) {
        long[] values = new long[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (long) array.getJsonNumber(i).longValue();
        }
        return ArrayLong.of(values);
    }

    /**
     * Converts the given numeric JSON array to a {@code ListUInteger}.
     *
     * @param array an array of numbers
     * @return a new {@code ListUInteger}
     */
    public static ListUInteger toListUInteger(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (int) array.getJsonNumber(i).intValue();
        }
        return ArrayUInteger.of(values);
    }

    /**
     * Converts the given numeric JSON array to a ListInteger.
     *
     * @param array an array of numbers
     * @return a new ListInteger
     */
    public static ListInteger toListInt(JsonArray array) {
        int[] values = new int[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (int) array.getJsonNumber(i).intValue();
        }
        return ArrayInteger.of(values);
    }

    /**
     * Converts the given numeric JSON array to a {@code ListUShort}.
     *
     * @param array an array of numbers
     * @return a new {@code ListUShort}
     */
    public static ListUShort toListUShort(JsonArray array) {
        short[] values = new short[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (short) array.getJsonNumber(i).intValue();
        }
        return ArrayUShort.of(values);
    }

    /**
     * Converts the given numeric JSON array to a ListShort.
     *
     * @param array an array of numbers
     * @return a new ListShort
     */
    public static ListShort toListShort(JsonArray array) {
        short[] values = new short[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (short) array.getJsonNumber(i).intValue();
        }
        return ArrayShort.of(values);
    }

    /**
     * Converts the given numeric JSON array to a {@code ListUByte}.
     *
     * @param array an array of numbers
     * @return a new {@code ListUByte}
     */
    public static ListUByte toListUByte(JsonArray array) {
        byte[] values = new byte[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (byte) array.getJsonNumber(i).intValue();
        }
        return ArrayUByte.of(values);
    }

    /**
     * Converts the given numeric JSON array to a ListByte.
     *
     * @param array an array of numbers
     * @return a new ListByte
     */
    public static ListByte toListByte(JsonArray array) {
        byte[] values = new byte[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = (byte) array.getJsonNumber(i).intValue();
        }
        return ArrayByte.of(values);
    }

    /**
     * Converts the given string JSON array to a List of Strings.
     *
     * @param array an array of strings
     * @return a new List of Strings
     */
    public static List<String> toListString(JsonArray array) {
        List<String> strings = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            strings.add(array.getString(i));
        }
        return strings;
    }


    /**
     * Converts the given JSON array to a List of Instants.
     *
     * @param array an array
     * @return a new List of Timestamps
     */
    public static List<Instant> toListTimestamp(JsonArray array) {
        List<Instant> timestamps = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            if (array.isNull(i)) {
                timestamps.add(null);
            } else {
                timestamps.add(Instant.ofEpochSecond(array.getJsonNumber(i).longValue() / 1000, (int) (array.getJsonNumber(i).longValue() % 1000) * 1000000));
            }
        }
        return timestamps;
    }

    /**
     * Converts the given List of String to a string JSON array.
     *
     * @param list a List of Strings
     * @return an array of strings
     */
    public static JsonArrayBuilder fromListString(List<String> list) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (String element : list) {
            // TODO: Not clear how to handle nulls. Converting them to empty strings.
            if (element == null) {
                element = "";
            }
            b.add(element);
        }
        return b;
    }

    /**
     * Converts the given List of Timestamp to a JSON array.
     *
     * @param list a List of Timestamps
     * @return an array
     */
    public static JsonArrayBuilder fromListTimestamp(List<Instant> list) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (Instant element : list) {
            if (element == null) {
                b.addNull();
            } else {
                b.add(element.getEpochSecond() * 1000 + element.getNano() / 1000000);
            }
        }
        return b;
    }

    /**
     * Converts the given ListNumber to a number JSON array.
     *
     * @param list a list of numbers
     * @return an array of numbers
     */
    public static JsonArrayBuilder fromListNumber(ListNumber list) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        if (list instanceof ListInteger || list instanceof ListUShort || list instanceof ListShort || list instanceof ListUByte || list instanceof ListByte) {
            for (int i = 0; i < list.size(); i++) {
                b.add(list.getInt(i));
            }
        } else if (list instanceof ListLong || list instanceof ListUInteger) {
            for (int i = 0; i < list.size(); i++) {
                b.add(list.getLong(i));
            }
        } else if (list instanceof ListULong) {
            for (int i = 0; i < list.size(); i++) {
                b.add(UnsignedConversions.toBigInteger(list.getLong(i)));
            }
        } else {
            for (int i = 0; i < list.size(); i++) {
                double value = list.getDouble(i);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    b.addNull();
                } else {
                    b.add(value);
                }
            }
        }
        return b;
    }

}
