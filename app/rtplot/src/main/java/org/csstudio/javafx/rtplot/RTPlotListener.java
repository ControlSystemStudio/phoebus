/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.HorizontalNumericAxis;

/** Listener to changes in the plot
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
public interface RTPlotListener<XTYPE extends Comparable<XTYPE>>
{
    /** Invoked when the X axis range has changed */
    default public void changedXAxis(Axis<XTYPE> x_axis) {};

    /** Invoked when the Y axis range has changed */
    default public void changedYAxis(YAxis<XTYPE> y_axis) {};

    /** Invoked when auto scale is enabled or disabled by user interaction */
    default public void changedAutoScale(Axis<?> axis) {};

    /** Invoked when grid is enabled/disabled by user interaction */
    default public void changedGrid(Axis<?> axis) {};

    /** Invoked when logarithmic mode is enabled/disabled by user interaction
     *  @param axis {@link YAxis} or {@link HorizontalNumericAxis} with changed log setting
     */
    default public void changedLogarithmic(Axis<?> axis) {};

    /** Invoked when a PlotMarker has been moved
     *  @param index Index 0, .. of the {@link PlotMarker}
     */
    default public void changedPlotMarker(int index) {};

    /** Invoked when Annotations have been changed */
    default public void changedAnnotations() {};

    /** Invoked when Cursors changed */
    default public void changedCursors() {};

    /** Invoked when toolbar displayed/hidden */
    default public void changedToolbar(boolean visible) {};

    /** Invoked when legend displayed/hidden */
    default public void changedLegend(boolean visible) {}
}
