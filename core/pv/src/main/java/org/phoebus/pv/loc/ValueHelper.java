/*******************************************************************************
 * Copyright (c) 2016 Oak Ridge National Laboratory.
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

import org.diirt.vtype.AlarmSeverity;
import org.diirt.vtype.VDouble;
import org.diirt.vtype.VDoubleArray;
import org.diirt.vtype.VEnum;
import org.diirt.vtype.VLong;
import org.diirt.vtype.VString;
import org.diirt.vtype.VStringArray;
import org.diirt.vtype.VTable;
import org.diirt.vtype.VType;
import org.diirt.vtype.ValueFactory;

/** Parser for initial value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ValueHelper
{
    /** Parse local PV name
     *  @param base_name "name", "name(value)" or "name&lt;type>(value)"
     *  @return Name, type-or-null, value-or-null
     *  @throws Exception on error
     */
    public static String[] parseName(final String base_name) throws Exception
    {
        // Could use regular expression, but this allows more specific error messages
        String name=null, type=null, value=null;

        // Locate type
        int sep = base_name.indexOf('<');
        if (sep >= 0)
        {
            final int end = base_name.indexOf('>', sep+1);
            if (end <= sep)
                throw new Exception("Missing '>' to define type in " + base_name);
            name = base_name.substring(0, sep);
            type = base_name.substring(sep+1, end);
        }

        // Locate value
        sep = base_name.indexOf('(');
        if (sep > 0)
        {
            final int end = base_name.lastIndexOf(')');
            if (end <= sep)
                throw new Exception("Missing ')' of initial value in " + base_name);
            value = base_name.substring(sep+1, end);
            if (name == null)
                name = base_name.substring(0, sep);
        }

        if (name == null)
            name = base_name.trim();

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
                return ValueFactory.newVDouble(0.0,
                                               ValueFactory.newAlarm(AlarmSeverity.UNDEFINED, "UDF"),
                                               ValueFactory.timeNow(),
                                               ValueFactory.displayNone());
            if (items.size() == 1)
                return ValueFactory.toVType(getInitialDoubles(items)[0]);
            else
                throw new Exception("Expected one number, got " + items);
        }

        if (type == VLong.class)
        {
            if (items.size() == 1)
                return ValueFactory.toVType((long) getInitialDoubles(items)[0]);
            else
                throw new Exception("Expected one number, got " + items);
        }

        if (type == VString.class)
        {
            if (items == null  ||  items.size() == 1)
                return ValueFactory.toVType(getInitialStrings(items).get(0));
            else
                throw new Exception("Expected one string, got " + items);
        }

        if (type == VDoubleArray.class)
            return ValueFactory.toVType(getInitialDoubles(items));

        if (type == VStringArray.class)
            return ValueFactory.toVType(getInitialStrings(items));

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
            return ValueFactory.newVEnum(initial, labels, ValueFactory.alarmNone(), ValueFactory.timeNow());
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
            return ValueFactory.newVTable(types, headers, values);
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
                return ValueFactory.newVDouble( ((Number)new_value).doubleValue());
            try
            {
                return ValueFactory.newVDouble( Double.parseDouble(Objects.toString(new_value)) );
            }
            catch (NumberFormatException ex)
            {
                // Does PV have the initial 0.0 UNDEFINED value,
                // and the type may be changed to the first assigned data type?
                if (change_from_double)
                {   // Change to string?
                    if (new_value instanceof String)
                        return ValueFactory.newVString(Objects.toString(new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());
                    // Change to double[]?
                    if (new_value instanceof double[])
                        return ValueFactory.toVType(new_value);
                    try
                    {
                        if (new_value instanceof List)
                        {
                            final double[] numbers = getInitialDoubles((List<?>)new_value);
                            return ValueFactory.toVType(numbers);
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore, try next type
                    }
                    if (new_value instanceof String[])
                        return ValueFactory.newVStringArray(Arrays.asList((String[]) new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());
                    if (new_value instanceof List)
                    {   // Assert each list element is a String
                        final List<String> strings = new ArrayList<>();
                        for (Object item : (List<?>)new_value)
                            strings.add(Objects.toString(item));
                        return ValueFactory.newVStringArray(strings, ValueFactory.alarmNone(), ValueFactory.timeNow());
                    }
                }
                throw new Exception("Cannot parse number from '" + new_value + "'");
            }
        }

        if (type == VLong.class)
        {
            if (new_value instanceof Number)
                return ValueFactory.toVType(((Number)new_value).longValue());
            try
            {
                return ValueFactory.toVType( (long) Double.parseDouble(Objects.toString(new_value)) );
            }
            catch (NumberFormatException ex)
            {
                throw new Exception("Cannot parse number from '" + new_value + "'");
            }
        }

        if (type == VString.class)
            // Stringify anything
            return ValueFactory.newVString(Objects.toString(new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());

        if (type == VDoubleArray.class)
        {   // Pass double[]
            if (new_value instanceof double[])
                return ValueFactory.toVType(new_value);
            // Pass List
            if (new_value instanceof List)
            {
                final double[] numbers = getInitialDoubles((List<?>)new_value);
                return ValueFactory.toVType(numbers);
            }

            // Parse string "1, 2, 3"
            if (new_value instanceof String)
            {
                final List<String> items = splitInitialItems(Objects.toString(new_value));
                final double[] numbers = getInitialDoubles(items);
                return ValueFactory.toVType(numbers);
            }
        }

        if (type == VStringArray.class)
        {   // Pass String
            if (new_value instanceof String)
                return ValueFactory.newVStringArray(Arrays.asList((String) new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());
            // Pass String[]
            if (new_value instanceof String[])
                return ValueFactory.newVStringArray(Arrays.asList((String[]) new_value), ValueFactory.alarmNone(), ValueFactory.timeNow());
            if (new_value instanceof List)
            {   // Assert each list element is a String
                final List<String> strings = new ArrayList<>();
                for (Object item : (List<?>)new_value)
                    strings.add(Objects.toString(item));
                return ValueFactory.newVStringArray(strings, ValueFactory.alarmNone(), ValueFactory.timeNow());
            }
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
