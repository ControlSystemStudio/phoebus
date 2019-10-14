/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import org.csstudio.apputil.formula.Node;
import org.csstudio.apputil.formula.VTypeHelper;
import org.epics.util.array.ArrayDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;

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
    public VType eval()
    {
        final VType a = left.eval();
        final VType b = right.eval();
        final Alarm alarm = VTypeHelper.highestAlarmOf(a, b);
        final Time time = VTypeHelper.lastestTimeOf(a, b);
        if (VTypeHelper.isNumericArray(a) && VTypeHelper.isNumericArray(b))
        {
            final int n = Math.min(VTypeHelper.getArraySize(a),
                                   VTypeHelper.getArraySize(b));
            final double[] result = new double[n];
            for (int i=0; i<n; ++i)
                result[i] = calc(VTypeHelper.getDouble(a, i),
                                 VTypeHelper.getDouble(b, i));
            return VDoubleArray.of(ArrayDouble.of(result), alarm, time, Display.displayOf(a));
        }
        else if (a instanceof VString  ||  b instanceof VString)
        {
            final String result = VTypeHelper.getString(a) + VTypeHelper.getString(b);
            return VString.of(result, alarm, time);
        }
        else
        {
            final double result = calc(VTypeHelper.getDouble(a),
                                       VTypeHelper.getDouble(b));
            return VDouble.of(result, alarm, time, Display.displayOf(a));
        }
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
