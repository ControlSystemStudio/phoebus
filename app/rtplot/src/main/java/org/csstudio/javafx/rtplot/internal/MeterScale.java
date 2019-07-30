/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** 'Round' numeric scale for a meter.
 *  @author Kay Kasemir
 */
public class MeterScale extends NumericAxis
{
    private int center_x, center_y;

    private int scale_rx, scale_ry;

    private int start_angle, angle_range;

    /** Create scale with label and listener. */
    public MeterScale(final String name, final PlotPartListener listener)
    {
        super(name, listener,
              true,       // 'Horizontal'
              0.0, 10.0); // Initial range
    }

    public void configure(final int center_x, final int center_y,
                          final int scale_rx, final int scale_ry,
                          final int start_angle, final int angle_range)
    {
        this.center_x = center_x;
        this.center_y = center_y;
        this.scale_rx = scale_rx;
        this.scale_ry = scale_ry;
        this.start_angle = start_angle;
        this.angle_range = angle_range;
        dirty_ticks = true;
        requestLayout();
    }

    public int getCenterX()
    {
        return center_x;
    }

    public int getCenterY()
    {
        return center_y;
    }

    public int getRadiusX()
    {
        return scale_rx;
    }

    public int getRadiusY()
    {
        return scale_ry;
    }

    public int getStartAngle()
    {
        return start_angle;
    }

    public int getAngleRange()
    {
        return angle_range;
    }

    public double getAngle(final double value)
    {
        final AxisRange<Double> range = getValueRange();
        return start_angle + (value - range.getLow()) * angle_range / (range.getHigh() - range.getLow());
    }

    @Override
    public final int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        // Not used
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D gc, final Rectangle plot_bounds)
    {
        if (! isVisible())
            return;

        final Rectangle region = getBounds();

        final Stroke old_width = gc.getStroke();
        final Color old_fg = gc.getColor();
        final Color foreground = GraphicsUtils.convert(getColor());
        gc.setColor(foreground);
        gc.setFont(scale_font);

        super.paint(gc);

        computeTicks(gc);

        // Scale
        // gc.drawArc(center_x - scale_rx,
        //            center_y - scale_ry,
        //            2*scale_rx,    2*scale_ry,
        //            start_angle, angle_range);

        // Major tick marks
        Rectangle avoid = null;
        for (MajorTick<Double> tick : ticks.getMajorTicks())
        {
            final double angle = Math.toRadians(getAngle(tick.getValue()));
            gc.setStroke(TICK_STROKE);
            gc.drawLine((int) (center_x + (scale_rx-TICK_LENGTH)*Math.cos(angle) + 0.5),
                        (int) (center_y - (scale_ry-TICK_LENGTH)*Math.sin(angle) + 0.5),
                        (int) (center_x + (scale_rx)*Math.cos(angle) + 0.5),
                        (int) (center_y - (scale_ry)*Math.sin(angle) + 0.5));

            gc.setStroke(old_width);

            // Tick Label
            final int cx = (int) (center_x + (scale_rx-3*TICK_LENGTH)*Math.cos(angle) + 0.5);
            final int cy = (int) (center_y - (scale_ry-3*TICK_LENGTH)*Math.sin(angle) + 0.5);
            avoid = drawTickLabel(gc, cx, cy, tick.getLabel(), avoid);
        }

        // Minor tick marks
        for (MinorTick<Double> tick : ticks.getMinorTicks())
        {
            final double angle = Math.toRadians(getAngle(tick.getValue()));
            gc.drawLine((int) (center_x + (scale_rx-TICK_LENGTH)*Math.cos(angle) + 0.5),
                        (int) (center_y - (scale_ry-TICK_LENGTH)*Math.sin(angle) + 0.5),
                        (int) (center_x + (scale_rx)*Math.cos(angle) + 0.5),
                        (int) (center_y - (scale_ry)*Math.sin(angle) + 0.5));
        }

        // Label: centered
        gc.setFont(label_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, getName());
        gc.drawString(getName(),
                      region.x + (region.width - metrics.width)/2,
                      region.y + (metrics.y + region.height - metrics.height)/2);
        gc.setColor(old_fg);
    }

    /** @param gc
     *  @param cx Screen location for center of label
     *  @param cy Screen location for center of label
     *  @param mark Label text
     *  @param avoid Outline of previous label to avoid
     *  @return Outline of this label or the last one if skipping this label
     */
    private Rectangle drawTickLabel(final Graphics2D gc, final int cx, final int cy,
                                    final String mark, final Rectangle avoid)
    {
        gc.setFont(scale_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, mark);
        int tx = cx - metrics.width/2;
        int ty = cy - metrics.height/2;

        final Rectangle outline = new Rectangle(tx, ty, metrics.width, metrics.height);
        if (avoid != null  &&  outline.intersects(avoid))
            return avoid;
        // Debug: Outline of text
        // gc.drawRect(tx, ty, metrics.width, metrics.height);
        gc.drawString(mark, tx, ty+metrics.height);
        return outline;
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Double tick)
    {
        // NOP
    }
}
