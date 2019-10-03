/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;

/** One computational node.
 *  @author Kay Kasemir
 */
public class ConstantNode implements Node
{
    final ArrayDouble value;

    public ConstantNode(final double value)
    {
        this.value = ArrayDouble.of(value);
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
    public boolean hasSubnode(final String node)
    {
        return false;
    }

    @Override
    public String toString()
    {
        return Double.toString(value.getDouble(0));
    }
}
