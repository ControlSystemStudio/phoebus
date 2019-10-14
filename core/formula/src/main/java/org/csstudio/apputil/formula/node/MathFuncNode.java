/*******************************************************************************
 * Copyright (c) 2010-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import static org.csstudio.apputil.formula.Formula.logger;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.csstudio.apputil.formula.Node;
import org.csstudio.apputil.formula.VTypeHelper;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VType;

/** Node for evaluating any of the java.lang.Math.* functions
 *  @author Xiaosong Geng
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MathFuncNode implements Node
{
    final private String function;
    final private Node args[];
    final private Method method;

    /** Construct node for math function.
     *
     *  @param function One of the java.lang.Math.* method names
     *  @param n Argument node
     *  @throws Exception On error
     */
    @SuppressWarnings("rawtypes")
    public MathFuncNode(final String function, final Node args[]) throws Exception
    {
        this.function = function;
        this.args = args;
        Class argcls[] = new Class[args.length];
        for (int i = 0; i < args.length; i++)
            argcls[i] = double.class;
        method = Math.class.getDeclaredMethod(function, argcls);
    }

    @Override
    public VType eval()
    {
        // Evaluate all arguments
        final Object arglist[] = new Object[args.length];
        for (int i = 0; i < args.length; i++)
        {
            final VType v = args[i].eval();
            arglist[i] = Double.valueOf(VTypeHelper.toDouble(v));
        }

        double value;
        try
        {
            final Object result = method.invoke(null, arglist);
            if (result instanceof Number)
                value = ((Number) result).doubleValue();
            else
                throw new Exception("Expected number, got " + result);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Formula math function error for " + this, ex);
            value = Double.NaN;
        }

        return VDouble.of(value, Alarm.none(), Time.now(), Display.none());
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
        final StringBuffer b = new StringBuffer(function);
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
