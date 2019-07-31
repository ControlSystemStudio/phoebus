/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.awt.BasicStroke;
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
    private Color minor = Color.YELLOW,
                  major = Color.RED;

    private int center_x, center_y;

    private int scale_rx, scale_ry;

    private int start_angle, angle_range;

    private volatile double lolo = Double.NaN,
                            low = Double.NaN,
                            high = Double.NaN,
                            hihi = Double.NaN;

    /** Create scale with label and listener. */
    public MeterScale(final String name, final PlotPartListener listener)
    {
        super(name, listener,
              true,       // 'Horizontal'
              0.0, 10.0); // Initial range
    }

    /** @param minor Minor alarm range (low, high) color
     *  @param major Majow alarm range (lolo, hihi) color
     */
    public void setLimitColors(final Color minor, final Color major)
    {
        this.minor = minor;
        this.major = major;
        requestLayout();
    }

    /** Set alarm limits
     *  @param lolo Really way low
     *  @param low  Somewhat low
     *  @param high A little high, maybe
     *  @param hihi Way, way high
     */
    public void setLimits(final double lolo, final double low,
                          final double high, final  double hihi)
    {
        if (Double.doubleToLongBits(this.lolo) == Double.doubleToLongBits(lolo)  &&
            Double.doubleToLongBits(this.low)  == Double.doubleToLongBits(low)   &&
            Double.doubleToLongBits(this.high) == Double.doubleToLongBits(high)  &&
            Double.doubleToLongBits(this.hihi) == Double.doubleToLongBits(hihi))
            return;
        this.lolo = lolo;
        this.low = low;
        this.high = high;
        this.hihi = hihi;
        requestLayout();
    }

    /** Configure scale layout
     *  @param center_x Needle base, center of scale X
     *  @param center_y .. and Y
     *  @param scale_rx X radius of scale
     *  @param scale_ry Y radius
     *  @param start_angle Start angle in degrees, 0 being 3:00 o'clock, CCW
     *  @param angle_range Range, CCW from start
     */
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
        if (Double.isFinite(value))
        {
            final AxisRange<Double> range = getValueRange();
            return start_angle + (value - range.getLow()) * angle_range / (range.getHigh() - range.getLow());
        }
        else
            return Double.NaN;
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
        gc.setFont(scale_font);

        super.paint(gc);

        computeTicks(gc);

        // Arc along center of scale, between outer and inner edge,
        // for alarm limit sections
        gc.setStroke(new BasicStroke(TICK_LENGTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1.0f));

        double last = start_angle;
        double next = getAngle(lolo);
        if (next < last)
        {
            gc.setColor(major);
            gc.drawArc(center_x-scale_rx+TICK_LENGTH/2,
                       center_y-scale_ry+TICK_LENGTH/2,
                       2*(scale_rx-TICK_LENGTH/2),
                       2*(scale_ry-TICK_LENGTH/2),
                       (int) last, (int) (next - last));
            last = next;
        }
        next = getAngle(low);
        if (next < last)
        {
            gc.setColor(minor);
            gc.drawArc(center_x-scale_rx+TICK_LENGTH/2,
                       center_y-scale_ry+TICK_LENGTH/2,
                       2*(scale_rx-TICK_LENGTH/2),
                       2*(scale_ry-TICK_LENGTH/2),
                       (int) last, (int) (next - last));
            last = next;
        }
        last = start_angle + angle_range;
        next = getAngle(hihi);
        if (next > last)
        {
            gc.setColor(major);
            gc.drawArc(center_x-scale_rx+TICK_LENGTH/2,
                       center_y-scale_ry+TICK_LENGTH/2,
                       2*(scale_rx-TICK_LENGTH/2),
                       2*(scale_ry-TICK_LENGTH/2),
                       (int) next, (int) (last - next));
            last = next;
        }
        next = getAngle(high);
        if (next > last)
        {
            gc.setColor(minor);
            gc.drawArc(center_x-scale_rx+TICK_LENGTH/2,
                       center_y-scale_ry+TICK_LENGTH/2,
                       2*(scale_rx-TICK_LENGTH/2),
                       2*(scale_ry-TICK_LENGTH/2),
                       (int) next, (int) (last - next));
            last = next;
        }

        // Arc around outer edge of scale
        gc.setStroke(old_width);
        gc.setColor(foreground);
        gc.drawArc(center_x-scale_rx,
                   center_y-scale_ry,
                   2*scale_rx,
                   2*scale_ry,
                   start_angle, angle_range);

        // Arc around inner edge of scale
        // gc.drawArc(center_x-scale_rx+TICK_LENGTH,
        //            center_y-scale_ry+TICK_LENGTH,
        //            2*(scale_rx-TICK_LENGTH),
        //            2*(scale_ry-TICK_LENGTH),
        //            start_angle, angle_range);

        // Major tick marks
        Rectangle avoid = null;
        for (MajorTick<Double> tick : ticks.getMajorTicks())
        {
            // (x, y): Where the needle would point for this value
            final double angle = Math.toRadians(getAngle(tick.getValue()));
            final double dx = scale_rx*Math.cos(angle);
            final double dy = scale_ry*Math.sin(angle);
            final int x = (int) (center_x + dx + 0.5);
            final int y = (int) (center_y - dy + 0.5);

            // Scale dx, dy to get TICK_LENGTH
            final double len = Math.sqrt(dx*dx+dy*dy);
            double factor = TICK_LENGTH/len;

            // Draw tick mark from (x, y) to center
            gc.setStroke(TICK_STROKE);
            gc.drawLine(x, y,
                        (int) (x - dx*factor + 0.5),
                        (int) (y + dy*factor + 0.5));
            gc.setStroke(old_width);

            // Tick labels are 3 tick length inward
            factor = 3*TICK_LENGTH/len;
            avoid = drawTickLabel(gc,
                                  (int) (x - dx*factor + 0.5),
                                  (int) (y + dy*factor + 0.5),
                                  tick.getLabel(), avoid);
        }

        // Minor tick marks
        for (MinorTick<Double> tick : ticks.getMinorTicks())
        {
            final double angle = Math.toRadians(getAngle(tick.getValue()));
            final double dx = scale_rx*Math.cos(angle);
            final double dy = scale_ry*Math.sin(angle);
            final int x = (int) (center_x + dx + 0.5);
            final int y = (int) (center_y - dy + 0.5);
            final double len = Math.sqrt(dx*dx+dy*dy);
            final double factor = TICK_LENGTH/len;

            final int x1 = (int) (x - dx*factor + 0.5);
            final int y1 = (int) (y + dy*factor + 0.5);
            gc.drawLine(x, y, x1, y1);
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
