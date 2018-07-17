/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

/** Logarithmic Transformation from {@link Double} x1..x2 into y1..y2 range.
 *  @author Kay Kasemir
 */
public class LogScreenTransform implements ScreenTransform<Double>
{
    private double base;
    final private LinearScreenTransform linear;

    public LogScreenTransform()
    {
        this(new LinearScreenTransform());
    }

    private LogScreenTransform(final LinearScreenTransform linear)
    {
        this.linear = linear;
    }

    /** {@inheritDoc} */
    @Override
    public void config(final Double x1, final Double x2, final double y1, final double y2)
    {
        base = y1;
        linear.config(Log10.log10(x1), Log10.log10(x2), y1, y2);
    }

    /** {@inheritDoc} */
    @Override
    public double transform(final Double x)
    {
        if (x <= 0)
            return base;
        return linear.transform(Log10.log10(x));
    }

    /** {@inheritDoc} */
    @Override
    public Double inverse(final double y)
    {
        return Log10.pow10(linear.inverse(y));
    }

    /** {@inheritDoc} */
    @Override
    public LogScreenTransform copy()
    {
        return new LogScreenTransform(linear.copy());
    }
}
