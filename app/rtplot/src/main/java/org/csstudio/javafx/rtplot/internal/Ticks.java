/*******************************************************************************
 * Copyright (c) 2010-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.List;

import org.csstudio.javafx.rtplot.data.PlotDataItem;

/** Tick marks of an X or Y Axis.
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public abstract class Ticks<XTYPE>
{
    protected volatile List<MajorTick<XTYPE>> major_ticks = Collections.emptyList();
    protected volatile List<MinorTick<XTYPE>> minor_ticks = Collections.emptyList();

    /** How many percent of the available space should be used for labels? */
    final public static int FILL_PERCENTAGE = 60;

    /** Used to check if a requested new range
     *  can be handled
     *  @param low Desired low limit of the axis range.
     *  @param high Desired high limit of the axis range.
     *  @return <code>true</code> if that range can be handled,
     *          <code>false</code> if that range should be avoided.
     */
    public abstract boolean isSupportedRange(XTYPE low, XTYPE high);

    /** Compute tick information.
     *
     *  @param low Low limit of the axis range.
     *  @param high High limit of the axis range.
     *  @param gc GC for determining width of labels.
     *  @param screen_width Width of axis on screen.
     */
    public abstract void compute(XTYPE low, XTYPE high, Graphics2D gc, int screen_width);

    /** @return Major tick marks */
    public final List<MajorTick<XTYPE>> getMajorTicks()
    {
        return major_ticks;
    }

    /** @return Minor tick marks */
    public final List<MinorTick<XTYPE>> getMinorTicks()
    {
        return minor_ticks;
    }

    /** @return Returns the tick formatted as text. */
    public abstract String format(XTYPE tick);

    /** @return Returns the tick formatted as text, using the next higher detail format. */
    public abstract String formatDetailed(XTYPE tick);
}
