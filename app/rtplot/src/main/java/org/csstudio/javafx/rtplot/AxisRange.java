/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.util.Objects;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

/** Value range of an {@link Axis}
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AxisRange<XTYPE extends Comparable<XTYPE>>
{
    /** Low end of value range. */
    protected XTYPE low;

    /** High end of value range. */
    protected XTYPE high;

    /** @param low Low end of value range
     *  @param high High end of value range
     */
    public AxisRange(final XTYPE low, final XTYPE high)
    {
        this.low = Objects.requireNonNull(low);
        this.high = Objects.requireNonNull(high);
    }

    /** @return Returns low end of axis value range. */
    final public XTYPE getLow()
    {
        return low;
    }

    /** @return Returns high end of axis value range. */
    final public XTYPE getHigh()
    {
        return high;
    }

    /** @param value Value to check
     *  @return <code>true</code> if value is within axis range
     */
    public boolean contains(final XTYPE value)
    {
        final boolean normal = low.compareTo(high) < 0;
        if (normal)
            return low.compareTo(value) <= 0  &&  value.compareTo(high) <= 0;
        return high.compareTo(value) <= 0  &&  value.compareTo(low) <= 0;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof AxisRange))
            return false;
        final AxisRange<XTYPE> other = (AxisRange) obj;
        return other.low.equals(low)  &&
               other.high.equals(high);
    }

    // Should have hashCode() when implementing equals()
    @Override
    public int hashCode()
    {
        final int prime = 31;
        return (prime * high.hashCode()) + low.hashCode();
    }

    @Override
    public String toString()
    {
        return "AxisRange(" + low + ", " + high + ")";
    }
}
