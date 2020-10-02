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

import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.input.ContextMenuEvent;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.SpinnerWidget;
import org.csstudio.display.builder.representation.javafx.Cursors;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.Display;
import org.epics.vtype.VNumber;
import org.epics.vtype.VType;
import org.phoebus.ui.javafx.Styles;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 */
@SuppressWarnings("nls")
public class SpinnerRepresentation extends RegionBaseRepresentation<Spinner<String>, SpinnerWidget>
{
    /** Is user actively editing the content, so updates should be suppressed? */
    private volatile boolean active = false;

    private final DirtyFlag dirty_style = new DirtyFlag();
    private final DirtyFlag dirty_content = new DirtyFlag();
    private final UntypedWidgetPropertyListener styleListener = this::styleChanged;
    private final UntypedWidgetPropertyListener behaviourListener = this::behaviorChanged;
    private final UntypedWidgetPropertyListener contentListener = this::contentChanged;
    private final WidgetPropertyListener<String> pvNameListener = this::pvnameChanged;

    protected volatile String value_text = "<?>";
    protected volatile VType value = null;
    private volatile double value_max  = 100.0;
    private volatile double value_min  = 0.0;

    @Override
    protected final Spinner<String> createJFXNode() throws Exception
    {
        final Spinner<String> spinner = new Spinner<>();
        spinner.setValueFactory(createSVF());
        spinner.focusedProperty().addListener((property, oldval, newval)->
        {
            if (!spinner.isFocused())
                restore();
            active = false;
        });
        spinner.getEditor().setOnKeyPressed((final KeyEvent event) ->
        {
            switch (event.getCode())
            {
            case ESCAPE: //TODO: fix: escape key event not sensed
                // Revert original value, leave active state
                restore();
                active = false;
                break;
            case ENTER:
                // Submit value, leave active state
                submit();
                active = false;
                break;
            //incrementing by keyboard
            case UP:
            case PAGE_UP:
                if (!active)
                    spinner.getValueFactory().increment(1);
                break;
            case DOWN:
            case PAGE_DOWN:
                if (!active)
                    spinner.getValueFactory().decrement(1);
                break;
            default:
                // Any other key results in active state
                active = true;
            }
        });

        // Disable the contemporary triggering of a value change and of the
        // opening of contextual menu when right-clicking on the spinner's
        // buttons.
        spinner.addEventFilter(MouseEvent.ANY, e ->
        {
            if (e.getButton() == MouseButton.SECONDARY)
                e.consume();
        });

        // This code manages layout,
        // because otherwise for example border changes would trigger
        // expensive Node.notifyParentOfBoundsChange() recursing up the scene graph
        spinner.setManaged(false);

        spinner.getEditor().setOnContextMenuRequested((event) ->
        {
            event.consume();
            toolkit.fireContextMenu(model_widget, (int)event.getScreenX(), (int)event.getScreenY());
        });

        spinner.getEditor().setPadding(new Insets(0, 0, 0, 0));

        return spinner;
    }

    /** Restore representation to last known value,
     *  replacing what user might have entered
     */
    private void restore()
    {
        //The old value is restored.
        jfx_node.getEditor().setText(jfx_node.getValueFactory().getValue());
    }

    /** Submit value entered by user */
    private void submit()
    {
        //The value factory retains the old values, and will be updated as scheduled below.
        final String text = jfx_node.getEditor().getText();
        Object value =
                FormatOptionHandler.parse(model_widget.runtimePropValue().getValue(), text, model_widget.propFormat().getValue());
        if (value instanceof Number)
        {
            if (((Number)value).doubleValue() < value_min)
                value = value_min;
            else if (((Number)value).doubleValue() > value_max)
                value = value_max;
        }
        logger.log(Level.FINE, "Writing '" + text + "' as " + value + " (" + value.getClass().getName() + ")");
        toolkit.fireWrite(model_widget, value);

        // Wrote value. Expected is either
        // a) PV receives that value, PV updates to
        //    submitted value or maybe a 'clamped' value
        // --> We'll receive contentChanged() and update the value factory.
        // b) PV doesn't receive the value and never sends
        //    an update. The value factory retains the old value,
        // --> Schedule an update to the new value.
        //
        // This could result in a little flicker:
        // User enters "new_value".
        // We send that, but retain "old_value" to handle case b)
        // PV finally sends "new_value", and we show that.
        //
        // In practice, this rarely happens because we only schedule an update.
        // By the time it executes, we already have case a.
        // If it does turn into a problem, could introduce toolkit.scheduleDelayedUpdate()
        // so that case b) only restores the old 'value_text' after some delay,
        // increasing the chance of a) to happen.
        dirty_content.mark();
        toolkit.scheduleUpdate(this);
    }

    private SpinnerValueFactory<String> createSVF()
    {
        SpinnerValueFactory<String> svf = new TextSpinnerValueFactory();
        svf.setValue(value_text);
        return svf;
    }

