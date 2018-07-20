/*******************************************************************************
 * Copyright (c) 2010-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.TimeScreenTransform;

/** 'X' or 'horizontal' axis for time stamps.
 *  @see HorizontalNumericAxis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TimeAxis extends AxisPart<Instant>
{
    /** Create axis with label and listener. */
    public static TimeAxis forDuration(final String name, final PlotPartListener listener,
            final Duration duration)
    {
        final Instant end = Instant.now();
        final Instant start = end.minus(duration);
        return new TimeAxis(name, listener, start, end);
    }

    /** Create axis with label and listener. */
    public TimeAxis(final String name, final PlotPartListener listener,
            final Instant start, final Instant end)
    {
        super(name, listener,
              true, // Horizontal
              start, end,
              new TimeScreenTransform(),
              new TimeTicks());
    }

    /** {@inheritDoc} */
    @Override
    public final int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        logger.log(Level.FINE, "TimeAxis({0}) layout for {1}", new Object[] { getName(),  region });

        gc.setFont(label_font);
        final int label_size = gc.getFontMetrics().getHeight();
        gc.setFont(scale_font);
        final int scale_size = gc.getFontMetrics().getHeight();
        // Need room for ticks, two tick labels, and axis label
        // Plus a few pixels space at the bottom.
        return TICK_LENGTH + 2*scale_size + (getName().isEmpty() ? 0 : label_size);
    }

    /** {@inheritDoc} */
    @Override
    public void zoom(final int center, final double factor)
    {
        final Instant fixed = getValue(center);
        final Instant new_low = fixed.minus(scaledDuration(range.getLow(), fixed, factor));
        final Instant new_high = fixed.plus(scaledDuration(fixed, range.getHigh(), factor));
        setValueRange(new_low, new_high);
    }

    /** Scale Duration by floating point number
     *  @param start Start of a duration
     *  @param end End of a duration
     *  @param factor Scaling factor
     *  @return Scaled Duration
     */
    final private static Duration scaledDuration(final Instant start, final Instant end, final double factor)
    {
        final Duration duration = Duration.between(start, end);
        final double scaled = (duration.getSeconds() + 1e-9*duration.getNano()) * factor;
        final int seconds = (int)scaled;
        final int nano = (int) ((scaled - seconds) * 1e9);
        return Duration.ofSeconds(seconds, nano);
    }

    /** {@inheritDoc} */
    @Override
    public void pan(final AxisRange<Instant> original_range, final Instant t1, final Instant t2)
    {
        final Instant low = original_range.getLow();
        final Instant high = original_range.getHigh();
        final Duration shift = Duration.between(t2, t1);
        setValueRange(low.plus(shift), high.plus(shift));
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D gc, final Rectangle plot_bounds)
    {
        if (! isVisible())
            return;

        super.paint(gc);
        final Rectangle region = getBounds();

        final Stroke old_width = gc.getStroke();
        final Color old_fg = gc.getColor();
        final Color foreground = GraphicsUtils.convert(getColor());
        gc.setColor(foreground);
        gc.setFont(scale_font);

        // Simple line for the axis
        gc.drawLine(region.x, region.y, region.x + region.width-1, region.y);

        // Axis and Tick marks
        computeTicks(gc);

        Rectangle avoid = null;
        for (MajorTick<Instant> tick : ticks.getMajorTicks())
        {
            final int x = getScreenCoord(tick.getValue());

            // Major tick marks
            gc.setStroke(TICK_STROKE);
            gc.drawLine(x, region.y, x, region.y + TICK_LENGTH);

            // Grid line
            if (show_grid)
            {   // Dashed line
                gc.setColor(grid_color);
                gc.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] { 2 }, 0));
                gc.drawLine(x, plot_bounds.y, x, region.y-1);
                gc.setColor(foreground);
            }
            gc.setStroke(old_width);

            // Tick Label
            avoid = drawTickLabel(gc, x, tick.getLabel(), false, avoid);
        }

        for (MinorTick<Instant> tick : ticks.getMinorTicks())
        {
            final int x = getScreenCoord(tick.getValue());
            gc.drawLine(x, region.y, x, region.y + MINOR_TICK_LENGTH);
        }

        if (! getName().isEmpty())
        {   // Label: centered at bottom of region
            gc.setFont(label_font);
            final Rectangle metrics = GraphicsUtils.measureText(gc, getName());
            GraphicsUtils.drawMultilineText(gc,
                         region.x + (region.width - metrics.width)/2,
                         region.y + region.height - metrics.height + metrics.y - 1,
                         getName());
        }

        gc.setColor(old_fg);
    }

    private Rectangle drawTickLabel(final Graphics2D gc, final int x, final String mark, final boolean floating, final Rectangle avoid)
    {
        final Rectangle region = getBounds();
        gc.setFont(scale_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, mark);
        int tx = x - metrics.width/2;
        // Correct location of rightmost label to remain within region
        if (tx + metrics.width > region.x + region.width)
            tx = region.x + region.width - metrics.width;

        final Rectangle outline = new Rectangle(tx-BORDER, region.y + TICK_LENGTH-BORDER, metrics.width+2*BORDER, metrics.height+2*BORDER);
        if (floating)
        {
            gc.drawLine(x, region.y, x, region.y + TICK_LENGTH);
            gc.clearRect(outline.x, outline.y, outline.width, outline.height);
            gc.drawRect(outline.x, outline.y, outline.width, outline.height);
        }

        if (avoid != null  &&  outline.intersects(avoid))
            return avoid;
        // Debug: Outline of text
        // gc.drawRect(tx, region.y + TICK_LENGTH, metrics.width, metrics.height);
        GraphicsUtils.drawMultilineText(gc, tx, region.y + TICK_LENGTH + metrics.y, mark);
        return outline;
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Instant tick)
    {
        final int x = getScreenCoord(tick);
        final String mark = ticks.formatDetailed(tick);
        drawTickLabel(gc, x, mark, true, null);
    }
}
