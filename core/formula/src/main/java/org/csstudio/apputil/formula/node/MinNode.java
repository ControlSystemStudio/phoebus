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
public class MinNode implements Node
{
    private final Node args[];

    public MinNode(final Node args[])
    {
        this.args = args;
    }

    @Override
    public ListNumber eval()
    {
        if (args.length <= 0)
            return ArrayDouble.of();

        /// Evaluate each argument, find common element count
        final ListNumber[] v = new ListNumber[args.length];
        int n = 0;
        for (int i = 0; i < args.length; i++)
        {
            v[i] = args[i].eval();
            if (i==0  ||  v[i].size() < n)
                n = v[i].size();
        }
        if (n <= 0)
            return ArrayDouble.of();

        // Compute result[e] = min(v_i[e])
        final double[] result = new double[n];
        for (int e=0; e<n; ++e)
            for (int i = 0; i < args.length; i++)
                if (i==0 || v[i].getDouble(e) < result[e])
                    result[e] = v[i].getDouble(e);
        return ArrayDouble.of(result);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final Node node)
    {
        for (Node arg : args)
            if (arg == node  ||  arg.hasSubnode(node))
                return true;
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(final String name)
    {
        for (Node arg : args)
            if (arg.hasSubnode(name))
                return true;
        return false;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        final StringBuffer b = new StringBuffer("min(");
        for (int i = 0; i < args.length; i++)
        {
            if (i>0)
                b.append(", ");
            b.append(args[i].toString());
        }
        b.append(")");
        return b.toString();
    }
}
