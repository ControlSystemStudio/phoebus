/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.phoebus.framework.macros.MacroHandler;
import org.phoebus.framework.macros.MacroValueProvider;
import org.phoebus.framework.macros.Macros;

/** Stack of macros, allows pushing new values and popping back to previous macros
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MacroContext implements MacroValueProvider
{
    /** Map of macro names to values */
    final private Stack<Map<String, String>> stack = new Stack<>();

    /** Initialize
     *  @param names_and_values String of the form "macro=value, macro=value"
     *  @throws Exception on malformed input
     */
    public MacroContext(final String names_and_values) throws Exception
    {
        pushMacros(names_and_values);
    }

    /** Add new macros, replacing macros of same name
     *  @param names_and_values String of the form "macro=value, macro=value"
     *  @throws Exception on malformed input
     *  @see #popMacros()
     */
    public void pushMacros(final String names_and_values) throws Exception
    {
        final Map<String, String> macros = new HashMap<>();
        if (! stack.isEmpty())
            macros.putAll(stack.peek());

        final Macros added = Macros.fromSimpleSpec(names_and_values);
        added.forEach((name, value) -> macros.put(name, value));

        stack.push(macros);
    }

    /** Restore macros as they were before last <code>push</code>
     *  @see #pushMacros(String)
     *  @throws IllegalStateException is <code>push</code> has not been called
     */
    public void popMacros()
    {
        if (stack.size() > 1)
            stack.pop();
        else
            throw new IllegalStateException("No macros have been pushed");
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(final String name)
    {
        return stack.peek().get(name);
    }

    /** @param text Text that may contain "$(macro)"
     *  @return Text where macros have been replaced by their values
     *  @throws Exception on error in macros
     */
    public String resolveMacros(final String text) throws Exception
    {
        return MacroHandler.replace(this, text);
    }

    /** @return String representation for debugging */
    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        int level = 0;
        for (Map<String, String> macros : stack)
        {
            buf.append(level + ": ");
            final String names[] = macros.keySet().toArray(new String[macros.size()]);
            boolean first = true;
            for (String name: names)
            {
                if (first)
                    first = false;
                else
                    buf.append(", ");
                buf.append(name + "=\"" + macros.get(name) + "\"");
            }
            ++level;
            if (level < stack.size())
                buf.append("\n");
        }
        return buf.toString();
    }
}