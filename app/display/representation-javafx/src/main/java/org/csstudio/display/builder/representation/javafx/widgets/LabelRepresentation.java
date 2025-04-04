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
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.widgets.LabelWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.phoebus.ui.javafx.TextUtils;

import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class LabelRepresentation extends RegionBaseRepresentation<Label, LabelWidget>
{
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener contentChangedListener = this::contentChanged;
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;
    private volatile Pos pos;

    /** Was there ever any transformation applied to the jfx_node?
     *
     *  <p>Used to optimize:
     *  If there never was a rotation, don't even _clear()_ it
     *  to keep the Node's nodeTransformation == null
     */
    private boolean was_ever_transformed = false;

    @Override
    public Label createJFXNode() throws Exception
    {
        final Label label = new Label();
        label.getStyleClass().add("text_update");
        return label;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                                 model_widget.propVerticalAlignment().getValue());
        model_widget.propWidth().addUntypedPropertyListener(styleChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(styleChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(styleChangedListener);
        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(styleChangedListener);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(styleChangedListener);
        model_widget.propRotationStep().addUntypedPropertyListener(styleChangedListener);
        model_widget.propWrapWords().addUntypedPropertyListener(styleChangedListener);

        // Changing the text might require a resize,
        // so handle those properties together.
        model_widget.propText().addUntypedPropertyListener(contentChangedListener);
        model_widget.propAutoSize().addUntypedPropertyListener(contentChangedListener);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(styleChangedListener);
        model_widget.propHeight().removePropertyListener(styleChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(styleChangedListener);
        model_widget.propTransparent().removePropertyListener(styleChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.propHorizontalAlignment().removePropertyListener(styleChangedListener);
        model_widget.propVerticalAlignment().removePropertyListener(styleChangedListener);
        model_widget.propRotationStep().removePropertyListener(styleChangedListener);
        model_widget.propWrapWords().removePropertyListener(styleChangedListener);
        model_widget.propText().removePropertyListener(contentChangedListener);
        model_widget.propAutoSize().removePropertyListener(contentChangedListener);
        super.unregisterListeners();
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                                 model_widget.propVerticalAlignment().getValue());
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        // First check text because autosize might change size..
        if (dirty_content.checkAndClear())
        {
            final String text = model_widget.propText().getValue();
            jfx_node.setText(text);
            if (model_widget.propAutoSize().getValue())
            {
                final Dimension2D size = TextUtils.computeTextSize(JFXUtil.convert(model_widget.propFont().getValue()), text);
                model_widget.propWidth().setValue(  (int) Math.ceil(size.getWidth()) );
                model_widget.propHeight().setValue( (int) Math.ceil(size.getHeight()) );
                dirty_style.mark();
            }
        }
        // .. and then check for size changes
        if (dirty_style.checkAndClear())
        {
            final int width = model_widget.propWidth().getValue();
            final int height = model_widget.propHeight().getValue();
            final RotationStep rotation = model_widget.propRotationStep().getValue();
            switch (rotation)
            {
            case NONE:
                jfx_node.setPrefSize(width, height);
                jfx_node.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                if (was_ever_transformed)
                    jfx_node.getTransforms().clear();
                break;
            case NINETY:
                jfx_node.setPrefSize(height, width);
                jfx_node.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-height, 0));
                was_ever_transformed = true;
                break;
            case ONEEIGHTY:
                jfx_node.setPrefSize(width, height);
                jfx_node.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-width, -height));
                was_ever_transformed = true;
                               break;
            case MINUS_NINETY:
                jfx_node.setPrefSize(height, width);
                jfx_node.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(0, -width));
                was_ever_transformed = true;
                break;
            }
            jfx_node.setAlignment(pos);
            jfx_node.setTextAlignment(TextAlignment.values()[model_widget.propHorizontalAlignment().getValue().ordinal()]);
            jfx_node.setWrapText(model_widget.propWrapWords().getValue());

            Color color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
            jfx_node.setTextFill(color);
            if (model_widget.propTransparent().getValue())
                jfx_node.setBackground(null); // No fill
            else
            {   // Fill background
                color = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
                jfx_node.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
            }
            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        }
    }
}
