/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;

/** Named Variable.
 *  @author Kay Kasemir
 */
public class VariableNode implements Node
{
    /** Name of the variable. */
    final private String name;

    /** Current value of the variable. */
    private ListNumber value;

    /** Create Variable with given name. */
    public VariableNode(final String name)
    {
        this(name, Double.NaN);
    }

    /** Create Variable with given name and value. */
    public VariableNode(final String name, final double value)
    {
        this(name, ArrayDouble.of(value));
    }

    /** Create Variable with given name and value. */
    public VariableNode(final String name, final ListNumber value)
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
        setValue(ArrayDouble.of(value));
    }

    /** @param New value of variable. */
    public void setValue(final ListNumber value)
    {
        this.value = value;
    }

    /** @return Returns the value. */
    public ListNumber getValue()
    {
        return value;
    }

    @Override
    public ListNumber eval()
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
