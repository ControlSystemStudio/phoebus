/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (C) 2016 European Spallation Source ERIC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.csstudio.display.builder.representation.javafx.widgets;


import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.controlsfx.control.ToggleSwitch;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.SlideButtonWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;


/**
 * JavaFX representation of the SlideButton model.
 *
 * @author claudiorosati, European Spallation Source ERIC
 * @version 1.0.0 21 Aug 2018
 */
@SuppressWarnings("nls")
public class SlideButtonRepresentation extends RegionBaseRepresentation<HBox, SlideButtonWidget> {

    /*
     * This code was written as close as possible to the CheckBoxRepresentation's one.
     */

    private final DirtyFlag dirty_size    = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final DirtyFlag dirty_style   = new DirtyFlag();
    private final UntypedWidgetPropertyListener   sizeChangedListener  = this::sizeChanged;
    private final UntypedWidgetPropertyListener   styleChangedListener = this::styleChanged;
    private final WidgetPropertyListener<Integer> bitChangedListener   = this::bitChanged;
    private final WidgetPropertyListener<String>  labelChangedListener = this::labelChanged;
    private final WidgetPropertyListener<VType>   valueChangedListener = this::valueChanged;

    protected volatile int     bit          = 0;
    protected volatile boolean enabled      = true;
    protected volatile String  labelContent = "";
    protected volatile boolean state        = false;
    protected volatile int     value        = 0;

    private volatile ToggleSwitch button;
    private volatile Label        label;

    private volatile Color  foreground;
    private volatile String state_colors;

    private volatile AtomicBoolean updating = new AtomicBoolean();

    @Override
    public void updateChanges ( ) {

        super.updateChanges();

        if ( dirty_size.checkAndClear() ) {
            if ( model_widget.propAutoSize().getValue() ) {
                jfx_node.setPrefSize(-1, -1);
                jfx_node.autosize();
                model_widget.propWidth().setValue((int) jfx_node.getWidth());
                model_widget.propHeight().setValue((int) jfx_node.getHeight());
            }
            else
                jfx_node.setPrefSize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());
        }

        if ( dirty_style.checkAndClear() ) {

            button.setStyle(state_colors);

            label.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            label.setText(labelContent);
            label.setTextFill(foreground);

            // Don't disable the widget, because that would also remove the context menu etc.
            // Just apply a style that matches the disabled look.
            enabled = model_widget.propEnabled().getValue() && model_widget.runtimePropPVWritable().getValue();

            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);

            // Since jfx_node.isManaged() == false, need to trigger layout
            jfx_node.layout();

