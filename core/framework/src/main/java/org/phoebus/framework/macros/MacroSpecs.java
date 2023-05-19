/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Macro specifications
 *
 *  <p>Macro specifications are an ordered list of name-value-pairs:
 *  <pre>
 *  A=a
 *  B=b
 *  X=$(A)
 *  A=$(B)
 *  B=$(X)
 *  </pre>
 *
 *  <p>Macro specifications are used to configure for example
 *  display widgets.
 *  At runtime, they are expanded into {@link MacroValues}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroSpecs
{
    /** List of name-value pairs for macro name and specification.
     *  Order of macro definitions is preserved.
     *  Multiple specifications for the same macro will re-define its value.
     *  Thread-safe using COWAL assuming that specifications are usually short,
     *  between none and just a few per display widget.
     */
    private final CopyOnWriteArrayList<AbstractMap.SimpleImmutableEntry<String, String>> specs = new CopyOnWriteArrayList<>();

    /** Regular expression for valid macro names */
    public final static Pattern MACRO_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.\\-\\[\\]]*");

    /** Check macro name
     *
     *  <p>Permitted macro names are a subset of valid XML names:
     *  <ul>
     *  <li>Must start with character
     *  <li>May then contain characters or numbers
     *  <li>May also contain underscores
     *  <li>This check also permits brackets and dots
     *      for path-type properties like "traces[1].name"
     *  </ul>
     * @param name Macro name to check
     * @return Error message or <code>null</code> if name is valid
     */
    public static String checkMacroName(final String name)
    {
        // Could use one big reg.ex. but try to provide error message for each case.
        if (name == null  ||  name.isEmpty())
            return "Empty macro name";
        if (name.indexOf('$') >= 0)
            return "Macro name '" + name + "' contains recursive macro";
        if (! MACRO_NAME_PATTERN.matcher(name).matches())
            return "Invalid macro name '" + name + "': Must start with character, then contain characters, numbers or underscores";
        if (name.toLowerCase().startsWith("xml"))
            return "Invalid macro name '" + name + "': Must not start with 'XML' or 'xml'";
        return null;
    }

    /** Parse macro information from "macro1=value1, macro2=value2" type text
     *
     *  <p>Format:
     *  Macro name as described in {{@link #checkMacroName(String)},
     *  followed by '=' and value.
     *  Surrounding spaces are removed, and comma separates subsequent macro and value:
     *  <pre> M1 = Value1 , M2 = Value2 </pre>
     *
     *  <p>Value must be enclosed in '"' quotes if it contains surrounding spaces or comma:
     *  <pre> MSG = "This is a message with comma, quoted!" , M2 = Value2 </pre>
     *
     *  To include quotes in the value, the value must be quoted and the embedded quotes
     *  escaped:
     *  <pre> MSG = "This is a \"Message\""</pre>
     *
     *  @param names_and_values "M1=Value1, M2=Value2"
     *  @return {@link MacroSpecs}
     *  @throws Exception on error
     */
    public static MacroSpecs fromSimpleSpec(final String names_and_values) throws Exception
    {
        final MacroSpecs macros = new MacroSpecs();

        final int len = names_and_values.length();
        int pos = 0;
        while (pos < len)
        {
            // Locate next '=' in name = value
            final int sep = names_and_values.indexOf('=', pos);
            if (sep < 0)
                break;

            // Fetch name
            String name = names_and_values.substring(pos, sep).trim();
            String error = checkMacroName(name);
            if (error != null)
                throw new Exception("Error parsing '" + names_and_values + "': " + error);

            // Fetch value
            String value = null;
            pos = sep + 1;
            int end = pos;
            // Locate end, either a ',' or via quoted text
            while (true)
            {
                if (end >= len)
                {
                    value = names_and_values.substring(pos, end).trim();
                    break;
                }

                char c = names_and_values.charAt(end);
                if (c == ',')
                {
                    value = names_and_values.substring(pos, end).trim();
                    ++end;
                    break;
                }
                if (c == '"')
                {
                    // Locate end of quoted text, skipping escaped quotes
                    int close = end+1;
                    while (close < len)
                    {
                        if (names_and_values.charAt(close) == '"'  &&
                            names_and_values.charAt(close-1) != '\\')
                        {
                            value = names_and_values.substring(end+1, close).replace("\\", "");

                            // Advance to ',' or end
                            end = close+1;
                            while (end < len  &&
                                   (names_and_values.charAt(end) == ' ' ||
                                    names_and_values.charAt(end) == ','))
                                ++end;
                            break;
                        }
                        ++close;
                    }
                    break;
                }
                ++end;
            }

            if (value == null)
                throw new Exception("Error parsing '" + names_and_values + "': Missing value");

            macros.add(name, value);
            pos = end;
        }

        return macros;
    }

    /** Create empty macro map */
    public MacroSpecs()
    {
    }

    /** Create copy of existing macros
     *  @param other The source macros to be copied.
     */
    public MacroSpecs(final MacroSpecs other)
    {
        if (other != null)
            specs.addAll(other.specs);
    }

    /** @return Are the macros empty? */
    public boolean isEmpty()
    {
        return specs.isEmpty();
    }

    /** Add a macro
     *  @param name Name of the macro
     *  @param value Value of the macro
     *  @throws IllegalArgumentException for illegal macro name
     *  @see #checkMacroName(String)
     */
    public void add(final String name, final String value)
    {
        final String error = checkMacroName(name);
        if (error != null)
            throw new IllegalArgumentException(error);
        specs.add(new SimpleImmutableEntry<>(name, value));
    }

    /** Visit a thread-safe snapshot of all macro specifications
     *  @param action Invoked with each macro name and value specification
     */
    public void forEach(final BiConsumer<String, String> action)
    {
        specs.forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    // Hash based on content
    @Override
    public int hashCode()
    {
        return specs.hashCode();
    }

    // Compare based on content
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof MacroSpecs))
            return false;
        final MacroSpecs other = (MacroSpecs) obj;
        return other.specs.equals(specs);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        return "[ " + specs.stream()
                           .map(entry -> entry.getKey() + "='" + entry.getValue() + "'")
                           .collect(Collectors.joining(", ")) +
               " ]";
    }
}
