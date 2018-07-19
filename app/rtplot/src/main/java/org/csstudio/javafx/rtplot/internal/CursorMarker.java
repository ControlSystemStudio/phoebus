/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** Mark where a trace crosses the cursor.
 *
 *  <p>Markers sort by y-position, and are painted in order
 *  with a certain gap between them.
 *
 *  @author Davy Dequidt - Original org.csstudio.swt.xygraph.figures.HoverLabels
 *  @author Kay Kasemir
 */
class CursorMarker implements Comparable<CursorMarker>
{
    /** Border around the marker's text */
    final private static int BORDER = 3;

    /** Size of the 'arrow' from point to text */
    final private static int ARROW = 20;

    /** Number of markers to shuffle to avoid overlap */
    final private static int MAX_SHUFFLE = 2;

    final private int x, y;
    final private Color rgb;
    final private String label;

    /** @param x Pixel position
     *  @param y Pixel position
     *  @param color Color
     *  @param label Label
     */
    public CursorMarker(final int x, final int y, final Color color, final String label)
    {
        this.x = x;
        this.y = y;
        this.rgb = color;
        this.label = label;
    }

    // Comparable
    @Override
    public int compareTo(final CursorMarker other)
    {
        return Integer.compare(y, other.y);
    }

    /** @param gc GC
     *  @param markers {@link CursorMarker}s to draw
     *  @param bounds
     */
    public static void drawMarkers(final Graphics2D gc, final List<CursorMarker> markers, final Rectangle bounds)
    {
        int height = 10; // Non-zero guess, updated as markers are drawn
        int last_y = -1;
        int moved = 0;
        for (CursorMarker mark : markers)
        {
            // 'y' of markers is sorted low .. high
            int y = mark.y;
            // If marker overlaps last one, try to move it down, but not too often
            if (last_y >= 0  &&  last_y + height >= y)
            {
                if (++moved <= MAX_SHUFFLE)
                    y = last_y + height;
                else
                    continue;
            }
            else // At least one fit without shuffle, reset 'moved' count
                moved = 0;
            final int mark_height = drawMark(gc, y, mark, bounds);
            height = Math.max(height, mark_height + 2 * BORDER);
            last_y = y;
        }
    }

    private static int drawMark(final Graphics2D gc, final int y, final CursorMarker mark, final Rectangle bounds)
    {
    	final Rectangle metrics = GraphicsUtils.measureText(gc, mark.label);
    	final int dir = (mark.x + ARROW + metrics.width + BORDER <= bounds.width) ? 1 : -1;
    	final int[] outline_x = new int[]
        {
            mark.x,
            mark.x + dir * ARROW,
            mark.x + dir *(ARROW + metrics.width + BORDER),
            mark.x + dir *(ARROW + metrics.width + BORDER),
            mark.x + dir * ARROW,
        };
        final int[] outline_y = new int[]
        {
            mark.y,
            y - metrics.height/2 - BORDER,
            y - metrics.height/2 - BORDER,
            y + metrics.height/2 + BORDER,
            y + metrics.height/2 + BORDER,
        };

        final Color orig_fill = gc.getColor();

        gc.setColor(gc.getBackground());
        gc.fillPolygon(outline_x, outline_y, 5);

        gc.setColor(mark.rgb);
        gc.drawPolygon(outline_x, outline_y, 5);
        if (dir > 0)
            gc.drawString(mark.label, mark.x + ARROW, y - metrics.height/2 + metrics.y);
        else
            gc.drawString(mark.label, mark.x - ARROW - metrics.width, y - metrics.height/2 + metrics.y);

        gc.setColor(orig_fill);

        return metrics.height;
    }
}