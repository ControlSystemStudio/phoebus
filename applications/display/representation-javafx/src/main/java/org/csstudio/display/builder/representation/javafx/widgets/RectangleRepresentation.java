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
import org.csstudio.display.builder.model.widgets.RectangleWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class RectangleRepresentation extends JFXBaseRepresentation<Rectangle, RectangleWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_look = new DirtyFlag();
    private volatile Color background, line_color;
    private volatile boolean ignore_mouse = false;

    @Override
    public Rectangle createJFXNode() throws Exception
    {
        final Rectangle rect = new Rectangle();
        updateColors();
        return rect;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propCornerWidth().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propCornerHeight().addUntypedPropertyListener(this::sizeChanged);

        model_widget.propBackgroundColor().addUntypedPropertyListener(this::lookChanged);
        model_widget.propTransparent().addUntypedPropertyListener(this::lookChanged);
        model_widget.propLineColor().addUntypedPropertyListener(this::lookChanged);
        model_widget.propLineWidth().addUntypedPropertyListener(this::lookChanged);
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
        }
    }
}
