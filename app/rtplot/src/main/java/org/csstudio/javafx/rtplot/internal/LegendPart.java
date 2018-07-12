/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** Plot part for legend.
 *
 *  <p>Lists trace names.
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kunal Shroff - Original Composite/Label based LegendHandler
 *  @author Kay Kasemir
 */
public class LegendPart<XTYPE extends Comparable<XTYPE>> extends PlotPart
{
    private volatile boolean visible = true;

    private volatile int grid_x = 100, grid_y = 15, base_offset = 0;

    public LegendPart(String name, PlotPartListener listener)
    {
        super(name, listener);
    }

    /** @return <code>true</code> if legend is visible */
    public boolean isVisible()
    {
        return visible;
    }

    /** @param show <code>true</code> if legend should be displayed */
    public void setVisible(final boolean show)
    {
        visible = show;
    }

    /** Compute height
     *  @param gc
     *  @param bounds_width
     *  @param font
     *  @param traces
     *  @return Desired height in pixels
     */
    public int getDesiredHeight(final Graphics2D gc, final int bounds_width,
                                final Font font, final List<Trace<XTYPE>> traces )
    {
        if (! visible)
            return 0;

        // Determine largest legend entry
        final Font orig_font = gc.getFont();
        gc.setFont(font);
        final FontMetrics metrics = gc.getFontMetrics();
        base_offset = metrics.getLeading() + metrics.getAscent();
        final int max_height = metrics.getHeight();

        int max_width = 1; // Start with 1 pixel to avoid later div-by-0
        for (Trace<XTYPE> trace : traces)
        {
            final int width = metrics.stringWidth(trace.getLabel());
            if (width > max_width)
                max_width = width;
        }
        // Arrange in grid with some extra space
        grid_x = max_width + max_height / 2;
        grid_y = max_height;

        gc.setFont(orig_font);

        final int items = traces.size();
        final int items_per_row = Math.max(1, bounds_width / grid_x); // Round down, counting full items
        final int rows = (items + items_per_row-1) / items_per_row;   // Round up
        return rows * grid_y;
    }

    /** Paint the legend
     *  @param gc
     *  @param font
     *  @param traces
     */
    public void paint(final Graphics2D gc, final Font font,
                      final List<Trace<XTYPE>> traces)
    {
        if (! visible)
            return;

        final Rectangle bounds = getBounds();
        // Anything to draw?
        if (bounds.height <= 0)
        	return;

        final Color orig_color = gc.getColor();
        final Font orig_font = gc.getFont();
        gc.setFont(font);
        super.paint(gc);

        int x = bounds.x, y = bounds.y + base_offset;
        for (Trace<XTYPE> trace : traces)
        {
			gc.setColor(GraphicsUtils.convert(trace.getColor()));
            gc.drawString(trace.getLabel(), x, y);
            x += grid_x;
            if (x > bounds.width - grid_x)
            {
                x = bounds.x;
                y += grid_y;
            }
        }
        gc.setFont(orig_font);
        gc.setColor(orig_color);
    }
}
