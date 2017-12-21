/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import javafx.geometry.Rectangle2D;

/** Listener to changes in the image plot
 *  @author Kay Kasemir
 */
public interface RTImagePlotListener
{
    /** Invoked when the user moves the cursor over the image
     *
     *  <p>If cursor is outside of the image, all values will be <code>Double.NaN</code> resp. <code>-1</code>
     *
     *  @param x Coordinate of cursor position on X axis or <code>Double.NaN</code>
     *  @param y Coordinate of cursor position on Y axis or <code>Double.NaN</code>
     *  @param xi Horizontal coordinate of cursor position in image or <code>-1</code>
     *  @param yi Vertical coordinate of cursor position in image or <code>-1</code>
     *  @param value Pixel value of image at that location or <code>Double.NaN</code>
     */
    default public void changedCursorInfo(double x, double y, int xi, int yi, double value) {}

    /** Invoked when the user positions the crosshair in the image
     *
     *  @param x Coordinate of cursor position on X axis or <code>Double.NaN</code>
     *  @param y Coordinate of cursor position on Y axis or <code>Double.NaN</code>
     */
    default public void changedCrosshair(double x, double y) {}

    /** Invoked when the user moves a region of interest
     *
     *  @param index Index 0, .. of the R.O.I.
     *  @param name Name of the R.O.I.
     *  @param region Region in coordinates of X, Y axes
     */
    default public void changedROI(int index, String name, Rectangle2D region) {}

    /** Invoked when the X axis range was changed by user */
    default public void changedXAxis(double low, double high) {}

    /** Invoked when the Y axis range was changed by user */
    default public void changedYAxis(double low, double high) {}

    /** Invoked when auto scale is enabled or disabled by user interaction
     *  @param autoscale Autoscale on/off
     */
    default public void changedAutoScale(boolean autoscale) {}

    /** Invoked when logarithmic mode is enabled/disabled by user interaction
     *  @param logscale Logarithmic mode on/off
     */
    default public void changedLogarithmic(boolean logscale) {}

    /** Invoked when value range is changed by user interaction
     *  @param minimum Value range start ..
     *  @param maximum .. and end
     */
    default public void changedValueRange(double minimum, double maximum)  {}
}
