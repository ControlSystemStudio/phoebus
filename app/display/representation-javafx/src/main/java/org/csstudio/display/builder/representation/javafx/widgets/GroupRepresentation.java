/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.List;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.properties.WidgetFont;
import org.csstudio.display.builder.model.widgets.GroupWidget;
import org.csstudio.display.builder.model.widgets.GroupWidget.Style;
import org.csstudio.display.builder.representation.javafx.JFXUtil;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class GroupRepresentation extends JFXBaseRepresentation<Pane, GroupWidget>
{
    private static final Insets TITLE_PADDING = new Insets(1, 3, 1, 3);
    private static final int BORDER_WIDTH = 1;
    static final BorderWidths EDIT_NONE_BORDER = new BorderWidths(0.5, 0.5, 0.5, 0.5, false, false, false, false);
    static final BorderStrokeStyle EDIT_NONE_DASHED = new BorderStrokeStyle(
        StrokeType.INSIDE,
        StrokeLineJoin.MITER,
        StrokeLineCap.BUTT,
        10,
        0,
        List.of(Double.valueOf(11.11), Double.valueOf(7.7), Double.valueOf(3.3), Double.valueOf(7.7))
    );

    private final DirtyFlag dirty_border = new DirtyFlag();
    private final UntypedWidgetPropertyListener borderChangedListener = this::borderChanged;

    // top-level 'Pane' provides background color and border

    /** Label on top of background */
    private Label label;

    /** Inner pane that holds child widgets */
    private Pane inner;

    private volatile boolean firstUpdate = true;
    private volatile int inset = 10;
    private volatile Color foreground_color, background_color;

    @Override
    public Pane createJFXNode() throws Exception
    {
        label = new Label();
        inner = new Pane();

        computeColors();

        return new Pane(label, inner);
    }

    @Override
    protected Parent getChildParent(final Parent parent)
    {
        return inner;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propForegroundColor().addUntypedPropertyListener(borderChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(borderChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(borderChangedListener);
        model_widget.propName().addUntypedPropertyListener(borderChangedListener);
        model_widget.propStyle().addUntypedPropertyListener(borderChangedListener);
        model_widget.propFont().addUntypedPropertyListener(borderChangedListener);
        model_widget.propWidth().addUntypedPropertyListener(borderChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(borderChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propForegroundColor().removePropertyListener(borderChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(borderChangedListener);
        model_widget.propTransparent().removePropertyListener(borderChangedListener);
        model_widget.propName().removePropertyListener(borderChangedListener);
        model_widget.propStyle().removePropertyListener(borderChangedListener);
        model_widget.propFont().removePropertyListener(borderChangedListener);
        model_widget.propWidth().removePropertyListener(borderChangedListener);
        model_widget.propHeight().removePropertyListener(borderChangedListener);
        super.unregisterListeners();
    }

    private void borderChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        computeColors();
        dirty_border.mark();
        toolkit.scheduleUpdate(this);
    }

    private void computeColors()
    {
        foreground_color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
        background_color = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_border.checkAndClear())
        {
            if (model_widget.propTransparent().getValue())
                jfx_node.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));
            else
                jfx_node.setBackground(new Background(new BackgroundFill(background_color, null, null)));

            final WidgetFont font = model_widget.propFont().getValue();

            label.setFont(JFXUtil.convert(font));
            label.setText(model_widget.propName().getValue());

            final Style style = model_widget.propStyle().getValue();
            int x = model_widget.propX().getValue();
            int y = model_widget.propY().getValue();
            int width = model_widget.propWidth().getValue();
            int height = model_widget.propHeight().getValue();

            //  Reset position and size as if style == Style.NONE.
            int[] insets = new int[4];

            System.arraycopy(model_widget.runtimePropInsets().getValue(), 0, insets, 0, insets.length);

            final boolean hasChildren = !model_widget.runtimeChildren().getValue().isEmpty();
            if (hasChildren)
            {
                inner.relocate(- insets[0], - insets[1]);
                x += insets[0];
                y += insets[1];
                width -= insets[2];
                height -= insets[3];
            }

            switch (style)
            {
                case NONE:
                {
                    inset = 0;
                    insets[0] = 0;
                    insets[1] = 0;
                    insets[2] = 0;
                    insets[3] = 0;

                    // In edit mode, show outline because otherwise hard to
                    // handle the totally invisible group
                    if (toolkit.isEditMode())
                        jfx_node.setBorder(new Border(new BorderStroke(foreground_color, EDIT_NONE_DASHED, CornerRadii.EMPTY, EDIT_NONE_BORDER)));
                    else
                        jfx_node.setBorder(null);

                    label.setVisible(false);
                    break;
                }
                case LINE:
                {
                    inset = 0;
                    insets[0] = BORDER_WIDTH;
                    insets[1] = BORDER_WIDTH;
                    insets[2] = 2 * insets[0];
                    insets[3] = 2 * insets[1];

                    jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                    label.setVisible(false);
                    break;
                }
                case TITLE:
                {
                    inset = 2 + (int) ( 1.2 * font.getSize() );
                    insets[0] = BORDER_WIDTH;
                    insets[1] = inset + BORDER_WIDTH;
                    insets[2] = 2 * insets[0];
                    insets[3] = insets[0] + insets[1];

                    jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                    label.setVisible(true);
                    label.relocate(0, BORDER_WIDTH);
                    label.setPadding(TITLE_PADDING);
                    label.setPrefSize(width + ( ( !firstUpdate && hasChildren ) ? insets[2] : 0 ), inset);
                    label.setTextFill(background_color);
                    label.setBackground(new Background(new BackgroundFill(foreground_color, CornerRadii.EMPTY, Insets.EMPTY)));
                    break;
                }
                case GROUP:
                default:
                {
                    inset = 2 + (int) ( 1.2 * font.getSize() );
                    insets[0] = inset;
                    insets[1] = inset;
                    insets[2] = 2 * insets[0];
                    insets[3] = 2 * insets[1];

                    jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT, new Insets(inset / 2))));
                    label.setVisible(true);
                    label.relocate(inset, 0);
                    label.setPadding(TITLE_PADDING);
                    label.setPrefSize(Label.USE_COMPUTED_SIZE, Label.USE_COMPUTED_SIZE);
                    label.setTextFill(foreground_color);
                    label.setBackground(new Background(new BackgroundFill(background_color, CornerRadii.EMPTY, Insets.EMPTY)));
                    break;
                }
            }

            inner.relocate(insets[0], insets[1]);
            model_widget.runtimePropInsets().setValue(insets);

            if (firstUpdate)
                firstUpdate = false;
            else if (hasChildren)
            {
                x -= insets[0];
                y -= insets[1];
                width += insets[2];
                height += insets[3];

                model_widget.propX().setValue(x);
                model_widget.propY().setValue(y);
                model_widget.propWidth().setValue(width);
                model_widget.propHeight().setValue(height);
            }

            jfx_node.relocate(x, y);
            jfx_node.setPrefSize(width, height);
        }
    }
}
