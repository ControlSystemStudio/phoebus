/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.macros;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Macro information
 *
 *  <p>Holds macros and their value
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Macros implements MacroValueProvider
{
    private final Map<String, String> macros = new ConcurrentHashMap<>();

    public final static Pattern MACRO_NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_.\\[\\]]*");

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

    /** Create empty macro map */
    public Macros()
    {
    }

    /** Create copy of existing macros
     *  @param other
     */
    public Macros(final Macros other)
    {
        macros.putAll(other.macros);
    }

    /** @return Are the macros empty? */
    public boolean isEmpty()
    {
        return macros.isEmpty();
    }

    /** Merge two macro maps
     *
     *  <p>Optimized for cases where <code>base</code> or <code>addition</code> are empty,
     *  but will never _change_ any macros.
     *  If a merge is necessary, it returns a new <code>Macros</code> instance.
     *
     *  @param base Base macros
     *  @param addition Additional macros that may override 'base'
     *  @return Merged macros
     */
    public static Macros merge(final Macros base, final Macros addition)
    {
        // Optimize if one is empty
        if (addition == null  ||  addition.macros.isEmpty())
            return base;
        if (base == null  ||  base.macros.isEmpty())
            return addition;
        // Construct new macros
        final Macros merged = new Macros();
        merged.macros.putAll(base.macros);
        merged.macros.putAll(addition.macros);
        return merged;
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
        macros.put(name, value);
    }

    /** @return Macro names, sorted alphabetically */
    public Collection<String> getNames()
    {
        final List<String> names = new ArrayList<>(macros.keySet());
        Collections.sort(names);
        return names;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(final String name)
    {
        return macros.get(name);
    }

    // Hash based on content
    @Override
    public int hashCode()
    {
        return macros.hashCode();
    }

    // Compare based on content
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof Macros))
            return false;
        final Macros other = (Macros) obj;
        return other.macros.equals(macros);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        return "[" + getNames().stream()
                               .map((macro) -> macro + " = '" + macros.get(macro) + "'")
                               .collect(Collectors.joining(", ")) +
               "]";
    }
}
