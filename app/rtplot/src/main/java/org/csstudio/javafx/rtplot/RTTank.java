/*******************************************************************************
 * Copyright (c) 2014-2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.javafx.rtplot.internal.PlotPart;
import org.csstudio.javafx.rtplot.internal.PlotPartListener;
import org.csstudio.javafx.rtplot.internal.YAxisImpl;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

/** Tank with scale
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RTTank extends Canvas
{
    /** Area of this canvas */
    protected volatile Rectangle area = new Rectangle(0, 0, 0, 0);

    /** Background color */
    private volatile Color background = Color.WHITE;

    /** Fill color */
    private volatile Color empty = Color.LIGHT_GRAY.brighter().brighter();
    private volatile Color empty_shadow = Color.LIGHT_GRAY;

    /** Fill color */
    private volatile Color fill = Color.BLUE;
    private volatile Color fill_highlight = new Color(72, 72, 255);

    /** Current value, i.e. fill level */
    private volatile double value = 5.0;

    /** Does layout need to be re-computed? */
    protected final AtomicBoolean need_layout = new AtomicBoolean(true);

    /** Throttle updates, enforcing a 'dormant' period */
    private final UpdateThrottle update_throttle;

    /** Buffer for image of the tank and scale */
    private volatile Image plot_image = null;

    /** Is the scale displayed or not. */
    private volatile boolean scale_visible = true;

    /** Listener to {@link PlotPart}s, triggering refresh of canvas */
    protected final PlotPartListener plot_part_listener = new PlotPartListener()
    {
        @Override
        public void layoutPlotPart(final PlotPart plotPart)
        {
            need_layout.set(true);
        }

        @Override
        public void refreshPlotPart(final PlotPart plotPart)
        {
            requestUpdate();
        }
    };

    /** Redraw the canvas on UI thread by painting the 'plot_image' */
    final private Runnable redraw_runnable = () ->
    {
        final GraphicsContext gc = getGraphicsContext2D();
        final Image image = plot_image;
        if (image != null)
            synchronized (image)
            {
                gc.drawImage(image, 0, 0);
            }
    };

    final private YAxisImpl<Double> scale = new YAxisImpl<>("", plot_part_listener);

    final private PlotPart plot_area = new PlotPart("main", plot_part_listener);



    /** Constructor */
    public RTTank()
    {
        final ChangeListener<? super Number> resize_listener = (prop, old, value) ->
        {
            area = new Rectangle((int)getWidth(), (int)getHeight());
            need_layout.set(true);
            requestUpdate();
        };
        widthProperty().addListener(resize_listener);
        heightProperty().addListener(resize_listener);

        // 50Hz default throttle
        update_throttle = new UpdateThrottle(50, TimeUnit.MILLISECONDS, () ->
        {
            plot_image = updateImageBuffer();
            redrawSafely();
        });
    }

    /** Update the dormant time between updates
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setUpdateThrottle(final long dormant_time, final TimeUnit unit)
    {
        update_throttle.setDormantTime(dormant_time, unit);
    }

    /** @param font Scale font */
    public void setFont(final Font font)
    {
        scale.setLabelFont(font);
        scale.setScaleFont(font);
    }

    /** @param color Background color */
    public void setBackground(final javafx.scene.paint.Color color)
    {
        background = GraphicsUtils.convert(Objects.requireNonNull(color));
    }

    /** @param color Foreground color */
    public void setForeground(final javafx.scene.paint.Color color)
    {
        scale.setColor(color);
    }

    /** @param color Color for empty region */
    public void setEmptyColor(final javafx.scene.paint.Color color)
    {
        empty = GraphicsUtils.convert(Objects.requireNonNull(color));
        empty_shadow = new Color(
                Math.max(0, empty.getRed()   - 32),
                Math.max(0, empty.getGreen() - 32),
                Math.max(0, empty.getBlue()  - 32),
                empty.getAlpha()
            );
    }

    /** @param color Color for filled region */
    public void setFillColor(final javafx.scene.paint.Color color)
    {
        fill = GraphicsUtils.convert(Objects.requireNonNull(color));
        final int saturationContribution = (int) ( 48.f * Color.RGBtoHSB(fill.getRed(), fill.getGreen(), fill.getBlue(), null)[1] );
        fill_highlight = new Color(
            Math.min(255, fill.getRed()   + 32 + saturationContribution),
            Math.min(255, fill.getGreen() + 32 + saturationContribution),
            Math.min(255, fill.getBlue()  + 32 + saturationContribution),
            empty.getAlpha()
        );
    }

    /** @param visible Whether the scale must be displayed or not. */
    public void setScaleVisible (boolean visible)
    {
        if (visible != scale_visible)
        {
            scale_visible = visible;
            need_layout.set(true);
        }
    }

    /** Set value range
     *  @param low
     *  @param high
     */
    public void setRange(final double low, final double high)
    {
        scale.setValueRange(low, high);
    }

    /** @param value Set value */
    public void setValue(final double value)
    {
        if (Double.isFinite(value))
            this.value = value;
        else
            this.value = scale.getValueRange().getLow();
        requestUpdate();
    }

    /** Compute layout of plot components */
    private void computeLayout(final Graphics2D gc, final Rectangle bounds)
    {
        final Rectangle scale_region = new Rectangle(bounds);
        scale_region.width = scale.getDesiredPixelSize(scale_region, gc);

        final int[] ends = scale.getPixelGaps(gc);
        scale_region.y += ends[1];
        scale_region.height -= ends[0] + ends[1];

        scale.setBounds(scale_region);

        if (scale_visible)
            plot_area.setBounds(bounds.x + scale_region.width, bounds.y+ends[1], bounds.width-scale_region.width, bounds.height-ends[0]-ends[1]);
        else
            plot_area.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    /** Draw all components into image buffer */
    protected Image updateImageBuffer()
    {
        final Rectangle area_copy = area;
        if (area_copy.width <= 0  ||  area_copy.height <= 0)
            return null;

        final BufferUtil buffer = BufferUtil.getBufferedImage(area_copy.width, area_copy.height);
        if (buffer == null)
            return null;
        final BufferedImage image = buffer.getImage();
        final Graphics2D gc = buffer.getGraphics();

        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        if (need_layout.getAndSet(false))
            computeLayout(gc, area_copy);

        final Rectangle plot_bounds = plot_area.getBounds();

        gc.setColor(background);
        gc.fillRect(0, 0, area_copy.width, area_copy.height);

        if (scale_visible)
            scale.paint(gc, plot_bounds);

        plot_area.paint(gc);

        final AxisRange<Double> range = scale.getValueRange();
        final boolean normal = range.getLow() <= range.getHigh();
        final double min = Math.min(range.getLow(), range.getHigh());
        final double max = Math.max(range.getLow(), range.getHigh());
        final double current = value;
        final int level;
        if (current <= min)
            level = 0;
        else if (current >= max)
            level = plot_bounds.height;
        else if (max == min)
            level = 0;
        else
            level = (int) (plot_bounds.height * (current - min) / (max - min) + 0.5);

        final int arc = Math.min(plot_bounds.width, plot_bounds.height) / 10;
        gc.setPaint(new GradientPaint(plot_bounds.x, 0, empty, plot_bounds.x+plot_bounds.width/2, 0, empty_shadow, true));

        gc.fillRoundRect(plot_bounds.x, plot_bounds.y, plot_bounds.width, plot_bounds.height, arc, arc);

        gc.setPaint(new GradientPaint(plot_bounds.x, 0, fill, plot_bounds.x+plot_bounds.width/2, 0, fill_highlight, true));
        if (normal)
            gc.fillRoundRect(plot_bounds.x, plot_bounds.y+plot_bounds.height-level, plot_bounds.width, level, arc, arc);
        else
            gc.fillRoundRect(plot_bounds.x, plot_bounds.y, plot_bounds.width, level, arc, arc);
        gc.setColor(background);

        gc.dispose();

        // Convert to JFX
        return SwingFXUtils.toFXImage(image, null);
    }

    /** Request a complete redraw of the plot */
    final public void requestUpdate()
    {
        update_throttle.trigger();
    }

    /** Redraw the current image and cursors
     *  <p>May be called from any thread.
     */
    final void redrawSafely()
    {
        Platform.runLater(redraw_runnable);
    }

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {   // Stop updates which could otherwise still use
        // what's about to be disposed
        update_throttle.dispose();
    }
}
