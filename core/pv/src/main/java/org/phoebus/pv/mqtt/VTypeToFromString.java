/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.pv.mqtt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.phoebus.util.array.ListBoolean;
import org.phoebus.util.array.ListByte;
import org.phoebus.util.array.ListInt;
import org.phoebus.util.array.ListLong;
import org.phoebus.util.array.ListNumber;
import org.phoebus.util.array.ListShort;
import org.phoebus.util.text.NumberFormats;
import org.phoebus.vtype.VBoolean;
import org.phoebus.vtype.VBooleanArray;
import org.phoebus.vtype.VDouble;
import org.phoebus.vtype.VDoubleArray;
import org.phoebus.vtype.VEnum;
import org.phoebus.vtype.VEnumArray;
import org.phoebus.vtype.VLong;
import org.phoebus.vtype.VNumber;
import org.phoebus.vtype.VNumberArray;
import org.phoebus.vtype.VString;
import org.phoebus.vtype.VStringArray;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;

/** Parser for initial value
 *  @author Kay Kasemir, Megan Grodowitz
 */
@SuppressWarnings("nls")
public class VTypeToFromString
{
    private static final NumberFormat nf = NumberFormats.toStringFormat();

    protected static StringBuilder VTArraytoString(VNumberArray array, StringBuilder sb)
    {
        ListNumber data = array.getData();
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            if (data instanceof ListByte || data instanceof ListShort || data instanceof ListInt || data instanceof ListLong) {
                sb.append(nf.format(data.getLong(i)));
            } else {
                sb.append(nf.format(data.getDouble(i)));
            }
        }
        sb.append("]");

