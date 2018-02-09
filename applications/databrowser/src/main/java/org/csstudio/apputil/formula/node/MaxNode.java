/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;

/** One computational node.
 *  @author Kay Kasemir
 */
public class MaxNode implements Node
{
    private final Node args[];

    public MaxNode(final Node args[])
    {
        this.args = args;
    }

    @Override
    public double eval()
    {
        double result = 0.0;
        for (int i = 0; i < args.length; i++)
        {
            final double v = args[i].eval();
            if (i==0  ||  v > result)
                result = v;
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasSubnode(Node node)
    {
        for (Node arg : args)
            if (arg == node || arg.hasSubnode(node))
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
        final StringBuffer b = new StringBuffer("max(");
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