    private class TextSpinnerValueFactory extends SpinnerValueFactory<String>
    {
        // Constructors
        TextSpinnerValueFactory()
        {
            this(model_widget.propMinimum().getDefaultValue(),
                 model_widget.propMaximum().getDefaultValue(),
                 model_widget.propIncrement().getDefaultValue());
        }

        TextSpinnerValueFactory(double min, double max, double stepIncrement)
        {
            setStepIncrement(stepIncrement);
            setMin(min);
            setMax(max);
            setConverter(new StringConverter<String>()
            {
                @Override
                public String toString(String object)
                {
                    return object;
                }

                @Override
                public String fromString(String text)
                {
                    return text;
                }
            });
        }

        // Properties
        private DoubleProperty stepIncrement = new SimpleDoubleProperty(this, "stepIncrement");
        public final void setStepIncrement(double value)
        {
            stepIncrement.set(value);
        }
        public final double getStepIncrement()
        {
            return stepIncrement.get();
        }
        /**
         * Sets the amount to increment or decrement by, per step.
         */
        public final DoubleProperty stepIncrementProperty()
        {
            return stepIncrement;
        }

        private DoubleProperty min = new SimpleDoubleProperty(this, "min");
        public final void setMin(double value)
        {
            min.set(value);
        }
        public final double getMin()
        {
            return min.get();
        }
        /**
         * Sets the minimum possible value.
         */
        public final DoubleProperty minProperty()
        {
            return min;
        }

        private DoubleProperty max = new SimpleDoubleProperty(this, "max");
        public final void setMax(double value)
        {
            max.set(value);
        }
        public final double getMax()
        {
            return max.get();
        }
        /**
         * Sets the maximum possible value.
         */
        public final DoubleProperty maxProperty()
        {
            return max;
        }

        //TODO: Is really better to have this separate?
        private ObjectPropertyBase<VType> vtypeValue = new ObjectPropertyBase<>()
        {
            @Override
            public Object getBean()
            {
                return this; //return the TextSpinnerValueFactory
            }
            @Override
            public String getName()
            {
                return "vtypeValue";
            }

        };
        public final void setVTypeValue(VType value)
        {
            vtypeValue.set(value);
        }
        public final VType getVTypeValue()
        {
            return vtypeValue.get();
        }
        /**
         * Sets the associated VType value.
         */
        //implement if needed: public final ObjectPropertyBase<VType> vtypeValueProperty()

        // Increment and decrement
        @Override
        public void decrement(int steps)
        {
            if (!toolkit.isEditMode() && model_widget.propEnabled().getValue())
                writeResultingValue(-steps*getStepIncrement());
        }

        @Override
        public void increment(int steps)
        {
            if (!toolkit.isEditMode() && model_widget.propEnabled().getValue())
                writeResultingValue(steps*getStepIncrement());
        }

        private void writeResultingValue(double change)
        {
            double value;
            if (!(getVTypeValue() instanceof VNumber))
            {
                scheduleContentUpdate();
                return;
            }
            value = ((VNumber)getVTypeValue()).getValue().doubleValue();
            if (Double.isNaN(value) || Double.isInfinite(value)) return;
            value += change;
            if (value < getMin()) value = getMin();
            else if (value > getMax()) value = getMax();
            toolkit.fireWrite(model_widget, value);
        }
    };

    /** @param value Current value of PV
     *  @return Text to show, "<pv name>" if disconnected (no value)
     */
    private String computeText(final VType value)
    {
        if (value == null)
            return "<" + model_widget.propPVName().getValue() + ">";
        return FormatOptionHandler.format(value,
                                          model_widget.propFormat().getValue(),
                                          model_widget.propPrecision().getValue(),
                                          model_widget.propShowUnits().getValue());
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(styleListener);
        model_widget.propHeight().addUntypedPropertyListener(styleListener);
        model_widget.propButtonsOnLeft().addUntypedPropertyListener(styleListener);

        model_widget.propForegroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(styleListener);
        model_widget.propFont().addUntypedPropertyListener(styleListener);
        model_widget.propEnabled().addUntypedPropertyListener(styleListener);
        model_widget.runtimePropPVWritable().addUntypedPropertyListener(styleListener);

        model_widget.propIncrement().addUntypedPropertyListener(behaviourListener);
        model_widget.propMinimum().addUntypedPropertyListener(behaviourListener);
        model_widget.propMaximum().addUntypedPropertyListener(behaviourListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(behaviourListener);

        model_widget.propFormat().addUntypedPropertyListener(contentListener);
        model_widget.propPrecision().addUntypedPropertyListener(contentListener);
        model_widget.propShowUnits().addUntypedPropertyListener(contentListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(contentListener);

        model_widget.propPVName().addPropertyListener(pvNameListener);

        behaviorChanged(null, null, null);
        contentChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(styleListener);
        model_widget.propHeight().removePropertyListener(styleListener);
        model_widget.propButtonsOnLeft().removePropertyListener(styleListener);
        model_widget.propForegroundColor().removePropertyListener(styleListener);
        model_widget.propBackgroundColor().removePropertyListener(styleListener);
        model_widget.propFont().removePropertyListener(styleListener);
        model_widget.propEnabled().removePropertyListener(styleListener);
        model_widget.runtimePropPVWritable().removePropertyListener(styleListener);
        model_widget.propIncrement().removePropertyListener(behaviourListener);
        model_widget.propMinimum().removePropertyListener(behaviourListener);
        model_widget.propMaximum().removePropertyListener(behaviourListener);
        model_widget.propLimitsFromPV().removePropertyListener(behaviourListener);
        model_widget.propFormat().removePropertyListener(contentListener);
        model_widget.propPrecision().removePropertyListener(contentListener);
        model_widget.propShowUnits().removePropertyListener(contentListener);
        model_widget.runtimePropValue().removePropertyListener(contentListener);
        model_widget.propPVName().removePropertyListener(pvNameListener);
        super.unregisterListeners();
    }

    @Override
    protected boolean isFilteringEditModeClicks()
    {
        return true;
    }

    private void styleChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_style.mark();
        toolkit.scheduleUpdate(this);
    }

    private void behaviorChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        updateLimits();
        final TextSpinnerValueFactory factory = (TextSpinnerValueFactory)jfx_node.getValueFactory();
        factory.setStepIncrement(model_widget.propIncrement().getValue());
        factory.setMin(value_min);
        factory.setMax(value_max);
    }


