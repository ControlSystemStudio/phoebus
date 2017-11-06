/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

/** Linear Transformation from {@link Double} x1..x2 into y1..y2 range.
 *  @author Kay Kasemir
 */
public class LinearScreenTransform implements ScreenTransform<Double>
{
    /** Linear conversion parameters, initialized for 1:1 */
    private double a = 1.0, b = 0.0;

    /** {@inheritDoc} */
    @Override
    public synchronized void config(final Double x1, final Double x2, final double y1, final double y2)
    {
        final double d = x2 - x1;
        if (d != 0.0)
        {
            a = (y2 - y1) / d;
            b = (x2*y1 - x1*y2) / d;
            if (a != 0.0)
                return;
        }
        a = 1.0;
        b = 0.0;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized double transform(final Double x)
    {
        return a*x + b;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Double inverse(final double y)
    {
        return (y-b)/a;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized LinearScreenTransform copy()
    {
        final LinearScreenTransform result = new LinearScreenTransform();
        result.a = a;
        result.b = b;
        return result;
    }
}
