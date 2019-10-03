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
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ListNumber;

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
    public ListNumber eval()
    {
        // Evaluate all arguments, get common element count
        final ListNumber[] v = new ListNumber[args.length];
        int n = 0;
        for (int i = 0; i < args.length; i++)
        {
            v[i] = args[i].eval();
            if (i==0  ||  v[i].size() < n)
                n = v[i].size();
        }
        if (n == 0)
            return ArrayDouble.of();

        // results[e] = method(arg1[e], arg2[e], ...)
        final double[] results = new double[n];
        final Object arglist[] = new Object[args.length];
        for (int e=0; e<n; ++e)
        {
            for (int i = 0; i < args.length; i++)
                arglist[i] = Double.valueOf(v[i].getDouble(e));
            try
            {
                final Object result = method.invoke(null, arglist);
                if (result instanceof Number)
                    results[e] = ((Number) result).doubleValue();
                else
                    throw new Exception("Expected number, got " + result);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Formula math function error for " + this, ex);
                results[e] = Double.NaN;
            }
        }

        return ArrayDouble.of(results);
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
