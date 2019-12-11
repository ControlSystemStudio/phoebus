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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.YAxis;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.IntList;

/** A 'Y' or 'vertical' axis.
 *  <p>
 *  The plot maintains one or more Y axes.
 *  Each trace to plot needs to be assigned to a Y axis.
 *
 *  @param <XTYPE> Data type of the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class YAxisImpl<XTYPE extends Comparable<XTYPE>> extends NumericAxis implements YAxis<XTYPE>
{
    /** How to label the axis */
    final private AxisLabelProvider<XTYPE> label_provider;

    /** Computed in getPixelWidth:
     *  Location of labels, and Y-separation between labels,
     *  used to show SEPARATOR.
     *
     *  Calls to getPixelWidth and paint will come from the same
     *  thread which updates the plot.
     *
     *  Number of label_provider entries should match the size of
     *  label_x and label_y entries after they're set in getDesiredPixelSize,
     *  but note that label_provider could change at any time:
     *  getPixelWidth ran with N labels,
     *  labels change, requesting new layout and thus call to getDesiredPixelSize,
     *  but paint() is called before that happened.
     */
    final private IntList label_x = new IntList(2), label_y = new IntList(2);
    private int label_y_separation;

    /** Show on right side? */
    private volatile boolean is_right = false;

    /** Traces on this axis.
     *
     *  <p>{@link CopyOnWriteArrayList} adds thread safety.
     *  In addition, SYNC on traces when adding a trace
     *  to avoid duplicates
     */
    final private List<TraceImpl<XTYPE>> traces = new CopyOnWriteArrayList<>();

    /** Construct a new Y axis.
     *  <p>
     *  Note that end users will typically <b>not</b> create new Y axes,
     *  but instead ask the <code>Chart</code> for a new or existing axis.
     *  <p>
     *  @param name The axis name.
     *  @param listener Listener.
     */
    public YAxisImpl(final String name, final PlotPartListener listener)
    {
        super(name, listener,
                false,      // vertical
                0.0, 10.0); // Initial range
        label_provider = new AxisLabelProvider<>(this);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsingAxisName()
    {
        return label_provider.isUsingAxisName();
    }

    /** {@inheritDoc} */
    @Override
    public void useAxisName(final boolean use_axis_name)
    {
        if (label_provider.isUsingAxisName() == use_axis_name)
            return;
        label_provider.useAxisName(use_axis_name);
        requestLayout();
        requestRefresh();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isUsingTraceNames()
    {
        return label_provider.isUsingTraceNames();
    }

    /** {@inheritDoc} */
    @Override
    public void useTraceNames(final boolean use_trace_names)
    {
        if (label_provider.isUsingTraceNames() == use_trace_names)
            return;
        label_provider.useTraceNames(use_trace_names);
        requestLayout();
        requestRefresh();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOnRight()
    {
        return is_right;
    }

    /** {@inheritDoc} */
    @Override
    public void setOnRight(final boolean right)
    {
        if (is_right == right)
            return;
        is_right = right;
        requestLayout();
        requestRefresh();
    }

    /** Add trace to axis
     *  @param trace {@link Trace}
     *  @throws IllegalArgumentException if trace already on axis
     */
    void addTrace(final TraceImpl<XTYPE> trace)
    {
        Objects.requireNonNull(trace);
        // CopyOnWriteArrayList is thread-safe, but race between 2 threads
        // could still end up with duplicate.
        // Could synchronized here, but then FindBugs complains.
        traces.add(trace);
        // So checking afterwards if there are more than the one expected entries
        if (traces.stream().filter((existing) -> existing == trace).count() > 1)
            throw new IllegalArgumentException("Trace " + trace.getName() + " already on Y Axis " + getName());
        requestLayout();
    }

    /** Remove a trace from the axis.
     *  <p>
     *  Not meant for end users, the trace is supposed to remove itself.
     */
    public void removeTrace(final Trace<XTYPE> trace)
    {
        Objects.requireNonNull(trace);
        if (! traces.remove(trace))
            throw new Error("Internal YAxis error. Axis " + getName() + " does not hold trace " + trace.getName());
        requestLayout();
    }

    /** 'Current' list of traces as thread-safe, read-only iterable,
     *  a snapshot of the underlying {@link CopyOnWriteArrayList}
     *
     * @return Current list of traces for an axis.
     */
    Iterable<TraceImpl<XTYPE>> getTraces()
    {
        return traces;
    }

    /** {@inheritDoc */
    @Override
    public int getDesiredPixelSize(final Rectangle region, final Graphics2D gc)
    {
        logger.log(Level.FINE, "YAxis({0}) layout for {1}", new Object[] { getName(),  region });

        if (! isVisible())
            return 0;

        gc.setFont(label_font);
        FontMetrics metrics = gc.getFontMetrics();
        final int x_sep = metrics.getHeight();
        // Start layout of labels at x=0, 'left',
        // to determine how many fit into one line.
        // Later update these relative x positions based on 'left' or 'right' axis.
        int x = 0;
        int lines = 0;

        // Compute layout of labels
        label_provider.start();
        label_x.clear();
        label_y.clear();
        final IntList label_length = new IntList(2);
        while (label_provider.hasNext())
        {
            label_y_separation = metrics.stringWidth(label_provider.getSeparator());
            label_length.add(metrics.stringWidth(label_provider.getLabel()));
        }
        while (label_provider.hasNext());

        // Compute location of each label
        int next = 0;
        final int N = label_length.size();
        while (next < N)
        {   // Determine how many can fit on one line
            int many = 0;
            int height = 0;
            for (int i=next; i<N; ++i)
                if (height + label_length.get(i) < region.height)
                {
                    ++many;
                    height += label_length.get(i);
                    if (i > 0)
                        height += label_y_separation;
                }
                else
                    break;
            // Can't fit any? Show one, will be clipped
            if (many == 0)
            {
                many = 1;
                height = region.height;
            }
            // Draw one line
            int y = region.y + (region.height+height)/2;
            for (int i=next; i<next+many; ++i)
            {
                y -= label_length.get(i);
                label_x.add(x);
                label_y.add(y);
                if (i < N-1)
                    y -= label_y_separation;
            }
            x += x_sep;
            next += many;
            ++lines;
        }

        final int x_correction = is_right ? region.x + region.width - lines*x_sep : region.x;
        for (int i=label_x.size()-1; i>=0; --i)
            label_x.set(i, label_x.get(i) + x_correction);

        gc.setFont(scale_font);
        metrics = gc.getFontMetrics();
        final int scale_size = metrics.getHeight();

        // Width of labels, width of (rotated) axis text, tick markers.
        return lines * x_sep + scale_size + TICK_LENGTH;
    }

    /** {@inheritDoc} */
    @Override
    public int[] getPixelGaps(final Graphics2D gc)
    {
        if (! isVisible())
            return super.getPixelGaps(gc);

        gc.setFont(scale_font);
        final FontMetrics metrics = gc.getFontMetrics();

        final List<MajorTick<Double>> major_ticks = ticks.getMajorTicks();
        if (major_ticks.isEmpty())
            return super.getPixelGaps(gc);

        // Measure first and last tick
        final int low = metrics.stringWidth(major_ticks.get(0).getLabel());
        final int high = metrics.stringWidth(major_ticks.get(major_ticks.size()-1).getLabel());

        return new int[] { low / 2, high / 2 };
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
        final Color old_bg = gc.getBackground();
        final Color old_fg = gc.getColor();
        final Color foreground = GraphicsUtils.convert(getColor());
        gc.setColor(foreground);
        gc.setFont(scale_font);

        // Simple line for the axis
        final int line_x, tick_x, minor_x;
        if (is_right)
        {
            line_x = region.x;
            tick_x = region.x + TICK_LENGTH;
            minor_x = region.x + MINOR_TICK_LENGTH;
        }
        else
        {
            line_x = region.x + region.width-1;
            tick_x = region.x + region.width-1 - TICK_LENGTH;
            minor_x = region.x + region.width-1 - MINOR_TICK_LENGTH;
        }
        gc.drawLine(line_x, region.y, line_x, region.y + region.height-1);
        computeTicks(gc);

        // Major tick marks
        Rectangle avoid = null;
        for (MajorTick<Double> tick : ticks.getMajorTicks())
        {
            final int y = getScreenCoord(tick.getValue());
            gc.setStroke(TICK_STROKE);
            gc.drawLine(line_x, y, tick_x, y);

            // Grid line
            if (show_grid)
            {   // Dashed line
                gc.setColor(grid_color);
                gc.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1, new float[] { 5 }, 0));
                gc.drawLine(plot_bounds.x, y, plot_bounds.x + plot_bounds.width-1, y);
                gc.setColor(foreground);
            }
            gc.setStroke(old_width);

            // Tick Label
            avoid = drawTickLabel(gc, y, tick.getLabel(), false, avoid);
        }

        // Minor tick marks
        for (MinorTick<Double> tick : ticks.getMinorTicks())
        {
            final int y = getScreenCoord(tick.getValue());
            gc.drawLine(minor_x, y, line_x, y);
        }

        gc.setColor(old_fg);
        gc.setBackground(old_bg);

        gc.setFont(label_font);
        paintLabels(gc);
    }

    protected void paintLabels(final Graphics2D gc)
    {
        if (label_y == null)
            return;
        final Color old_fg = gc.getColor();
        label_provider.start();
        int i = 0;

        while (label_provider.hasNext()  &&  i < label_x.size())
        {   // Draw labels at pre-computed locations
            if (i > 0)
                GraphicsUtils.drawVerticalText(gc, label_x.get(i-1), label_y.get(i-1) - label_y_separation,
                        label_provider.getSeparator(), !is_right);
            gc.setColor(GraphicsUtils.convert(label_provider.getColor()));
            GraphicsUtils.drawVerticalText(gc,
                    label_x.get(i), label_y.get(i), label_provider.getLabel(), !is_right);
            gc.setColor(old_fg);
            ++i;
        }
    }

    /** @param gc
     *  @param screen_y Screen location of label along the axis
     *  @param mark Label text
     *  @param floating Add 'floating' box?
     *  @param avoid Outline of previous label to avoid
     *  @return Outline of this label or the last one if skipping this label
     */
    private Rectangle drawTickLabel(final Graphics2D gc, final int screen_y, final String mark, final boolean floating, final Rectangle avoid)
    {
        final Rectangle region = getBounds();
        gc.setFont(scale_font);
        final FontMetrics metrics = gc.getFontMetrics();
        final int mark_height = metrics.stringWidth(mark);
        final int mark_width = metrics.getHeight();
        final int x = is_right ? region.x + TICK_LENGTH : region.x + region.width - TICK_LENGTH - mark_width;
        int y = screen_y  - mark_height/2;
        // Correct location of top label to remain within region
        if (y < 0)
            y = 0;

        final Rectangle outline = new Rectangle(x-BORDER,  y-BORDER, mark_width+2*BORDER, mark_height+2*BORDER);
        if (floating)
        {
            if (is_right)
                gc.drawLine(x - TICK_LENGTH, screen_y, x, screen_y);
            else
                gc.drawLine(x + mark_width, screen_y, x + mark_width + TICK_LENGTH, screen_y);

            // Box around label
            gc.clearRect(outline.x, outline.y, outline.width, outline.height);
            gc.drawRect(outline.x, outline.y, outline.width, outline.height);
        }

        if (avoid != null  &&  outline.intersects(avoid))
            return avoid;
        // Debug: Outline of text
        // gc.drawRect(x,  y, mark_width, mark_height); // Debug outline of tick label
        GraphicsUtils.drawVerticalText(gc, x, y, mark, !is_right);
        return outline;
    }

    /** {@inheritDoc} */
    @Override
    public void drawTickLabel(final Graphics2D gc, final Double tick)
    {
        if (! isVisible())
            return;

        final int y0 = getScreenCoord(tick);
        final String mark = ticks.formatDetailed(tick);

        drawTickLabel(gc, y0, mark, true, null);
    }
}
