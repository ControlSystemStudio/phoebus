/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class PolygonRepresentation extends JFXBaseRepresentation<Polygon, PolygonWidget>
{
    private final DirtyFlag dirty_display = new DirtyFlag();

    @Override
    public Polygon createJFXNode() throws Exception
    {
        final Polygon polygon = new Polygon();
        polygon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        polygon.setStrokeLineCap(StrokeLineCap.BUTT);
        return polygon;
    }

    @Override
    protected void registerListeners()
    {
        // Polygon can't use the default x/y handling from super.registerListeners();
        model_widget.propVisible().addUntypedPropertyListener(this::displayChanged);
        model_widget.propX().addUntypedPropertyListener(this::displayChanged);
        model_widget.propY().addUntypedPropertyListener(this::displayChanged);
        model_widget.propBackgroundColor().addUntypedPropertyListener(this::displayChanged);
        model_widget.propLineColor().addUntypedPropertyListener(this::displayChanged);
        model_widget.propLineWidth().addUntypedPropertyListener(this::displayChanged);
        model_widget.propPoints().addUntypedPropertyListener(this::displayChanged);
    }

    private void displayChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
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
                // Change points x/y relative to widget location into
                // on-screen location
                final int x = model_widget.propX().getValue();
                final int y = model_widget.propY().getValue();
                final Double[] points = model_widget.propPoints().getValue().asDoubleArray();
                for (int i=0; i<points.length; i+= 2)
                {
                    points[i] += x;
                    points[i+1] += y;
                }
                jfx_node.getPoints().setAll(points);
                jfx_node.setFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()));
                jfx_node.setStroke(JFXUtil.convert(model_widget.propLineColor().getValue()));
                jfx_node.setStrokeWidth(model_widget.propLineWidth().getValue());
            }
            else
                jfx_node.setVisible(false);
        }
    }
}
