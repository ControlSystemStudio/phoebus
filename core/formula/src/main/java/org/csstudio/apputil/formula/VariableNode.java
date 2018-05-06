/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula;

/** Named Variable.
 *  @author Kay Kasemir
 */
public class VariableNode implements Node
{
    /** Name of the variable. */
    final private String name;

    /** Current value of the variable. */
    private double value;

    /** Create Variable with given name. */
    public VariableNode(final String name)
    {
        this.name = name;
        this.value = Double.NaN;
    }

    /** Create Variable with given name and value. */
    public VariableNode(final String name, final double value)
    {
        this.name = name;
        this.value = value;
    }

    /** @return Returns the name. */
    final public String getName()
    {
        return name;
    }

    /** Set to a new value. */
    public void setValue(final double value)
    {
        this.value = value;
    }

    /** @return Returns the value. */
    public double getValue()
    {
        return value;
    }

    @Override
    public double eval()
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
