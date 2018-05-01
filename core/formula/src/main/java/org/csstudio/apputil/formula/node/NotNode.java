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
public class NotNode extends AbstractUnaryNode
{
    public NotNode(final Node n)
    {
        super(n);
    }

    @Override
    public double eval()
    {
        final double a = n.eval();
        return (a != 0) ? 0.0 : 1.0;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "! (" + n + ")";
    }
}
