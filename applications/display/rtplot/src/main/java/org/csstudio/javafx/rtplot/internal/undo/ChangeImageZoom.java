/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal.undo;

import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.Messages;
import org.phoebus.ui.undo.UndoableAction;

/** Un-doable action to modify value range of axes
 *  @author Kay Kasemir
 */
public class ChangeImageZoom  extends UndoableAction
{
    final private Axis<Double> x_axis, y_axis;
    final private AxisRange<Double> original_x_range, new_x_range,
                                    original_y_range, new_y_range;

    /** @param x_axis X Axis or <code>null</code>
     *  @param original_x_range Original ..
     *  @param new_x_range .. and new X range, or <code>null</code>
     *  @param y_axis Y Axis or <code>null</code>
     *  @param original_y_range Original ..
     *  @param new_y_range .. and new Y range, or <code>null</code>
     */
    public ChangeImageZoom(
            final Axis<Double> x_axis,
            final AxisRange<Double> original_x_range,
            final AxisRange<Double> new_x_range,
            final Axis<Double> y_axis,
            final AxisRange<Double> original_y_range,
            final AxisRange<Double> new_y_range)
    {
    	this(Messages.Zoom_In, x_axis, original_x_range, new_x_range, y_axis, original_y_range, new_y_range);
    }
    /** @param name Name of undo action
     *  @param x_axis X Axis or <code>null</code>
     *  @param original_x_range Original ..
     *  @param new_x_range .. and new X range, or <code>null</code>
     *  @param y_axis Y Axis or <code>null</code>
     *  @param original_y_range Original ..
     *  @param new_y_range .. and new Y range, or <code>null</code>
     */
    public ChangeImageZoom(
    		final String name,
            final Axis<Double> x_axis,
            final AxisRange<Double> original_x_range,
            final AxisRange<Double> new_x_range,
            final Axis<Double> y_axis,
            final AxisRange<Double> original_y_range,
            final AxisRange<Double> new_y_range)
    {
        super(name);
        this.x_axis = x_axis;
        this.original_x_range = original_x_range;
        this.new_x_range = new_x_range;
        this.y_axis = y_axis;
        this.original_y_range = original_y_range;
        this.new_y_range = new_y_range;
    }

    @Override
    public void run()
    {
        if (x_axis != null)
            x_axis.setValueRange(new_x_range.getLow(), new_x_range.getHigh());
        if (y_axis != null)
            y_axis.setValueRange(new_y_range.getLow(), new_y_range.getHigh());
    }

    @Override
    public void undo()
    {
        if (x_axis != null)
            x_axis.setValueRange(original_x_range.getLow(), original_x_range.getHigh());
        if (y_axis != null)
            y_axis.setValueRange(original_y_range.getLow(), original_y_range.getHigh());
    }
}
