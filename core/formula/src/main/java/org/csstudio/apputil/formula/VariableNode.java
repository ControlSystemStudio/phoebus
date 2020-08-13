/*******************************************************************************
 * Copyright (c) 2010-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Named Variable.
 *  @author Kay Kasemir
 */
public class VariableNode implements Node
{
    /** Name of the variable. */
    final private String name;

    /** Current value of the variable. */
    private volatile VType value;

    /** Create Variable with given name. */
    public VariableNode(final String name)
    {
        this(name, Double.NaN);
    }

    /** Create Variable with given name and value. */
    public VariableNode(final String name, final double value)
    {
        this(name, VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
    }

    /** Create Variable with given name and value. */
    public VariableNode(final String name, final VType value)
    {
        this.name = name;
        this.value = value;
    }

    /** @return Returns the name. */
    final public String getName()
    {
        return name;
    }

    /** @param New value of variable. */
    public void setValue(final double value)
    {
        setValue(VDouble.of(value, Alarm.none(), Time.now(), Display.none()));
    }

    /** @param New value of variable. */
    public void setValue(final VType value)
    {
        this.value = value;
    }

    @Override
    public VType eval()
    {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final Node node)
    {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final String name)
    {
        return this.name.equals(name);
    }

    @Override
    public String toString()
    {
        return name;
    }
}
