/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.util;

import java.time.Instant;

/** Linear Transformation from {@link Instant} x1..x2 into y1..y2 range.
 *
 *  @author Kay Kasemir
 */
public class TimeScreenTransform implements ScreenTransform<Instant>
{
    // Compare to LinearScreenTransform

    /** Linear conversion parameters, initialized for 1:1 */
    private double a = 1.0, b = 0.0;

    /** {@inheritDoc} */
    @Override
    public synchronized void config(final Instant i1, final Instant i2, final double y1, final double y2)
    {
        final double x1 = i1.getEpochSecond() + 1e-9*i1.getNano();
        final double x2 = i2.getEpochSecond() + 1e-9*i2.getNano();
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
    public synchronized double transform(final Instant i)
    {
        final double x = i.getEpochSecond() + 1e-9*i.getNano();
        return a*x + b;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Instant inverse(double y)
    {
        final double x = (y-b)/a;
        final int seconds = (int) x;
        final int nano = (int) ((x - seconds) * 1e9);
        return Instant.ofEpochSecond(seconds, nano);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized TimeScreenTransform copy()
    {
        final TimeScreenTransform result = new TimeScreenTransform();
        result.a = a;
        result.b = b;
        return result;
    }
}
