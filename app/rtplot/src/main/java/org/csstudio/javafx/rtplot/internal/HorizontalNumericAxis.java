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
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** 'X' or 'horizontal' axis for numbers.
 *
 *  <p>'visible' controls whether the labels and tick marks are shown.
 *  Allows having no labels and ticks, yet still show a grid and axis name.
 *
 *  <p>To fully hide axis, set visible and grid to false, and with empty name.
 *
 *  @see TimeAxis
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class HorizontalNumericAxis extends NumericAxis
{
    /** Create axis with label and listener. */
    public HorizontalNumericAxis(final String name, final PlotPartListener listener)
    {
        super(name, listener,
              true,       // Horizontal
              0.0, 10.0); // Initial range
    }

    /** {@inheritDoc} */
    @Override
    public final int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        logger.log(Level.FINE,  "XAxis layout");

        gc.setFont(label_font);
        final int label_size = getName().isEmpty() ?  0 : gc.getFontMetrics().getHeight();
        gc.setFont(scale_font);
        final int scale_size = gc.getFontMetrics().getHeight();

        // Need room for ticks, tick labels, and axis label
        if (isVisible())
            return TICK_LENGTH + label_size + scale_size;
        else
            return label_size;
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D gc, final Rectangle plot_bounds)
    {
        final boolean visible = isVisible();
        if (! visible  &&  !show_grid  && getName().isEmpty())
            return;

        final Rectangle region = getBounds();

        final Stroke old_width = gc.getStroke();
        final Color old_fg = gc.getColor();
        final Color foreground = GraphicsUtils.convert(getColor());
        gc.setColor(foreground);
        gc.setFont(scale_font);

        super.paint(gc);

        // Axis and Tick marks
        gc.drawLine(region.x, region.y, region.x + region.width-1, region.y);

        computeTicks(gc);

        // Major tick marks
        Rectangle avoid = null;
        for (MajorTick<Double> tick : ticks.getMajorTicks())
        {
            final int x = getScreenCoord(tick.getValue());
            if (visible)
            {
                gc.setStroke(TICK_STROKE);
                gc.drawLine(x, region.y, x, region.y + TICK_LENGTH - 1);
            }

            // Grid line
            if (show_grid)
            {   // Dashed line
                gc.setColor(grid_color);
                gc.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[] { 5 }, 0));
                gc.drawLine(x, plot_bounds.y, x, plot_bounds.y + plot_bounds.height-1);
                gc.setColor(foreground);
            }
            gc.setStroke(old_width);

            // Tick Label
            if (visible)
                avoid = drawTickLabel(gc, x, tick.getLabel(), false, avoid);
        }

        // Minor tick marks
        if (visible)
            for (MinorTick<Double> tick : ticks.getMinorTicks())
            {
                final int x = getScreenCoord(tick.getValue());
                gc.drawLine(x, region.y, x, region.y + TICK_LENGTH - 1);
            }

        // Label: centered at bottom of region
        gc.setFont(label_font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, getName());
        gc.drawString(getName(),
                      region.x + (region.width - metrics.width)/2,
                      region.y + metrics.y + region.height - metrics.height);
        gc.setColor(old_fg);
    }

    /** @param gc
     *  @param screen_y Screen location of label along the axis
     *  @param mark Label text
     *  @param floating Add 'floating' box?
     *  @param avoid Outline of previous label to avoid
     *  @return Outline of this label or the last one if skipping this label
     */
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
        gc.drawString(mark, tx, region.y + metrics.y + TICK_LENGTH);
        return outline;
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Double tick)
    {
        final int x = getScreenCoord(tick);
        final String mark = ticks.formatDetailed(tick);
        drawTickLabel(gc, x, mark, true, null);
    }
}
