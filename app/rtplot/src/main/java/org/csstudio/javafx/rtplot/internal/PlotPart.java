/*******************************************************************************
 * Copyright (c) 2014-2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Objects;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

import javafx.scene.paint.Color;

/** Base for all parts of the {@link Plot}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PlotPart
{
    /** Debug flag:
     *  Enables a dummy thread for each plot part
     *  that uses CPU and triggers bogus refreshes
     *  of the part.
     */
    final private static boolean debug = false;
    private long debug_runs = 0;

    private volatile String name;

    private volatile Color color = Color.BLACK;

    final private PlotPartListener listener;

    /** Screen region occupied by this part */
    private volatile Rectangle bounds = new Rectangle(0, 0, 10, 10);

    /** @param name Part name
     *  @param listener {@link PlotPartListener}, or <code>null</code> if set via <code>setListener</code>
     */
    public PlotPart(final String name, final PlotPartListener listener)
    {
        this.name = name;
        this.listener = Objects.requireNonNull(listener);
        if (debug)
        {
            final Thread debug_thread = new Thread(() ->
            {
                try
                {
                    Thread.sleep(2000);
                    while (true)
                    {
                        for (long busy=0; busy<100000L; ++busy)
                            Thread.sleep(0);
                        ++debug_runs;
                        requestRefresh();
                    }
                }
                catch (InterruptedException ex)
                {
                    ex.printStackTrace();
                }
            }, "PlotPartDebug");
            debug_thread.setDaemon(true);
            debug_thread.start();
        }
    }

    /** @return Part name */
    public String getName()
    {
        return name;
    }

    /** @return Part name */
    public void setName(final String name)
    {
        Objects.requireNonNull(name);
        synchronized (this)
        {
            if (name.equals(this.name))
                return;
            this.name = name;
        }
        requestLayout();
        requestRefresh();
    }

    /** @return Color to use for this part */
    public Color getColor()
    {
        return color;
    }

    /** @param color Color to use for this part */
    public void setColor(final Color color)
    {
        Objects.requireNonNull(color);
        synchronized (this)
        {
            if (color.equals(this.color))
                return;
            this.color = color;
        }
        requestRefresh();
    }

    /** @param bounds New screen coordinates */
    public void setBounds(final Rectangle bounds)
    {   // Pass on to clone bounds
        setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /** @param x New screen coordinate
     *  @param y New screen coordinate
     *  @param width New screen coordinate
     *  @param height New screen coordinate
     */
    public void setBounds(final int x, final int y, final int width, final int height)
    {
        bounds = new Rectangle(x, y, width, height);
        logger.log(Level.FINER, "setBound({0}) to {1}", new Object[] { getName(), bounds });
    }

    /** @return Screen coordinates */
    public Rectangle getBounds()
    {
        return bounds;
    }

    /** Derived part can call to request re-computation of layout */
    protected void requestLayout()
    {
        listener.layoutPlotPart(this);
    }

    /** Derived part can call to request refresh */
    protected void requestRefresh()
    {
        listener.refreshPlotPart(this);
    }

    /** Invoked to paint the part.
     *
     *  <p>Is invoked on background thread.
     *  <p>Derived part can override, should invoke super.
     *
     *  @param gc {@link Graphics2D} for painting in background thread
     */
    public void paint(final Graphics2D gc)
    {
        if (debug)
        {
            java.awt.Color old_fg = gc.getColor();
            gc.setColor(new java.awt.Color(255, 0, 255));
            final Stroke old_lw = gc.getStroke();
            final int lw = 1;
            gc.setStroke(new BasicStroke(lw));

            gc.drawRect(bounds.x+lw/2, bounds.y+lw/2, bounds.width-lw, bounds.height-lw);

            final String text = name + " " + Long.toString(debug_runs);
            final Rectangle measure = GraphicsUtils.measureText(gc, text);
            // Center
            final int tx = bounds.x + (bounds.width - measure.width) / 2;
            final int ty = bounds.y + (bounds.height - measure.height) / 2;
            gc.drawString(text, tx, ty + measure.y);
            gc.drawRect(tx, ty, measure.width, measure.height);

            gc.setStroke(old_lw);
            gc.setColor(old_fg);
        }
    }
}
