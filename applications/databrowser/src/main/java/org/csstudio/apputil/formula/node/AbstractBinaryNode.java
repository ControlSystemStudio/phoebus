/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;

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
