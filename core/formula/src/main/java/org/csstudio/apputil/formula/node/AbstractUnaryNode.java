/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;

/** Abstract base for unary nodes.
 *  @author Kay Kasemir
 */
abstract class AbstractUnaryNode implements Node
{
    protected final Node n;

    public AbstractUnaryNode(final Node n)
    {
        this.n = n;
    }

    /** {@inheritDoc} */
    @Override
    final public boolean hasSubnode(final Node node)
    {
        return n == node  ||  n.hasSubnode(node);
    }

    /** {@inheritDoc} */
    @Override
    final public boolean hasSubnode(final String name)
    {
        return n.hasSubnode(name);
    }
}