            if (model_widget.propAutoSize().getValue())
                sizeChanged(null, null, null);
        }

        if ( dirty_content.checkAndClear() ) {
            if ( updating.compareAndSet(false, true) ) {
                button.setSelected(state);
                updating.set(false);
            }
        }

    }

    @Override
    public HBox createJFXNode ( ) throws Exception {

        button = new ToggleSwitch();

        button.setMinSize(37, 20);
        button.setPrefSize(37, 20);
        button.setGraphicTextGap(0);
        button.setMnemonicParsing(false);

        if (! toolkit.isEditMode() )
            button.addEventFilter(MouseEvent.MOUSE_RELEASED, event ->
            {
                // To avoid setting a new value when context menu is requested,
                // slide only if primary button was pressed.
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    handleSlide();
                }
                event.consume();
            });

        label = new Label(labelContent);

        label.setMaxWidth(Double.MAX_VALUE);
        label.setMnemonicParsing(false);
        HBox.setHgrow(label, Priority.ALWAYS);
        HBox hbox = new HBox(6, button, label);
        hbox.setAlignment(Pos.CENTER_RIGHT);
        return hbox;

    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    protected void registerListeners ( ) {
        super.registerListeners();

        model_widget.propAutoSize().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(sizeChangedListener);
        model_widget.propWidth().addUntypedPropertyListener(sizeChangedListener);

        labelChanged(model_widget.propLabel(), null, model_widget.propLabel().getValue());

        model_widget.propLabel().addPropertyListener(labelChangedListener);

        styleChanged(null, null, null);

        model_widget.propEnabled().addUntypedPropertyListener(styleChangedListener);
        model_widget.propFont().addUntypedPropertyListener(styleChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propOffColor().addUntypedPropertyListener(styleChangedListener);
        model_widget.propOnColor().addUntypedPropertyListener(styleChangedListener);
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
        model_widget.propAutoSize().removePropertyListener(sizeChangedListener);
        model_widget.propHeight().removePropertyListener(sizeChangedListener);
        model_widget.propWidth().removePropertyListener(sizeChangedListener);
        model_widget.propLabel().removePropertyListener(labelChangedListener);
        model_widget.propEnabled().removePropertyListener(styleChangedListener);
        model_widget.propFont().removePropertyListener(styleChangedListener);
        model_widget.propForegroundColor().removePropertyListener(styleChangedListener);
        model_widget.propOffColor().removePropertyListener(styleChangedListener);
        model_widget.propOnColor().removePropertyListener(styleChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleChangedListener);
        model_widget.propBit().removePropertyListener(bitChangedListener);
        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
        super.unregisterListeners();
    }

    private void bitChanged ( final WidgetProperty<Integer> property, final Integer old_value, final Integer new_value ) {

        bit = ( new_value != null ? new_value : model_widget.propBit().getValue() );

        stateChanged(bit, value);

    }

    private void confirm ( ) {

        final boolean prompt;

        switch ( model_widget.propConfirmDialog().getValue() ) {
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

        if ( prompt ) {

            final String message = model_widget.propConfirmMessage().getValue();
            final String password = model_widget.propPassword().getValue();

            if ( password.length() > 0 ) {
                if ( toolkit.showPasswordDialog(model_widget, message, password) == null ) {
                    return;
                }
            } else if ( !toolkit.showConfirmationDialog(model_widget, message) ) {
                return;
            }

        }

        final int new_val = ( bit < 0 ) ? ( value == 0 ? 1 : 0 ) : ( value ^ ( 1 << bit ) );

        toolkit.fireWrite(model_widget, new_val);

    }

    private void handleSlide ( ) {
        if ( !updating.get() ) {
            if ( enabled ) {
                logger.log(Level.FINE, "{0} slided", model_widget);
                Platform.runLater(this::confirm);
            }
        }
    }

    private void labelChanged ( final WidgetProperty<String> property, final String old_value, final String new_value ) {

        labelContent = ( new_value != null ) ? new_value : model_widget.propLabel().getValue();

        dirty_style.mark();
        toolkit.scheduleUpdate(this);

    }

    private void sizeChanged ( final WidgetProperty<?> property, final Object old_value, final Object new_value ) {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void stateChanged ( final int new_bit, final int new_value ) {

        state = ( new_bit < 0 ) ? new_value != 0 : ( ( new_value >> new_bit ) & 1 ) == 1;

        dirty_content.mark();
        toolkit.scheduleUpdate(this);

    }

    private void styleChanged ( final WidgetProperty<?> property, final Object old_value, final Object new_value ) {

        foreground = JFXUtil.convert(model_widget.propForegroundColor().getValue());
        state_colors = "-db-toggle-switch-off: " + JFXUtil.webRGB(model_widget.propOffColor().getValue()) + ";" + "-db-toggle-switch-on: " + JFXUtil.webRGB(model_widget.propOnColor().getValue()) + ";";

        dirty_style.mark();
        toolkit.scheduleUpdate(this);

    }

    private void valueChanged ( final WidgetProperty<VType> property, final VType old_value, final VType new_value ) {

        value = VTypeUtil.getValueNumber(new_value).intValue();

        stateChanged(bit, value);

    }
}
