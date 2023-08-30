/*******************************************************************************
 * Copyright (c) 2022 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.macros;

/** Macro provider that falls back to Java system properties or environment variables
 *  @author Kay Kasemir
 */
public class MacroOrSystemProvider implements MacroValueProvider
{
    final private MacroValueProvider macros;

    /** @param macros Base macros */
    public MacroOrSystemProvider(final MacroValueProvider macros)
    {
        this.macros = macros;
    }

    /** Get value for macro
     *  @param name Name of the macro
     *  @return Value of the macro or <code>null</code> if not defined
     */
    @Override
    public String getValue(final String name)
    {
        String value = macros.getValue(name);
        if (value != null)
            return value;

        // Fall back to Java system properties
        value = System.getProperty(name);
        if (value != null)
            return value;

        // Finally, fall back to environment variables
        return System.getenv(name);
    }
}