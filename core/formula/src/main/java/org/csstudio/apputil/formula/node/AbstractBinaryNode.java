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
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;

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
    public VType eval()
    {
        final VType a = left.eval();
        final VType b = right.eval();
        final Alarm alarm = VTypeHelper.highestAlarmOf(a, b);
        final Time time = VTypeHelper.lastestTimeOf(a, b);
        return calc(a, b, alarm, time);
    }

    /** Default implementation of binary `a X b` operation,
     *  calling a per-element `calc(double, double)` for numeric arrays.
     *
     *  <p>Derived class can add handling for strings etc.
     *
     *  @param a First {@link VType}
     *  @param b Second {@link VType}
     *  @param alarm Highest {@link Alarm}
     *  @param time Latest {@link Time}
     *  @return {@link VType} for result
     */
    protected VType calc(final VType a, final VType b, final Alarm alarm, final Time time)
    {
        if (VTypeHelper.isNumericArray(a) && VTypeHelper.isNumericArray(b))
        {
            final int n = Math.min(VTypeHelper.getArraySize(a),
                                   VTypeHelper.getArraySize(b));
            final double[] result = new double[n];
            for (int i=0; i<n; ++i)
                result[i] = calc(VTypeHelper.toDouble(a, i),
                                 VTypeHelper.toDouble(b, i));
            return VDoubleArray.of(ArrayDouble.of(result), alarm, time, Display.displayOf(a));
        }
        else
        {
            final double result = calc(VTypeHelper.toDouble(a),
                                       VTypeHelper.toDouble(b));
            return VDouble.of(result, alarm, time, Display.displayOf(a));
        }
    }

    /** Scalar (or element-wise) operation to implement by derived class
     *
     *  @param a First value
     *  @param b second value
     *  @return Result
     */
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
