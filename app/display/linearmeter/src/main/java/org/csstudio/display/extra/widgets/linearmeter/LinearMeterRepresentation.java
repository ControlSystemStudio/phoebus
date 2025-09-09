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

        // Set min, max, and alarm limits according to settings.
        // Some or all of these may be overridden by the PV when
        // values are received, if the option "Limits from PV"
        // is not set to "No limits from PV".
        double minimum = model_widget.propMinimum().getValue();
        double maximum = model_widget.propMaximum().getValue();
        double loLo = model_widget.propLevelLoLo().getValue();
        double low = model_widget.propLevelLow().getValue();
        double high = model_widget.propLevelHigh().getValue();
        double hiHi = model_widget.propLevelHiHi().getValue();

        double minMaxTolerance = model_widget.propMinMaxTolerance().getValue();

        meter = new RTLinearMeter(initialValue,
                                  model_widget.propWidth().getValue(),
                                  model_widget.propHeight().getValue(),
                                  minimum,
                                  maximum,
                                  minMaxTolerance,
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
                                  widgetColorToAWTColor(model_widget.propKnobColor().getValue()),
                                  model_widget.propShowWarnings().getValue(),
                                  model_widget.propLogScale().getValue());
        meter.setDisplayMode(model_widget.propDisplayMode().getValue());
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

        addWidgetPropertyListener(model_widget.propLogScale(), (property, oldValue, newValue) -> {
            meter.setLogScale(newValue);
            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propShowWarnings(), (property, oldValue, newValue) -> {
            meter.setShowWarnings(newValue);
            layoutChanged(null, null, null);
        });

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

            meter.withWriteLock(() -> {
                boolean validRange = Double.isFinite(new_value) && Double.isFinite(model_widget.propMaximum().getValue());
                meter.setRange(new_value, model_widget.propMaximum().getValue(), validRange);
                if (toolkit.isEditMode() && validRange) {
                    meter.setCurrentValue((new_value + model_widget.propMaximum().getValue()) / 2.0);
                }
            });

            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMaximum(), (property, old_value, new_value) -> {

            meter.withWriteLock(() -> {
                boolean validRange = Double.isFinite(new_value) && Double.isFinite(model_widget.propMinimum().getValue());
                meter.setRange(model_widget.propMinimum().getValue(), new_value, validRange);
                if (toolkit.isEditMode() && validRange) {
                    meter.setCurrentValue((new_value + model_widget.propMinimum().getValue()) / 2.0);
                }
            });

            layoutChanged(null, null, null);
        });

        addWidgetPropertyListener(model_widget.propMinMaxTolerance(), (property, old_value, new_value) -> {
            meter.setMinMaxTolerance(new_value);
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

        addWidgetPropertyListener(model_widget.propDisplayMode(), (property, old_value, new_value) -> {
            meter.setDisplayMode(new_value);
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

    int minimumSize = 2;
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
            meter.withWriteLock(() -> {
                int w = model_widget.propWidth().getValue();
                int h = model_widget.propHeight().getValue();
                model_widget.propWidth().setValue(h);
                model_widget.propHeight().setValue(w);
                meter.setHorizontal(horizontal);
            });
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
    boolean newObservedMinAndMaxValues = false;

    private void valueChanged(WidgetProperty<?> property, Object old_value, Object new_value) {
        if (new_value instanceof VDouble) {
            meter.withWriteLock(() -> {
                VDouble vDouble = ((VDouble) new_value);
                double newValue = vDouble.getValue();

                if (!Double.isNaN(newValue)) {
                    if (Double.isNaN(observedMin) || newValue < observedMin) {
                        observedMin = newValue;
                        newObservedMinAndMaxValues = true;
                    }

                    if (Double.isNaN(observedMax) || newValue > observedMax) {
                        observedMax = newValue;
                        newObservedMinAndMaxValues = true;
                    }
                }

                Display display = vDouble.getDisplay();

                // Set the units:
                if (model_widget != null && model_widget.propShowUnits().getValue()) {
                    meter.setUnits(display.getUnit());
                }
                if (model_widget != null) {
                    LinearMeterWidget.LimitsFromPV limitsFromPVSetting = model_widget.propLimitsFromPV().getValue();
                    if (limitsFromPVSetting.equals(LinearMeterWidget.LimitsFromPV.LimitsFromPV) ||
                            limitsFromPVSetting.equals(LinearMeterWidget.LimitsFromPV.MinAndMaxFromPV)) {
                        Range displayRange = display.getDisplayRange();
                        if (displayRange != null
                                && Double.isFinite(displayRange.getMinimum())
                                && Double.isFinite(displayRange.getMaximum())
                                && displayRange.getMaximum() - displayRange.getMinimum() > 0.0) {
                            if (meter.linearMeterScale.getValueRange().getLow() != displayRange.getMinimum() || meter.linearMeterScale.getValueRange().getHigh() != displayRange.getMaximum() || !meter.getValidRange()) {
                                meter.setRange(displayRange.getMinimum(), displayRange.getMaximum(), true);
                            }
                        } else {
                            Range controlRange = display.getControlRange();
                            if (controlRange != null
                                    && Double.isFinite(controlRange.getMinimum())
                                    && Double.isFinite(controlRange.getMaximum())
                                    && controlRange.getMaximum() - controlRange.getMinimum() > 0.0) {
                                if (meter.linearMeterScale.getValueRange().getLow() != controlRange.getMinimum() || meter.linearMeterScale.getValueRange().getHigh() != controlRange.getMaximum() || !meter.getValidRange()) {
                                    meter.setRange(controlRange.getMinimum(), controlRange.getMaximum(), true);
                                }
                            } else if (newObservedMinAndMaxValues && !Double.isNaN(observedMin) && !Double.isNaN(observedMax)) {
                                meter.setRange(observedMin - 1, observedMax + 1, false);
                                newObservedMinAndMaxValues = false;
                            } else if (meter.linearMeterScale.getValueRange().getLow() != 0.0 || meter.linearMeterScale.getValueRange().getHigh() != 100) {
                                meter.setRange(0.0, 100.0, false);
                            }
                        }
                    }
                    if (limitsFromPVSetting.equals(LinearMeterWidget.LimitsFromPV.LimitsFromPV) ||
                            limitsFromPVSetting.equals(LinearMeterWidget.LimitsFromPV.AlarmLimitsFromPV)) {
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
                meter.setCurrentValue(newValue);
            });
        }
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();

        if (dirty_look.checkAndClear())
        {
            meter.withWriteLock(() -> {
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
            });
        }
    }

    @Override
    public void dispose()
    {
        meter.dispose();
        super.dispose();
    }
}
