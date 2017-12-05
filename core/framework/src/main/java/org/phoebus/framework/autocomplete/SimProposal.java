/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.framework.autocomplete;

/** Proposal for "sim://..." PVs
 *
 *  <p>Description includes the optional parameters.
 *
 *  <p>When applied to user text,
 *  it will preserve parameters that the user had
 *  already entered.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SimProposal extends Proposal
{
    private final String[] arguments;

    public SimProposal(final String name, final String... arguments)
    {
        super(name);
        this.arguments = arguments;
    }

    @Override
    public String getDescription()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append(value);
        if (arguments.length > 0)
        {
            buf.append('(');
            for (int i=0; i<arguments.length; ++i)
                if (i>0)
                    buf.append(", ").append(arguments[i]);
                else
                    buf.append(arguments[i]);
            buf.append(')');
        }
        return buf.toString();
    }

    @Override
    public String apply(String text)
    {
        // Text could be "sine" or "sim://sine(-10, 10, 2)"
        final int arg_start = text.indexOf('(');
        // No arguments?
        if (arg_start < 0)
            return super.apply(text);

        // Preserve arguments
        return value + text.substring(arg_start);
    }
}
