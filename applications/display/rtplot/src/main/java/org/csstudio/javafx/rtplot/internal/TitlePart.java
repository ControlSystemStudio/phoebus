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
import java.awt.Graphics2D;
import java.awt.Rectangle;

import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

/** Part for plot title
 *  @author Kay Kasemir
 */
public class TitlePart extends PlotPart
{
    /** @param name Part name
     *  @param listener {@link PlotPartListener}, or <code>null</code> if set via <code>setListener</code>
     */
    public TitlePart(final String name, final PlotPartListener listener)
    {
        super(name, listener);
    }

    /** @param gc
     *  @param font
     *  @return Desired height in pixels
     */
    public int getDesiredHeight(final Graphics2D gc, final Font font)
    {
        final String text = getName();
        if (text.isEmpty())
            return 0;
        return gc.getFontMetrics(font).getHeight();
    }

    public void paint(final Graphics2D gc, final Font font)
    {
        final String text = getName();
        if (text.isEmpty())
            return;

        final Font orig_font = gc.getFont();
        gc.setFont(font);
        super.paint(gc);
        
        final Color old_fg = gc.getColor();
        gc.setColor(GraphicsUtils.convert(getColor()));

        final Rectangle bounds = getBounds();
        final Rectangle metrics = GraphicsUtils.measureText(gc, text);
        
        final int tx = bounds.x + (bounds.width - metrics.width) / 2;
        final int ty = bounds.y + metrics.y + (bounds.height - metrics.height) / 2;
        gc.drawString(text, tx, ty);
        gc.setColor(old_fg);
        gc.setFont(orig_font);
    }
}
