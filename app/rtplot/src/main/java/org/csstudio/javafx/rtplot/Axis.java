/*******************************************************************************
 * Copyright (c) 2014 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/** Public Interface for X and Y axes.
 *
 *  <p>Handles the basic screen-to-value transformation.
 *
 *  @param <T> Data type of this axis
 *  @author Kay Kasemir
 */
public interface Axis<T extends Comparable<T>>
{
    /** @return Axis name */
    public String getName();

    /** @return Axis name */
    public void setName(final String name);

    /** @return Color to use for this axis */
    public Color getColor();

    /** @param color Color to use for this axis */
    public void setColor(final Color color);

    /** @return Font to use for this axis */
    public Font getLabelFont();

    /** @param font Font to use for this axis */
    public void setLabelFont(final Font font);

    /** @return Font to use for this axis */
    public Font getScaleFont();

    /** @param font Font to use for this axis */
    public void setScaleFont(final Font font);

    /** @return <code>true</code> if grid lines are drawn */
    public boolean isGridVisible();

    /** @param visible Should grid be visible? */
    public void setGridVisible(final boolean grid);

    /** @return <code>true</code> if axis is visible */
    public boolean isVisible();

    /** @param visible Should axis be visible? */
    public void setVisible(final boolean visible);

    /** Configure the Axis to auto-scale or not.
     *  <p>
     *  Initial default is <code>false</code>, i.e. no auto-scale.
     *  @return <code>true</code> if autoscale setting was changed
     */
    public boolean setAutoscale(boolean do_autoscale);

    /** @return <code>true</code> if the axis is auto-scaling. */
    public boolean isAutoscale();

    /** Get the screen coordinates of the given value.
     *  <p>
     *  Values are mapped from value to screen coordinates via
     *  'transform', except for infinite values, which get mapped
     *  to the edge of the screen range.
     *
     *  @return Returns the value transformed in screen coordinates.
     */
    public int getScreenCoord(final T value);

    /** @return Returns screen coordinate transformed into a value. */
    public T getValue(final int coord);

    /** @return Returns value range. */
    public AxisRange<T> getValueRange();

    /** Set the new value range.
     *  @param low Low end of range
     *  @param high High end of range
     *  @return <code>true</code> if this actually did something.
     */
    public boolean setValueRange(final T low, final T high);
}
