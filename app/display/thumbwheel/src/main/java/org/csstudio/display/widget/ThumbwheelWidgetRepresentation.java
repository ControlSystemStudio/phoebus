package org.csstudio.display.widget;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import java.util.logging.Level;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

public class ThumbwheelWidgetRepresentation extends RegionBaseRepresentation<ThumbWheel, ThumbwheelWidget> {

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();
    private final DirtyFlag dirty_behavior = new DirtyFlag();

    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final UntypedWidgetPropertyListener limitsChangedListener = this::limitsChanged;

    private final WidgetPropertyListener<Boolean> enablementChangedListener = this::enablementChanged;
    private final ChangeListener<Number> thumbwheelValueChangedListener = this::thumbwheelValueChanged;
    private final WidgetPropertyListener<VType> valueChangedListener = this::valueChanged;

    private volatile boolean enabled = false;
    private volatile double value = 0.0;
    private volatile double min = ThumbwheelWidget.DEFAULT_MIN;
    private volatile double max = ThumbwheelWidget.DEFAULT_MAX;

    @Override
    protected ThumbWheel createJFXNode() throws Exception {
        final ThumbWheel thumbWheel = new ThumbWheel();
        return thumbWheel;
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleListener);
        model_widget.propHeight().addUntypedPropertyListener(styleListener);

        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propIncrementColor().addUntypedPropertyListener(styleListener);
        model_widget.propDecrementColor().addUntypedPropertyListener(styleListener);
        model_widget.propInvalidColor().addUntypedPropertyListener(styleListener);

        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propDecimalDigits().addUntypedPropertyListener(styleListener);
        model_widget.propIntegerDigits().addUntypedPropertyListener(styleListener);

        model_widget.propMinimum().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propMaximum().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(limitsChangedListener);

        model_widget.propEnabled().addPropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().addPropertyListener(enablementChangedListener);

        model_widget.propGraphicVisible().addUntypedPropertyListener(styleListener);
        model_widget.propScrollEnabled().addUntypedPropertyListener(styleListener);
        model_widget.propSpinnerShaped().addUntypedPropertyListener(styleListener);

        jfx_node.valueProperty().addListener(thumbwheelValueChangedListener);
        model_widget.runtimePropValue().addPropertyListener(valueChangedListener);

