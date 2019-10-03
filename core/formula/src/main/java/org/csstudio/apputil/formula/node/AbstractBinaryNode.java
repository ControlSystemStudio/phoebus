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

/** Abstract base for binary nodes.
 *  @author Kay Kasemir
 */
abstract class AbstractBinaryNode implements Node
{
    protected final Node left;
    protected final Node right;

    public AbstractBinaryNode(final Node left, final Node right)
    {
        this.left = left;
        this.right = right;
    }

    @Override
    public ListNumber eval()
    {
        final ListNumber a = left.eval();
        final ListNumber b = right.eval();
        final int n = Math.min(a.size(), b.size());
        final double[] result = new double[n];
        for (int i=0; i<n; ++i)
            result[i] = calc(a.getDouble(i), b.getDouble(i));
        return ArrayDouble.of(result);
    }

    abstract protected double calc(double a, double b);

    /** {@inheritDoc} */
    @Override
    final public boolean hasSubnode(final Node node)
    {
        return left == node          || right == node ||
               left.hasSubnode(node) || right.hasSubnode(node);
    }

    /** {@inheritDoc} */
    @Override
    final public boolean hasSubnode(final String name)
    {
        return left.hasSubnode(name) || right.hasSubnode(name);
    }
}
