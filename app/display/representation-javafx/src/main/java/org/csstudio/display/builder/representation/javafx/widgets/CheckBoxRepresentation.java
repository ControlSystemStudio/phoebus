/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.logging.Level;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.CheckBoxWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.ui.javafx.TextUtils;

import javafx.application.Platform;
import javafx.geometry.Dimension2D;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class CheckBoxRepresentation extends RegionBaseRepresentation<CheckBox, CheckBoxWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_style = new DirtyFlag();
    private final UntypedWidgetPropertyListener sizeChangedListener = this::sizeChanged;
    private final UntypedWidgetPropertyListener styleChangedListener = this::styleChanged;
    private final WidgetPropertyListener<Integer> bitChangedListener = this::bitChanged;
    private final WidgetPropertyListener<String> labelChangedListener = this::labelChanged;
    private final WidgetPropertyListener<VType> valueChangedListener = this::valueChanged;

    protected volatile int bit = 0;
    protected volatile int value = 0;
    protected volatile boolean state = false;
    protected volatile String label = "";
    protected volatile boolean enabled = true;

    @Override
    protected final CheckBox createJFXNode() throws Exception
    {
        final CheckBox checkbox = new CheckBox(label);
        checkbox.setMinSize(ButtonBase.USE_PREF_SIZE, ButtonBase.USE_PREF_SIZE);
        checkbox.setMnemonicParsing(false);

        if (! toolkit.isEditMode())
            checkbox.setOnAction(event -> handlePress());
        return checkbox;
    }

    /** @param respond to button press */
    private void handlePress()
    {
        if (! enabled)
        {   // Ignore, restore current state of PV
            jfx_node.setSelected(state);
            return;
        }
        logger.log(Level.FINE, "{0} pressed", model_widget);
        // Ideally, PV will soon report the written value.
        // But for now restore the 'current' value of the PV
        // because PV may not change as desired,
        // so assert that widget always reflects the correct value.
        valueChanged(null, null, model_widget.runtimePropValue().getValue());
        Platform.runLater(this::confirm);
    }

    private void confirm()
    {
        final boolean prompt;
        switch (model_widget.propConfirmDialog().getValue())
        {
            case BOTH:
                prompt = true;
                break;
            case PUSH:
                prompt = !state;
                break;
            case RELEASE:
                prompt = state;
                break;
            case NONE:
            default:
                prompt = false;
        }

        if (prompt)
        {
            final String message = model_widget.propConfirmMessage().getValue();
            final String password = model_widget.propPassword().getValue();
            if (password.length() > 0)
            {
                if (toolkit.showPasswordDialog(model_widget, message, password) == null)
                    return;
            }
            else if (!toolkit.showConfirmationDialog(model_widget, message))
                return;
        }

        final int new_val = (bit < 0) ? (value == 0 ? 1 : 0) : (value ^ (1 << bit));
        toolkit.fireWrite(model_widget, new_val);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propAutoSize().addUntypedPropertyListener(sizeChangedListener);

        labelChanged(model_widget.propLabel(), null, model_widget.propLabel().getValue());
        model_widget.propLabel().addPropertyListener(labelChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleChangedListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(styleChangedListener);

        bitChanged(model_widget.propBit(), null, model_widget.propBit().getValue());
        model_widget.propBit().addPropertyListener(bitChangedListener);
        model_widget.runtimePropValue().addPropertyListener(valueChangedListener);

        // Initial Update
        valueChanged(null, null, model_widget.runtimePropValue().getValue());
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propAutoSize().removePropertyListener(sizeChangedListener);
        model_widget.propLabel().removePropertyListener(labelChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.propEnabled().removePropertyListener(styleChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleChangedListener);
        model_widget.propBit().removePropertyListener(bitChangedListener);
        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
        super.unregisterListeners();
    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void labelChanged(final WidgetProperty<String> property, final String old_value, final String new_value)
    {
        label = new_value != null ? new_value : model_widget.propLabel().getValue();
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void bitChanged(final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value)
    {
        bit = (new_value != null ? new_value : model_widget.propBit().getValue());
        stateChanged(bit, value);
    }

    private void valueChanged(final WidgetProperty<VType> property, final VType old_value, final VType new_value)
    {
        value = VTypeUtil.getValueNumber(new_value).intValue();
        stateChanged(bit, value);
    }

    private void stateChanged(final int new_bit, final int new_value)
    {
        state  = (new_bit < 0) ? new_value != 0 : ((new_value >> new_bit) & 1) == 1;
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_size.checkAndClear())
        {
            jfx_node.setPrefWidth(model_widget.propWidth().getValue());
            jfx_node.setPrefHeight(model_widget.propHeight().getValue());
            if (model_widget.propAutoSize().getValue())
            {
                final String text = jfx_node.getText();
                if (text == null || text.isEmpty())
                {
                    model_widget.propWidth().setValue(18);
                    model_widget.propHeight().setValue(18);
                }
                else
                {
                    final Dimension2D size = TextUtils.computeTextSize(JFXUtil.convert(model_widget.propFont().getValue()), text);
                    //  Heuristics that seems working (at least on macOS) with fonts sized from 6 to 64.
                    final int offset = 5 + (int) (2.1 * Math.exp(size.getHeight() / 23.0));
                    model_widget.propWidth().setValue(18 + offset + (int) Math.ceil(size.getWidth()));
                    model_widget.propHeight().setValue(Math.max(18, (int) Math.ceil(size.getHeight())));
                }
            }
        }
        if (dirty_content.checkAndClear())
            jfx_node.setSelected(state);
        if (dirty_style.checkAndClear())
        {
            jfx_node.setText(label);
            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            jfx_node.setTextFill(JFXUtil.convert(model_widget.propForegroundColor().getValue()));

            // Don't disable the widget, because that would also remove the
            // context menu etc.
            // Just apply a style that matches the disabled look.
            enabled = model_widget.propEnabled().getValue() &&
                      model_widget.runtimePropPVWritable().getValue();
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            if (model_widget.propAutoSize().getValue())
                sizeChanged(null, null, null);
        }
    }
}