        enablementChanged(null, null, null);

    }

    @Override
    protected void unregisterListeners() {
        super.unregisterListeners();

        model_widget.propWidth().removePropertyListener(styleListener);
        model_widget.propHeight().removePropertyListener(styleListener);

        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propIncrementColor().removePropertyListener(styleListener);
        model_widget.propDecrementColor().removePropertyListener(styleListener);
        model_widget.propInvalidColor().removePropertyListener(styleListener);

        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propDecimalDigits().removePropertyListener(styleListener);
        model_widget.propIntegerDigits().removePropertyListener(styleListener);

        model_widget.propMinimum().removePropertyListener(limitsChangedListener);
        model_widget.propMaximum().removePropertyListener(limitsChangedListener);
        model_widget.propLimitsFromPV().removePropertyListener(limitsChangedListener);

        model_widget.propEnabled().removePropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enablementChangedListener);

        model_widget.propGraphicVisible().removePropertyListener(styleListener);
        model_widget.propScrollEnabled().removePropertyListener(styleListener);
        model_widget.propSpinnerShaped().removePropertyListener(styleListener);

        jfx_node.valueProperty().removeListener(thumbwheelValueChangedListener);
        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);

    }

    @Override
    public void updateChanges() {
        super.updateChanges();
        if (dirty_enablement.checkAndClear()) {
            jfx_node.setDisable(!enabled);
        }
        if (dirty_style.checkAndClear()) {

            jfx_node.setBackgroundColor(JFXUtil.convert(model_widget.propBackgroundColor().getValue()));
            jfx_node.setForegroundColor(JFXUtil.convert(model_widget.propForegroundColor().getValue()));
            jfx_node.setPrefWidth(model_widget.propWidth().getValue());
            jfx_node.setPrefHeight(model_widget.propHeight().getValue());

            jfx_node.setIncrementButtonsColor(JFXUtil.convert(model_widget.propIncrementColor().getValue()));
            jfx_node.setDecrementButtonsColor(JFXUtil.convert(model_widget.propDecrementColor().getValue()));
            jfx_node.setInvalidColor(JFXUtil.convert(model_widget.propInvalidColor().getValue()));

            jfx_node.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            jfx_node.setDecimalDigits(model_widget.propDecimalDigits().getValue());
            jfx_node.setIntegerDigits(model_widget.propIntegerDigits().getValue());

//            jfx_node.setEditable(!toolkit.isEditMode() && enabled);
//            jfx_node.getEditor().setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);

            jfx_node.setGraphicVisible(model_widget.propGraphicVisible().getValue());
            jfx_node.setScrollEnabled(model_widget.propScrollEnabled().getValue());
            jfx_node.setSpinnerShaped(model_widget.propSpinnerShaped().getValue());


        }
        if(dirty_behavior.checkAndClear()) {
            jfx_node.setMinValue(min);
            jfx_node.setMaxValue(max);
        }
        // If the value has changed,
        // Then get the runtime value from the PV
        if (dirty_value.checkAndClear()) {
            jfx_node.setValue(value);
        }
        jfx_node.layout();
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    // Thumbwheel value changes when the user interacts with it directly, such as
    // incrementing or decrementing the values via buttons
    private void thumbwheelValueChanged(final ObservableValue<? extends Number> property, final Number old_value, final Number new_value)
    {
        toolkit.fireWrite(model_widget, new_value);
    }

    // Value change is triggered when the PV value changes
    private void valueChanged(final WidgetProperty<? extends VType> property, final VType old_value, final VType new_value)
    {
        // If the widget is getting limits from the PV, then they may have changed
        if (model_widget.propLimitsFromPV().getValue()) {
            limitsChanged(null, null, null);
        }

        final VType vtype = model_widget.runtimePropValue().getValue();
        double newval = VTypeUtil.getValueNumber(vtype).doubleValue();

        // If no value available, then set to zero
        // Otherwise use the PV value
        if (Double.isNaN(newval))
        {
            logger.log(Level.FINE, model_widget + " PV has invalid value " + vtype);
            value = 0;
        }
        else {
            value = newval;
        }
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    // Enablement changes are triggered by changes to the enabled flag on the widget,
    // and on the pv writable flag
    private void enablementChanged(final WidgetProperty<Boolean> property, final Boolean old_value, final Boolean new_value)
    {
        enabled =   model_widget.propEnabled().getValue() &&
                    model_widget.runtimePropPVWritable().getValue();
        dirty_enablement.mark();
        toolkit.scheduleUpdate(this);
    }

    // Determine the new limits, and mark the behavior as changed
    private void limitsChanged(WidgetProperty<?> widgetProperty, Object old_value, Object new_value) {

        // Initialize to default values
        double new_min = ThumbwheelWidget.DEFAULT_MIN;
        double new_max = ThumbwheelWidget.DEFAULT_MAX;

        // If the widget is using PV limits, attempt to use those values
        if (model_widget.propLimitsFromPV().getValue()) {

            // Try to get display range from PV
            final Display display_info = Display.displayOf(model_widget.runtimePropValue().getValue());
            if (display_info != null) {

                // Should use the 'control' range but fall back to 'display' range
                if (display_info.getControlRange().isFinite()) {
                    new_min = display_info.getControlRange().getMinimum();
                    new_max = display_info.getControlRange().getMaximum();
                }
                else {
                    new_min = display_info.getDisplayRange().getMinimum();
                    new_max = display_info.getDisplayRange().getMaximum();
                }
            }
            // Else do nothing; use the defaults above.

        }
        // Else, use the limits defined on the widget
        else {
            new_min = model_widget.propMinimum().getValue();
            new_max = model_widget.propMaximum().getValue();
        }

        // Finally, set the min and max if they've changed,
        // and mark as having changed
        if (Double.compare(min, new_min) != 0) {
            min = new_min;
            dirty_behavior.mark();
        }
        if (Double.compare(max, new_max) != 0) {
            max = new_max;
            dirty_behavior.mark();
        }

    }


}
