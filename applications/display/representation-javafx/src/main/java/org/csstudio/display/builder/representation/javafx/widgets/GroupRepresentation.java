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

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class GroupRepresentation extends JFXBaseRepresentation<Pane, GroupWidget>
{
    private final DirtyFlag dirty_border = new DirtyFlag();

    private static final int border_width = 1;

    // top-level 'Pane' provides background color and border

    /** Label on top of background */
    private Label label;

    /** Inner pane that holds child widgets */
    private Pane inner;

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
        final UntypedWidgetPropertyListener listener = this::borderChanged;
        model_widget.propForegroundColor().addUntypedPropertyListener(listener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(listener);
        model_widget.propName().addUntypedPropertyListener(listener);
        model_widget.propStyle().addUntypedPropertyListener(listener);
        model_widget.propFont().addUntypedPropertyListener(listener);
        model_widget.propWidth().addUntypedPropertyListener(listener);
        model_widget.propHeight().addUntypedPropertyListener(listener);
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
            final int width = model_widget.propWidth().getValue();
            final int height = model_widget.propHeight().getValue();

            jfx_node.setPrefSize(width, height);
            jfx_node.setBackground(new Background(new BackgroundFill(background_color, null, null)));

            final WidgetFont font = model_widget.propFont().getValue();
            label.setFont(JFXUtil.convert(font));
            label.setText(model_widget.propName().getValue());

            final Style style = model_widget.propStyle().getValue();
            if (style == Style.NONE)
            {
                inset = 0;
                jfx_node.setBorder(null);

                label.setVisible(false);

                inner.relocate(0, 0);
                model_widget.runtimePropInsets().setValue(new int[] { 0, 0 });
            }
            else if (style == Style.TITLE)
            {
                inset = (int) (1.2*font.getSize());
                jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

                label.setVisible(true);
                label.relocate(0, 0);
                label.setPrefSize(width, inset);
                label.setTextFill(background_color);
                label.setBackground(new Background(new BackgroundFill(foreground_color, CornerRadii.EMPTY, Insets.EMPTY)));

                inner.relocate(border_width, inset+border_width);
                model_widget.runtimePropInsets().setValue(new int[] { border_width, inset+border_width });
            }
            else if (style == Style.LINE)
            {
                inset = 0;
                jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

                label.setVisible(false);

                inner.relocate(border_width, border_width);
                model_widget.runtimePropInsets().setValue(new int[] { border_width, border_width });
            }
            else // GROUP
            {
                inset = (int) (1.2*font.getSize());
                jfx_node.setBorder(new Border(new BorderStroke(foreground_color, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT,
                                                               new Insets(inset/2))));
                label.setVisible(true);
                label.relocate(inset, 0);
                label.setPrefSize(Label.USE_COMPUTED_SIZE, Label.USE_COMPUTED_SIZE);
                label.setTextFill(foreground_color);
                label.setBackground(new Background(new BackgroundFill(background_color, CornerRadii.EMPTY, Insets.EMPTY)));

                inner.relocate(inset, inset);
                model_widget.runtimePropInsets().setValue(new int[] { inset, inset });
            }
        }
    }
}
