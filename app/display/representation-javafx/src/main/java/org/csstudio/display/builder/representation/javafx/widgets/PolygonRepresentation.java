/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.PolygonWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.collections.ObservableList;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class PolygonRepresentation extends PolyBaseRepresentation<Polygon, PolygonWidget>
{
    private final DirtyFlag dirty_display = new DirtyFlag();
    private final UntypedWidgetPropertyListener displayChangedListener = this::displayChanged;

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
        super.registerListeners();

        model_widget.propVisible().addUntypedPropertyListener(displayChangedListener);
        model_widget.propX().addUntypedPropertyListener(displayChangedListener);
        model_widget.propY().addUntypedPropertyListener(displayChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineColor().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineWidth().addUntypedPropertyListener(displayChangedListener);
        model_widget.propLineStyle().addUntypedPropertyListener(displayChangedListener);
        model_widget.propPoints().addUntypedPropertyListener(displayChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        super.unregisterListeners();

        model_widget.propVisible().removePropertyListener(displayChangedListener);
        model_widget.propX().removePropertyListener(displayChangedListener);
        model_widget.propY().removePropertyListener(displayChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(displayChangedListener);
        model_widget.propLineColor().removePropertyListener(displayChangedListener);
        model_widget.propLineWidth().removePropertyListener(displayChangedListener);
        model_widget.propLineStyle().removePropertyListener(displayChangedListener);
        model_widget.propPoints().removePropertyListener(displayChangedListener);
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
                jfx_node.getPoints().setAll(scalePoints());
                jfx_node.setFill(JFXUtil.convert(model_widget.propBackgroundColor().getValue()));
                jfx_node.setStroke(JFXUtil.convert(model_widget.propLineColor().getValue()));
                jfx_node.setStrokeWidth(model_widget.propLineWidth().getValue());
                final int line_width = Math.max(1, model_widget.propLineWidth().getValue());
                final ObservableList<Double> dashes = jfx_node.getStrokeDashArray();
                // Scale dashes, dots and gaps by line width;
                // matches legacy opibuilder resp. Draw2D
                dashes.setAll(JFXUtil.getDashArray(model_widget.propLineStyle().getValue(), line_width));
            }
            else
                jfx_node.setVisible(false);
        }
    }
}
