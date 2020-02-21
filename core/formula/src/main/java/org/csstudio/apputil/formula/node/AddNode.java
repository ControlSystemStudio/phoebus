/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

/** One computational node.
 *  @author Kay Kasemir
 */
public class AddNode extends AbstractBinaryNode
{
    public AddNode(final Node left, final Node right)
    {
        super(left, right);
    }

    @Override
    protected VType calc(final VType a, final VType b, final Alarm alarm, final Time time)
    {
        if (a instanceof VString  ||  b instanceof VString)
        {
            final String result = VTypeHelper.toString(a) + VTypeHelper.toString(b);
            return VString.of(result, alarm, time);
        }
        return super.calc(a, b, alarm, time);
    }

    @Override
    protected double calc(double a, double b)
    {
        return a + b;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "(" + left + " + " + right + ")";
    }
}
