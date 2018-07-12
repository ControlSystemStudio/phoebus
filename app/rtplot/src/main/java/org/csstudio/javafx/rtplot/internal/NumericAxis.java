/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.internal.util.LinearScreenTransform;
import org.csstudio.javafx.rtplot.internal.util.Log10;
import org.csstudio.javafx.rtplot.internal.util.LogScreenTransform;

/** Base for a numeric axis
 *  @author Kay Kasemir
 */
public abstract class NumericAxis extends AxisPart<Double>
{
    protected NumericAxis(final String name, final PlotPartListener listener,
            final boolean horizontal, final Double low_value, final Double high_value)
    {
        super(name, listener, horizontal, low_value, high_value,
              new LinearScreenTransform(), new LinearTicks());
    }

    /** {@inheritDoc} */
    public void setLogarithmic(boolean use_log)
    {
        if (use_log == isLogarithmic())
            return;
        if (use_log)
            updateScaling(new LogScreenTransform(), new LogTicks());
        else
            updateScaling(new LinearScreenTransform(), new LinearTicks());
    }

    /** @return <code>true</code> if the axis is logarithmic. */
    public boolean isLogarithmic()
    {
        return ! (transform instanceof LinearScreenTransform);
    }

    /** @param order_of_magnitude If value range exceeds this threshold, use exponential notation */
    public void setExponentialThreshold(long order_of_magnitude)
    {
        final Ticks<Double> safe_ticks = ticks;
        if (! (safe_ticks instanceof LinearTicks))
            return;

        ((LinearTicks)safe_ticks).setExponentialThreshold(order_of_magnitude);

        dirty_ticks = true;
        requestLayout();
    }

    /** {@inheritDoc} */
    @Override
    public void zoom(final int center, final double factor)
    {
        if (isLogarithmic())
        {
            final double fixed = Log10.log10(getValue(center));
            final double new_low  = fixed - (fixed - Log10.log10(getValueRange().getLow())) * factor;
            final double new_high = fixed + (Log10.log10(getValueRange().getHigh()) - fixed) * factor;
            setValueRange(Log10.pow10(new_low), Log10.pow10(new_high));
        }
        else
        {
            final double fixed = getValue(center);
            final double new_low  = fixed - (fixed - getValueRange().getLow()) * factor;
            final double new_high = fixed + (getValueRange().getHigh() - fixed) * factor;
            setValueRange(new_low, new_high);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void pan(final AxisRange<Double> original_range, final Double start, final Double end)
    {
        if (isLogarithmic())
        {
            final double shift = Log10.log10(end) - Log10.log10(start);
            final double low = Log10.log10(original_range.getLow());
            final double high = Log10.log10(original_range.getHigh());
            setValueRange(Log10.pow10(low - shift), Log10.pow10(high - shift));
        }
        else
        {
            final double shift = end - start;
            setValueRange(original_range.getLow() - shift, original_range.getHigh() - shift);
        }
    }
}
