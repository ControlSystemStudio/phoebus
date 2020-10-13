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
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class RectangleRepresentation extends JFXBaseRepresentation<Rectangle, RectangleWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookChangedListener = this::lookChanged;
    private final UntypedWidgetPropertyListener sizeChangedListener = this::sizeChanged;
    private volatile Color background, line_color;
    private volatile boolean ignore_mouse = false;

    @Override
    public Rectangle createJFXNode() throws Exception
    {
        final Rectangle rect = new Rectangle();
        rect.setStrokeLineJoin(StrokeLineJoin.ROUND);
        rect.setStrokeLineCap(StrokeLineCap.BUTT);
        updateColors();
        return rect;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propCornerWidth().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propCornerHeight().addUntypedPropertyListener(sizeChangedListener);

        model_widget.propBackgroundColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineColor().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineWidth().addUntypedPropertyListener(lookChangedListener);
        model_widget.propLineStyle().addUntypedPropertyListener(lookChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propCornerWidth().removePropertyListener(sizeChangedListener);
        model_widget.propCornerHeight().removePropertyListener(sizeChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(lookChangedListener);
        model_widget.propTransparent().removePropertyListener(lookChangedListener);
        model_widget.propLineColor().removePropertyListener(lookChangedListener);
        model_widget.propLineWidth().removePropertyListener(lookChangedListener);
        model_widget.propLineStyle().removePropertyListener(lookChangedListener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
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
        final boolean transparent = model_widget.propTransparent().getValue();
        background = transparent
                   ? Color.TRANSPARENT
                   : JFXUtil.convert(model_widget.propBackgroundColor().getValue());
        line_color = JFXUtil.convert(model_widget.propLineColor().getValue());

        // Displays converted from EDM can have transparent rectangles
        // on top of other content, including buttons.
        // Such rectangles should pass mouse clicks through to the underlying widgets,
        // at runtime, unless there are actions on the rectangle
        // https://github.com/ControlSystemStudio/cs-studio/issues/2149
        ignore_mouse = transparent  &&
                       ! toolkit.isEditMode()  &&
                       model_widget.propActions().getValue().getActions().isEmpty();
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            jfx_node.setWidth(model_widget.propWidth().getValue());
            jfx_node.setHeight(model_widget.propHeight().getValue());
            jfx_node.setArcWidth(2 * model_widget.propCornerWidth().getValue());
            jfx_node.setArcHeight(2 * model_widget.propCornerHeight().getValue());
        }
        if (dirty_look.checkAndClear())
        {
            jfx_node.setMouseTransparent(ignore_mouse);
            jfx_node.setFill(background);
            jfx_node.setStroke(line_color);
            jfx_node.setStrokeWidth(model_widget.propLineWidth().getValue());
            jfx_node.setStrokeType(StrokeType.INSIDE);
            final int line_width = Math.max(1, model_widget.propLineWidth().getValue());
            final ObservableList<Double> dashes = jfx_node.getStrokeDashArray();
            // Scale dashes, dots and gaps by line width;
            // matches legacy opibuilder resp. Draw2D
            dashes.setAll(JFXUtil.getDashArray(model_widget.propLineStyle().getValue(), line_width));
        }
    }
}
