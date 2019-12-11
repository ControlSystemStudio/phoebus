/*******************************************************************************
 * Copyright (c) 2014-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.javafx.rtplot.internal;

import static org.csstudio.javafx.rtplot.Activator.logger;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Activator;
import org.csstudio.javafx.rtplot.Annotation;
import org.csstudio.javafx.rtplot.Axis;
import org.csstudio.javafx.rtplot.AxisRange;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.PlotMarker;
import org.csstudio.javafx.rtplot.RTPlotListener;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.undo.ChangeAxisRanges;
import org.csstudio.javafx.rtplot.internal.undo.UpdateAnnotationAction;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.ScreenTransform;
import org.phoebus.ui.dialog.DialogHelper;
import org.phoebus.ui.javafx.BufferUtil;
import org.phoebus.ui.javafx.DoubleBuffer;
import org.phoebus.ui.javafx.PlatformInfo;
import org.phoebus.ui.undo.UndoableAction;

import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;

/** Plot with axes and area that displays the traces
 *
 *  <p>A 'canvas' that draws its content in a background thread.
 *  Content is based on {@link PlotPart}s, which actually use
 *  AWT to perform the drawing.
 *
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class Plot<XTYPE extends Comparable<XTYPE>> extends PlotCanvasBase
{
    /** Foreground color */
    javafx.scene.paint.Color foreground = javafx.scene.paint.Color.BLACK;

    /** Background color */
    private volatile Color background = Color.WHITE,
                           grid = Color.DARK_GRAY;

    /** Opacity (0 .. 100 %) of 'area' */
    private volatile int opacity = 20;

    public static final String FONT_FAMILY = "Liberation Sans";

    /** When background is 100% transparent (alpha=0),
     *  the plot will no longer capture any mouse events,
     *  it's invisible to the mouse.
     *  Patching that color with an almost transparent one
     *  avoids the issue.
     */
    private static final Color ALMOST_TRANSPARENT = new Color(0, 0, 0, 1);

    /** Font to use for, well, title */
    private volatile Font title_font = new Font(FONT_FAMILY, Font.BOLD, 18);

    /** Font to use for legend */
    private volatile Font legend_font = new Font(FONT_FAMILY, Font.PLAIN, 12);

    final private TitlePart title_part;
    final private List<Trace<XTYPE>> traces = new CopyOnWriteArrayList<>();
    final private AxisPart<XTYPE> x_axis;
    final private List<YAxisImpl<XTYPE>> y_axes = new CopyOnWriteArrayList<>();
    final private PlotPart plot_area;
    final private TracePainter<XTYPE> trace_painter = new TracePainter<>();
    final private List<AnnotationImpl<XTYPE>> annotations = new CopyOnWriteArrayList<>();
    final private LegendPart<XTYPE> legend;

    final private PlotProcessor<XTYPE> plot_processor;

    private boolean show_crosshair = false;

    private AxisRange<XTYPE> mouse_start_x_range;

    /** Initial range of the mouse_y_axis or all Y axes, depending on mouse mode */
    private List<AxisRange<Double>> mouse_start_y_ranges = new ArrayList<>();
    /** Initial autorange setting of the mouse_y_axis or all Y axes, depending on mouse mode */
    private List<Boolean> pre_pan_auto_scales = new ArrayList<>();
    private int mouse_y_axis = -1;

    // Annotation-related info. If mouse_annotation is set, the rest should be set.
    private AnnotationImpl<XTYPE> mouse_annotation = null;
    private Point2D mouse_annotation_start_offset;
    private XTYPE mouse_annotation_start_position;
    private double mouse_annotation_start_value;

    final private List<RTPlotListener<XTYPE>> listeners = new CopyOnWriteArrayList<>();

    // All PlotMarkers
    private final List<PlotMarker<XTYPE>> plot_markers = new CopyOnWriteArrayList<>();
    // Selected plot marker that's being moved by the mouse
    private PlotMarker<XTYPE> plot_marker = null;

    /** Constructor
     *  @param active Active mode where plot reacts to mouse/keyboard?
     *  @param type Type of X axis
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Plot(final Class<XTYPE> type, final boolean active)
    {
        super(active);
        plot_processor = new PlotProcessor<>(this);

        // To avoid unchecked cast, X axis would need to be passed in,
        // but its listener can only be created within this class.
        // When passing X axis in, its listener needs to be set
        // in here, but an axis design with final listener was preferred.
        if (type == Double.class)
            x_axis = (AxisPart) new HorizontalNumericAxis(Messages.AxisNameDefX, plot_part_listener);
        else if (type == Instant.class)
            x_axis = (AxisPart) TimeAxis.forDuration(Messages.AxisNameDefT, plot_part_listener, Duration.ofMinutes(2));
        else
            throw new IllegalArgumentException("Cannot handle " + type.getName());

        addYAxis(Messages.AxisNameDefY);
        title_part = new TitlePart("", plot_part_listener);
        plot_area = new PlotPart("main", plot_part_listener);
        legend = new LegendPart<>("legend", plot_part_listener);

        if (active)
        {
            setOnMousePressed(this::mouseDown);
            setOnMouseMoved(this::mouseMove);
            setOnMouseDragged(this::mouseMove);
            setOnMouseReleased(this::mouseUp);
            setOnMouseExited(this::mouseExit);
        }
    }

    /** @param listener Listener to add */
    public void addListener(final RTPlotListener<XTYPE> listener)
    {
        Objects.requireNonNull(listener);
        listeners.add(listener);
    }

    /** @param listener Listener to remove */
    public void removeListener(final RTPlotListener<XTYPE> listener)
    {
        Objects.requireNonNull(listener);
        listeners.remove(listener);
    }

    /** @param color Foreground color */
    public void setForeground(final javafx.scene.paint.Color color)
    {
        foreground = color;

        // Use foreground color for the grid.
        // For dark foreground (black on presumably white background),
        // use a lighter shade as the grid.
        // For bright foreground (white on presumably black background),
        // use a darker shade.
        if (foreground.getBrightness() > 0.5)
            grid = GraphicsUtils.convert(foreground.darker());
        else
            grid = GraphicsUtils.convert(foreground.brighter());
    }

    /** @param color Background color */
    public void setBackground(final Color color)
    {
        if (color.getAlpha() <= 0)
            background = ALMOST_TRANSPARENT;
        else
            background = color;
    }

    /** Opacity (0 .. 100 %) of 'area' */
    // 'setOpacity', as used in original SWT implementation,
    // is already used by JFX Node base class
    public void setAreaOpacity(final int opacity)
    {
        this.opacity = opacity;
    }

    /** @param color Grid color */
    public void setGridColor(final Color color)
    {
        grid = color;
    }

    /** @return Title */
    public String getTitle()
    {
        return title_part.getName();
    }

    /** @param title Title */
    public void setTitle(final String title)
    {
        title_part.setName(title == null ? "" : title);
    }

    /** @param font Font to use for title */
    public void setTitleFont(final Font font)
    {
        title_font = font;
        need_layout.set(true);
        requestUpdate();
    }

    /** @return <code>true</code> if legend is visible */
    public boolean isLegendVisible()
    {
        return legend.isVisible();
    }

    /** @param show <code>true</code> if legend should be displayed */
    public void showLegend(final boolean show)
    {
        legend.setVisible(show);
        need_layout.set(true);
        requestUpdate();
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedLegend(show);
    }

    /** @param font Font to use for scale */
    public void setLegendFont(final Font font)
    {
        legend_font = font;
        need_layout.set(true);
        requestUpdate();
    }

    /** @return X/Time axis */
    public AxisPart<XTYPE> getXAxis()
    {
        return x_axis;
    }

    /** Add another Y axis
     *  @param name
     *  @return Y Axis that was added
     */
    public YAxis<XTYPE> addYAxis(final String name)
    {
        YAxisImpl<XTYPE> axis = new YAxisImpl<>(name, plot_part_listener);
        y_axes.add(axis);
        need_layout.set(true);
        return axis;
    }

    /** @return Y axes */
    public List<YAxisImpl<XTYPE>> getYAxes()
    {
        final List<YAxisImpl<XTYPE>> copy = new ArrayList<>();
        copy.addAll(y_axes);
        return copy;
    }

    /** @param index Index of Y axis to remove */
    public void removeYAxis(final int index)
    {
        y_axes.remove(index);
        need_layout.set(true);
    }

    /** Add trace to the plot
     *  @param trace {@link Trace}, where axis must be a valid Y axis index
     */
    public void addTrace(final TraceImpl<XTYPE> trace)
    {
        traces.add(trace);
        try
        {
            y_axes.get(trace.getYAxis()).addTrace(trace);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            logger.log(Level.WARNING, "Cannot add trace to axis " + trace.getYAxis(), ex);
        }
        need_layout.set(true);
        requestUpdate();
    }

    /** @param trace Trace to move from its current Y axis
     *  @param new_y_axis Index of new Y Axis
     */
    public void moveTrace(final TraceImpl<XTYPE> trace, final int new_y_axis)
    {
        Objects.requireNonNull(trace);
        try
        {
            y_axes.get(trace.getYAxis()).removeTrace(trace);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            logger.log(Level.WARNING, "Cannot remove trace from axis " + trace.getYAxis(), ex);
        }
        trace.setYAxis(new_y_axis);
        try
        {
            y_axes.get(trace.getYAxis()).addTrace(trace);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            logger.log(Level.WARNING, "Cannot assign trace to axis " + trace.getYAxis(), ex);
        }
    }

    /** @return Thread-safe, read-only traces of the plot */
    public Iterable<Trace<XTYPE>> getTraces()
    {
        return traces;
    }

    /** Remove trace from plot
     *  @param trace {@link Trace}, where axis must be a valid Y axis index
     */
    public void removeTrace(final Trace<XTYPE> trace)
    {
        Objects.requireNonNull(trace);
        traces.remove(trace);
        y_axes.get(trace.getYAxis()).removeTrace(trace);
        need_layout.set(true);
        requestUpdate();
    }

    /** Add plot marker
     *  @param color
     *  @param interactive
     *  @return {@link PlotMarker}
     */
    public PlotMarker<XTYPE> addMarker(final javafx.scene.paint.Color color, final boolean interactive, final XTYPE position)
    {   // Return a PlotMarker that triggers a redraw as it's changed
        final PlotMarker<XTYPE> marker = new PlotMarker<>(color, interactive, position)
        {
            @Override
            public void setPosition(XTYPE position)
            {
                super.setPosition(position);
                requestUpdate();
            }
        };
        plot_markers.add(marker);
        return marker;
    }

    /** @return {@link PlotMarker}s */
    public List<PlotMarker<XTYPE>> getMarkers()
    {
        return plot_markers;
    }

    /** @param index Index of Marker to remove
     *  @throws IndexOutOfBoundsException
     */
    public void removeMarker(final int index)
    {
        plot_markers.remove(index);
        requestUpdate();
    }

    /** Select plot marker at mouse position?
     *  @return Was plot marker set?
     */
    private boolean selectPlotMarker()
    {
        if (! mouse_start.isPresent())
            return false;
        final int x = (int) (mouse_start.get().getX() + 0.5);
        for (PlotMarker<XTYPE> marker : plot_markers)
        {
            // Ignore non-interactive marker
            if (! marker.isInteractive())
                continue;
            final int mx = x_axis.getScreenCoord(marker.getPosition());
            if (Math.abs(mx - x) < 5)
            {
                plot_marker = marker;
                return true;
            }
        }
        return false;
    }

    /** De-select an plot marker */
    private void deselectPlotMarker()
    {
        if (plot_marker != null)
        {
            plot_marker = null;
            requestUpdate();
        }
    }

    /** Add Annotation to a trace,
     *  determining initial position based on some
     *  sample of that trace
     *  @param trace Trace to which a Annotation should be added
     *  @param text Text for the annotation
     */
    public void addAnnotation(final Trace<XTYPE> trace, final String text)
    {
        Objects.requireNonNull(trace);
        plot_processor.createAnnotation(trace, text);
    }

    /** @param annotation Annotation to add */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addAnnotation(final Annotation<XTYPE> annotation)
    {
        Objects.requireNonNull(annotation);
        if (annotation instanceof AnnotationImpl)
            annotations.add((AnnotationImpl)annotation);
        else
            annotations.add(new AnnotationImpl<>(annotation.isInternal(),
                                                      annotation.getTrace(), annotation.getPosition(),
                                                      annotation.getValue(), annotation.getOffset(),
                                                      annotation.getText()));
        requestUpdate();
        fireAnnotationsChanged();
    }

    /** @return Current {@link AnnotationImpl}s */
    public List<AnnotationImpl<XTYPE>> getAnnotations()
    {
        return annotations;
    }

    /** Update location and value of annotation
     *  @param annotation {@link AnnotationImpl} to update
     *  @param position New position
     *  @param value New value
     */
    public void updateAnnotation(final AnnotationImpl<XTYPE> annotation, final XTYPE position, final double value, final String info,
            final Point2D offset)
    {
        annotation.setLocation(position, value, info);
        annotation.setOffset(offset);
        requestUpdate();
        fireAnnotationsChanged();
    }

    /** Update text of annotation
     *  @param annotation {@link Annotation} to update.
     *         Must be an existing annotation obtained from <code>getAnnotations()</code>
     *  @param text New text
     *  @throws IllegalArgumentException if annotation is unknown
     */
    public void updateAnnotation(final Annotation<XTYPE> annotation, final String text)
    {
        final int index = annotations.indexOf(annotation);
        if (index < 0)
            throw new IllegalArgumentException("Unknown annotation " + annotation);
        annotations.get(index).setText(text);
        requestUpdate();
        fireAnnotationsChanged();
    }

    /** @param annotation Annotation to remove */
    public void removeAnnotation(final Annotation<XTYPE> annotation)
    {
        annotations.remove(annotation);
        requestUpdate();
        fireAnnotationsChanged();
    }

    /** Select Annotation at mouse position?
     *  @return Was a mouse annotation set?
     */
    private boolean selectMouseAnnotation()
    {
        if (mouse_start.isPresent())
            for (AnnotationImpl<XTYPE> annotation : annotations)
                if (annotation.isSelected(mouse_start.get()))
                {
                    mouse_annotation_start_offset = annotation.getOffset();
                    mouse_annotation_start_position = annotation.getPosition();
                    mouse_annotation_start_value = annotation.getValue();
                    mouse_annotation = annotation;
                    requestUpdate();
                    return true;
                }
        return false;
    }

    /** De-select an Annotation */
    private void deselectMouseAnnotation()
    {
        final AnnotationImpl<XTYPE> anno = mouse_annotation;
        if (anno != null)
        {
            undo.add(new UpdateAnnotationAction<>(this, anno,
                    mouse_annotation_start_position, mouse_annotation_start_value,
                    mouse_annotation_start_offset,
                    anno.getPosition(), anno.getValue(),
                    anno.getOffset()));
            anno.deselect();
            mouse_annotation = null;
            requestUpdate();
        }
    }

    /** Compute layout of plot components */
    private void computeLayout(final Graphics2D gc, final Rectangle bounds)
    {
        // Title on top, as high as desired
        final int title_height = title_part.getDesiredHeight(gc, title_font);
        title_part.setBounds(0, 0, bounds.width, title_height);

        // Legend on bottom, as high as desired
        final int legend_height = legend.getDesiredHeight(gc, bounds.width, legend_font, traces);
        legend.setBounds(0,  bounds.height-legend_height, bounds.width, legend_height);

        // X Axis as high as desired. Width will depend on Y axes.
        final int x_axis_height = x_axis.getDesiredPixelSize(bounds, gc);
        final int y_axis_height = bounds.height - title_height - x_axis_height - legend_height;

        // Ask each Y Axis for its widths, which changes based on number of labels
        // and how they are laid out
        int total_left_axes_width = 0, total_right_axes_width = 0;
        int plot_width = bounds.width;

        final List<YAxisImpl<XTYPE>> save_copy = new ArrayList<>(y_axes);

        // Could call axis.getPixelGaps(gc), determine max space needed above & below all axes,
        // but for now the top & right label is shifted to stay within the region,
        // and the bottom & left labels are almost always OK to reach beyond their axis region.

        // First, lay out 'left' axes in reverse order to get "2, 1, 0" on the left of the plot.
        for (YAxisImpl<XTYPE> axis : save_copy)
            if (! axis.isOnRight())
            {
                final Rectangle axis_region = new Rectangle(total_left_axes_width, title_height, plot_width, y_axis_height);
                axis_region.width = axis.getDesiredPixelSize(axis_region, gc);
                axis.setBounds(axis_region);
                total_left_axes_width += axis_region.width;
                plot_width -= axis_region.width;
            }
        // Then lay out 'right' axes, also in reverse order, to get "0, 1, 2" on right side of plot.
        for (YAxisImpl<XTYPE> axis : save_copy)
            if (axis.isOnRight())
            {
                final Rectangle axis_region = new Rectangle(total_left_axes_width, title_height, plot_width, y_axis_height);
                axis_region.width = axis.getDesiredPixelSize(axis_region, gc);
                total_right_axes_width += axis_region.width;
                axis_region.x = bounds.width - total_right_axes_width;
                axis.setBounds(axis_region);
                plot_width -= axis_region.width;
            }

        // So far the areas have been computed to exactly touch, without any overlap:
        //   |::::::::::::::::::::::::::::::::::
        //   Y:::::::::::::Plot:::::::::::::::::
        //   |::::::::::::::::::::::::::::::::::
        // 0-+*:::::::::::::::::::::::::::::::::
        //    +------- X -----------------------
        //    |
        //    0
        //
        // Adjusting such that the origin (x,y) = (0,0) is in the lower left corner,
        // and both axis markers meet there like this:
        //   |::::::::::::::::::::::::::::::::::
        //   Y:::::::::::::Plot:::::::::::::::::
        //   |::::::::::::::::::::::::::::::::::
        // 0-*-------- X -----------------------
        //   |
        //   0


        // X axis move up (and higher) by one pixel to overlap the bottom pixel line of the plot_area,
        // and moving X axis left (and longer) to get the leftmost data in plot onto the Y axis
        x_axis.setBounds(total_left_axes_width-1, title_height+y_axis_height-1, plot_width+1, x_axis_height+1);

        plot_area.setBounds(total_left_axes_width-1, title_height, plot_width+1, y_axis_height);
    }

    /** Buffers used to create the next image buffer */
    private final DoubleBuffer buffers = new DoubleBuffer();

    /** Draw all components into image buffer */
    @Override
    protected BufferedImage updateImageBuffer()
    {
        final Rectangle area_copy = area;
        if (area_copy.width <= 0  ||  area_copy.height <= 0)
            return null;

        plot_processor.autoscale();

        final BufferUtil buffer = buffers.getBufferedImage(area_copy.width, area_copy.height);
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

        if (need_layout.getAndSet(false))
            computeLayout(gc, area_copy);

        final Rectangle plot_bounds = plot_area.getBounds();

        if (background.getAlpha() < 255)
        {   // Transparent background:
            // Enable alpha and clear image
            final Composite orig_composite = gc.getComposite();
            gc.setComposite(AlphaComposite.Clear);
            gc.fillRect(0, 0, area_copy.width, area_copy.height);
            gc.setComposite(orig_composite);
        }

        if (background.getAlpha() > 0)
        {
            gc.setColor(background);
            gc.fillRect(0, 0, area_copy.width, area_copy.height);
        }
        // else: Skip fully transparent background (was already 'cleared')

        title_part.setColor(foreground);
        title_part.paint(gc, title_font);
        legend.paint(gc, legend_font, traces);

        // Fetch x_axis transformation and use that to paint all traces,
        // because X Axis tends to change from scrolling
        // while we're painting traces
        x_axis.setGridColor(grid);
        x_axis.paint(gc, plot_bounds);
        final ScreenTransform<XTYPE> x_transform = x_axis.getScreenTransform();
        for (YAxisImpl<XTYPE> y_axis : y_axes)
        {
            y_axis.setGridColor(grid);
            y_axis.paint(gc, plot_bounds);
        }

        gc.setClip(plot_bounds.x, plot_bounds.y, plot_bounds.width, plot_bounds.height);

        // Shade plot region beyond 'now'
        // Lay this 'on top' of grid, then add the traces.
        if (x_axis instanceof TimeAxis  &&  Activator.shady_future.getAlpha() > 0)
        {
            final int future_x = ((TimeAxis)x_axis).getScreenCoord(Instant.now());
            gc.setColor(Activator.shady_future);
            gc.fillRect(future_x, 0, area_copy.width - future_x, area_copy.height);
        }

        plot_area.paint(gc);

        for (YAxisImpl<XTYPE> y_axis : y_axes)
            for (Trace<XTYPE> trace : y_axis.getTraces())
                trace_painter.paint(gc, plot_area.getBounds(), opacity, x_transform, y_axis, trace);

        drawPlotMarkers(gc);
        gc.setClip(null);

        // Annotations use label font
        for (AnnotationImpl<XTYPE> annotation : annotations)
        {
            try
            {
                annotation.updateValue(annotation.getPosition());
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Cannot update annotation", ex);
            }
            annotation.paint(gc, x_axis, y_axes.get(annotation.getTrace().getYAxis()));
        }

        return image;
    }

    /** Draw the {@link PlotMarker}s
     *  @param gc Graphics context
     */
    private void drawPlotMarkers(final Graphics2D gc)
    {
        final int y0 = plot_area.getBounds().y;
        final int y1 = y0 + plot_area.getBounds().height;
        final Stroke old_stroke = gc.getStroke();
        gc.setStroke(AxisPart.TICK_STROKE);
        for (PlotMarker<XTYPE> marker : plot_markers)
        {
            gc.setColor(GraphicsUtils.convert(marker.getColor()));
            final int x = x_axis.getScreenCoord(marker.getPosition());
            gc.drawLine(x, y0, x, y1);
        }
        gc.setStroke(old_stroke);
    }

    /** Draw visual feedback (rubber band rectangle etc.)
     *  for current mouse mode
     *  @param gc GC
     */
    @Override
    protected void drawMouseModeFeedback(final Graphics2D gc)
    {   // Safe copy, then check null (== isPresent())
        final Point2D current = mouse_current.orElse(null);
        if (current == null)
            return;

        // Compute values at cursor
        final int x = (int) current.getX();
        final XTYPE location = x_axis.getValue(x);
        List<CursorMarker> markers;
        try
        {
            markers = CursorMarker.compute(this, x, location);
            fireCursorsChanged();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot compute cursor markers", ex);
            markers = Collections.emptyList();
        }

        final Point2D start = mouse_start.orElse(null);
        final Rectangle plot_bounds = plot_area.getBounds();

        if (mouse_mode == MouseMode.PAN_X  ||  mouse_mode == MouseMode.PAN_Y || mouse_mode == MouseMode.PAN_PLOT ||
            (mouse_annotation != null  &&  start != null))
        {
            // NOP, minimize additional UI thread drawing to allow better 'pan' updates
            //      and also hide the crosshair when moving an annotation
        }
        else if (show_crosshair  &&  plot_bounds.contains(current.getX(), current.getY()))
        {   // Cross-hair Cursor
            gc.setStroke(MOUSE_FEEDBACK_BACK);
            gc.setColor(background);
            gc.drawLine(plot_bounds.x, (int)current.getY(), plot_bounds.x + plot_bounds.width, (int)current.getY());
            gc.drawLine((int)current.getX(), plot_bounds.y, (int)current.getX(), plot_bounds.y + plot_bounds.height);
            gc.setStroke(MOUSE_FEEDBACK_FRONT);
            gc.setColor(GraphicsUtils.convert(foreground));
            gc.drawLine(plot_bounds.x, (int)current.getY(), plot_bounds.x + plot_bounds.width, (int)current.getY());
            gc.drawLine((int)current.getX(), plot_bounds.y, (int)current.getX(), plot_bounds.y + plot_bounds.height);
            // Corresponding axis ticks
            gc.setBackground(background);
            x_axis.drawTickLabel(gc, x_axis.getValue((int)current.getX()));
            for (YAxisImpl<XTYPE> axis : y_axes)
                axis.drawTickLabel(gc, axis.getValue((int)current.getY()));
            // Trace markers
            CursorMarker.drawMarkers(gc, markers, area);
        }

        if (mouse_mode == MouseMode.ZOOM_IN_X  &&  start != null)
            drawZoomXMouseFeedback(gc, plot_bounds, start, current);
        else if (mouse_mode == MouseMode.ZOOM_IN_Y  &&  start != null)
            drawZoomYMouseFeedback(gc, plot_bounds, start, current);
        else if (mouse_mode == MouseMode.ZOOM_IN_PLOT  &&  start != null)
            drawZoomMouseFeedback(gc, plot_bounds, start, current);
    }

    /** @param show Show the cross-hair cursor? */
    public void showCrosshair(final boolean show)
    {
        if (show_crosshair == show)
            return;
        show_crosshair = show;
        // Redraw once to show or hide crosshair
        requestRedraw();
    }

    /** return Show the cross-hair cursor? */
    public boolean isCrosshairVisible()
    {
        return show_crosshair;
    }

    /** onMousePressed */
    private void mouseDown(final MouseEvent e)
    {
        // Don't start mouse actions when user invokes context menu
        if (! e.isPrimaryButtonDown()  ||  (PlatformInfo.is_mac_os_x && e.isControlDown()))
            return;
        final Point2D current = new Point2D(e.getX(), e.getY());
        mouse_start = mouse_current = Optional.of(current);

        final int clicks = e.getClickCount();
        if (selectMouseAnnotation() ||
            selectPlotMarker())
            return;
        if ((mouse_mode == MouseMode.NONE || mouse_mode == MouseMode.PAN) && clicks == 2)
        {
        	// NOP: edit axis ranges (let RTPlot handle it)
        }
    	else if (mouse_mode == MouseMode.PAN)
        {   // Determine start of 'pan'
            // For affected Y axes, i.e. mouse_y_axis or all,
            // store the pre-pan auto scale state for un-do
            // and disable autoscaling during mouse actions.
            mouse_start_x_range = x_axis.getValueRange();
            mouse_start_y_ranges.clear();
            pre_pan_auto_scales.clear();
            mouse_y_axis = -1;
            for (int i=0; i<y_axes.size(); ++i)
            {
                final YAxisImpl<XTYPE> axis = y_axes.get(i);
                if (axis.getBounds().contains(current.getX(), current.getY()))
                {
                    mouse_y_axis = i;
                    mouse_mode = MouseMode.PAN_Y;

                    mouse_start_y_ranges.add(axis.getValueRange());
                    pre_pan_auto_scales.add(axis.isAutoscale());
                    if (axis.setAutoscale(false))
                        fireAutoScaleChange(axis);
                    return;
                }
            }
            if (plot_area.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.PAN_PLOT;
                if (x_axis.setAutoscale(false))
                    fireAutoScaleChange(x_axis);
                for (YAxisImpl<XTYPE> axis : y_axes)
                {
                    mouse_start_y_ranges.add(axis.getValueRange());
                    pre_pan_auto_scales.add(axis.isAutoscale());
                    if (axis.setAutoscale(false))
                        fireAutoScaleChange(axis);
                }
            }
            else if (x_axis.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.PAN_X;
                if (x_axis.setAutoscale(false))
                    fireAutoScaleChange(x_axis);
            }
        }
        else if (mouse_mode == MouseMode.ZOOM_IN  &&  clicks == 1)
        {   // Determine start of 'rubberband' zoom.
            // Reset cursor from SIZE* to CROSS.
            for (int i=0; i<y_axes.size(); ++i)
                if (y_axes.get(i).getBounds().contains(current.getX(), current.getY()))
                {
                    mouse_y_axis = i;
                    mouse_mode = MouseMode.ZOOM_IN_Y;
                    return;
                }
            if (plot_area.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.ZOOM_IN_PLOT;
                PlotCursors.setCursor(this, mouse_mode);
            }
            else if (x_axis.getBounds().contains(current.getX(), current.getY()))
            {
                mouse_mode = MouseMode.ZOOM_IN_X;
            }
        }
        else if ((mouse_mode == MouseMode.ZOOM_IN && clicks == 2)  ||  mouse_mode == MouseMode.ZOOM_OUT)
            zoomInOut(current.getX(), current.getY(), ZOOM_FACTOR);
    }

    public static class AxisClickInfo
    {
        /** Clicked axis */
        public final NumericAxis axis;

        /** true if click was on high-value end of axis; else, false */
        public final boolean isHighEnd;

        /** Dimensions and location of click region */
        public final Rectangle area;

        public AxisClickInfo(NumericAxis axis, boolean isHighEnd,
                Rectangle area)
        {
            this.axis = axis;
            this.isHighEnd = isHighEnd;
            this.area = area;
        }
    }

    /** Size of the area used to edit axis limits */
    private static final int AXIS_LIMIT_BOX_WIDTH = 100, AXIS_LIMIT_BOX_HEIGHT = 30;

    /**
     * Check if the mouse double-clicked on the end of an axis, and if mouse_mode is PAN or NONE.
     * If true, return information about the clicked axis; if not, return null.
     * @param event MouseEvent to get info for
     */
    public AxisClickInfo axisClickInfo(MouseEvent event)
    {
        if ((mouse_mode == MouseMode.NONE || mouse_mode == MouseMode.PAN) && event.getClickCount() == 2)
        {
            // For event.getX(), etc. to work as desired, 'this' must be the source of the MouseEvent
            if (!this.equals(event.getSource()))
                event = event.copyFor(this, event.getTarget());
        	final double click_x = event.getX(),
        	             click_y = event.getY();
        	// Do the upper or lower end regions of any y-axis contain the click?
        	for (YAxisImpl<XTYPE> axis : y_axes)
        	{
        	    final Rectangle bounds = axis.getBounds();
        	    if (bounds.contains(click_x, click_y))
        	    {
        	        final int x = axis.isOnRight() ? bounds.x + bounds.width - AXIS_LIMIT_BOX_WIDTH : bounds.x;
        	        if (click_y > bounds.y + bounds.height/2)
                        return new AxisClickInfo(axis, false, new Rectangle(x, bounds.y + bounds.height - AXIS_LIMIT_BOX_HEIGHT, AXIS_LIMIT_BOX_WIDTH, AXIS_LIMIT_BOX_HEIGHT));
        	        else
                        return new AxisClickInfo(axis, true, new Rectangle(x, bounds.y, AXIS_LIMIT_BOX_WIDTH, AXIS_LIMIT_BOX_HEIGHT));
        	    }
    		}
        	// Do the left-side (lesser) or right-side (greater) end regions of the x-axis contain it?
        	if (x_axis instanceof NumericAxis)
        	{
        	    final NumericAxis axis = (NumericAxis)x_axis;
                final Rectangle bounds = axis.getBounds();
                if (bounds.contains(click_x, click_y))
                {
                    if (click_x > bounds.x + bounds.width / 2)
                        return new AxisClickInfo(axis, true, new Rectangle(bounds.x + bounds.width - AXIS_LIMIT_BOX_WIDTH, bounds.y, AXIS_LIMIT_BOX_WIDTH, AXIS_LIMIT_BOX_HEIGHT));
                    else
                        return new AxisClickInfo(axis, false, new Rectangle(bounds.x, bounds.y, AXIS_LIMIT_BOX_WIDTH, AXIS_LIMIT_BOX_HEIGHT));
                }
        	}
        }
    	return null;
    }

    /** setOnMouseMoved */
    private void mouseMove(final MouseEvent e)
    {
        final Point2D current = new Point2D(e.getX(), e.getY());
        mouse_current = Optional.of(current);

        // While zooming, when mouse is quickly dragged outside the widget
        // and then released, the 'mouseUp' event is sometimes missing.
        // --> When seeing an active mouse move w/o button press,
        //     treat that just like a release.
        if (mouse_mode.ordinal() >= MouseMode.ZOOM_IN_X.ordinal()  &&  !e.isPrimaryButtonDown())
        {
            mouseUp(e);
            return;
        }

        PlotCursors.setCursor(this, mouse_mode);

        final Point2D start = mouse_start.orElse(null);

        final AnnotationImpl<XTYPE> anno = mouse_annotation;
        if (plot_marker != null)
        {
            plot_marker.setPosition(x_axis.getValue((int) current.getX()));
            requestUpdate();
            firePlotMarkersChanged(plot_markers.indexOf(plot_marker));
        }
        else if (anno != null  &&  start != null)
        {
            if (anno.getSelection() == AnnotationImpl.Selection.Body)
            {
                anno.setOffset(
                        new Point2D((int)(mouse_annotation_start_offset.getX() + current.getX() - start.getX()),
                                    (int)(mouse_annotation_start_offset.getY() + current.getY() - start.getY())));
                requestUpdate();
                fireAnnotationsChanged();
            }
            else
            {
                if (anno.setPosition(x_axis.getValue((int)current.getX())))
                {
                    requestUpdate();
                    fireAnnotationsChanged();
                }
            }
        }
        else if (mouse_mode == MouseMode.PAN_X  &&  start != null)
            x_axis.pan(mouse_start_x_range, x_axis.getValue((int)start.getX()), x_axis.getValue((int)current.getX()));
        else if (mouse_mode == MouseMode.PAN_Y  &&  start != null)
        {
            final YAxisImpl<XTYPE> axis = y_axes.get(mouse_y_axis);
            axis.pan(mouse_start_y_ranges.get(0), axis.getValue((int)start.getY()), axis.getValue((int)current.getY()));
        }
        else if (mouse_mode == MouseMode.PAN_PLOT  &&  start != null)
        {
            x_axis.pan(mouse_start_x_range, x_axis.getValue((int)start.getX()), x_axis.getValue((int)current.getX()));
            try
            {
                for (int i=0; i<y_axes.size(); ++i)
                {
                    final YAxisImpl<XTYPE> axis = y_axes.get(i);
                    axis.pan(mouse_start_y_ranges.get(i), axis.getValue((int)start.getY()), axis.getValue((int)current.getY()));
                }
            }
            catch (IndexOutOfBoundsException ex)
            {   // Axes could have been removed while looping. Never mind panning it.
                logger.log(Level.FINE, "Axis removed?", ex);
            }
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_X  ||
                 mouse_mode == MouseMode.ZOOM_IN_Y  ||
                 mouse_mode == MouseMode.ZOOM_IN_PLOT)
        {   // Show mouse feedback for ongoing zoom
            requestRedraw();
        }
        else if (show_crosshair)
            requestRedraw();
    }

    /** setOnMouseReleased */
    private void mouseUp(final MouseEvent e)
    {
        deselectPlotMarker();
        deselectMouseAnnotation();

        final Point2D start = mouse_start.orElse(null);
        final Point2D current = mouse_current.orElse(null);
        if (start == null  ||  current == null)
            return;

        if (mouse_mode == MouseMode.PAN_X)
        {
            mouseMove(e);
            undo.add(new ChangeAxisRanges<>(this, Messages.Pan_X, x_axis, mouse_start_x_range, x_axis.getValueRange(), false, false));
            fireXAxisChange();
            mouse_mode = MouseMode.PAN;
        }
        else if (mouse_mode == MouseMode.PAN_Y)
        {
            mouseMove(e);
            final YAxisImpl<XTYPE> y_axis = y_axes.get(mouse_y_axis);
            undo.add(new ChangeAxisRanges<>(this, Messages.Pan_Y,
                    Arrays.asList(y_axis),
                    mouse_start_y_ranges,
                    Arrays.asList(y_axis.getValueRange()),
                    pre_pan_auto_scales));
            if (y_axis.setAutoscale(false))
                fireAutoScaleChange(y_axis);
            fireYAxisChange(y_axis);
            mouse_mode = MouseMode.PAN;
        }
        else if (mouse_mode == MouseMode.PAN_PLOT)
        {
            mouseMove(e);
            final List<AxisRange<Double>> current_y_ranges = new ArrayList<>();
            for (YAxisImpl<XTYPE> axis : y_axes)
                current_y_ranges.add(axis.getValueRange());
            undo.add(new ChangeAxisRanges<>(this, Messages.Pan,
                    x_axis, mouse_start_x_range, x_axis.getValueRange(), false, false,
                    y_axes, mouse_start_y_ranges, current_y_ranges, pre_pan_auto_scales));
            fireXAxisChange();
            for (YAxisImpl<XTYPE> axis : y_axes)
            {
                if (axis.setAutoscale(false))
                    fireAutoScaleChange(axis);
                fireYAxisChange(axis);
            }
            mouse_mode = MouseMode.PAN;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_X)
        {   // X axis increases going _right_ just like mouse 'x' coordinate
            if (Math.abs(start.getX() - current.getX()) > ZOOM_PIXEL_THRESHOLD)
            {
                final int low = (int) Math.min(start.getX(), current.getX());
                final int high = (int) Math.max(start.getX(), current.getX());
                final AxisRange<XTYPE> original_x_range = x_axis.getValueRange();
                final AxisRange<XTYPE> new_x_range = new AxisRange<>(x_axis.getValue(low), x_axis.getValue(high));
                undo.execute(new ChangeAxisRanges<>(this, Messages.Zoom_In_X, x_axis, original_x_range, new_x_range, x_axis.isAutoscale(), false));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_Y)
        {   // Mouse 'y' increases going _down_ the screen
            if (Math.abs(start.getY() - current.getY()) > ZOOM_PIXEL_THRESHOLD)
            {
                final int high = (int)Math.min(start.getY(), current.getY());
                final int low = (int) Math.max(start.getY(), current.getY());
                final YAxisImpl<XTYPE> axis = y_axes.get(mouse_y_axis);
                undo.execute(new ChangeAxisRanges<>(this, Messages.Zoom_In_Y,
                        Arrays.asList(axis),
                        Arrays.asList(axis.getValueRange()),
                        Arrays.asList(new AxisRange<>(axis.getValue(low), axis.getValue(high))),
                        Arrays.asList(axis.isAutoscale())));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
        else if (mouse_mode == MouseMode.ZOOM_IN_PLOT)
        {
            if (Math.abs(start.getX() - current.getX()) > ZOOM_PIXEL_THRESHOLD  ||
                Math.abs(start.getY() - current.getY()) > ZOOM_PIXEL_THRESHOLD)
            {   // X axis increases going _right_ just like mouse 'x' coordinate
                int low = (int) Math.min(start.getX(), current.getX());
                int high = (int) Math.max(start.getX(), current.getX());
                final AxisRange<XTYPE> original_x_range = x_axis.getValueRange();
                final AxisRange<XTYPE> new_x_range = new AxisRange<>(x_axis.getValue(low), x_axis.getValue(high));

                // Mouse 'y' increases going _down_ the screen
                final List<AxisRange<Double>> original_y_ranges = new ArrayList<>();
                final List<AxisRange<Double>> new_y_ranges = new ArrayList<>();
                final List<Boolean> original_autoscale_values = new ArrayList<>();
                high = (int) Math.min(start.getY(), current.getY());
                low = (int) Math.max(start.getY(), current.getY());
                for (YAxisImpl<XTYPE> axis : y_axes)
                {
                    original_y_ranges.add(axis.getValueRange());
                    new_y_ranges.add(new AxisRange<>(axis.getValue(low), axis.getValue(high)));
                    original_autoscale_values.add(axis.isAutoscale());
                }
                undo.execute(new ChangeAxisRanges<>(this, Messages.Zoom_In, x_axis, original_x_range, new_x_range, x_axis.isAutoscale(), false,
                                                         y_axes, original_y_ranges, new_y_ranges, original_autoscale_values));
            }
            mouse_mode = MouseMode.ZOOM_IN;
        }
    }

    /** Zoom 'in' or 'out' from where the mouse was clicked
     *  @param x Mouse coordinate
     *  @param y Mouse coordinate
     *  @param factor Zoom factor, positive to zoom 'out'
     */
    @Override
    protected void zoomInOut(final double x, final double y, final double factor)
    {
        if (x_axis.getBounds().contains(x, y))
        {   // Zoom X axis
            final AxisRange<XTYPE> orig = x_axis.getValueRange();
            final boolean was_auto = x_axis.isAutoscale();
            if (x_axis.setAutoscale(false))
                fireAutoScaleChange(x_axis);
            x_axis.zoom((int)x, factor);
            undo.add(new ChangeAxisRanges<>(this, Messages.Zoom_Out_X, x_axis, orig, x_axis.getValueRange(), was_auto, false));
            fireXAxisChange();
        }
        else if (plot_area.getBounds().contains(x, y))
        {   // Zoom X..
            final AxisRange<XTYPE> orig_x = x_axis.getValueRange();
            final boolean was_auto = x_axis.isAutoscale();
            if (x_axis.setAutoscale(false))
                fireAutoScaleChange(x_axis);
            x_axis.zoom((int)x, factor);
            fireXAxisChange();
            // .. and Y axes
            final List<Boolean> old_autoscale = new ArrayList<>(y_axes.size());
            final List<AxisRange<Double>> old_range = new ArrayList<>(y_axes.size()),
                                          new_range = new ArrayList<>(y_axes.size());
            for (YAxisImpl<XTYPE> axis : y_axes)
            {
                old_autoscale.add(axis.isAutoscale());
                old_range.add(axis.getValueRange());
                axis.zoom((int)y, factor);
                new_range.add(axis.getValueRange());
                fireYAxisChange(axis);
            }
            undo.execute(new ChangeAxisRanges<>(this, Messages.Zoom_Out,
                    x_axis, orig_x, x_axis.getValueRange(),
                    was_auto, false,
                    y_axes, old_range, new_range, old_autoscale));
        }
        else
        {   // Zoom specific Y axis
            for (YAxisImpl<XTYPE> axis : y_axes)
                if (axis.getBounds().contains(x, y))
                {
                    final AxisRange<Double> orig = axis.getValueRange();
                    final boolean orig_autoscale = axis.isAutoscale();
                    if (axis.setAutoscale(false))
                        fireAutoScaleChange(axis);
                    axis.zoom((int)y, factor);
                    fireYAxisChange(axis);
                    undo.add(new ChangeAxisRanges<>(this, Messages.Zoom_Out_Y,
                            Arrays.asList(axis),
                            Arrays.asList(orig),
                            Arrays.asList(axis.getValueRange()),
                            Arrays.asList(orig_autoscale)));
                    break;
                }
        }
    }

    /** setOnMouseExited */
    private void mouseExit(final MouseEvent e)
    {
        deselectMouseAnnotation();

        // Redraw if we are actively panning or zooming, or crosshair needs to hide
        final boolean need_redraw = mouse_mode.ordinal() > MouseMode.INTERNAL_MODES.ordinal()  ||  show_crosshair;
        // Clear mouse position so drawMouseModeFeedback() won't restore cursor
        mouse_current = Optional.empty();
        // Reset cursor
        PlotCursors.setCursor(this, Cursor.DEFAULT);
        if (need_redraw)
            requestRedraw();
    }

    /** Stagger the range of axes
     *  @param disable_autoscale Disable autoscaling, or keep it as is?
     */
    public void stagger(final boolean disable_autoscale)
    {
        if (disable_autoscale)
            for (YAxisImpl<XTYPE> axis : y_axes)
                if (axis.setAutoscale(false))
                    fireAutoScaleChange(axis);
        plot_processor.stagger();
    }

    /** Enable autoscale
     *
     *  <p>.. for the value axis currently under the cursor,
     *  or all axes.
     */
    public void enableAutoScale()
    {
        final Point2D current = mouse_current.orElse(null);
        if (current == null)
            return;

        // Which axes to autoscale?
        final List<YAxisImpl<XTYPE>> axes = new ArrayList<>();
        final List<AxisRange<Double>> ranges =  new ArrayList<>();
        final List<Boolean> original_auto = new ArrayList<>();
        final List<Boolean> new_auto = new ArrayList<>();

        // Autoscale all if mouse in general plot region
        final boolean all = plot_area.getBounds().contains(current.getX(), current.getY());
        for (YAxisImpl<XTYPE> axis : y_axes)
            if (all  ||  axis.getBounds().contains(current.getX(), current.getY()))
            {   // Autoscale this axis
                if (!axis.isAutoscale())
                {
                    axes.add(axis);
                    ranges.add(axis.getValueRange());
                    original_auto.add(false);
                    new_auto.add(true);
                }
                // Only this axis?
                if (! all)
                    break;
            }
        if (! axes.isEmpty())
            undo.execute(new ChangeAxisRanges<>(this, Messages.Zoom_In,
                                                     null, null, null, false, false,
                                                     axes, ranges, ranges,
                                                     original_auto, new_auto));
    }

    /** Notify listeners */
    public void fireXAxisChange()
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedXAxis(x_axis);
    }

    /** Notify listeners */
    public void fireYAxisChange(final YAxisImpl<XTYPE> axis)
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedYAxis(axis);
    }

    /** Notify listeners */
    public void fireAutoScaleChange(final Axis<?> axis)
    {
        for (RTPlotListener<?> listener : listeners)
            listener.changedAutoScale(axis);
    }

    /** Notify listeners */
    public void fireGridChange(final Axis<?> axis)
    {
        for (RTPlotListener<?> listener : listeners)
            listener.changedGrid(axis);
    }

    /** Notify listeners */
    public void fireLogarithmicChange(final Axis<?> axis)
    {
        for (RTPlotListener<?> listener : listeners)
            listener.changedLogarithmic(axis);
    }

    /** Notify listeners */
    private void firePlotMarkersChanged(final int marker_index)
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedPlotMarker(marker_index);
    }

    /** Notify listeners */
    void fireAnnotationsChanged()
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedAnnotations();
    }

    /** Notify listeners */
    private void fireCursorsChanged()
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedCursors();
    }

    /** Notify listeners */
    public void fireToolbarChange(final boolean show)
    {
        for (RTPlotListener<XTYPE> listener : listeners)
            listener.changedToolbar(show);
    }

    /** Should be invoked when plot no longer used to release resources */
    @Override
    public void dispose()
    {
        super.dispose();

        // Release memory ASAP
        traces.clear();
        y_axes.clear();
        annotations.clear();
        listeners.clear();
        plot_markers.clear();
        plot_marker = null;
    }

    /**
     * Resets the X and Y axis ranges to the initial values. These initial values
     * are found by looking for the first {@link ChangeAxisRanges} action in the undo stack
     * obtained from the {@link org.phoebus.ui.undo.UndoableActionManager}. A
     * {@link ChangeAxisRanges} is created upon
     * the first zoom or pan action and hence contains the information about the initial X and
     * Y axes ranges.
     * The action taken in this method is itself a {@link ChangeAxisRanges} using the plot's current X and Y axis ranges
     * are used as the "original" ranges. It is executed as an {@link UndoableAction} and
     * can therefore be undone to return to the previous zoom/pan state.
     * If the undo stack is empty or does not contain any {@link ChangeAxisRanges} actions,
     * nothing happens.
     * A check is performed to determine if the number of traces (Y axes) in the current plot is the same as it
     * was initially. If not, an error dialog is shown to the effect that a reset is not possible.
     */
    public void resetAxisRanges()
    {
        final List<UndoableAction> undoableActions = undo.getUndoStack();
        if(undoableActions.isEmpty())
            return;

        for(UndoableAction undoableAction : undoableActions)
        {
            if (undoableAction instanceof ChangeAxisRanges)
            {
                @SuppressWarnings("unchecked")
                final ChangeAxisRanges<XTYPE> changeAxisRanges = (ChangeAxisRanges<XTYPE>)undoableAction;
                final AxisRange<XTYPE> originalXRange = changeAxisRanges.getOriginalXRange();
                final List<AxisRange<Double>> originalYRanges = changeAxisRanges.getOriginalYRanges();
                final AxisRange<XTYPE> currentXRange = x_axis.getValueRange();
                if (y_axes.size() != originalYRanges.size())
                {
                    final Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle(Messages.resetAxisRangesErrorTitle);
                    error.setHeaderText(Messages.resetAxisRangesErrorHeaderText);
                    DialogHelper.positionDialog(error, this, -100, -50);
                    error.showAndWait();
                    return;
                }
                final List<AxisRange<Double>> currentYRanges =  new ArrayList<>(y_axes.size());
                final List<Boolean> currentAutoScaleValues = new ArrayList<>(y_axes.size());
                for (YAxisImpl<XTYPE> axis : y_axes)
                {
                    currentYRanges.add(axis.getValueRange());
                    currentAutoScaleValues.add(axis.isAutoscale());
                }
                final ChangeAxisRanges<XTYPE> restoreAxisRanges = new ChangeAxisRanges<>(this, Messages.Zoom_In, x_axis, currentXRange, originalXRange, x_axis.isAutoscale(), false,
                        y_axes, currentYRanges, originalYRanges, currentAutoScaleValues);
                undo.execute(restoreAxisRanges);
                break;
            }
        }
    }
}
