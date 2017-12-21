/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.data;

import java.time.Instant;

/** One 'point' of a trace in the plot
 *  @param <XTYPE> Data type used for the position of a sample,
 *                 {@link Double} or {@link Instant}
 *  @author Kay Kasemir
 */
public interface PlotDataItem<XTYPE extends Comparable<XTYPE>>
{
    /** Numeric position or Time stamp of the sample */
    public XTYPE getPosition();

    /** @return Value */
    public double getValue();

    /** @return Standard deviation, or {@link Double#NaN} */
    public double getStdDev();

    /** @return Minimum in case main value is an 'average', or {@link Double#NaN} */
    public double getMin();

    /** @return Maximum in case main value is an 'average', or {@link Double#NaN} */
    public double getMax();

    /** @return Any informational string that might work as e.g. a Tool-tip. */
    public String getInfo();
}