        return sb;
    }

    protected static StringBuilder VTArraytoString(List<String> data, StringBuilder sb)
    {
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("\"").append(data.get(i)).append("\"");
        }
        sb.append("]");

        return sb;
    }

    protected static StringBuilder VTArraytoString(ListBoolean data, StringBuilder sb)
    {
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(data.getBoolean(i));
        }
        sb.append("]");

        return sb;
    }

    public static String ToString( Object value ) throws Exception
    {
        if (value instanceof VNumberArray)
        {
            return VTArraytoString(((VNumberArray) value), new StringBuilder()).toString();
        }
        if (value instanceof VStringArray)
        {
            return VTArraytoString(((VStringArray) value).getData(), new StringBuilder()).toString();
        }
        if (value instanceof VBooleanArray)
        {
            return VTArraytoString(((VBooleanArray) value).getData(), new StringBuilder()).toString();
        }
        if (value instanceof VEnumArray)
        {
            return VTArraytoString(((VEnumArray) value).getData(), new StringBuilder()).toString();
        }

        StringBuilder sb = new StringBuilder();

        if (value instanceof VString)
        {
            return sb.append("\"").append(((VString)value).getValue()).append("\"").toString();
        }
        if (value instanceof VBoolean)
        {
            return sb.append(((VBoolean)value).getValue()).toString();
        }
        if (value instanceof VNumber)
        {
            return sb.append(((VNumber)value).getValue()).toString();
        }
        if (value instanceof VEnum)
        {
            final VEnum vEnum = (VEnum)value;
            sb.append(vEnum.getValue())
            .append("(")
            .append(vEnum.getIndex())
            .append(")");
            return sb.toString();
        }

        throw new Exception ("Cannot change unknown type to String " + value.getClass().getName());
    }

    /** @param items String Items
     *  @return Number of quoted items
     */
    public static int countQuotedStrings(final List<String> items)
    {
        int count = 0;
        for (String item : items)
            if (item.startsWith("\""))
                count++;
        return count;
    }

    public static Class<? extends VType> determineValueType(final List<String> items)
    {
        if (countQuotedStrings(items) <= 0)
        {
            try
            {
                parseDoubles(items);

                if (items.size() == 1)
                    return VDouble.class;
                else
                    return VDoubleArray.class;
            }
            catch (Exception ex)
            {
                ;
            }
        }

        if (items.size() == 1)
            return VString.class;
        else
            return VStringArray.class;
    }

    public static VType FromString ( String text ) throws Exception
    {
        List<String> items = splitStringList(text);

        Class<? extends VType> type = determineValueType(items);

        return FromString (text, type);
    }

    public static VType FromString ( final String text, Class<? extends VType> type ) throws Exception
    {
        if (type == VDouble.class)
            return FromStringVDouble(text);
        if (type == VString.class)
            return FromStringVString(text);

        throw new Exception("Unhandled class: " + type.getCanonicalName());

    }

    public static VType FromStringVDouble(final String text) throws Exception
    {
        try
        {
            return ValueFactory.newVDouble( Double.parseDouble(text) );
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VDouble from '" + text + "'");
        }
    }

    public static VType FromStringVLong(final String text) throws Exception
    {
        try
        {
            return ValueFactory.toVType( (long) Double.parseDouble(text) );
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse VLong from '" + text + "'");
        }
    }

    public static VType FromStringVString(final String text)
    {
        return ValueFactory.newVString(stripQuotes(text), ValueFactory.alarmNone(), ValueFactory.timeNow());
    }

    public static VType FromStringVDoubleArray(final String text) throws Exception
    {
        final List<String> items = splitStringList(text);
        final double[] numbers = parseDoubles(items);
        return ValueFactory.toVType(numbers);
    }

    public static VType FromStringVStringArray(final String text) throws Exception
    {
        final List<String> strings = new ArrayList<>();
        final List<String> items = splitStringList(text);

        for (String item : items)
            strings.add(stripQuotes(item));

        return ValueFactory.newVStringArray(strings, ValueFactory.alarmNone(), ValueFactory.timeNow());
    }

    /** Split text into comma separated list
     *
     *  <p>Items are separated by comma.
     *  Items are trimmed of surrounding whitespace.
     *
     *  <p>Spaces and commas inside quotes are retained.
     *  Quotes inside quotes need to be escaped.
     *
     *  @param text Text to parse
     *  @return Items from text
     *  @throws Exception on error
     */
    public static List<String> splitStringList(final String text) throws Exception
    {
        List<String> items = new ArrayList<>();

        int pos = 0, start_next = 0;
        boolean initial_bracket = false;
        boolean is_esc = false;
        boolean inside_quotes = false;
        int len = text.length();

        while (pos < len)
        {
            final char c = text.charAt(pos);

            if ((c == '[') && (pos == 0))
            {
                // check for initial bracket
                len--;
                start_next = 1;
                initial_bracket = true;
            }
            else if (is_esc)
            {
                // valid escape sequence: \b \t \n \f \r \' \" \\
                if (("btnfr".indexOf(c) == -1) && ( c != '\'' ) && (c != '"') && (c != '\\'))
                    throw new Exception("Invalid escape sequence \\" + c);

                // move to next character, this one was escaped
                is_esc = false;
            }
            else if (c == '\\')
            {
                // escape next character
                is_esc = true;
            }
            else if (c == '"')
            {
                // either entering or exiting quoted section
                inside_quotes = !inside_quotes;
            }
            else if ((c == ',') && !inside_quotes)
            {
                String next_item = "";
                if ((pos - start_next) > 0)
                    next_item = text.substring(start_next, pos).trim();
                items.add(next_item);
                start_next = pos+1;
            }

            pos++;
        }

        if ((initial_bracket) && ( (pos >= text.length()) || (text.charAt(pos) != ']') ) )
        {
            throw new Exception("Missing closing bracket");
        }
        if (inside_quotes)
        {
            throw new Exception("Missing closing quote");
        }
        if (is_esc)
        {
            throw new Exception("Trailing escape character");
        }

        String next_item = "";
        if ((pos - start_next) > 0)
            next_item = text.substring(start_next, pos).trim();
        items.add(next_item);

        return items;
    }

    public static String stripQuotes(final String text)
    {
        if (text.length() < 2)
            return text;

        if ((text.charAt(0) == '"') && (text.charAt(text.length()-1) == '"'))
            return text.substring(1,text.length()-1);

        return text;
    }




    /** @param items String Items
     *  @return Numeric values for all items
     *  @throws Exception on error
     */
    public static double[] parseDoubles(List<?> items) throws Exception
    {
        final double[] values = new double[items.size()];
        for (int i=0; i<values.length; ++i)
            try
        {
                values[i] = Double.parseDouble(Objects.toString(items.get(i)));
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Cannot parse number from " + items.get(i));
        }

        return values;
    }


    /** Convert new value to desired type
     *
     *  <p>For a {@link VEnum}, this allows writing either another enum,
     *  a number for the index, or a string for an enum label.
     *
     *  <p>For numbers, allows writing strings which are then parsed into numbers.
     *
     * @param new_value
     * @param type
     * @param old_value
     * @return
     * @throws Exception
     */
    public static VType convert(final Object new_value, Class<? extends VType> type, final VType old_value) throws Exception
    {
        // Already matching VType?
        if (type.isInstance(new_value))
            return (VType) new_value;

        // Is data already a VType (allowing a different one)?
        if (new_value instanceof VType)
            return (VType) new_value;

        if (type == VDouble.class)
        {
            if (new_value instanceof Number)
                return ValueFactory.newVDouble( ((Number)new_value).doubleValue());

            return FromStringVDouble(Objects.toString(new_value));
        }

        if (type == VLong.class)
        {
            if (new_value instanceof Number)
                return ValueFactory.toVType(((Number)new_value).longValue());

            return FromStringVLong(Objects.toString(new_value));
        }

        if (type == VString.class)
            return FromStringVString(Objects.toString(new_value));

        if (type == VDoubleArray.class)
        {   // Pass double[]
            if (new_value instanceof double[])
                return ValueFactory.toVType(new_value);
            // Pass List
            if (new_value instanceof List)
            {
                final double[] numbers = parseDoubles((List<?>)new_value);
                return ValueFactory.toVType(numbers);
            }
            return FromStringVDoubleArray(Objects.toString(new_value));
        }

        if (type == VStringArray.class)
        {
            if (new_value instanceof String[])
                return ValueFactory.newVStringArray(Arrays.asList((String[]) new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());

            if (new_value instanceof List)
            {   // Assert each list element is a String
                final List<String> strings = new ArrayList<>();
                for (Object item : (List<?>)new_value)
                    strings.add(Objects.toString(item));
                return ValueFactory.newVStringArray(strings, ValueFactory.alarmNone(), ValueFactory.timeNow());
            }
            return FromStringVStringArray(Objects.toString(new_value));
        }

        if (type == VEnum.class)
        {
            final List<String> labels = ((VEnum)old_value).getLabels();
            final int index;
            if (new_value instanceof Number)
                index = ((Number)new_value).intValue();
            else
                index = labels.indexOf(Objects.toString(new_value));
            return ValueFactory.newVEnum(index, labels, ValueFactory.alarmNone(), ValueFactory.timeNow());
        }

        throw new Exception("Expected type " + type.getSimpleName() + " but got " + new_value.getClass().getName());
    }
}

