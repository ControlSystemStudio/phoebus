/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.apputil.formula.node;

import java.lang.reflect.Method;

import org.csstudio.apputil.formula.Node;

/** Node for evaluating any of the java.lang.Math.* functions
 *  @author Xiaosong Geng
 *  @author Kay Kasemir
 */
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
    public double eval()
    {
        final Object arglist[] = new Object[args.length];
        for (int i = 0; i < args.length; i++)
        {
            arglist[i] = Double.valueOf(args[i].eval());
        }

        try
        {
            Object result = method.invoke(null, arglist );
            if (result instanceof Double)
                return ((Double) result).doubleValue();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0.0;
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
    @SuppressWarnings("nls")
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
