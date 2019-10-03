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
public class IfNode implements Node
{
    private final Node cond;
    private final Node yes;
    private final Node no;

    public IfNode(final Node cond, final Node yes, final Node no)
    {
        this.cond = cond;
        this.yes = yes;
        this.no = no;
    }

    @Override
    public ListNumber eval()
    {
        final ListNumber c = cond.eval();
        final ListNumber yv = yes.eval();
        final ListNumber nv = no.eval();
        final int n = Math.min(c.size(), Math.min(yv.size(), nv.size()));
        final double[] result = new double[n];
        for (int i=0; i<n; ++i)
            result[i] = c.getByte(i) != 0 ? yv.getDouble(i) : nv.getDouble(i);
        return ArrayDouble.of(result);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final Node node)
    {
        return cond == node  ||  yes == node  ||  no == node ||
               cond.hasSubnode(node) || yes.hasSubnode(node) ||
               no.hasSubnode(node);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final String name)
    {
        return cond.hasSubnode(name) || yes.hasSubnode(name) ||
               no.hasSubnode(name);
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "(" + cond + ") ? (" + yes + ") : (" + no + ")";
    }

}
