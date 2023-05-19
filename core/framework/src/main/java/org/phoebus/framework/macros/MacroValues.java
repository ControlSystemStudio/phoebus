/*******************************************************************************
 * Copyright (c) 2023 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import static org.phoebus.framework.macros.Macros.logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;


// TODO Replace 'Macros' with this?
/** Macro values
 *
 *  <p>Holds the expanded values from {@link MacroSpecs}.
 *  Assuming the following macro specifications,
 *  <pre>
 *  A=a
 *  B=b
 *  X=$(A)
 *  A=$(B)
 *  B=$(X)
 *  </pre>
 *  the expanded macro values will be
 *  <pre>
 *  A=b
 *  B=a
 *  X=a
 *  </pre>
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroValues implements MacroValueProvider
{
    /** Map of macro name to (expanded) macro value
     *  Order of macro definitions is not preserved,
     *  emphasis is on fast lookup of values by name.
     *  Each macro has exactly one value, holding the
     *  last one from the specification in case specification
     *  re-defines it.
     *  Thread-safe.
     */
    private final ConcurrentHashMap<String, String> macros = new ConcurrentHashMap<>();

    /** Create empty macro map */
    public MacroValues()
    {
    }

    /** Create copy of existing macros
     *  @param other The source macros to be copied or <code>null</code>
     */
    public MacroValues(final MacroValues other)
    {
        if (other != null)
            macros.putAll(other.macros);
    }

    /** Initialize from macro specs
     *  @param macro_specs {@link MacroSpecs}
     */
    public MacroValues(final MacroSpecs macro_specs)
    {
        add(macro_specs);
    }

    /** @return Are the macros empty? */
    public boolean isEmpty()
    {
        return macros.isEmpty();
    }

    /** Add macros from specification
     *  @param macro_specs {@link MacroSpecs}
     */
    public void add(final MacroSpecs macro_specs)
    {
        macro_specs.forEach((name, spec) ->
        {
            try
            {
                add(name, spec);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error in " + macro_specs, ex);
            }
        });
    }

    /** Add a macro value
     *  @param name Name of the macro
     *  @param value Value of the macro, may contain macros "$(NAME)" or "${NAME}" which will be expanded
     *  @throws IllegalArgumentException for illegal macro name
     *  @throws Exception on error, including a value that uses unknown macros that cannot be expanded
     *  @see MacroSpecs#checkMacroName(String)
     */
    public void add(final String name, final String value) throws Exception
    {
        final String error = MacroSpecs.checkMacroName(name);
        if (error != null)
            throw new IllegalArgumentException(error);

        final String expanded = MacroHandler.replace(this, value);
        if (MacroHandler.containsMacros(expanded))
        {
            // Not fatal, in fact common when creating displays,
            // but log with exception to get stack trace in case
            // origin of macro needs to be debugged
            logger.log(Level.WARNING, "Incomplete macro expansion " + name + "='" + expanded + "'",
                       new Exception("Macro spec " + name + "='" + value + "' does not fully resolve"));
        }
        macros.put(name, expanded);
    }

    /** @return Macro names, sorted alphabetically */
    public Collection<String> getNames()
    {
        final List<String> names = new ArrayList<>(macros.keySet());
        Collections.sort(names);
        return names;
    }

    /** Perform given action for each name/value (names are not sorted)
     *  @param action Invoked with each name/value
     */
    public void forEach(final BiConsumer<String, String> action)
    {
        macros.forEach(action);
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
        if (! (obj instanceof MacroValues))
            return false;
        final MacroValues other = (MacroValues) obj;
        return other.macros.equals(macros);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        synchronized (macros)
        {
            return "[ " + getNames().stream()
                                    .map((macro) -> macro + " = '" + macros.get(macro) + "'")
                                    .collect(Collectors.joining(", ")) +
                   " ]";
        }
    }
}
