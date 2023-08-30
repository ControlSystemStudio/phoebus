/*******************************************************************************
 * Copyright (c) 2019-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import static org.csstudio.apputil.formula.Formula.logger;

import java.util.logging.Level;

import org.csstudio.apputil.formula.Node;
import org.csstudio.apputil.formula.spi.FormulaFunction;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Node for evaluating an SPI-provided function
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SPIFuncNode implements Node
{
    final private FormulaFunction function;
    final private Node args[];

    /** Construct node for SPI function.
     *
     *  @param function {@link FormulaFunction}
     *  @param n Argument node
     */
    public SPIFuncNode(final FormulaFunction function, final Node args[])
    {
        this.function = function;
        this.args = args;
        // Should be called with the correct number of arguments
        if (!function.isVarArgs() && args.length != function.getArguments().size())
            throw new IllegalStateException("Wrong number of arguments for " + function.getSignature());
    }

    @Override
    public VType eval()
    {
        // Evaluate all arguments
        final VType arglist[] = new VType[args.length];
        for (int i = 0; i < arglist.length; i++)
            arglist[i] = args[i].eval();

        try
        {
            return function.compute(arglist);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Formula function error for " + this, ex);
            return VDouble.of(Double.NaN, Alarm.disconnected(), Time.now(), Display.none());
        }
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

    @Override
    public String toString()
    {
        final StringBuffer b = new StringBuffer(function.getName());
        b.append("(");
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
