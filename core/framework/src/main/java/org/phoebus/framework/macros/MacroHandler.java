/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handler for {@link Macros}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroHandler
{
    /** Max. recursion level to guard against recursive macros that never resolve */
    // In principle, could try to detect loops like
    // A=$(B)
    // B=$(A)
    // Current implementation quits after MAX_RECURSION attempts because
    // that's much simpler and plenty fast.
    private static final int MAX_RECURSION = 20;

    /** Recursion counter */
    private static final AtomicInteger recursions = new AtomicInteger();

    // Pattern for $(xxx) or ${xxx}, or $(x=y) or ${x=y}, asserting that there is NO leading '\' to escape it
    // "=" is matched with any number of whitespace characters (space, tab, etc.) on either side
    private static final Pattern spec = Pattern
            .compile("(?<!\\\\)\\$\\((" + Macros.MACRO_NAME_PATTERN + ")((\\s*=\\s*).*)?\\)" +
                     "|" +
                     "(?<!\\\\)\\$\\{(" + Macros.MACRO_NAME_PATTERN + ")((\\s*=\\s*).*)?\\}");

    /** Check if input contains unresolved macros
     *  @param input Text that may contain macros "$(NAME)" or "${NAME}", even escaped ones because they need to be un-escaped
     *  @return <code>true</code> if there is at least one potential macro
     */
    public static boolean containsMacros(final String input)
    {
        return input != null  &&  input.indexOf('$') >= 0;
    }

    /** Replace macros in input
     *
     *  @param macros {@link MacroValueProvider} to use
     *  @param input Text that may contain macros "$(NAME)" or "${NAME}",
     *               also allowing nested "${{INNER}}"
     *  @return Text where all macros have been resolved
     *  @throws Exception on error, including recursive macro that never resolves
     */
    public static String replace(final MacroValueProvider macros, final String input) throws Exception
    {
        // Replace macros, then un-escape escaped dollar signs
        final String replaced = replace(macros, input, 0);
        return replaced.replace("\\$", "$");
    }

    /** Replace macros in input
     *
     *  @param macros {@link MacroValueProvider} to use
     *  @param input Text that may contain macros "$(NAME)" or "${NAME}",
     *               also allowing nested "${{INNER}}"
     *  @param from Position within input where replacement should start
     *  @return Text where all macros have been resolved
     *  @throws Exception on error
     */
    private static String replace(final MacroValueProvider macros, final String input,
                                  final int from) throws Exception
    {
        try
        {
            if (recursions.incrementAndGet() > MAX_RECURSION)
                throw new Exception("Recursive macro " + input);
            // Recursion and default values:
            // Default values provide a possible way to resolve recursive macros. If recursion
            // is detected, the default value could be used.
            // However, with the current implementation, there is no way to recover the original
            // default value. For example, replacing $(S=a) with the macro S=$(S) would throw an
            // error, since the expected default value, "a", is overwritten on the first recursion.

            // Short cut if there is nothing to replace
            if (input.indexOf('$',  from) < 0)
                return input;

            // Find first macro
            final Matcher matcher = spec.matcher(input);
            if (matcher.find(from) == false)
                return input;

            // Was it a $(macro) or ${macro}?
            final int which = matcher.start(1) > 0 ? 1 : 4;

            // Find macro name and default value
            final String name = input.substring(matcher.start(which), matcher.end(which));
            // Find default value between end of "=" group and end of "=y" group
            final String def_val = matcher.end(which + 1) < 0 ? null
                    : input.substring(matcher.end(which + 2), matcher.end(which + 1));

            // Start and end of macro name
            final int start = matcher.start(0);
            final int end = matcher.end(0);

            // Resolve
            final String value = macros.getValue(name);
            if (value != null || def_val != null)
            {
                // Replace macro in input, removing the '$(' resp. ')'
                final String result = input.substring(0, start) + (value != null ? value : def_val) + input.substring(end);
                // Text has now changed.
                // Subsequent calls to find() would return indices for the original text
                // which are no longer valid for the changed text
                // -> Recurse with updated text for next macro,
                //    which also handles nested $($(INNER))
                return replace(macros, result, 0);
            }
            else
            { // Leave macro unresolved, continue with remaining input
                return replace(macros, input, end + 1);
            }
        }
        finally
        {
            recursions.decrementAndGet();
        }
    }
}
