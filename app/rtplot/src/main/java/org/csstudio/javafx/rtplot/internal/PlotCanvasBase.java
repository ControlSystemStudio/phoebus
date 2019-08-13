/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
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
import java.awt.RenderingHints;
import java.awt.geom.IllegalPathStateException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.csstudio.javafx.rtplot.Activator;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.DoubleBuffer;
import org.phoebus.ui.javafx.UpdateThrottle;
import org.phoebus.ui.undo.UndoableActionManager;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

/** Base for plots
 *
 *  <p>Based on an {@link ImageView}.
 *  Container needs to call <code>setSize</code>.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract class PlotCanvasBase extends ImageView
{
    // Implementation used to be based on JFX Canvas,
    // drawing the plot's image and then adding cursor mode feedback.
    // JFX canvas, however, can queue up rendering requests,
    // including multiple copies of to-be-rendered images,
    // which quickly exhausts memory for large plots.
    //
    // Using reflection to check the Canvas.current and its internal
    // buffer one can warn about this.
    // Calling canvas.getGraphicsContext2D().clearRect(0, 0, width, height)
    // will help to shrink the rendering queue IF it's being processed
    // ( http://stackoverflow.com/questions/18097404/how-can-i-free-canvas-memory ).
    //
    // Overall, however, ImageView avoids memory issues because it
    // only holds a reference to the current image.

    protected static final int ARROW_SIZE = 8;

    protected static final double ZOOM_FACTOR = 1.2;

    /** When using 'rubberband' to zoom in, need to select a region
     *  at least this wide resp. high.
     *  Smaller regions are likely the result of an accidental
     *  click-with-jerk, which would result into a huge zoom step.
     */
    protected static final int ZOOM_PIXEL_THRESHOLD = 5;

    /** Strokes used for mouse feedback */
    protected static final BasicStroke MOUSE_FEEDBACK_BACK = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER),
                                       MOUSE_FEEDBACK_FRONT = new BasicStroke(1);

    /** Support for un-do and re-do */
    protected final UndoableActionManager undo = new UndoableActionManager(50);

    /** Area of this plot */
    protected volatile Rectangle area = new Rectangle(0, 0, 0, 0);

    /** Suppress updates triggered by axis changes from layout or autoscale
     *
     *  Calling updateImageBuffer can trigger axis changes because of layout
     *  or autoscale, which call the plot_part_listener.
     */
    private volatile boolean in_update = false;

    /** Does layout need to be re-computed? */
    protected final AtomicBoolean need_layout = new AtomicBoolean(true);

    /** Does plot image to be re-created? */
    protected final AtomicBoolean need_update = new AtomicBoolean(true);

    /** Throttle updates, enforcing a 'dormant' period */
    private final UpdateThrottle update_throttle;

    /** Buffer for image and color bar
     *
     *  <p>UpdateThrottle calls updateImageBuffer() to set the image
     *  in its thread, then redrawn in UI thread.
     */
    private volatile BufferedImage plot_image = null;

    /** Listener to {@link PlotPart}s, triggering refresh of plot */
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
            if (! in_update)
                requestUpdate();
        }
    };

    /** (Double) buffer used to combine the plot with mouse feedback overlays */
    private final DoubleBuffer buffers = new DoubleBuffer();

    /** Has a call to redraw_runnable already been queued?
     *  Cleared when redraw_runnable is executed
     */
    private final AtomicBoolean pending_redraw = new AtomicBoolean();

    private WritableImage awt_jfx_convert_buffer = null;

    /** Debug option to show update performance */
    private static final boolean show_updates = Boolean.parseBoolean(System.getProperty("org.csstudio.javafx.rtplot.update_counter"));
    private long update_counter = 0, last_counter = 0, next_rate_update = 0;
    private double update_rate = 0;

    /** Redraw the plot on UI thread by painting the 'plot_image' */
    private final Runnable redraw_runnable = () ->
    {
        // Indicate that a redraw has occurred
        pending_redraw.set(false);

        final BufferedImage copy = plot_image;
        if (copy != null)
        {
            // Create copy of basic plot
            if (copy.getType() != BufferedImage.TYPE_INT_ARGB)
                throw new IllegalPathStateException("Need TYPE_INT_ARGB for direct buffer access, not " + copy.getType());
            final int width = copy.getWidth(), height = copy.getHeight();
            final BufferUtil buffer = buffers.getBufferedImage(width, height);
            final BufferedImage combined = buffer.getImage();
            final int[] src  = ((DataBufferInt)     copy.getRaster().getDataBuffer()).getData();
            final int[] dest = ((DataBufferInt) combined.getRaster().getDataBuffer()).getData();
            System.arraycopy(src, 0, dest, 0, width * height);

            // Add mouse mode feedback
            final Graphics2D gc = buffer.getGraphics();
            gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gc.setColor(Color.BLACK);
            drawMouseModeFeedback(gc);

            if (show_updates)
            {   // Add update info to lower left corner of image
                ++update_counter;
                final long now = System.currentTimeMillis();
                if (now > next_rate_update)
                {
                    final long diff = update_counter - last_counter;
                    update_rate = (update_rate * 5.0 + diff) / 6.0;
                    last_counter = update_counter;
                    next_rate_update = now + 1000;
                }
                final String text = String.format("%d (%.1f Hz)", update_counter, update_rate);
                gc.setPaint(Color.WHITE);
                gc.drawString(text, 1, height-2);
                gc.setPaint(Color.BLACK);
                gc.drawString(text, 2, height-3);
            }

            // Convert to JFX image and show
            if (awt_jfx_convert_buffer == null  ||
                awt_jfx_convert_buffer.getWidth() != width ||
                awt_jfx_convert_buffer.getHeight() != height)
                awt_jfx_convert_buffer = new WritableImage(width, height);
            // SwingFXUtils.toFXImage(combined, image);
            awt_jfx_convert_buffer.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), dest, 0, width);

            setImage(awt_jfx_convert_buffer);
        }
    };

    protected MouseMode mouse_mode = MouseMode.NONE;
    protected Optional<Point2D> mouse_start = Optional.empty();
    protected volatile Optional<Point2D> mouse_current = Optional.empty();

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     */
    protected PlotCanvasBase(final boolean active)
    {
        // 200ms = 5Hz default throttle
        update_throttle = new UpdateThrottle(200, TimeUnit.MILLISECONDS, () ->
        {
            if (need_update.getAndSet(false))
            {
                in_update = true;
                final BufferedImage latest = updateImageBuffer();
                in_update = false;
                if (latest == null)
                    // Update failed, request another
                    requestUpdate();
                else
                    plot_image = latest;
            }
            if (!pending_redraw.getAndSet(true))
                Platform.runLater(redraw_runnable);
        }, Activator.thread_pool);

        if (active)
        {
            setOnMouseEntered(this::mouseEntered);
            setOnScroll(this::wheelZoom);
        }
    }

    /** Call to update size of plot
     *
     *  @param width
     *  @param height
     */
    public void setSize(final double width, final double height)
    {
      area = new Rectangle((int)width, (int)height);
      need_layout.set(true);
      requestUpdate();
    }

    /** @return {@link UndoableActionManager} for this plot */
    public UndoableActionManager getUndoableActionManager()
    {
        return undo;
    }

    /** Update the dormant time between updates
     *  @param dormant_time How long throttle remains dormant after a trigger
     *  @param unit Units for the dormant period
     */
    public void setUpdateThrottle(final long dormant_time, final TimeUnit unit)
    {
        update_throttle.setDormantTime(dormant_time, unit);
    }

    /** Request a complete redraw of the plot with new layout */
    final public void requestLayout()
    {
        need_layout.set(true);
        need_update.set(true);
        update_throttle.trigger();
    }

    /** Request a complete update of plot image */
    final public void requestUpdate()
    {
        need_update.set(true);
        update_throttle.trigger();
    }

    /** Request redraw of current image and cursors */
    final void requestRedraw()
    {
        update_throttle.trigger();
    }

    /** Draw all components into image buffer
     *  @return Latest image, must be of type BufferedImage.TYPE_INT_ARGB
     */
    protected abstract BufferedImage updateImageBuffer();

    protected abstract void drawMouseModeFeedback(Graphics2D gc);

    /** Draw the zoom indicator for a horizontal zoom, i.e. on an X axis
     *
     *  @param gc GC to use
     *  @param plot_bounds Plot area where to draw the zoom indicator
     *  @param start Initial mouse position
     *  @param current Current mouse position
     */
    protected void drawZoomXMouseFeedback(final Graphics2D gc, final Rectangle plot_bounds, final Point2D start, final Point2D current)
    {
        final int left = (int) Math.min(start.getX(), current.getX());
        final int right = (int) Math.max(start.getX(), current.getX());
        final int width = right - left;
        final int mid_y = plot_bounds.y + plot_bounds.height / 2;

        for (int i=0; i<2; ++i)
        {
            if (i==0)
            {
                gc.setColor(java.awt.Color.WHITE);
                gc.setStroke(MOUSE_FEEDBACK_BACK);
            }
            else
            {
                gc.setColor(java.awt.Color.BLACK);
                gc.setStroke(MOUSE_FEEDBACK_FRONT);
            }
            // Range on axis
            gc.drawRect(left, (int)start.getY(), width, 1);
            // Left, right vertical bar
            gc.drawLine(left, plot_bounds.y, left, plot_bounds.y + plot_bounds.height);
            gc.drawLine(right, plot_bounds.y, right, plot_bounds.y + plot_bounds.height);
            if (width >= 5*ARROW_SIZE)
            {
                gc.drawLine(left, mid_y, left + 2*ARROW_SIZE, mid_y);
                gc.drawLine(left+ARROW_SIZE, mid_y-ARROW_SIZE, left + 2*ARROW_SIZE, mid_y);
                gc.drawLine(left+ARROW_SIZE, mid_y+ARROW_SIZE, left + 2*ARROW_SIZE, mid_y);

                gc.drawLine(right, mid_y, right - 2*ARROW_SIZE, mid_y);
                gc.drawLine(right-ARROW_SIZE, mid_y-ARROW_SIZE, right - 2*ARROW_SIZE, mid_y);
                gc.drawLine(right-ARROW_SIZE, mid_y+ARROW_SIZE, right - 2*ARROW_SIZE, mid_y);
            }
        }
    }

    /** Draw the zoom indicator for a vertical zoom, i.e. on a Y axis
     *
     *  @param gc GC to use
     *  @param plot_bounds Plot area where to draw the zoom indicator
     *  @param start Initial mouse position
     *  @param current Current mouse position
     */
    protected void drawZoomYMouseFeedback(final Graphics2D gc, final Rectangle plot_bounds, final Point2D start, final Point2D current)
    {
        final int top = (int) Math.min(start.getY(), current.getY());
        final int bottom = (int) Math.max(start.getY(), current.getY());
        final int height = bottom - top;
        final int mid_x = plot_bounds.x + plot_bounds.width / 2;

        for (int i=0; i<2; ++i)
        {
            if (i==0)
            {
                gc.setColor(java.awt.Color.WHITE);
                gc.setStroke(MOUSE_FEEDBACK_BACK);
            }
            else
            {
                gc.setColor(java.awt.Color.BLACK);
                gc.setStroke(MOUSE_FEEDBACK_FRONT);
            }
            // Range on axis
            gc.drawRect((int)start.getX(), top, 1, height);
            // Top, bottom horizontal bar
            gc.drawLine(plot_bounds.x, top, plot_bounds.x + plot_bounds.width, top);
            gc.drawLine(plot_bounds.x, bottom, plot_bounds.x + plot_bounds.width, bottom);
            if (height >= 5 * ARROW_SIZE)
            {
                gc.drawLine(mid_x, top, mid_x, top + 2*ARROW_SIZE);
                gc.drawLine(mid_x-ARROW_SIZE, top+ARROW_SIZE, mid_x, top + 2*ARROW_SIZE);
                gc.drawLine(mid_x+ARROW_SIZE, top+ARROW_SIZE, mid_x, top + 2*ARROW_SIZE);

                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x, bottom);
                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x-ARROW_SIZE, bottom - ARROW_SIZE);
                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x+ARROW_SIZE, bottom - ARROW_SIZE);
            }
        }
    }

    /** Draw the zoom indicator for zoom, i.e. a 'rubberband'
     *
     *  @param gc GC to use
     *  @param plot_bounds Plot area where to draw the zoom indicator
     *  @param start Initial mouse position
     *  @param current Current mouse position
     */
    protected void drawZoomMouseFeedback(final Graphics2D gc, final Rectangle plot_bounds, final Point2D start, final Point2D current)
    {
        final int left = (int) Math.min(start.getX(), current.getX());
        final int right = (int) Math.max(start.getX(), current.getX());
        final int top = (int) Math.min(start.getY(), current.getY());
        final int bottom = (int) Math.max(start.getY(), current.getY());
        final int width = right - left;
        final int height = bottom - top;
        final int mid_x = left + width / 2;
        final int mid_y = top + height / 2;

        for (int i=0; i<2; ++i)
        {
            if (i==0)
            {   // White 'background' to help rectangle show up on top
                // of dark images
                gc.setColor(java.awt.Color.WHITE);
                gc.setStroke(MOUSE_FEEDBACK_BACK);
            }
            else
            {
                gc.setColor(java.awt.Color.BLACK);
                gc.setStroke(MOUSE_FEEDBACK_FRONT);
            }
            // Main 'rubberband' rect
            gc.drawRect(left, top, width, height);
            if (width >= 5*ARROW_SIZE)
            {
                gc.drawLine(left, mid_y, left + 2*ARROW_SIZE, mid_y);
                gc.drawLine(left+ARROW_SIZE, mid_y-ARROW_SIZE, left + 2*ARROW_SIZE, mid_y);
                gc.drawLine(left+ARROW_SIZE, mid_y+ARROW_SIZE, left + 2*ARROW_SIZE, mid_y);

                gc.drawLine(right, mid_y, right - 2*ARROW_SIZE, mid_y);
                gc.drawLine(right-ARROW_SIZE, mid_y-ARROW_SIZE, right - 2*ARROW_SIZE, mid_y);
                gc.drawLine(right-ARROW_SIZE, mid_y+ARROW_SIZE, right - 2*ARROW_SIZE, mid_y);
            }
            if (height >= 5*ARROW_SIZE)
            {
                gc.drawLine(mid_x, top, mid_x, top + 2*ARROW_SIZE);
                gc.drawLine(mid_x-ARROW_SIZE, top+ARROW_SIZE, mid_x, top + 2*ARROW_SIZE);
                gc.drawLine(mid_x+ARROW_SIZE, top+ARROW_SIZE, mid_x, top + 2*ARROW_SIZE);

                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x, bottom);
                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x-ARROW_SIZE, bottom - ARROW_SIZE);
                gc.drawLine(mid_x, bottom - 2*ARROW_SIZE, mid_x+ARROW_SIZE, bottom - ARROW_SIZE);
            }
        }
    }

    /** @param mode New {@link MouseMode}
     *  @throws IllegalArgumentException if mode is internal
     */
    public void setMouseMode(final MouseMode mode)
    {
        if (mode.ordinal() >= MouseMode.INTERNAL_MODES.ordinal())
            throw new IllegalArgumentException("Not permitted to set " + mode);
        mouse_mode = mode;
        PlotCursors.setCursor(this, mouse_mode);
    }

    /** onMouseEntered */
    protected void mouseEntered(final MouseEvent e)
    {
        getScene().setCursor(getCursor());
    }

    /** Zoom in/out triggered by mouse wheel
     *  @param event Scroll event
     */
    protected void wheelZoom(final ScrollEvent event)
    {
        // Invoked by mouse scroll wheel.
        // Only allow zoom (with control), not pan.
        if (! event.isControlDown())
            return;

        if (event.getDeltaY() > 0)
            zoomInOut(event.getX(), event.getY(), 1.0/ZOOM_FACTOR);
        else if (event.getDeltaY() < 0)
            zoomInOut(event.getX(), event.getY(), ZOOM_FACTOR);
        else
            return;
        event.consume();
    }

    /** Zoom 'in' or 'out' from where the mouse was clicked
     *  @param x Mouse coordinate
     *  @param y Mouse coordinate
     *  @param factor Zoom factor, positive to zoom 'out'
     */
    protected abstract void zoomInOut(final double x, final double y, final double factor);

    /** Should be invoked when plot no longer used to release resources */
    public void dispose()
    {   // Stop updates which could otherwise still use
        // what's about to be disposed
        update_throttle.dispose();
    }
}
