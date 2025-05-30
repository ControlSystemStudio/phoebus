package org.csstudio.display.widget;

import javafx.scene.layout.Region;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.epics.vtype.VType;

import java.util.logging.Level;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

public class ThumbwheelWidgetRepresentation extends RegionBaseRepresentation<ThumbWheel, ThumbwheelWidget> {

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();

    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final WidgetPropertyListener<Boolean> negativeNumbersChangedListener = this::negativeNumbersChanged;
    private final WidgetPropertyListener<Integer> widgetWidthChangedListener = this::widgetWidthChanged;
    private final WidgetPropertyListener<Integer> widgetHeightChangedListener = this::widgetHeightChanged;

    private final WidgetPropertyListener<Boolean> enablementChangedListener = this::enablementChanged;
    private final WidgetPropertyListener<VType> valueChangedListener = this::valueChanged;

    private volatile boolean enabled = false;
    private volatile double value = 0.0;

    @Override
    protected ThumbWheel createJFXNode() throws Exception {
        final ThumbWheel thumbWheel = new ThumbWheel(model_widget.propWidth().getValue(),
                                                     model_widget.propHeight().getValue(),
                                                     model_widget.propNegativeNumbers().getValue(),
                                                     this::writeValueToPV);
        if (toolkit.isEditMode()) {
            // A transparent "Region" covering the widget in edit mode prevents the buttons from being clickable in edit mode:
            thumbWheel.add(new Region(),0, 0, thumbWheel.getColumnCount(), thumbWheel.getRowCount());
        }
        return thumbWheel;
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleListener);
        model_widget.propWidth().addPropertyListener(widgetWidthChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(styleListener);
        model_widget.propHeight().addPropertyListener(widgetHeightChangedListener);

        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propIncrementColor().addUntypedPropertyListener(styleListener);
        model_widget.propDecrementColor().addUntypedPropertyListener(styleListener);
        model_widget.propInvalidColor().addUntypedPropertyListener(styleListener);

        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propDecimalDigits().addUntypedPropertyListener(styleListener);
        model_widget.propIntegerDigits().addUntypedPropertyListener(styleListener);
        model_widget.propNegativeNumbers().addPropertyListener(negativeNumbersChangedListener);

        model_widget.propEnabled().addPropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().addPropertyListener(enablementChangedListener);

        model_widget.propGraphicVisible().addUntypedPropertyListener(styleListener);
        model_widget.propSpinnerShaped().addUntypedPropertyListener(styleListener);

        model_widget.runtimePropValue().addPropertyListener(valueChangedListener);

        enablementChanged(null, null, null);

    }

    @Override
    protected void unregisterListeners() {
        super.unregisterListeners();

        model_widget.propWidth().removePropertyListener(styleListener);
        model_widget.propWidth().removePropertyListener(widgetWidthChangedListener);
        model_widget.propHeight().removePropertyListener(styleListener);
        model_widget.propHeight().removePropertyListener(widgetHeightChangedListener);

        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propIncrementColor().removePropertyListener(styleListener);
        model_widget.propDecrementColor().removePropertyListener(styleListener);
        model_widget.propInvalidColor().removePropertyListener(styleListener);

        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propDecimalDigits().removePropertyListener(styleListener);
        model_widget.propIntegerDigits().removePropertyListener(styleListener);
        model_widget.propNegativeNumbers().removePropertyListener(negativeNumbersChangedListener);

        model_widget.propEnabled().removePropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enablementChangedListener);

        model_widget.propGraphicVisible().removePropertyListener(styleListener);
        model_widget.propSpinnerShaped().removePropertyListener(styleListener);

        model_widget.runtimePropValue().removePropertyListener(valueChangedListener);

    }

    @Override
    public void updateChanges() {
        super.updateChanges();
        if (dirty_enablement.checkAndClear()) {
            setDisabledLook(enabled, jfx_node.getChildren());
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

            jfx_node.setGraphicVisible(model_widget.propGraphicVisible().getValue());
            jfx_node.setSpinnerShaped(model_widget.propSpinnerShaped().getValue());


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

    // Thumbwheel value is written to the PV when the user
    // interacts with it directly, by incrementing or
    // decrementing the values via buttons
    private void writeValueToPV(final Number new_value)
    {
        if (enabled)
            toolkit.fireWrite(model_widget, new_value);
    }

    // Value change is triggered when the PV value changes
    private void valueChanged(final WidgetProperty<? extends VType> property, final VType old_value, final VType new_value)
    {
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

    private void negativeNumbersChanged(WidgetProperty<Boolean> widgetProperty, Boolean old_value, Boolean new_value) {
        jfx_node.setHasNegativeSign(new_value);
        jfx_node.update(true);
    }

    private void widgetWidthChanged(WidgetProperty<Integer> widgetProperty, Integer old_value, Integer new_value) {
        jfx_node.setWidgetWidth(new_value);
    }

    private void widgetHeightChanged(WidgetProperty<Integer> widgetProperty, Integer old_value, Integer new_value) {
        jfx_node.setWidgetHeight(new_value);
    }
}
