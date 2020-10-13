/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.Arrays;
import java.util.List;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.PolylineWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class PolylineRepresentation extends PolyBaseRepresentation<Group, PolylineWidget>
{
    private final DirtyFlag dirty_display = new DirtyFlag();
    private final UntypedWidgetPropertyListener displayChangedListener = this::displayChanged;

    @Override
    public Group createJFXNode() throws Exception
    {
        final Polyline polyline = new Polyline();
        polyline.setStrokeLineJoin(StrokeLineJoin.MITER);
        polyline.setStrokeLineCap(StrokeLineCap.BUTT);
        return new Group(polyline, new Arrow(), new Arrow());
    }

    public static class Arrow extends Polygon
    {
        /** Adjust points of arrow
         *
         *  @param x1 x-coordinate for base of arrow (end of line)
         *  @param y1 y-coordinate for base of arrow (end of line)
         *  @param x2 x-coordinate for line extending from arrow
         *  @param y2 y-coordinate for line extending from arrow
         *  @param length Arrow length
         */
        public void adjustPoints(final double x1, final double y1, final double x2, final double y2, final int length)
        {
            getPoints().clear();
            getPoints().addAll(points(x1, y1, x2, y2, length));
        }

        //calculates points from coordinates of arrow's extending line
        private List<Double> points(final double x1, final double y1, final double x2, final double y2, final int length)
        {
            //calculate lengths (x-projection, y-projection, and magnitude) of entire arrow, including extending line
            final double dx = x1 - x2;
            final double dy = y1 - y2;
            final double d = Math.sqrt(dx * dx + dy * dy);
            //calculate x- and y-coordinates for midpoint of arrow base
            final double x0 = (d != 0) ? x1 - dx * length / d : x1;
            final double y0 = (d != 0) ? y1 - dy * length / d : y1;
            //calculate offset between midpoint and ends of arrow base
            final double x_ = (y1 - y0) / 4;
            final double y_ = (x1 - x0) / 4;
            //return result
            return Arrays.asList(x0 + x_, y0 - y_, x1, y1, x0 - x_, y0 + y_);
        }
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        model_widget.propVisible().addUntypedPropertyListener(displayChangedListener);
        model_widget.propX().addUntypedPropertyListener(displayChangedListener);
        model_widget.propY().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineColor().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineWidth().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineStyle().addUntypedPropertyListener(displayChangedListener);
        model_widget.propPoints().addUntypedPropertyListener(displayChangedListener);
        model_widget.propArrows().addUntypedPropertyListener(displayChangedListener);
        model_widget.propArrowLength().addUntypedPropertyListener(displayChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        super.unregisterListeners();

        model_widget.propVisible().removePropertyListener(displayChangedListener);
        model_widget.propX().removePropertyListener(displayChangedListener);
        model_widget.propY().removePropertyListener(displayChangedListener);
        model_widget.propLineColor().removePropertyListener(displayChangedListener);
        model_widget.propLineWidth().removePropertyListener(displayChangedListener);
        model_widget.propLineStyle().removePropertyListener(displayChangedListener);
        model_widget.propPoints().removePropertyListener(displayChangedListener);
        model_widget.propArrows().removePropertyListener(displayChangedListener);
        model_widget.propArrowLength().removePropertyListener(displayChangedListener);
    }

    @Override
    protected void displayChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_display.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        // Not using default handling of X/Y super.updateChanges();
        if (dirty_display.checkAndClear())
        {
            if (model_widget.propVisible().getValue())
            {
                jfx_node.setVisible(true);
                final Double[] points = scalePoints();
                final List<Node> children = jfx_node.getChildrenUnmodifiable();
                final Color color = JFXUtil.convert(model_widget.propLineColor().getValue());
                // Line width of 0 breaks the dash computation
                final int line_width = Math.max(1, model_widget.propLineWidth().getValue());
                final int arrows_val = model_widget.propArrows().getValue().ordinal();
                final int length = model_widget.propArrowLength().getValue();
                int i = 0;
                for (Node child : children)
                {
                    if (child instanceof Polyline)
                    {
                        final Polyline line = (Polyline) child;
                        line.getPoints().setAll(points);
                        final ObservableList<Double> dashes = line.getStrokeDashArray();
                        // Scale dashes, dots and gaps by line width;
                        // matches legacy opibuilder resp. Draw2D
                        dashes.setAll(JFXUtil.getDashArray(model_widget.propLineStyle().getValue(), line_width));
                    }
                    else //child instanceof Arrow
                    {
                        final Arrow arrow = (Arrow)child;
                        arrow.setFill(color);
                        if ((i & arrows_val) != 0 && points.length > 3)
                        {
                            arrow.setVisible(true);
                            if (i == 1) //to-arrow (pointing towards end point)
                                arrow.adjustPoints(points[0], points[1], points[2], points[3], length);
                            else //i == 2 //from-arrow (point towards first point)
                            {
                                final int len = points.length;
                                arrow.adjustPoints(points[len-2], points[len-1], points[len-4], points[len-3], length);
                            }
                        }
                        else
                            arrow.setVisible(false);
                    }
                    ((Shape) child).setStroke(color);
                    ((Shape) child).setStrokeWidth(line_width);
                    i++;
                }
            }
            else
                jfx_node.setVisible(false);
        }
    }
}
