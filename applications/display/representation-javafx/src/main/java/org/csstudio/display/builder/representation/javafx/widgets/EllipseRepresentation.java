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
import org.csstudio.display.builder.model.widgets.EllipseWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.StrokeType;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class EllipseRepresentation extends JFXBaseRepresentation<Ellipse, EllipseWidget>
{
    private final DirtyFlag dirty_position = new DirtyFlag();
    private final DirtyFlag dirty_look = new DirtyFlag();
    private Color background, line_color;

    @Override
    public Ellipse createJFXNode() throws Exception
    {
        final Ellipse ellipse = new Ellipse();
        updateColors();
        return ellipse;
    }

    @Override
    protected void registerListeners()
    {
        // JFX Ellipse is based on center, not top-left corner,
        // so can't use the default from super.registerListeners();
        model_widget.propVisible().addUntypedPropertyListener(this::positionChanged);
        model_widget.propX().addUntypedPropertyListener(this::positionChanged);
        model_widget.propY().addUntypedPropertyListener(this::positionChanged);
        model_widget.propWidth().addUntypedPropertyListener(this::positionChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::positionChanged);

        model_widget.propBackgroundColor().addUntypedPropertyListener(this::lookChanged);
        model_widget.propTransparent().addUntypedPropertyListener(this::lookChanged);
        model_widget.propLineColor().addUntypedPropertyListener(this::lookChanged);
        model_widget.propLineWidth().addUntypedPropertyListener(this::lookChanged);
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
            jfx_node.setStrokeWidth(model_widget.propLineWidth().getValue());
            jfx_node.setStrokeType(StrokeType.INSIDE);
        }
    }
}
