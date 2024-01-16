package org.csstudio.display.extra.widgets.linearmeter;

import java.util.ArrayDeque;
import java.util.Deque;

import javafx.util.Pair;

import org.csstudio.display.builder.model.BaseWidgetPropertyListener;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.properties.PropertyChangeHandler;
import org.csstudio.display.builder.model.properties.WidgetColor;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.display.builder.representation.javafx.widgets.RegionBaseRepresentation;
import org.epics.util.stats.Range;

import javafx.scene.layout.Pane;
import org.epics.vtype.Display;
import org.epics.vtype.VDouble;

@SuppressWarnings("nls")
public class LinearMeterRepresentation extends RegionBaseRepresentation<Pane, LinearMeterWidget>
{
    private DirtyFlag dirty_look = new DirtyFlag();

    private UntypedWidgetPropertyListener layoutChangedListener = this::layoutChanged;
    private WidgetPropertyListener<Boolean> orientationChangedListener = this::orientationChanged;
    private UntypedWidgetPropertyListener valueListener = this::valueChanged;
    private WidgetPropertyListener<Integer> widthChangedListener = this::widthChanged;
    private WidgetPropertyListener<Integer> heightChangedListener = this::heightChanged;
    private RTLinearMeter meter;

    private java.awt.Color widgetColorToAWTColor(WidgetColor widgetColor) {
        return new java.awt.Color(widgetColor.getRed(), widgetColor.getGreen(), widgetColor.getBlue(), widgetColor.getAlpha());
    }

    @Override
    public Pane createJFXNode()
    {
        double initialValue = toolkit.isEditMode() ? (model_widget.propMinimum().getValue() + model_widget.propMaximum().getValue()) / 2.0
                                                   : Double.NaN;

        double minimum, maximum;
        double loLo, low, high, hiHi;
        if (model_widget.propLimitsFromPV().getValue() && !toolkit.isEditMode()) {
            minimum = Double.NaN;
            maximum = Double.NaN;
            loLo = Double.NaN;
            low = Double.NaN;
            high = Double.NaN;
            hiHi = Double.NaN;
        }
        else {
            minimum = model_widget.propMinimum().getValue();
            maximum = model_widget.propMaximum().getValue();
            loLo = model_widget.propLevelLoLo().getValue();
            low = model_widget.propLevelLow().getValue();
            high = model_widget.propLevelHigh().getValue();
            hiHi = model_widget.propLevelHiHi().getValue();
        }

        meter = new RTLinearMeter(initialValue,
                                  model_widget.propWidth().getValue(),
                                  model_widget.propHeight().getValue(),
                                  minimum,
                                  maximum,
                                  loLo,
                                  low,
                                  high,
                                  hiHi,
                                  model_widget.propShowUnits().getValue(),
                                  model_widget.propShowLimits().getValue(),
                                  model_widget.propDisplayHorizontal().getValue(),
                                  model_widget.propIsGradientEnabled().getValue(),
                                  model_widget.propIsHighlightActiveRegionEnabled().getValue(),
                                  widgetColorToAWTColor(model_widget.propNormalStatusColor().getValue()),
                                  widgetColorToAWTColor(model_widget.propMinorWarningColor().getValue()),
                                  widgetColorToAWTColor(model_widget.propMajorWarningColor().getValue()),
                                  model_widget.propNeedleWidth().getValue(),
                                  widgetColorToAWTColor(model_widget.propNeedleColor().getValue()),
                                  model_widget.propKnobSize().getValue(),
                                  widgetColorToAWTColor(model_widget.propKnobColor().getValue()));
        meter.setSize(model_widget.propWidth().getValue(),model_widget.propHeight().getValue());
        meter.setHorizontal(model_widget.propDisplayHorizontal().getValue());
        meter.setManaged(false);
        return new Pane(meter);
    }

