/**
 * Copyright (C) 2019 European Spallation Source ERIC.
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.phoebus.applications.saveandrestore.ui.model;


import org.epics.util.array.*;
import org.epics.vtype.*;

import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;

public abstract class ValueFormat extends Format {

    // Number format to be used to format primitive values
    private NumberFormat numberFormat;

    /**
     * Formats the given data object. For scalars and arrays redirects
     * to the appropriate methods. For anything else uses Object.toString().
     *
     * @param data data object to format
     * @return a String representation
     */
    @Override
    public StringBuffer format(Object data, StringBuffer toAppendTo, java.text.FieldPosition pos) {
        if (data == null)
            return toAppendTo;

        if (data instanceof Scalar)
            return format((Scalar) data, toAppendTo, pos);

        if (data instanceof Array)
            return format((Array) data, toAppendTo, pos);

        return toAppendTo.append(data);
    }

    /**
     * Formats an scalar.
     *
     * @param scalar data object to format
     * @return a String representation
     */
    public String format(Scalar scalar) {
        return format((Object) scalar);
    }

    /**
     * Formats an array.
     *
     * @param array data object to format
     * @return a String representation
     */
    public String format(Array array) {
        return format((Object) array);
    }

    /**
     * Formats a scalar.
     *
     * @param scalar data object to format
     * @param toAppendTo output buffer
     * @param pos the field position
     * @return the output buffer
     */
    protected abstract StringBuffer format(Scalar scalar, StringBuffer toAppendTo, FieldPosition pos);

    /**
     * Formats an array.
     *
     * @param array data object to format
     * @param toAppendTo output buffer
     * @param pos the field position
     * @return the output buffer
     */
    protected abstract StringBuffer format(Array array, StringBuffer toAppendTo, FieldPosition pos);

    /**
     * Returns the NumberFormat used to format the numeric values.
     * If null, it will use the NumberFormat from the value Display.
     *
     * @return a NumberFormat
     */
    public NumberFormat getNumberFormat() {
        return numberFormat;
    }

    /**
     * Changes the NumberFormat used to format the numeric values.
     * If null, it will use the NumberFormat from the value Display.
     *
     * @param numberFormat a NumberFormat
     */
    public void setNumberFormat(NumberFormat numberFormat) {
        this.numberFormat = numberFormat;
    }

    @Override
    public Object parseObject(String source, ParsePosition pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Parses the string given the object as a reference.
     * <p>
     * This function will parse the string based on the object provided as a
     * reference (e.g. if reference is a VDouble, it will parse the string as a double).
     * The data in the reference will also be used for the actual parsing (e.g.
     * if reference is Enum, the labels will be used to match the string).
     *
     * @param source the text to parse
     * @param reference the vtype object to reference
     * @return the parsed representation
     */
    public Object parseObject(String source, VType reference) {
        if (reference instanceof VDouble) {
            return parseDouble(source);
        }
        if (reference instanceof VFloat) {
            return parseFloat(source);
        }
        if (reference instanceof VInt) {
            return parseInt(source);
        }
        if (reference instanceof VShort) {
            return parseShort(source);
        }
        if (reference instanceof VByte) {
            return parseByte(source);
        }
        if (reference instanceof VString) {
            return parseString(source);
        }
        if (reference instanceof VEnum) {
            return parseEnum(source, ((VEnum)reference).getDisplay().getChoices());
        }
        if (reference instanceof VDoubleArray) {
            return parseDoubleArray(source);
        }
        if (reference instanceof VFloatArray) {
            return parseFloatArray(source);
        }
        if (reference instanceof VIntArray) {
            return parseIntArray(source);
        }
        if (reference instanceof VShortArray) {
            return parseShortArray(source);
        }
        if (reference instanceof VByteArray) {
            return parseByteArray(source);
        }
        if (reference instanceof VStringArray) {
            return parseStringArray(source);
        }
        if (reference instanceof VEnumArray) {
            return parseEnumArray(source, ((VEnumArray) reference).getData());
        }

        throw new IllegalArgumentException("Type " + ValueUtil.typeOf(reference) + " is not supported");
    }

    /**
     * Parses the string and returns a double representation.
     * <p>
     * Default implementation uses {@link Double#parseDouble(java.lang.String) }
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public double parseDouble(String source) {
        try {
            double value = Double.parseDouble(source);
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Parses the string and returns a float representation.
     * <p>
     * Default implementation uses {@link Float#parseFloat(java.lang.String) }
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public float parseFloat(String source) {
        try {
            float value = Float.parseFloat(source);
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Parses the string and returns an integer representation.
     * <p>
     * Default implementation uses {@link Integer#parseInt(java.lang.String) }
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public int parseInt(String source) {
        try {
            int value = Integer.parseInt(source);
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Parses the string and returns a short representation.
     * <p>
     * Default implementation uses {@link Short#parseShort(java.lang.String) }
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public short parseShort(String source) {
        try {
            short value = Short.parseShort(source);
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Parses the string and returns a byte representation.
     * <p>
     * Default implementation uses {@link Byte#parseByte(java.lang.String) }
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public byte parseByte(String source) {
        try {
            byte value = Byte.parseByte(source);
            return value;
        } catch (NumberFormatException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * Parses the string and returns a string representation.
     * <p>
     * Default implementation returns the string.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public String parseString(String source) {
        return source;
    }

    /**
     * Parses the string and returns the index in the enum.
     * <p>
     * Default implementation matches the label and returns the index.
     *
     * @param source the text to parse
     * @param labels the labels for the enum
     * @return the parsed representation
     */
    public int parseEnum(String source, List<String> labels) {
        int index = labels.indexOf(source);
        if (index != -1) {
            return index;
        }
        throw new RuntimeException(source  + " is not part of enum " + labels);
    }

    /**
     * Parses the string and returns a double array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseDouble(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public ListDouble parseDoubleArray(String source) {
        String[] tokens = source.split(",");
        double[] values = new double[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parseDouble(tokens[i].trim());
        }
        return CollectionNumbers.toListDouble(values);
    }

    /**
     * Parses the string and returns a float array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseFloat(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public ListFloat parseFloatArray(String source) {
        String[] tokens = source.split(",");
        float[] values = new float[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parseFloat(tokens[i].trim());
        }
        return CollectionNumbers.toListFloat(values);
    }

    /**
     * Parses the string and returns an int array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseInt(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public ListInteger parseIntArray(String source) {
        String[] tokens = source.split(",");
        int[] values = new int[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parseInt(tokens[i].trim());
        }
        return CollectionNumbers.toListInt(values);
    }

    /**
     * Parses the string and returns a short array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseShort(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public ListShort parseShortArray(String source) {
        String[] tokens = source.split(",");
        short[] values = new short[tokens.length];
        for (short i = 0; i < values.length; i++) {
            values[i] = parseShort(tokens[i].trim());
        }
        return CollectionNumbers.toListShort(values);
    }

    /**
     * Parses the string and returns a byte array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseByte(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public ListByte parseByteArray(String source) {
        String[] tokens = source.split(",");
        byte[] values = new byte[tokens.length];
        for (byte i = 0; i < values.length; i++) {
            values[i] = parseByte(tokens[i].trim());
        }
        return CollectionNumbers.toListByte(values);
    }

    /**
     * Parses the string and returns a string array representation.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseString(java.lang.String) }.
     *
     * @param source the text to parse
     * @return the parsed representation
     */
    public List<String> parseStringArray(String source) {
        String[] tokens = source.split(",");
        List<String> values = new ArrayList<>();
        for (String token : tokens) {
            values.add(parseString(token.trim()));
        }
        return values;
    }

    /**
     * Parses the string and returns an array of indexes in the enum.
     * <p>
     * Default implementation expects a comma separated list, and parses
     * each element with {@link #parseEnum(java.lang.String, java.util.List) }.
     *
     * @param source the text to parse
     * @param labels the labels for the enum
     * @return the parsed representation
     */
    public ListInteger parseEnumArray(String source, List<String> labels) {
        String[] tokens = source.split(",");
        int[] values = new int[tokens.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = parseEnum(tokens[i].trim(), labels);
        }
        return CollectionNumbers.toListInt(values);
    }

}

