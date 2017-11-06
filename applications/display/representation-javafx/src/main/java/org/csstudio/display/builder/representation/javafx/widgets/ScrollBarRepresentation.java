/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.Objects;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ScrollBarWidget;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.vtype.Display;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueUtil;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
public class ScrollBarRepresentation extends RegionBaseRepresentation<ScrollBar, ScrollBarWidget>
{
    private final DirtyFlag dirty_size = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();

    private volatile double min = 0.0;
    private volatile double max = 100.0;
    private volatile boolean active = false; //is updating UI to match PV?
    private volatile boolean isValueChanging = false; //is user interacting with UI (need to suppress UI updates)?
    private volatile boolean enabled = false;

    @Override
    protected ScrollBar createJFXNode() throws Exception
    {
        final ScrollBar scrollbar = new ScrollBar();
        scrollbar.setOrientation(model_widget.propHorizontal().getValue() ? Orientation.VERTICAL : Orientation.HORIZONTAL);
        scrollbar.setFocusTraversable(true);
        scrollbar.setOnKeyPressed((final KeyEvent event) ->
        {
            switch (event.getCode())
            {
            case DOWN: jfx_node.decrement();
                break;
            case UP: jfx_node.increment();
                break;
            case PAGE_UP:
                //In theory, this may be unsafe; i.e. if max/min are changed
                //after node creation.
                jfx_node.adjustValue(max);
                break;
            case PAGE_DOWN:
                jfx_node.adjustValue(min);
                break;
            default: break;
            }
        });
        if (! toolkit.isEditMode())
        {   // Prevent UI value update while actively changing
            scrollbar.addEventFilter(MouseEvent.MOUSE_PRESSED,  event -> isValueChanging = true);
            scrollbar.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> isValueChanging = false);
        }
        enablementChanged(null, null, null);
        limitsChanged(null, null, null);

        return scrollbar;
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(this::sizeChanged);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(this::limitsChanged);
        model_widget.propMinimum().addUntypedPropertyListener(this::limitsChanged);
        model_widget.propMaximum().addUntypedPropertyListener(this::limitsChanged);
        model_widget.propHorizontal().addPropertyListener(this::sizeChanged);
        model_widget.propBarLength().addPropertyListener(this::sizeChanged);
        model_widget.propIncrement().addPropertyListener(this::sizeChanged);

        model_widget.propEnabled().addPropertyListener(this::enablementChanged);

        //Since both the widget's PV value and the ScrollBar node's value property might be
        //written to independently during runtime, both must be listened to.
        model_widget.runtimePropValue().addPropertyListener(this::valueChanged);
        jfx_node.valueProperty().addListener(this::nodeValueChanged);

    }

    private void sizeChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void enablementChanged(final WidgetProperty<Boolean> property, final Boolean old_value, final Boolean new_value)
    {
        enabled = model_widget.propEnabled().getValue()  &&
                  model_widget.runtimePropPVWritable().getValue();
        dirty_enablement.mark();
        toolkit.scheduleUpdate(this);
    }

    private void limitsChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        double min_val = model_widget.propMinimum().getValue();
        double max_val = model_widget.propMaximum().getValue();
        if (model_widget.propLimitsFromPV().getValue())
        {
            //Try to get display range from PV
            final Display display_info = ValueUtil.displayOf(model_widget.runtimePropValue().getValue());
            if (display_info != null)
            {
                min_val = display_info.getLowerDisplayLimit();
                max_val = display_info.getUpperDisplayLimit();
            }
        }
        //If invalid limits, fall back to 0..100 range
        if (min_val >= max_val)
        {
            min_val = 0.0;
            max_val = 100.0;
        }

        min = min_val;
        max = max_val;

        dirty_size.mark();
        toolkit.scheduleUpdate(this);
    }

    private void nodeValueChanged(final ObservableValue<? extends Number> property, final Number old_value, final Number new_value)
    {
        if (active)
            return;
        final Tooltip tip = jfx_node.getTooltip();
        if (model_widget.propShowValueTip().getValue())
        {
            final String text = Objects.toString(new_value);
            if (tip != null)
                tip.setText(text);
            else
                jfx_node.setTooltip(new Tooltip(text));
        }
        else if (tip != null)
            jfx_node.setTooltip(null);
        toolkit.fireWrite(model_widget, new_value);
    }

    private void valueChanged(final WidgetProperty<? extends VType> property, final VType old_value, final VType new_value)
    {
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_enablement.checkAndClear())
        {
            jfx_node.setDisable(! enabled);
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
        }
        if (dirty_size.checkAndClear())
        {
            jfx_node.setPrefHeight(model_widget.propHeight().getValue());
            jfx_node.setPrefWidth(model_widget.propWidth().getValue());
            jfx_node.setMin(min);
            jfx_node.setMax(max);
            jfx_node.setOrientation(model_widget.propHorizontal().getValue() ? Orientation.HORIZONTAL : Orientation.VERTICAL);
            jfx_node.setUnitIncrement(model_widget.propIncrement().getValue());
            jfx_node.setBlockIncrement(model_widget.propIncrement().getValue());
            jfx_node.setVisibleAmount(model_widget.propBarLength().getValue());
        }
        if (dirty_value.checkAndClear())
        {
            active = true;
            try
            {
                double newval = VTypeUtil.getValueNumber( model_widget.runtimePropValue().getValue() ).doubleValue();
                if (newval < min) newval = min;
                else if (newval > max) newval = max;
                if (!isValueChanging)
                    jfx_node.setValue(newval);
            }
            finally
            {
                if (active)
                    active = false;
            }
        }
    }
}
