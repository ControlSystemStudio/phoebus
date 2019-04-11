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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.LineStyle;
import org.csstudio.javafx.rtplot.PointType;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.TraceType;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;
import org.csstudio.javafx.rtplot.internal.util.IntList;
import org.csstudio.javafx.rtplot.internal.util.ScreenTransform;

/** Helper for painting a {@link Trace}
 *  @param <XTYPE> Data type of horizontal {@link Axis}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TracePainter<XTYPE extends Comparable<XTYPE>>
{
    // Implementation notes:
    // gc.drawPolyline() is faster than gc.drawLine() calls
    // plus it works better when using dashed or wide lines,
    // but it requires an int[] array of varying size.
    // IntList turned out to be about 3x faster than ArrayList<Integer>.

    /** Initial {@link IntList} size */
    private static final int INITIAL_ARRAY_SIZE = 2048;

    /** Fudge to avoid clip errors
     *
     *  <p>When coordinates are way outside the clip region,
     *  clipping fails and graphics are 'aliases' into the visible range.
     *  By moving clipped coordinates just 'OUTSIDE' the allowed region,
     *  rounding errors inside the clipping implementation are avoided.
     *  Strictly speaking, we'd have to compute the intersection of
     *  lines with the clip region, but this is much easier to implement.
     */
    final private static int OUTSIDE = 1000;
    private int x_min, x_max, y_min, y_max;

    final private int clipX(final double x)
    {
        if (x < x_min)
            return x_min;
        if (x > x_max)
            return x_max;
        return (int)x;
    }

    final private int clipY(final int y)
    {
        if (y < y_min)
            return y_min;
        if (y > y_max)
            return y_max;
        return y;
    }

    /** @param gc GC
     *  @param bounds Clipping bounds within which to paint
     *  @param opacity Opacity (0 .. 100 %) of 'area'
     *  @param x_transform Coordinate transform used by the x axis
     *  @param trace Trace, has reference to its value axis
     */
    final public void paint(final Graphics2D gc, final Rectangle bounds, final int opacity,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final Trace<XTYPE> trace)
    {
        if (! trace.isVisible())
            return;
        x_min = bounds.x - OUTSIDE;
        x_max = bounds.x + bounds.width + OUTSIDE;
        y_min = bounds.y - OUTSIDE;
        y_max = bounds.y + bounds.height + OUTSIDE;

        final Color old_color = gc.getColor();
        final Color old_bg = gc.getBackground();
        final Stroke old_width = gc.getStroke();

        final Color color = GraphicsUtils.convert(trace.getColor());
        final Color tpcolor = new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
        gc.setColor(color);

        // TODO Optimize drawing
        //
        // Determine first sample to draw via PlotDataSearch.findSampleLessOrEqual(),
        // then end drawing when reaching right end of area.
        //
        // Loop only once, performing drawMinMax, drawStdDev, drawValueStaircase in one loop
        //
        // For now, main point is that this happens in non-UI thread,
        // so the slower the better to test UI responsiveness.
        final PlotDataProvider<XTYPE> data = trace.getData();
        try
        {
            if (! data.getLock().tryLock(10, TimeUnit.SECONDS))
                throw new TimeoutException();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Skip painting " + trace + ", cannot lock " + data, ex);
            return;
        }
        try
        {
            final TraceType type = trace.getType();
            logger.log(Level.ALL, "Painting trace type " + type.toString());

            switch (type)
            {
            case NONE:
                break;
            case AREA:
                gc.setPaint(tpcolor);
                drawMinMaxArea(gc, x_transform, y_axis, data);
                gc.setPaint(color);
                drawStdDevLines(gc, x_transform, y_axis, data, trace.getWidth());
                drawValueStaircase(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case AREA_DIRECT:
                gc.setPaint(tpcolor);
                drawMinMaxArea(gc, x_transform, y_axis, data);
                gc.setPaint(color);
                drawStdDevLines(gc, x_transform, y_axis, data, trace.getWidth());
                drawValueLines(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case LINES:
                drawMinMaxLines(gc, x_transform, y_axis, data, trace.getWidth());
                gc.setPaint(tpcolor);
                drawStdDevLines(gc, x_transform, y_axis, data, trace.getWidth());
                gc.setPaint(color);
                drawValueStaircase(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case LINES_DIRECT:
                drawMinMaxLines(gc, x_transform, y_axis, data, trace.getWidth());
                gc.setPaint(tpcolor);
                drawStdDevLines(gc, x_transform, y_axis, data, trace.getWidth());
                gc.setPaint(color);
                drawValueLines(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case SINGLE_LINE:
                drawValueStaircase(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case SINGLE_LINE_DIRECT:
                drawValueLines(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case LINES_ERROR_BARS:
                drawErrorBars(gc, x_transform, y_axis, data, trace.getPointSize());
                drawValueLines(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
                break;
            case ERROR_BARS:
                // Compare error bars to area and min/max lines
                // gc.setPaint(tpcolor);
                // drawMinMaxArea(gc, x_transform, y_axis, data);
                // gc.setPaint(color);
                // drawMinMaxLines(gc, x_transform, y_axis, data, trace.getWidth());
                drawErrorBars(gc, x_transform, y_axis, data, trace.getPointSize());
                break;
            case BARS:
                final int width = trace.getWidth();
                if (width > 0)
                    drawBars(gc, x_transform, y_axis, data, width);
                else
                    drawHistogram(gc, x_transform, y_axis, data);
                break;
            default:
                drawValueStaircase(gc, x_transform, y_axis, data, trace.getWidth(), trace.getLineStyle());
            }

            final PointType point_type = trace.getPointType();
            if (point_type != PointType.NONE)
                drawPoints(gc, x_transform, y_axis, data, point_type, trace.getPointSize());
        }
        finally
        {
            data.getLock().unlock();
        }
        gc.setStroke(old_width);
        gc.setBackground(old_bg);
        gc.setColor(old_color);
    }

    // Basic dash patterns
    private static final float DASH[]       = { 10f,  5f };
    private static final float DOT[]        = {  2f, 10f };
    private static final float DASHDOT[]    = { 10f,  5f, 3f, 5f };
    private static final float DASHDOTDOT[] = { 10f,  5f, 3f, 5f, 3f, 5f };

    // Scale dash pattern as line width grows to prevent dots and dashes from merging
    private static float[] scale_dash(final float[] dash, final int line_width)
    {
        final int N = dash.length;
        final float[] scaled = new float[N];
        for (int i=0; i<N; ++i)
            scaled[i] = dash[i] * 0.5f * line_width;
        return scaled;
    }

    private final static Stroke createStroke(final int line_width, final LineStyle line_style)
    {
        switch (line_style)
        {
        case DASH:
            return new BasicStroke(line_width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, scale_dash(DASH, line_width), 0.0f);
        case DOT:
            return new BasicStroke(line_width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, scale_dash(DOT, line_width), 0.0f);
        case DASHDOT:
            return new BasicStroke(line_width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, scale_dash(DASHDOT, line_width), 0.0f);
        case DASHDOTDOT:
            return new BasicStroke(line_width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, scale_dash(DASHDOTDOT, line_width), 0.0f);
        case SOLID:
        default:
            return new BasicStroke(line_width);
        }
    }

    /** Draw values of data as staircase line
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param line_width
     *  @param line_style
     */
    final private void drawValueStaircase(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, final int line_width, final LineStyle line_style)
    {
        final IntList poly_x = new IntList(INITIAL_ARRAY_SIZE);
        final IntList poly_y = new IntList(INITIAL_ARRAY_SIZE);
        final int N = data.size();
        int last_x = -1, last_y = -1;
        gc.setStroke(createStroke(line_width, line_style));
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final int x = clipX(Math.round(x_transform.transform(item.getPosition())));
            final double value = item.getValue();
            if (poly_x.size() > 0  && x != last_x)
            {   // Staircase from last 'y'..
                poly_x.add(x);
                poly_y.add(last_y);
                last_x = x;
            }
            if (Double.isNaN(value))
            {
                flushPolyLine(gc, poly_x, poly_y, line_width);
                last_x = last_y = -1;
            }
            else
            {
                final int y = clipY(y_axis.getScreenCoord(value));
                if (last_x == x  &&  last_y == y)
                    continue;
                poly_x.add(x);
                poly_y.add(y);
                last_y = y;
            }
        }
        flushPolyLine(gc, poly_x, poly_y, line_width);
    }

    /** Draw values of data as direct line
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param line_width
     *  @param line_style
     */
    final private void drawValueLines(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, final int line_width, final LineStyle line_style)
    {
        final IntList value_poly_x = new IntList(INITIAL_ARRAY_SIZE);
        final IntList value_poly_y = new IntList(INITIAL_ARRAY_SIZE);
        final int N = data.size();

        gc.setStroke(createStroke(line_width, line_style));
        int last_x = -1, last_y = -1;
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final int x = clipX(Math.round(x_transform.transform(item.getPosition())));
            final double value = item.getValue();
            if (Double.isNaN(value))
                flushPolyLine(gc, value_poly_x, value_poly_y, line_width);
            else
            {
                final int y = clipY(y_axis.getScreenCoord(value));
                if (x == last_x  &&  y == last_y)
                    continue;
                value_poly_x.add(x);
                value_poly_y.add(y);
                last_x = x;
                last_y = y;
            }
        }
        flushPolyLine(gc, value_poly_x, value_poly_y, line_width);
    }

    /** Draw min/max outline
     *  @param graphics2D GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     */
    final private void drawMinMaxArea(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data)
    {
        final int N = data.size();
        // Assume N, might use less because end up with sections
        // separated by Double.NaN
        final IntList pos = new IntList(N);
        final IntList min = new IntList(N);
        final IntList max = new IntList(N);

        for (int i = 0;  i < N;  ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            double ymin = item.getMin();
            double ymax = item.getMax();
            if (Double.isNaN(ymin)  ||  Double.isNaN(ymax))
                flushPolyFill(gc, pos, min, max);
            else
            {
                final int x1 = clipX(x_transform.transform(item.getPosition()));
                final int y1min = clipY(y_axis.getScreenCoord(ymin));
                final int y1max = clipY(y_axis.getScreenCoord(ymax));
                pos.add(x1);
                min.add(y1min);
                max.add(y1max);
            }
        }
        flushPolyFill(gc, pos, min, max);
    }

    /** Draw min/max outline
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     */
    final private void drawMinMaxLines(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, final int line_width)
    {
        final IntList min_x = new IntList(INITIAL_ARRAY_SIZE);
        final IntList max_x = new IntList(INITIAL_ARRAY_SIZE);
        final IntList min_y = new IntList(INITIAL_ARRAY_SIZE);
        final IntList max_y = new IntList(INITIAL_ARRAY_SIZE);

        final int N = data.size();
        for (int i = 0;  i < N;  ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            double ymin = item.getMin();
            double ymax = item.getMax();
            if (Double.isNaN(ymin)  ||  Double.isNaN(ymax))
            {
                flushPolyLine(gc, min_x, min_y, line_width);
                flushPolyLine(gc, max_x, max_y, line_width);
            }
            else
            {
                final int x1 = clipX(x_transform.transform(item.getPosition()));
                final int y1min = clipY(y_axis.getScreenCoord(ymin));
                final int y1max = clipY(y_axis.getScreenCoord(ymax));
                min_x.add(x1);   min_y.add(y1min);
                max_x.add(x1);   max_y.add(y1max);
            }
        }
        flushPolyLine(gc, min_x, min_y, line_width);
        flushPolyLine(gc, max_x, max_y, line_width);
    }

    /** Draw std. deviation outline
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param line_width
     */
    final private void drawStdDevLines(final Graphics2D gc, final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, final int line_width)
    {
        final IntList lower_poly_y = new IntList(INITIAL_ARRAY_SIZE);
        final IntList upper_poly_y = new IntList(INITIAL_ARRAY_SIZE);
        final IntList lower_poly_x = new IntList(INITIAL_ARRAY_SIZE);
        final IntList upper_poly_x = new IntList(INITIAL_ARRAY_SIZE);

        final int N = data.size();
        for (int i = 0;  i < N;  ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            double value = item.getValue();
            double dev = item.getStdDev();
            if (Double.isNaN(value) ||  ! (dev > 0))
            {
                flushPolyLine(gc, lower_poly_x, lower_poly_y, line_width);
                flushPolyLine(gc, upper_poly_x, upper_poly_y, line_width);
            }
            else
            {
                final int x = clipX(x_transform.transform(item.getPosition()));
                final int low_y = clipY(y_axis.getScreenCoord(value - dev));
                final int upp_y = clipY(y_axis.getScreenCoord(value + dev));
                lower_poly_x.add(x);  lower_poly_y.add(low_y);
                upper_poly_x.add(x);  upper_poly_y.add(upp_y);
            }
        }
        flushPolyLine(gc, lower_poly_x, lower_poly_y, line_width);
        flushPolyLine(gc, upper_poly_x, upper_poly_y, line_width);
    }

    /** @param gc GC
     *  @param poly Points of poly line, will be cleared
     *  @param line_width
     */
    final private void flushPolyLine(final Graphics2D gc, final IntList poly_x, final IntList poly_y, final int line_width)
    {
        final int N = poly_x.size();
        if (N == 1)
            drawPoint(gc, poly_x.get(0), poly_y.get(0), line_width);
        else if (N > 1)
            gc.drawPolyline(poly_x.getArray(), poly_y.getArray(), N);
        poly_x.clear();
        poly_y.clear();
    }

    /** Draw error bar for each value
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param size
     */
    final private void drawErrorBars(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, final int size)
    {
        final int N = data.size();
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final double value = item.getValue();
            if (!Double.isNaN(value))
            {
                final int x = clipX(Math.round(x_transform.transform(item.getPosition())));
                final int y = clipY(y_axis.getScreenCoord(value));
                final double min = item.getMin();
                if (!Double.isNaN(min))
                {
                    final int ym = clipY(y_axis.getScreenCoord(min));
                    gc.drawLine(x, y, x, ym);
                    gc.drawLine(x-size/2, ym, x+size/2, ym);
                }
                final double max = item.getMax();
                if (!Double.isNaN(max))
                {
                    final int ym = clipY(y_axis.getScreenCoord(max));
                    gc.drawLine(x, y, x, ym);
                    gc.drawLine(x-size/2, ym, x+size/2, ym);
                }
            }
        }
    }

    /** Draw point for each value
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param point_type
     *  @param size
     */
    final private void drawPoints(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, PointType point_type, final int size)
    {
        final int N = data.size();
        int last_x = -1, last_y = -1;
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final double value = item.getValue();
            if (!Double.isNaN(value))
            {
                final int x = clipX(Math.round(x_transform.transform(item.getPosition())));
                final int y = clipY(y_axis.getScreenCoord(value));
                if (x == last_x  &&  y == last_y)
                    continue;
                switch (point_type)
                {
                case SQUARES:
                    gc.fillRect(x-size/2, y-size/2, size, size);
                    break;
                case DIAMONDS:
                    gc.fillPolygon(new int[] { x,        x+size/2, x,        x-size/2     },
                            new int[] { y-size/2, y,        y+size/2, y            }, 4);
                    break;
                case XMARKS:
                    gc.drawLine(x-size/2, y-size/2, x+size/2, y+size/2);
                    gc.drawLine(x-size/2, y+size/2, x+size/2, y-size/2);
                    break;
                case TRIANGLES:
                    gc.fillPolygon(new int[] { x,        x+size/2, x-size/2 },
                            new int[] { y-size/2, y+size/2, y+size/2 }, 3);
                    break;
                case CIRCLES:
                default:
                    drawPoint(gc, x, y, size);
                }
                last_x = x;
                last_y = y;
            }
        }
    }

    /** @param gc GC
     *  @param x Coordinate
     *  @param y .. of point on screen
     *  @param size
     */
    final private void drawPoint(final Graphics2D gc, final int x, final int y, final int size)
    {
        gc.fillOval(x-size/2, y-size/2, size, size);
    }

    /** Fill area. All lists will be cleared.
     *  @param gc GC
     *  @param pos Horizontal screen positions
     *  @param min Minimum 'y' values in screen coords
     *  @param max .. maximum
     */
    final private void flushPolyFill(final Graphics2D gc, final IntList pos, final IntList min, final IntList max)
    {
        final int N = pos.size();
        if (N <= 0)
            return;

        // 'direct' outline, point-to-point
        // Turn pos/min/max into array required by fillPolygon:
        // First sequence of x[], min[],
        // then x[], max[i] in reverse.
        final int N2 = N * 2;
        final int xpoints[] = new int[N2];
        final int ypoints[] = new int[N2];
        int tail = N2-1;
        for (int i=0; i<N; ++i)
        {   // i == 'head'
            xpoints[i] = xpoints[tail] = pos.get(i);
            ypoints[i] = min.get(i);
            ypoints[tail--] = max.get(i);
        }
        gc.fillPolygon(xpoints, ypoints, N2);

        pos.clear();
        min.clear();
        max.clear();
    }

    /** Draw bar for each value
     *
     *  <p>Bars are centered on each sample.
     *
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     *  @param width Width of each bar
     */
    final private void drawBars(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data, int width)
    {
        final int N = data.size();
        final int y0 = clipY(y_axis.getScreenCoord(0.0));
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final double value = item.getValue();
            if (Double.isNaN(value))
                continue;
            final int x = (int) Math.round(x_transform.transform(item.getPosition()));
            final int y = clipY(y_axis.getScreenCoord(value));
            if (y0 > y)
                gc.fillRect(x-width/2, y, width, y0-y);
            else // Value is below zero, draw down from y0
                gc.fillRect(x-width/2, y0, width, y-y0);
        }
    }

    /** Draw bar for each value
     *
     *  <p>Adjacent bars which start/end at the midpoints between samples.
     *
     *  @param gc GC
     *  @param x_transform Horizontal axis
     *  @param y_axis Value axis
     *  @param data Data
     */
    final private void drawHistogram(final Graphics2D gc,
            final ScreenTransform<XTYPE> x_transform, final YAxisImpl<XTYPE> y_axis,
            final PlotDataProvider<XTYPE> data)
    {
        // Bars need the x0, x1 center points between samples.
        // Each bar is drawn for the (last_x,last_y) while on sample (x,y):
        // Samples      :       (last_x,y)        (x,y)
        // Bar start/end:   x0              x1
        final int N = data.size();
        final int y0 = clipY(y_axis.getScreenCoord(0.0));
        int last_x1 = -1, last_x = -1, last_y = -1;
        for (int i=0; i<N; ++i)
        {
            final PlotDataItem<XTYPE> item = data.get(i);
            final double value = item.getValue();
            final int x = (int) Math.round(x_transform.transform(item.getPosition()));
            final int y = Double.isNaN(value) ?  -1  :  clipY(y_axis.getScreenCoord(value));
            if (last_x >= 0)
            {
                final int x0;
                if (last_x1 < 0)
                {   // 2nd sample. Make up a left edge for the first bar.
                    final int width = x - last_x;
                    x0 = last_x - width/2;
                }
                else
                    x0 = last_x1;
                final int x1 = (last_x + x)/2;
                if (last_y >= 0)
                    drawBar(gc, x0, x1, y0, last_y);
                last_x1 = x1;
            }
            last_x = x;
            last_y = y;
        }

        // Draw the last one..
        if (last_y >= 0)
        {   // Make up a left edge if there's only one sample
            if (last_x1 < 0)
                last_x1 = last_x - 10;
            final int width = (last_x - last_x1)*2;
            drawBar(gc, last_x1, last_x1 + width, y0, last_y);
        }
    }

    private void drawBar(final Graphics2D gc, final int x0, final int x1, final int y0, final int y)
    {
        final int width = x1 - x0;
        if (y > y0)
            gc.fillRect(x0, y0, width, y - y0);
        else
            gc.fillRect(x0, y, width, y0 - y);
    }
}