    private void contentChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        if (model_widget.propLimitsFromPV().getValue())
            behaviorChanged(null, null, null);
        value = model_widget.runtimePropValue().getValue();
        value_text = computeText(value);
        scheduleContentUpdate();
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

    private void scheduleContentUpdate()
    {
        dirty_content.mark();
        if (!active)
            toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_style.checkAndClear())
        {
            final String color = JFXUtil.webRGB(model_widget.propForegroundColor().getValue());
            jfx_node.editorProperty().getValue().setStyle("-fx-text-fill:" + color);
            final Color background = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
            jfx_node.editorProperty().getValue().setBackground(new Background(new BackgroundFill(background, CornerRadii.EMPTY, Insets.EMPTY)));
            jfx_node.resize(model_widget.propWidth().getValue(), model_widget.propHeight().getValue());

            // Enable if enabled by user and there's write access
            final boolean enabled = model_widget.propEnabled().getValue()  &&
                                    model_widget.runtimePropPVWritable().getValue();
            Styles.update(jfx_node, Styles.NOT_ENABLED, !enabled);
            jfx_node.setEditable(!toolkit.isEditMode() && enabled);
            jfx_node.getEditor().setCursor(enabled ? Cursor.DEFAULT : Cursors.NO_WRITE);

            jfx_node.getEditor().setFont(JFXUtil.convert(model_widget.propFont().getValue()));

            int x = jfx_node.getStyleClass().indexOf(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL);
            if (model_widget.propButtonsOnLeft().getValue())
            {
                if (x < 0)
                    jfx_node.getStyleClass().add(Spinner.STYLE_CLASS_ARROWS_ON_LEFT_VERTICAL);
            }
            else if (x > 0)
                jfx_node.getStyleClass().remove(x);
        }
        if (dirty_content.checkAndClear())
        {
            ( (TextSpinnerValueFactory)jfx_node.getValueFactory() ).setVTypeValue(value);
            jfx_node.getValueFactory().setValue(value_text);
        }

        jfx_node.layout();
    }

    /** Updates, if required, the limits */
    private void updateLimits()
    {
        //  Model's values.
        double newMin = model_widget.propMinimum().getValue();
        double newMax = model_widget.propMaximum().getValue();

        //  If invalid limits, fall back to 0..100 range.
        if (Double.isNaN(newMin) || Double.isNaN(newMax) || newMin > newMax)
        {
            newMin = 0.0;
            newMax = 100.0;
        }

        if (model_widget.propLimitsFromPV().getValue())
        {
            //  Try to get display range from PV.
            final Display display_info = Display.displayOf(model_widget.runtimePropValue().getValue());

            if (display_info != null)
            {
                // Use control range, falling back to display
                if (display_info.getControlRange().isFinite())
                {
                    newMin = display_info.getControlRange().getMinimum();
                    newMax = display_info.getControlRange().getMaximum();
                }
                else if (display_info.getDisplayRange().isFinite())
                {
                    newMin = display_info.getDisplayRange().getMinimum();
                    newMax = display_info.getDisplayRange().getMaximum();
                }
                // else: Leave at 0..100
            }
        }

        if (Double.compare(value_min, newMin) != 0)
            value_min = newMin;
        if (Double.compare(value_max, newMax) != 0)
            value_max = newMax;
    }

    @Override
    protected void attachTooltip()
    {
        // Use the formatted text for "$(pv_value)"
        TooltipSupport.attach(jfx_node, model_widget.propTooltip(), () -> value_text);
        // Show the tooltip for the editor part too
        TooltipSupport.attach(jfx_node.getEditor(), model_widget.propTooltip(), () -> value_text);
    }
}
