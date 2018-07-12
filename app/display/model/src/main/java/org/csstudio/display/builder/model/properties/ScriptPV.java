/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.util.Objects;

/** Description of one input to a script
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptPV
{
    private final String name;
    private final boolean trigger;

    /** @param name Name of PV
     *  @param trigger Do value updates from this input trigger execution of the script?
     */
    public ScriptPV(final String name, final boolean trigger)
    {
        this.name = Objects.requireNonNull(name);
        this.trigger = trigger;
    }

    /** @param name Name of triggering PV */
    public ScriptPV(final String name)
    {
        this(name, true);
    }

    /** @return Name of PV */
    public String getName()
    {
        return name;
    }

    /** @return Do value updates from this input trigger execution of the script? */
    public boolean isTrigger()
    {
        return trigger;
    }

    @Override
    public String toString()
    {
        if (trigger)
            return "PV '" + name + "'";
        else
            return "PV '" + name + "' (no trigger)";
    }
}
