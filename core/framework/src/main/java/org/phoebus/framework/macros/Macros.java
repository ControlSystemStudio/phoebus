/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Macro specifications and expanded runtime values
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
 *  At runtime, they are expanded, typically based on macros
 *  passed in from a container.
 *  Assuming that a container provides
 *  <pre>COUNT=42</pre>
 *  the above specification will result in
 *  <pre>
 *  A=b
 *  B=a
 *  COUNT=42
 *  X=a
 *  </pre>
 *
 *  @author Kay Kasemir
 */
public class Macros implements MacroValueProvider
{
    /** Logger for macro related messages */
    public static final Logger logger = Logger.getLogger(Macros.class.getPackageName());

    /** Regular expression for valid macro names */
    public final static Pattern MACRO_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.\\-\\[\\]]*");

    /** List of name-value pairs for macro specification.
     *  Order of macro definitions is preserved.
     *  Multiple specifications for the same macro will re-define its value.
     *  Thread-safe using COWAL assuming that specifications are usually short,
     *  between none and just a few per display widget.
     */
    private final CopyOnWriteArrayList<AbstractMap.SimpleImmutableEntry<String, String>> specs = new CopyOnWriteArrayList<>();

    /** Map of macro name to (expanded) macro value
     *  Order of macro definitions is not preserved,
     *  emphasis is on fast lookup of values by name.
     *  Each macro has exactly one value, holding the
     *  last one from the specification in case specification
     *  re-defines it.
     *  Thread-safe.
     */
    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();

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
     *  @return {@link Macros}
     *  @throws Exception on error
     */
    public static Macros fromSimpleSpec(final String names_and_values) throws Exception
    {
        final Macros macros = new Macros();

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

    /** Create empty macros */
    public Macros()
    {
    }

    /** Create copy of existing macros
     *  @param other The source macros to be copied.
     */
    public Macros(final Macros other)
    {
        if (other != null)
        {
            specs.addAll(other.specs);
            values.putAll(other.values);
        }
    }

    /** @return Are there any macro specifications? */
    public boolean isEmpty()
    {
        return specs.isEmpty();
    }

    /** Add a macro specification
     *  @param name Name of the macro
     *  @param spec Specification of the macro, that is value that might contain "$(NAME)"
     *  @throws IllegalArgumentException for illegal macro name
     *  @see #checkMacroName(String)
     */
    public void add(final String name, final String spec)
    {
        final String error = checkMacroName(name);
        if (error != null)
            throw new IllegalArgumentException(error);
        specs.add(new SimpleImmutableEntry<>(name, spec));
        // Expand this name=spec right away so getValue() won't return null.
        // Complete expansion typically requires calling expand(base)
        expandSpec(name, spec);
    }

    /** Expand macro specs into values
     *
     *  <p>The macro specifications remain unchanged,
     *  while the values are set to the input's values,
     *  then adding the expanded specs.
     *  @param base Base values (already expanded) to import before expanding specs
     */
    public void expandValues(final Macros base)
    {
        values.clear();

        // Add all base values (already expanded)
        if (base != null)
            values.putAll(base.values);

        // Add expanded specs
        for (AbstractMap.SimpleImmutableEntry<String, String> spec : specs)
        {
            final String name = spec.getKey();
            final String expanded = expandSpec(name, spec.getValue());
            if (MacroHandler.containsMacros(expanded))
            {
                // Not fatal, in fact common when creating displays,
                // but log with exception to get stack trace in case
                // origin of macro needs to be debugged
                logger.log(Level.WARNING, "Incomplete macro expansion " + name + "='" + expanded + "'",
                           new Exception("Macro spec " + name + "='" + spec + "' does not fully resolve"));
            }
        }
    }

    /** Expand a macro spec
     *  @param name Name of the macro
     *  @param spec Specification of the macro, may contain macros "$(NAME)" or "${NAME}" which will be expanded
     *  @return Expanded value
     */
    private String expandSpec(final String name, final String spec)
    {
        String expanded;
        try
        {
            expanded = MacroHandler.replace(this, spec);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Failed to expand " + name + "='" + spec + "'", ex);
            expanded = spec;
        }
        values.put(name, expanded);
        return expanded;
    }

    /** @return Names of expanded macros, sorted alphabetically */
    public Collection<String> getNames()
    {
        final List<String> names = new ArrayList<>(values.keySet());
        Collections.sort(names);
        return names;
    }

    /** Get value for macro
     *  @param name Name of the macro
     *  @return Expanded value of the macro or <code>null</code> if not defined or macro specs have not been expanded
     */
    @Override
    public String getValue(final String name)
    {
        return values.get(name);
    }

    // Note that there is no "getSpecNames()" and "getSpec(name)"
    // because for specifications, the same name might appear more than once
    // and order matters. So "getSpecNames()" would have to return
    // the same name multiple times, and "getSpec(name)" wouldn't know
    // which definition to return.
    // forEachSpec() iterates over all specs in the correct order

    /** Visit a thread-safe snapshot of all macro specifications
     *  @param action Invoked with each macro name and value specification
     */
    public void forEachSpec(final BiConsumer<String, String> action)
    {
        specs.forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }

    /** Visit a thread-safe snapshot of all expanded macro values
     *  @param action Invoked with each macro name and expanded value
     */
    public void forEach(final BiConsumer<String, String> action)
    {
        values.forEach(action);
    }

    // Hash based on specs
    @Override
    public int hashCode()
    {
        return specs.hashCode();
    }

    // Compare based on specs
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof Macros))
            return false;
        final Macros other = (Macros) obj;
        return other.specs.equals(specs);
    }

    /** @return String representation of macro specs for debugging
     *  @see #toExpandedString()
     */
    @Override
    public String toString()
    {
        return "[" + specs.stream()
                          .map(entry -> entry.getKey() + "='" + entry.getValue() + "'")
                          .collect(Collectors.joining(",")) +
               "]";
    }

    /** @return String representation of expanded macros for debugging
     *  @see #toString()
     */
    public String toExpandedString()
    {
        return "[" + getNames().stream()
                               .map((macro) -> macro + "='" + getValue(macro) + "'")
                               .collect(Collectors.joining(",")) +
               "]";
    }

}