    Deque<Pair<PropertyChangeHandler, BaseWidgetPropertyListener>> widgetPropertiesWithWidgetPropertyListeners = new ArrayDeque<>();
    private <T> void addWidgetPropertyListener(WidgetProperty<T> widgetProperty, WidgetPropertyListener<T> widgetPropertyListener) {
        widgetProperty.addPropertyListener(widgetPropertyListener);
        widgetPropertiesWithWidgetPropertyListeners.push(new Pair(widgetProperty, widgetPropertyListener));
    }
    private <T> void addUntypedWidgetPropertyListener(WidgetProperty<T> widgetProperty, UntypedWidgetPropertyListener widgetPropertyListener) {
        widgetProperty.addUntypedPropertyListener(widgetPropertyListener);
        widgetPropertiesWithWidgetPropertyListeners.push(new Pair(widgetProperty, widgetPropertyListener));
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        addWidgetPropertyListener(model_widget.propWidth(), widthChangedListener);
        addWidgetPropertyListener(model_widget.propHeight(), heightChangedListener);
        addUntypedWidgetPropertyListener(model_widget.propForeground(), layoutChangedListener);
        addUntypedWidgetPropertyListener(model_widget.propBackground(), layoutChangedListener);
        addUntypedWidgetPropertyListener(model_widget.propScaleVisible(), layoutChangedListener);
        addUntypedWidgetPropertyListener(model_widget.propFont(), layoutChangedListener);
        addUntypedWidgetPropertyListener(model_widget.propNeedleColor(), layoutChangedListener);

        addWidgetPropertyListener(model_widget.propShowUnits(), (property, old_value, new_value) -> {
            meter.setShowUnits(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propShowLimits(), (property, old_value, new_value) -> {
            meter.setShowLimits(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propIsGradientEnabled(), (property, old_value, new_value) -> {
            meter.setIsGradientEnabled(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propIsHighlightActiveRegionEnabled(), (property, old_value, new_value) -> {
            meter.setIsHighlightActiveRegionEnabled(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propNormalStatusColor(), (property, old_value, new_value) -> {
            meter.setNormalStatusColor(widgetColorToAWTColor(new_value));
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMinorWarningColor(), (property, old_value, new_value) -> {
            meter.setMinorAlarmColor(widgetColorToAWTColor(new_value));
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMajorWarningColor(), (property, old_value, new_value) -> {
            meter.setMajorAlarmColor(widgetColorToAWTColor(new_value));
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propNeedleWidth(), (property, old_value, new_value) -> {
            meter.setNeedleWidth(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propNeedleColor(), (property, old_value, new_value) -> {
            meter.setNeedleColor(widgetColorToAWTColor(new_value));
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMinimum(), (property, old_value, new_value) -> {

            synchronized (meter) {
                boolean validRange = Double.isFinite(new_value) && Double.isFinite(model_widget.propMaximum().getValue());
                meter.setRange(new_value, model_widget.propMaximum().getValue(), validRange);
                if (toolkit.isEditMode() && validRange) {
                    meter.setCurrentValue((new_value + model_widget.propMaximum().getValue()) / 2.0);
                }
            }

            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMaximum(), (property, old_value, new_value) -> {

            synchronized (meter) {
                boolean validRange = Double.isFinite(new_value) && Double.isFinite(model_widget.propMinimum().getValue());
                meter.setRange(model_widget.propMinimum().getValue(), new_value, validRange);
                if (toolkit.isEditMode() && validRange) {
                    meter.setCurrentValue((new_value + model_widget.propMinimum().getValue()) / 2.0);
                }
            }

            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propLevelLoLo(), (property, old_value, new_value) -> {
            meter.setLoLo(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propLevelLow(), (property, old_value, new_value) -> {
            meter.setLow(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propLevelHigh(), (property, old_value, new_value) -> {
            meter.setHigh(new_value);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propLevelHiHi(), (property, old_value, new_value) -> {
            meter.setHiHi(new_value);
            layoutChanged(null, null, null);
        });

        addUntypedWidgetPropertyListener(model_widget.runtimePropValue(), valueListener);

        addWidgetPropertyListener(model_widget.propDisplayHorizontal(), orientationChangedListener);

        addWidgetPropertyListener(model_widget.propKnobColor(), (property, old_value, new_value) -> {
            meter.setKnobColor(widgetColorToAWTColor(new_value));
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propKnobSize(), (property, old_value, new_value) -> {
            meter.setKnobSize(new_value);
            layoutChanged(null, null, null);
        });
    }

    @Override
    protected void unregisterListeners()
    {
        for (var widgetPropertyWithWidgetPropertyListener : widgetPropertiesWithWidgetPropertyListeners) {
            var widgetProperty = widgetPropertyWithWidgetPropertyListener.getKey();
            var widgetPropertyListener = widgetPropertyWithWidgetPropertyListener.getValue();
            widgetProperty.removePropertyListener(widgetPropertyListener);
        }
        widgetPropertiesWithWidgetPropertyListeners.clear();

        super.unregisterListeners();
    }

    int minimumSize = 25;
    private void widthChanged(WidgetProperty<Integer> prop, Integer old_value, Integer new_value)
    {
        if (new_value < minimumSize) {
            prop.setValue(minimumSize);
            return;
        }
        layoutChanged(prop, old_value, new_value);
    }

    private void heightChanged(WidgetProperty<Integer> prop, Integer old_value, Integer new_value)
    {
        if (new_value < minimumSize) {
            prop.setValue(minimumSize);
            return;
        }
        layoutChanged(prop, old_value, new_value);
    }

    private void orientationChanged(WidgetProperty<Boolean> prop, Boolean old, Boolean horizontal)
    {
        if (toolkit.isEditMode())
        {
            synchronized(meter) {
                int w = model_widget.propWidth().getValue();
                int h = model_widget.propHeight().getValue();
                model_widget.propWidth().setValue(h);
                model_widget.propHeight().setValue(w);
                meter.setHorizontal(horizontal);
            }
            layoutChanged(null, null, null);
        }
    }

    private void layoutChanged(WidgetProperty<?> property, Object old_value, Object new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private double observedMin = Double.NaN;
    private double observedMax = Double.NaN;
    private void valueChanged(WidgetProperty<?> property, Object old_value, Object new_value)
    {
        synchronized (meter) {    // "synchronized (meter) { ... }" is due to the reading of values from "meter.linearMeter",
                                  // which may otherwise be interleaved with writes to these values.
            if (new_value instanceof VDouble ) {
                VDouble vDouble = ((VDouble) new_value);
                double newValue = vDouble.getValue();
                meter.setCurrentValue(newValue);

                if (!Double.isNaN(newValue))
                    if (Double.isNaN(observedMin) || newValue < observedMin) {
                        observedMin = newValue;
                    }

                if (Double.isNaN(observedMax) || newValue > observedMax) {
                    observedMax = newValue;
                }

                Display display = vDouble.getDisplay();

                // Set the units:
                if (model_widget.propShowUnits().getValue() ) {
                    meter.setUnits(display.getUnit());
                }

                if (model_widget.propLimitsFromPV().getValue()) {
                    Range displayRange = display.getDisplayRange();
                    if (   displayRange != null
                            && Double.isFinite(displayRange.getMinimum())
                            && Double.isFinite(displayRange.getMaximum())
                            && displayRange.getMaximum() - displayRange.getMinimum() > 0.0) {
                        if (meter.linearMeterScale.getValueRange().getLow() != displayRange.getMinimum() || meter.linearMeterScale.getValueRange().getHigh() != displayRange.getMaximum() || !meter.getValidRange()) {
                            meter.setRange(displayRange.getMinimum(), displayRange.getMaximum(), true);
                        }
                    }
                    else {
                        Range controlRange = display.getControlRange();
                        if (   controlRange != null
                                && Double.isFinite(controlRange.getMinimum())
                                && Double.isFinite(controlRange.getMaximum())
                                && controlRange.getMaximum() - controlRange.getMinimum() > 0.0) {
                            if (meter.linearMeterScale.getValueRange().getLow() != controlRange.getMinimum() || meter.linearMeterScale.getValueRange().getHigh() != controlRange.getMaximum() || !meter.getValidRange()) {
                                meter.setRange(controlRange.getMinimum(), controlRange.getMaximum(), true);
                            }
                        }
                        else if (!Double.isNaN(observedMin) && !Double.isNaN(observedMax)) {
                            meter.setRange(observedMin - 1, observedMax + 1, false);
                        }
                        else {
                            meter.setRange(0.0, 100.0, false);
                        }
                    }

                    {
                        Range warningRange = display.getWarningRange();
                        if (warningRange != null) {
                            if (!Double.isNaN(warningRange.getMinimum()) && meter.getLow() != warningRange.getMinimum()) {
                                meter.setLow(warningRange.getMinimum());
                            }

                            if (!Double.isNaN(warningRange.getMaximum()) && meter.getHigh() != warningRange.getMaximum()) {
                                meter.setHigh(warningRange.getMaximum());
                            }
                        }
                    }

                    {
                        Range alarmRange = display.getAlarmRange();
                        if (alarmRange != null) {
                            if (!Double.isNaN(alarmRange.getMinimum()) && meter.getLoLo() != alarmRange.getMinimum()) {
                                meter.setLoLo(alarmRange.getMinimum());
                            }

                            if (!Double.isNaN(alarmRange.getMaximum()) && meter.getHiHi() != alarmRange.getMaximum()) {
                                meter.setHiHi(alarmRange.getMaximum());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_look.checkAndClear())
        {
            synchronized (meter) {
                boolean horizontal = model_widget.propDisplayHorizontal().getValue();
                int width = model_widget.propWidth().getValue();
                int height = model_widget.propHeight().getValue();
                meter.linearMeterScale.setHorizontal(horizontal);

                meter.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
                meter.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));

                meter.setFont(JFXUtil.convert(model_widget.propFont().getValue()));

                meter.setScaleVisible(model_widget.propScaleVisible().getValue());

                jfx_node.setPrefSize(width, height);

                meter.setSize(width, height);
            }
        }
    }

    @Override
    public void dispose()
    {
        meter.dispose();
        super.dispose();
    }
}
