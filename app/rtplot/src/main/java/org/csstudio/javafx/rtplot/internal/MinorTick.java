/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

/** Minor tick mark
 *  @author Kay Kasemir
 *  @param <XTYPE> Data type used for the {@link Ticks}
 */
@SuppressWarnings("nls")
public class MinorTick<XTYPE>
{
    final protected XTYPE value;

    public MinorTick(final XTYPE value)
    {
        this.value = value;
    }

    final public XTYPE getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "MinorTick(" + value + ")";
    }
}