/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.IllegalPathStateException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.internal.AxisPart;
import org.csstudio.javafx.rtplot.internal.MeterScale;
import org.csstudio.javafx.rtplot.internal.PlotPart;
import org.csstudio.javafx.rtplot.internal.PlotPartListener;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.DoubleBuffer;
import org.phoebus.ui.javafx.UpdateThrottle;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/** Meter with scale and needle
 *
 *  <p>Meter scale is painted in background thread.
 * UI thread adds needle and label, then updates image view.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RTMeter extends ImageView
{
    /** Width of the needle at base */
    private static final int NEEDLE_BASE = 2*AxisPart.TICK_WIDTH;

    /** Colors */
    private volatile Color foreground = Color.BLACK,
                           background = Color.WHITE,
                           needle = Color.RED,
                           knob = Color.GRAY;

    /** Fonts */
    private Font font;

    /** Area of this meter */
    protected volatile Rectangle area = new Rectangle(0, 0, 0, 0);

    /** Listener to {@link PlotPart}s (scale), triggering refresh of meter */
    protected final PlotPartListener plot_part_listener = new PlotPartListener()
    {
        @Override
        public void layoutPlotPart(final PlotPart plotPart)
        {
            if (! in_update)
                requestLayout();
        }

        @Override
        public void refreshPlotPart(final PlotPart plotPart)
        {
            if (! in_update)
                requestUpdate();
        }
    };

    private final MeterScale scale = new MeterScale("", plot_part_listener);

    private volatile double value = 0.0;

    private volatile String label = "";

    /** Suppress updates triggered by scale changes from layout
     *
     *  Calling updateMeterBackground can trigger changes because of layout,
     *  which call the plot_part_listener.
     */
    private volatile boolean in_update = false;

    /** Need full update of layout and scale? */
    protected final AtomicBoolean need_layout = new AtomicBoolean(true);

    /** Meter background (scale)
     *
     *  <p>UpdateThrottle calls updateMeterBackground() to set the image
     *  in its thread, then requests a redraw in UI thread which adds the needle and label.
     */
    private volatile BufferedImage meter_background = null;

    /** Buffers used to create the next background image */
    private final DoubleBuffer background_buffers = new DoubleBuffer();

    /** Throttle updates, enforcing a 'dormant' period, then triggers UI redraw */
    private final UpdateThrottle update_throttle;

    /** Has a call to redraw_runnable already been queued?
     *  Cleared when redraw_runnable is executed
     */
    private final AtomicBoolean pending_redraw = new AtomicBoolean();

    /** (Double) buffer used to combine the meter background with needle */
    private final DoubleBuffer buffers = new DoubleBuffer();

    private WritableImage awt_jfx_convert_buffer = null;

    /** Redraw on UI thread by adding needle to 'meter_background' */
    private final Runnable redraw_runnable = () ->
    {
        // Indicate that a redraw has occurred
        pending_redraw.set(false);

        final BufferedImage copy = meter_background;
        if (copy != null)
        {
            // Create copy of meter background
            if (copy.getType() != BufferedImage.TYPE_INT_ARGB)
                throw new IllegalPathStateException("Need TYPE_INT_ARGB for direct buffer access, not " + copy.getType());
            final int width = copy.getWidth(), height = copy.getHeight();
            final BufferUtil buffer = buffers.getBufferedImage(width, height);
            final BufferedImage combined = buffer.getImage();
            final int[] src  = ((DataBufferInt)     copy.getRaster().getDataBuffer()).getData();
            final int[] dest = ((DataBufferInt) combined.getRaster().getDataBuffer()).getData();
            System.arraycopy(src, 0, dest, 0, width * height);

            // Add needle & label
            final Graphics2D gc = buffer.getGraphics();
            drawValue(gc);

            // Convert to JFX image and show
            if (awt_jfx_convert_buffer == null  ||
                awt_jfx_convert_buffer.getWidth() != width ||
                awt_jfx_convert_buffer.getHeight() != height)
                awt_jfx_convert_buffer = new WritableImage(width, height);
            // SwingFXUtils.toFXImage(combined, image);
            awt_jfx_convert_buffer.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), dest, 0, width);

            setImage(awt_jfx_convert_buffer);
            logger.log(Level.FINE, "Redrew meter");
        }
    };

    public RTMeter()
    {
        // 100ms = 10Hz default throttle
        update_throttle = new UpdateThrottle(100, TimeUnit.MILLISECONDS, () ->
        {
            if (need_layout.getAndSet(false))
            {
                in_update = true;
                final BufferedImage latest = updateMeterBackground();
                in_update = false;
                if (latest == null)
                    // Update failed, request another
                    requestLayout();
                else
                    meter_background = latest;
            }
            if (!pending_redraw.getAndSet(true))
                Platform.runLater(redraw_runnable);
        });
    }

    /** Call to update size of meter
     *
     *  @param width
     *  @param height
     */
    public void setSize(final double width, final double height)
    {
        area = new Rectangle((int)width, (int)height);
        requestLayout();
    }

    /** @param color Forground (labels, tick marks) color */
    public void setForeground(final javafx.scene.paint.Color color)
    {
        foreground = GraphicsUtils.convert(color);
        scale.setColor(color);
    }

    /** @param color Background color */
    public void setBackground(final javafx.scene.paint.Color color)
    {
        background = GraphicsUtils.convert(color);
    }

    /** @param color Needle color */
    public void setNeedle(final javafx.scene.paint.Color color)
    {
        needle = GraphicsUtils.convert(color);
    }

    /** @param color Needle knob color */
    public void setKnob(final javafx.scene.paint.Color color)
    {
        knob = GraphicsUtils.convert(color);
    }

    /** @param minor Minor alarm range (low, high) color
     *  @param major Majow alarm range (lolo, hihi) color
     */
    public void setLimitColors(final javafx.scene.paint.Color minor,
                               final javafx.scene.paint.Color major)
    {
        scale.setLimitColors(GraphicsUtils.convert(minor),
                             GraphicsUtils.convert(major));
    }

    /** @param font Label font */
    public void setFont(javafx.scene.text.Font font)
    {
        scale.setScaleFont(font);
        this.font = GraphicsUtils.convert(font);
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
        scale.setLimits(lolo, low, high, hihi);
    }

    /** @param value Current value */
    public void setValue(final double value, final String label)
    {
        final AxisRange<Double> range = scale.getValueRange();
        if (value < range.low)
            this.value = range.low;
        else if (value > range.high)
            this.value = range.high;
        else
            this.value = value;
        this.label = label;
        requestUpdate();
    }

    /** @param min_val Minimum of value range
     *  @param max_val Maximum of value range
     */
    public void setRange(double min_val, double max_val)
    {
        scale.setValueRange(min_val, max_val);
    }

    /** Request a complete redraw with new layout */
    private void requestLayout()
    {
        need_layout.set(true);
        requestUpdate();
    }

    /** Request a complete update of image */
    private void requestUpdate()
    {
        update_throttle.trigger();
    }

    private void computeLayout(final Graphics2D gc, final Rectangle bounds)
    {
        logger.log(Level.FINE, "computeLayout");

        // Needle origin
        int center_x = bounds.x + bounds.width/2;
        int center_y = bounds.height-NEEDLE_BASE;

        // Start and range of scale
        int start_angle = 160;
        int angle_range = -140;

        // Radius (from origin) of scale
        int scale_rx = bounds.width /2 - AxisPart.TICK_WIDTH;
        int scale_ry = bounds.height - AxisPart.TICK_WIDTH-2*NEEDLE_BASE;

        scale.setBounds(bounds);
        scale.configure(center_x, center_y, scale_rx, scale_ry, start_angle, angle_range);
    }

    /** Draw meter background (scale) into image buffer
     *  @return Latest image, must be of type BufferedImage.TYPE_INT_ARGB
     */
    private BufferedImage updateMeterBackground()
    {
        final Rectangle area_copy = area;
        if (area_copy.width <= 0  ||  area_copy.height <= 0)
            return null;

        final BufferUtil buffer = background_buffers.getBufferedImage(area_copy.width, area_copy.height);
        if (buffer == null)
            return null;
        final BufferedImage image = buffer.getImage();
        final Graphics2D gc = buffer.getGraphics();

        // Really need AA for text to avoid anemic fonts.
        // AA for lines results in some fuzzyness,
        // but also required for any line that's not strictly horizontal or vertical.
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Text AA is implied in general AA
        // gc.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        computeLayout(gc, area_copy);

        gc.setColor(background);
        gc.fillRect(0, 0, area_copy.width, area_copy.height);

        scale.paint(gc, area_copy);

        return image;
    }

    /** Draw needle and label for current value */
    private void drawValue(final Graphics2D gc)
    {
        gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gc.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        gc.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Needle
        final double angle = Math.toRadians(scale.getAngle(value));
        final Stroke orig_stroke = gc.getStroke();
        gc.setStroke(AxisPart.TICK_STROKE);

        final int[] nx = new int[]
        {
            (int) (scale.getCenterX() + scale.getRadiusX() * Math.cos(angle) + 0.5),
            (int) (scale.getCenterX() + NEEDLE_BASE * Math.cos(angle + Math.PI/2) + 0.5),
            (int) (scale.getCenterX() + NEEDLE_BASE * Math.cos(angle - Math.PI/2) + 0.5),
        };
        final int[] ny = new int[]
        {
            (int) (scale.getCenterY() - scale.getRadiusY() * Math.sin(angle) + 0.5),
            (int) (scale.getCenterY() - NEEDLE_BASE * Math.sin(angle + Math.PI/2) + 0.5),
            (int) (scale.getCenterY() - NEEDLE_BASE * Math.sin(angle - Math.PI/2) + 0.5),
        };
        gc.setColor(needle);
        gc.fillPolygon(nx, ny, 3);

        gc.setColor(knob);
        gc.fillOval(scale.getCenterX()-NEEDLE_BASE, scale.getCenterY()-NEEDLE_BASE, 2*NEEDLE_BASE, 2*NEEDLE_BASE);

        gc.setStroke(orig_stroke);

        // Label
        gc.setColor(foreground);
        final Font orig_font = gc.getFont();
        gc.setFont(font);
        final Rectangle metrics = GraphicsUtils.measureText(gc, label);
        final Rectangle area_copy = area;
        final int tx = (area_copy.width - metrics.width)/2;
        final int ty = 2*(area_copy.height + metrics.height)/3;
        gc.drawString(label, tx, ty);
        gc.setFont(orig_font);
    }

    /** Should be invoked when meter no longer used to release resources */
    public void dispose()
    {
        // Release memory ASAP
        update_throttle.dispose();
        meter_background = null;
    }
}
