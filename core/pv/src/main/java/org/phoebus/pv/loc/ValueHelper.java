/*******************************************************************************
 * Copyright (c) 2016-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv.loc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.AlarmStatus;
import org.epics.vtype.Display;
import org.epics.vtype.EnumDisplay;
import org.epics.vtype.Time;
import org.epics.vtype.VBoolean;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VInt;
import org.epics.vtype.VLong;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;

/** Parser for initial value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ValueHelper
{
    static final Alarm UDF = Alarm.of(AlarmSeverity.UNDEFINED, AlarmStatus.UNDEFINED, "UDF");

    /** loc:// PV name: Starts with alpha, then alphanumeric or ':_-.' */
    private static final Pattern PV_NAME_PATTERN = Pattern.compile("([A-Za-z][-A-Za-z0-9:_.]*)(.*)", Pattern.DOTALL);

    /** Parse local PV name
     *  @param base_name "name", "name(value)" or "name&lt;type>(value)"
     *  @return Name, type-or-null, value-or-null
     *  @throws Exception on error
     */
    public static String[] parseName(final String base_name) throws Exception
    {
        final Matcher matcher = PV_NAME_PATTERN.matcher(base_name);
        if (! matcher.matches())
            throw new Exception("Missing PV name in " + base_name);

        final String name = matcher.group(1);
        String rest = matcher.group(2);

        // Could use regular expression for all, but this allows more specific error messages
        String type=null, value=null;

        // Locate type
        if (rest.startsWith("<"))
        {
            final int end = rest.indexOf('>', 1);
            if (end <= 0)
                throw new Exception("Missing '>' to define type in " + base_name);
            type = rest.substring(1, end);
            rest = rest.substring(end + 1);
        }

        // Locate value
        if (rest.startsWith("("))
        {
            final int end = rest.lastIndexOf(')');
            if (end <= 0)
                throw new Exception("Missing ')' of initial value in " + base_name);
            value = rest.substring(1, end);
        }

        return new String[] { name, type, value };
    }

    /** Split initial value text into items.
     *
     *  <p>Items are separated by comma.
     *  Items are trimmed of surrounding whitespace.
     *
     *  <p>Spaces and commata inside quotes are retained.
     *  Quotes inside quotes need to be escaped.
     *
     *  @param text Text to parse, may be <code>null</code> or empty
     *  @return Items from text, <code>null</code> if nothing provided
     *  @throws Exception on error
     */
    public static List<String> splitInitialItems(final String text) throws Exception
    {
        if (text == null  ||  text.isEmpty())
            return null;

        final List<String> items = new ArrayList<>();

        int pos = 0;
        while (pos < text.length())
        {
            final char c = text.charAt(pos);
            // Skip space
            if (c == ' '  ||  c == '\t')
                ++pos;

            // Handle quoted string
            else if (c == '"')
            {   // Locate closing, non-escaped quote
                int end = text.indexOf('"', pos+1);
                while (end > pos && text.charAt(end-1) == '\\')
                    end = text.indexOf('"', end+1);
                if (end < 0)
                    throw new Exception("Missing closing quote");
                items.add(text.substring(pos, end+1));
                pos = end + 1;
                // Advance to comma at end of string
                while (pos < text.length() && text.charAt(pos) != ',')
                    ++pos;
                ++pos;
            }

            // Handle unquoted item
            else
            {   // Locate comma
                int end = pos+1;
                while (end < text.length()  &&
                       text.charAt(end) != ',')
                    ++end;
                items.add(text.substring(pos, end).trim());
                pos = end+1;
            }
        }

        return items;
    }

    /** @param items Items from <code>splitInitialItems</code>
     *  @return <code>true</code> if at least one item is quoted
     */
    public static boolean haveInitialStrings(final List<String> items)
    {
        for (String item : items)
            if (item.startsWith("\""))
                return true;
        return false;
    }

    /** @param items Items from <code>splitInitialItems</code>
     *  @return All items as strings, surrounding quotes removed, un-escaping quotes
     */
    private static List<String> getInitialStrings(List<String> items)
    {
        if (items == null)
            return Arrays.asList("");
        final List<String> strings = new ArrayList<>(items.size());
        for (String item : items)
            if (item.startsWith("\""))
                strings.add(item.substring(1, item.length()-1).replace("\\\"", "\""));
            else
                strings.add(item);
        return strings;
    }

    /** @param items Items from <code>splitInitialItems</code>
     *  @return Numeric values for all items
     *  @throws Exception on error
     */
    public static double[] getInitialDoubles(List<?> items) throws Exception
    {
        final double[] values = new double[items.size()];
        for (int i=0; i<values.length; ++i)
        {
            try
            {
                final String text = Objects.toString(items.get(i));
                if (text.startsWith("0x"))
                    values[i] = Integer.parseInt(text.substring(2), 16);
                else
                    values[i] = Double.parseDouble(text);
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse number from " + items.get(i));
            }
        }

        return values;
    }

    /**
     * 
     * @param items Items from <code>splitInitialItems</code>
     * @return Boolean list of all items
     */
    private static List<Boolean> getInitialBooleans(List<String> items) {
        if (items == null)
            return Arrays.asList(Boolean.FALSE);
        return items.stream().map(item -> {
            return Boolean.parseBoolean(item);
        }).collect(Collectors.toList());
    }

    /** @param items Items from <code>splitInitialItems</code>, i.e. strings are quoted
     *  @param type Desired VType
     *  @return VType for initial value
     *  @throws Exception on error
     */
    public static VType getInitialValue(final List<String> items, Class<? extends VType> type) throws Exception
    {
        if (type == VDouble.class)
        {
            if (items == null)
                return VDouble.of(0.0, UDF, Time.now(), Display.none());
            if (items.size() == 1)
                return VDouble.of(getInitialDoubles(items)[0], Alarm.none(), Time.now(), Display.none());
            else
                throw new Exception("Expected one number, got " + items);
        }

        if (type == VLong.class)
        {
            if (items.size() == 1)
                return VLong.of((long) getInitialDoubles(items)[0], Alarm.none(), Time.now(), Display.none());
            else
                throw new Exception("Expected one number, got " + items);
        }

        if (type == VInt.class)
        {
            if (items.size() == 1)
                return VInt.of((long) getInitialDoubles(items)[0], Alarm.none(), Time.now(), Display.none());
            else
                throw new Exception("Expected one number, got " + items);
        }

        if (type == VBoolean.class)
        {
            if (items == null  ||  items.size() == 1)
                return VBoolean.of(getInitialBooleans(items).get(0), Alarm.none(), Time.now());
            else
                throw new Exception("Expected one boolean, got " + items);
        }

        if (type == VString.class)
        {
            if (items == null  ||  items.size() == 1)
                return VString.of(getInitialStrings(items).get(0), Alarm.none(), Time.now());
            else
                throw new Exception("Expected one string, got " + items);
        }

        if (type == VDoubleArray.class)
            return VDoubleArray.of(ArrayDouble.of(getInitialDoubles(items)), Alarm.none(), Time.now(), Display.none());

//        if (type == VBooleanArray.class)
//            return VBooleanArray.of(ArrayBoolean.of(getInitialBooleans(items)), Alarm.none(), Time.now());

        if (type == VStringArray.class)
            return VStringArray.of(getInitialStrings(items), Alarm.none(), Time.now());


        if (type == VEnum.class)
        {
            if (items.size() < 2)
                throw new Exception("VEnum needs at least '(index, \"Label0\")'");
            final int initial;
            try
            {
                initial = Integer.parseInt(items.get(0));
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse enum index", ex);
            }
            // Preserve original list
            final List<String> copy = new ArrayList<>(items.size()-1);
            for (int i=1; i<items.size(); ++i)
                copy.add(items.get(i));
            final List<String> labels = getInitialStrings(copy);
            return VEnum.of(initial, EnumDisplay.of(labels), Alarm.none(), Time.now());
        }

        if (type == VTable.class)
        {
            final List<String> headers = getInitialStrings(items);
            final List<Class<?>> types = new ArrayList<>();
            final List<Object> values = new ArrayList<>();
            while (headers.size() > values.size())
            {   // Assume each column is of type string, no values
                types.add(String.class);
                values.add(Collections.emptyList());
            }
            return VTable.of(types, headers, values);
        }
        throw new Exception("Cannot obtain type " + type.getSimpleName() + " from " + items);
    }


    /** Adapt new value to desired type
     *
     *  <p>For a {@link VEnum}, this allows writing either another enum,
     *  a number for the index, or a string for an enum label.
     *
     *  <p>For numbers, allows writing strings which are then parsed into numbers.
     *
     * @param new_value New value
     * @param type Current type of the PV
     * @param old_value Old value of PV, will be used to inspect e.g. enum labels
     * @param change_from_double Adapt to a new 'type' if 'new_value' doesn't match?
     * @return Adapted value
     * @throws Exception
     */
    public static VType adapt(final Object new_value, Class<? extends VType> type, final VType old_value,
                              final boolean change_from_double) throws Exception
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
                return VDouble.of(((Number)new_value).doubleValue(), Alarm.none(), Time.now(), Display.none());
            try
            {
                return VDouble.of(Double.parseDouble(Objects.toString(new_value)), Alarm.none(), Time.now(), Display.none());
            }
            catch (NumberFormatException ex)
            {
                // Does PV have the initial 0.0 UNDEFINED value,
                // and the type may be changed to the first assigned data type?
                if (change_from_double)
                {   // Change to string?
                    if (new_value instanceof String)
                        return VString.of(Objects.toString(new_value), Alarm.none(), Time.now());
                    // Change to double[]?
                    if (new_value instanceof double[])
                        return VDoubleArray.of(ArrayDouble.of((double[])new_value), Alarm.none(), Time.now(), Display.none());
                    try
                    {
                        if (new_value instanceof List)
                        {
                            final double[] numbers = getInitialDoubles((List<?>)new_value);
                            return VDoubleArray.of(ArrayDouble.of(numbers), Alarm.none(), Time.now(), Display.none());
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore, try next type
                    }
                    if (new_value instanceof String[])
                        return VStringArray.of(Arrays.asList((String[]) new_value), Alarm.none(), Time.now());
                    if (new_value instanceof List)
                    {   // Assert each list element is a String
                        final List<String> strings = new ArrayList<>();
                        for (Object item : (List<?>)new_value)
                            strings.add(Objects.toString(item));
                        return VStringArray.of(strings, Alarm.none(), Time.now());
                    }
                }
                throw new Exception("Cannot parse number from '" + new_value + "'");
            }
        }

        if (type == VLong.class)
        {
            if (new_value instanceof Number)
                return VLong.of(((Number)new_value).longValue(), Alarm.none(), Time.now(), Display.none());
            try
            {
                return VLong.of((long) Double.parseDouble(Objects.toString(new_value)), Alarm.none(), Time.now(), Display.none());
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse number from '" + new_value + "'");
            }
        }

        if (type == VInt.class)
        {
            if (new_value instanceof Number)
                return VInt.of(((Number)new_value).intValue(), Alarm.none(), Time.now(), Display.none());
            try
            {
                return VInt.of((int) Double.parseDouble(Objects.toString(new_value)), Alarm.none(), Time.now(), Display.none());
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse number from '" + new_value + "'");
            }
        }

        if (type == VBoolean.class) {
            if (new_value instanceof Boolean)
                return VBoolean.of((Boolean) new_value, Alarm.none(), Time.now());
            if (new_value instanceof Number)
            {
                try {
                    int value = Integer.parseInt(Objects.toString(new_value));
                    if (value == 0)
                    {
                        return VBoolean.of(Boolean.FALSE, Alarm.none(), Time.now());
                    } else
                    {
                        return VBoolean.of(Boolean.TRUE, Alarm.none(), Time.now());
                    }
                } catch (NumberFormatException ex) {
                    throw new Exception("Cannot parse boolean from '" + new_value + "'");
                }
            }
            if (new_value instanceof String)
            {
                return VBoolean.of(Boolean.parseBoolean(String.valueOf(new_value)), Alarm.none(), Time.now());
            }
        }

        if (type == VString.class)
            // Stringify anything
            return VString.of(Objects.toString(new_value), Alarm.none(), Time.now());

        if (type == VDoubleArray.class)
        {   // Pass double[]
            if (new_value instanceof double[])
                return VDoubleArray.of(ArrayDouble.of((double[])new_value), Alarm.none(), Time.now(), Display.none());
            // Pass List
            if (new_value instanceof List)
            {
                final double[] numbers = getInitialDoubles((List<?>)new_value);
                return VDoubleArray.of(ArrayDouble.of(numbers), Alarm.none(), Time.now(), Display.none());
            }

            // Parse string "1, 2, 3"
            if (new_value instanceof String)
            {
                final List<String> items = splitInitialItems(Objects.toString(new_value));
                final double[] numbers = getInitialDoubles(items);
                return VDoubleArray.of(ArrayDouble.of(numbers), Alarm.none(), Time.now(), Display.none());
            }
        }

        if (type == VStringArray.class)
        {   // Pass String
            if (new_value instanceof String)
                return VStringArray.of(Arrays.asList((String) new_value), Alarm.none(), Time.now());
            // Pass String[]
            if (new_value instanceof String[])
                return VStringArray.of(Arrays.asList((String[]) new_value), Alarm.none(), Time.now());
            if (new_value instanceof List)
            {   // Assert each list element is a String
                final List<String> strings = new ArrayList<>();
                for (Object item : (List<?>)new_value)
                    strings.add(Objects.toString(item));
                return VStringArray.of(strings, Alarm.none(), Time.now());
            }
        }

        if (type == VEnum.class)
        {
            final EnumDisplay meta = ((VEnum)old_value).getDisplay();
            final List<String> labels = meta.getChoices();
            final int index;
            if (new_value instanceof Number)
                index = ((Number)new_value).intValue();
            else
                index = labels.indexOf(Objects.toString(new_value));
            return VEnum.of(index, meta, Alarm.none(), Time.now());
        }

        throw new Exception("Expected type " + type.getSimpleName() + " but got " + new_value.getClass().getName());
    }
}
