/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.Objects;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.RotationStep;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.model.util.FormatOptionHandler;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.model.widgets.TextUpdateWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class TextUpdateRepresentation extends RegionBaseRepresentation<Control, TextUpdateWidget>
{
    // Based on 'interactive' property when widget is created,
    // uses either JFX Label (static) or TextArea (interactive).
    // Common base class is Control.

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final WidgetPropertyListener<String> pvNameListener = this::pvnameChanged;
    private volatile String value_text = "<?>";
    private volatile Pos pos;

    /** Was there ever any transformation applied to the jfx_node?
     *
     *  <p>Used to optimize:
     *  If there never was a rotation, don't even _clear()_ it
     *  to keep the Node's nodeTransformation == null
     */
    private boolean was_ever_transformed = false;


    @Override
    public Control createJFXNode() throws Exception
    {   // Start out 'disconnected' until first value arrives
        value_text = computeText(null);

        if (model_widget.propInteractive().getValue()  &&  !toolkit.isEditMode())
        {
            final TextArea area = new TextArea();
            area.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            area.setEditable(false);
            area.getStyleClass().add("text_entry");
            return area;
        }
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
        model_widget.propWidth().addUntypedPropertyListener(styleListener);
        model_widget.propHeight().addUntypedPropertyListener(styleListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propTransparent().addUntypedPropertyListener(styleListener);
        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propHorizontalAlignment().addUntypedPropertyListener(styleListener);
        model_widget.propVerticalAlignment().addUntypedPropertyListener(styleListener);
        model_widget.propRotationStep().addUntypedPropertyListener(styleListener);
        model_widget.propWrapWords().addUntypedPropertyListener(styleListener);
        model_widget.propFormat().addUntypedPropertyListener(contentListener);
        model_widget.propPrecision().addUntypedPropertyListener(contentListener);
        model_widget.propShowUnits().addUntypedPropertyListener(contentListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(contentListener);

        model_widget.propPVName().addPropertyListener(pvNameListener);

        // Initial update in case runtimePropValue already has value before we registered listener
        contentChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(styleListener);
        model_widget.propHeight().removePropertyListener(styleListener);
        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propTransparent().removePropertyListener(styleListener);
        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propHorizontalAlignment().removePropertyListener(styleListener);
        model_widget.propVerticalAlignment().removePropertyListener(styleListener);
        model_widget.propRotationStep().removePropertyListener(styleListener);
        model_widget.propWrapWords().removePropertyListener(styleListener);
        model_widget.propFormat().removePropertyListener(contentListener);
        model_widget.propPrecision().removePropertyListener(contentListener);
        model_widget.propShowUnits().removePropertyListener(contentListener);
        model_widget.runtimePropValue().removePropertyListener(contentListener);
        model_widget.propPVName().removePropertyListener(pvNameListener);

        super.unregisterListeners();
    }


    @Override
    protected void attachTooltip()
    {
        // Use the formatted text for "$(pv_value)"
        TooltipSupport.attach(jfx_node, model_widget.propTooltip(), () -> value_text);
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        pos = JFXUtil.computePos(model_widget.propHorizontalAlignment().getValue(),
                                 model_widget.propVerticalAlignment().getValue());
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    /** @param value Current value of PV
     *  @return Text to show, "<pv name>" if disconnected (no value)
     */
    private String computeText(final VType value)
    {
        Objects.requireNonNull(model_widget, "No widget");
        if (value == null)
            return "<" + model_widget.propPVName().getValue() + ">";
        if (value == PVWidget.RUNTIME_VALUE_NO_PV)
            return "";
        return FormatOptionHandler.format(value,
                                          model_widget.propFormat().getValue(),
                                          model_widget.propPrecision().getValue(),
                                          model_widget.propShowUnits().getValue());
    }

    private void pvnameChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {   // PV name typically changes in edit mode.
        // -> Show new PV name.
        // Runtime could deal with disconnect/reconnect for new PV name
        // -> Also OK to show disconnected state until runtime
        //    subscribes to new PV, so we eventually get values from new PV.
        value_text = computeText(null);
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final String new_text = computeText(model_widget.runtimePropValue().getValue());
        // Skip update if it's the same text
        if (value_text.equals(new_text))
            return;
        value_text = new_text;
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_style.checkAndClear())
        {
            final RotationStep rotation = model_widget.propRotationStep().getValue();
            final int width = model_widget.propWidth().getValue(),
                      height = model_widget.propHeight().getValue();
            switch (rotation)
            {
            case NONE:
                jfx_node.setPrefSize(width, height);
                if (was_ever_transformed)
                    jfx_node.getTransforms().clear();
                break;
            case NINETY:
                jfx_node.setPrefSize(height, width);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-height, 0));
                was_ever_transformed = true;
                break;
            case ONEEIGHTY:
                jfx_node.setPrefSize(width, height);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(-width, -height));
                was_ever_transformed = true;
                               break;
            case MINUS_NINETY:
                jfx_node.setPrefSize(height, width);
                jfx_node.getTransforms().setAll(new Rotate(-rotation.getAngle()),
                                                new Translate(0, -width));
                was_ever_transformed = true;
                break;
            }

            if (model_widget.propTransparent().getValue())
                jfx_node.setBackground(null); // No fill
            else
            {
                final Color color = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
                jfx_node.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
            }
            if (jfx_node instanceof Label)
            {
                final Label label = (Label) jfx_node;
                Color color = JFXUtil.convert(model_widget.propForegroundColor().getValue());
                label.setTextFill(color);
                label.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
                label.setAlignment(pos);
                label.setWrapText(model_widget.propWrapWords().getValue());
            }
            else
            {
                final TextArea area = (TextArea) jfx_node;
                final StringBuilder style = new StringBuilder(100);
                style.append("-fx-text-fill:");
                JFXUtil.appendWebRGB(style, model_widget.propForegroundColor().getValue()).append(";");

                // http://stackoverflow.com/questions/27700006/how-do-you-change-the-background-color-of-a-textfield-without-changing-the-border
                final WidgetColor back_color = model_widget.propBackgroundColor().getValue();
                style.append("-fx-control-inner-background: ");
                JFXUtil.appendWebRGB(style, back_color).append(";");
                area.setStyle(style.toString());
                area.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
                // Alignment (pos) not supported
                area.setWrapText(model_widget.propWrapWords().getValue());
            }
        }
        if (dirty_content.checkAndClear())
        {
            if (jfx_node instanceof Label)
                ((Label)jfx_node).setText(value_text);
            else
                ((TextArea)jfx_node).setText(value_text);
        }
    }
}
