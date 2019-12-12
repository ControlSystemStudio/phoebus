/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

/** Major tick mark
 *  @author Kay Kasemir
 *  @param <XTYPE> Data type used for the {@link Ticks}
 */
@SuppressWarnings("nls")
public class MajorTick<XTYPE> extends MinorTick<XTYPE>
{
    final private String label;

    public MajorTick(final XTYPE value, final String label)
    {
        super(value);
        this.label = label;
    }

    final public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return "MajorTick(" + value + ", '" + label + "')";
    }
}