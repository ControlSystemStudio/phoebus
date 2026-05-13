/*******************************************************************************
 * Copyright (c) 2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;

/** Modulo
 *  @author Kay Kasemir
 */
public class ModNode extends AbstractBinaryNode
{
    /**
     * Constructor
     * @param left , left node
     * @param right , right node
     */
	public ModNode(final Node left, final Node right)
    {
        super(left, right);
    }

    @Override
    protected double calc(final double a, final double b)
    {
        return a%b;
    }

    @Override
    public String toString()
    {
        return "(" + left + " % " + right + ")";
    }
}
