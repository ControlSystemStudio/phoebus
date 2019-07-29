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
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.javafx.rtplot.Annotation;
import org.csstudio.javafx.rtplot.Messages;
import org.csstudio.javafx.rtplot.Trace;
import org.csstudio.javafx.rtplot.data.PlotDataItem;
import org.csstudio.javafx.rtplot.data.PlotDataProvider;
import org.csstudio.javafx.rtplot.data.PlotDataSearch;
import org.csstudio.javafx.rtplot.internal.util.GraphicsUtils;

import javafx.geometry.Point2D;

/** Annotation that's displayed on a YAxis
 *  @param <XTYPE> Data type used for the {@link PlotDataItem}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AnnotationImpl<XTYPE extends Comparable<XTYPE>> extends Annotation<XTYPE>
{
    /** 'X' marks the spot, and this is it's radius. */
    final private static int X_RADIUS = 4;

    private static final Stroke DASH = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 3f, 3f }, 1.0f);

    /** What part of this annotation has been selected by the mouse? */
    public static enum Selection
    {   /** Nothing */
        None,
        /** The reference point, i.e. the location on the trace */
        Reference,
        /** The body of the annotation */
        Body
    };

    private Selection selected = Selection.None;

    /** Screen location of reference point, set when painted */
    private Optional<Point> screen_pos = Optional.empty();

    /** Screen location of annotation body, set when painted */
    private Optional<Rectangle> screen_box = Optional.empty();

    /** Constructor */
    public AnnotationImpl(final boolean internal, final Trace<XTYPE> trace, final XTYPE position, final double value, final Point2D offset, final String text)
    {
        super(internal, trace, position, value, offset, text);
    }

    /** Set to new position
     *  @param position
     *  @return true on change
     */
    boolean setPosition(final XTYPE position)
    {
        if (this.position != position)
        {
            this.position = position;
            return true;
        }
        return false;
    }

    /** Set to new location and info
     *  @param position
     *  @param value
     *  @param info
     *  @return true on change
     */
    boolean setLocation(final XTYPE position, final double value, final String info)
    {
        if (this.position != position  ||
            this.value != value)
        {
            this.position = position;
            this.value = value;
            this.info = info;
            return true;
        }
        return false;
    }

    /** @param offset New offset from reference point to body of annotation */
    public void setOffset(final Point2D offset)
    {
        this.offset = offset;
    }

    /** @param text New annotation text, may include '\n' */
    public void setText(final String text)
    {
        this.text = text;
    }

    /** Check if the provided mouse location would select the annotation
     *  @param point Location of mouse on screen
     *  @return <code>true</code> if this annotation gets selected at that mouse location
     */
    boolean isSelected(final Point2D point)
    {
        // In case the handle is above the rect,
        // select that first
        if (areWithinDistance(screen_pos, point))
        {
            selected = Selection.Reference;
            return true;
        }

        final Optional<Rectangle> rect = screen_box;
        if (rect.isPresent()  &&  rect.get().contains(point.getX(), point.getY()))
        {
            selected = Selection.Body;
            return true;
        }

        return false;
    }

    /** @return Current selection state */
    Selection getSelection()
    {
        return selected;
    }

    void deselect()
    {
        selected = Selection.None;
    }

    private boolean areWithinDistance(final Optional<Point> pos, final Point2D point)
    {
        if (pos.isPresent())
        {
            final double dx = Math.abs(pos.get().x - point.getX());
            final double dy = Math.abs(pos.get().y - point.getY());
            return dx*dx + dy*dy <= X_RADIUS*X_RADIUS;
        }
        return false;
    }

    /** Compute new annotation position, value, info
     *  @param location Location near which the annotation should latch to a sample
     *  @return <code>true</code> if annotation changed
     *  @throws Exception
     */
    boolean updateValue(final XTYPE location) throws Exception
    {
        final PlotDataSearch<XTYPE> search = new PlotDataSearch<>();
        final PlotDataProvider<XTYPE> data = getTrace().getData();
        if (! data.getLock().tryLock(10, TimeUnit.SECONDS))
            throw new TimeoutException("Cannot update annotation, no lock on " + data);
        try
        {
            final int index = search.findSampleLessOrEqual(data, location);
            if (index < 0)
                return false;
            final PlotDataItem<XTYPE> sample = data.get(index);
            return setLocation(sample.getPosition(), sample.getValue(), sample.getInfo());
        }
        finally
        {
            data.getLock().unlock();
        }
    }

    /** Paint the annotation on given gc and axes. */
    void paint(final Graphics2D gc, final AxisPart<XTYPE> xaxis, final YAxisImpl<XTYPE> yaxis)
    {
        // Position on screen (or maybe actually outside of plot?)
        int x = xaxis.getScreenCoord(position);
        final int y = Double.isFinite(value) ? yaxis.getScreenCoord(value) : yaxis.getScreenRange().getLow();
        final boolean in_range = xaxis.getScreenRange().contains(x);

        String value_text = yaxis.getTicks().formatDetailed(value);
        final String units = trace.getUnits();
        if (! units.isEmpty())
            value_text += " " + units;
        String info_text = info;
        if (info_text == null)
            info_text = "";
        String label;
        try
        {
            label = MessageFormat.format(text,
                new Object[]
                {
                    trace.getName(),
                    xaxis.getTicks().format(position),
                    value_text,
                    info_text
                });
        }
        catch (IllegalArgumentException ex)
        {
            logger.log(Level.WARNING, "Error in annotation format", ex);
            label = Messages.AnnotationErr + "\n'" + text + "'";
        }

        // Layout like this when in_range
        //
        //    Text
        //    Blabla
        //    ___________
        //   /
        //  O
        //
        // When not in range, the 'O' is at the end of the line.
        final Rectangle metrics = GraphicsUtils.measureText(gc, label);
        final int tx;
        if (in_range)
        {   // Position text relative to sample
            tx = (int) (x + offset.getX());
        }
        else
        {   // Position at left or right side of plot
            offset = new Point2D(20, 20);
            if (x <= xaxis.getScreenRange().getLow())
            {
                x = xaxis.getScreenRange().getLow();
                tx = (int) (x + X_RADIUS + offset.getX());
            }
            else
            {
                x = xaxis.getScreenRange().getHigh();
                tx = (int) (x - X_RADIUS - metrics.getWidth() - offset.getX());
            }
        }
        final int ty = (int) (y + offset.getY());

        // Text
        final int txt_top = ty - metrics.height;
        final Rectangle rect = new Rectangle(tx, txt_top, metrics.width, metrics.height);
        final Color o_col = gc.getColor();
        gc.setColor(new Color(255, 255, 255, 170));
        gc.fillRect(rect.x, rect.y, rect.width, rect.height);
        gc.setColor(GraphicsUtils.convert(trace.getColor()));
        GraphicsUtils.drawMultilineText(gc, tx, txt_top + metrics.y, label);

        // Line over or under the text, rectangle when selected
        final int line_x = (x <= tx + metrics.width/2) ? tx : tx+metrics.width;
        final int line_y = (y > ty - metrics.height/2) ? ty : ty-metrics.height;
        gc.setColor(Color.BLACK);
        Stroke old_stroke = null;
        if (! in_range)
        {   // Dash the lines of off-screen annotation
            old_stroke = gc.getStroke();
            gc.setStroke(DASH);
        }
        if (selected != Selection.None)
            gc.drawRect(rect.x, rect.y, rect.width, rect.height);
        else // '___________'
            gc.drawLine(tx, line_y, tx+metrics.width, line_y);

        // Marker 'O' around the actual x/y point, line to annotation.
        // Line first from actual point, will then paint the 'O' over it
        gc.drawLine(x, y, line_x, line_y);
        if (! in_range)
            gc.setStroke(old_stroke);

        // Fill white, then draw around to get higher-contrast 'O'
        gc.setColor(Color.WHITE);
        gc.fillOval(x-X_RADIUS, y-X_RADIUS, 2*X_RADIUS, 2*X_RADIUS);
        gc.setColor(Color.BLACK);
        gc.drawOval(x-X_RADIUS, y-X_RADIUS, 2*X_RADIUS, 2*X_RADIUS);

        // Update the screen position so that we can later 'select' this annotation.
        screen_pos = Optional.of(new Point(x, y));
        if (in_range)
            screen_box = Optional.of(rect);
        else // Can drag annotation's position back into plot but can't move the rect
            screen_box = Optional.empty();

        gc.setColor(o_col);
    }
}
