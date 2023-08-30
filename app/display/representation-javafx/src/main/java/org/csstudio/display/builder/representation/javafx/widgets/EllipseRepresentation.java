/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class EllipseRepresentation extends JFXBaseRepresentation<Ellipse, EllipseWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookChangedListener = this::lookChanged;
    private final UntypedWidgetPropertyListener positionChangedListener = this::positionChanged;
    private Color background, line_color;

    @Override
    public Ellipse createJFXNode() throws Exception
    {
        final Ellipse ellipse = new Ellipse();
        ellipse.setStrokeLineJoin(StrokeLineJoin.ROUND);
        ellipse.setStrokeLineCap(StrokeLineCap.BUTT);
        updateColors();
        return ellipse;
    }

    @Override
    protected void registerListeners()
    {
        if (! toolkit.isEditMode())
            attachTooltip();
        // JFX Ellipse is based on center, not top-left corner,
        // so can't use the default from super.registerListeners();
        model_widget.propVisible().addUntypedPropertyListener(positionChangedListener);
        model_widget.propX().addUntypedPropertyListener(positionChangedListener);
        model_widget.propY().addUntypedPropertyListener(positionChangedListener);
        model_widget.propWidth().addUntypedPropertyListener(positionChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(positionChangedListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineWidth().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineStyle().addUntypedPropertyListener(lookChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        detachTooltip();
        model_widget.propVisible().removePropertyListener(positionChangedListener);
        model_widget.propX().removePropertyListener(positionChangedListener);
        model_widget.propY().removePropertyListener(positionChangedListener);
        model_widget.propWidth().removePropertyListener(positionChangedListener);
        model_widget.propHeight().removePropertyListener(positionChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(lookChangedListener);
        model_widget.propTransparent().removePropertyListener(lookChangedListener);
        model_widget.propLineColor().removePropertyListener(lookChangedListener);
        model_widget.propLineWidth().removePropertyListener(lookChangedListener);
        model_widget.propLineStyle().removePropertyListener(lookChangedListener);
    }

    private void positionChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_position.mark();
        toolkit.scheduleUpdate(this);
    }

    private void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        updateColors();
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void updateColors()
    {
        background = model_widget.propTransparent().getValue()
                   ? Color.TRANSPARENT
                   : JFXUtil.convert(model_widget.propBackgroundColor().getValue());
        line_color = JFXUtil.convert(model_widget.propLineColor().getValue());
    }

    @Override
    public void updateChanges()
    {
        // Not using default handling of X/Y super.updateChanges();
        if (dirty_position.checkAndClear())
        {
            if (model_widget.propVisible().getValue())
            {
                jfx_node.setVisible(true);
                final int x = model_widget.propX().getValue();
                final int y = model_widget.propY().getValue();
                final int w = model_widget.propWidth().getValue();
                final int h = model_widget.propHeight().getValue();
                jfx_node.setCenterX(x + w/2);
                jfx_node.setCenterY(y + h/2);
                jfx_node.setRadiusX(w/2);
                jfx_node.setRadiusY(h/2);
            }
            else
                jfx_node.setVisible(false);
        }
        if (dirty_look.checkAndClear())
        {
            jfx_node.setFill(background);
            jfx_node.setStroke(line_color);
            jfx_node.setStrokeType(StrokeType.INSIDE);
            jfx_node.setStrokeWidth(model_widget.propLineWidth().getValue());
            final int line_width = Math.max(1, model_widget.propLineWidth().getValue());
            final ObservableList<Double> dashes = jfx_node.getStrokeDashArray();
            // Scale dashes, dots and gaps by line width;
            // matches legacy opibuilder resp. Draw2D
            dashes.setAll(JFXUtil.getDashArray(model_widget.propLineStyle().getValue(), line_width));
        }
    }
}
