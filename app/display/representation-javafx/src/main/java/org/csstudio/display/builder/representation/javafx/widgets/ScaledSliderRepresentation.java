/*******************************************************************************
 * Copyright (c) 2015-2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.converter.FormatStringConverter;
import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ScaledSliderWidget;
import org.csstudio.display.builder.representation.javafx.JFXPreferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import java.text.DecimalFormat;
import java.time.Instant;
import java.util.logging.Level;

import static org.csstudio.display.builder.representation.ToolkitRepresentation.logger;

/** Creates JavaFX item for model widget
 *  @author Amanda Carpenter
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScaledSliderRepresentation extends RegionBaseRepresentation<GridPane, ScaledSliderWidget>
{
    private final DirtyFlag dirty_layout = new DirtyFlag();
    private final DirtyFlag dirty_enablement = new DirtyFlag();
    private final DirtyFlag dirty_value = new DirtyFlag();
    private final UntypedWidgetPropertyListener layoutChangedListener = this::layoutChanged;
    private final UntypedWidgetPropertyListener limitsChangedListener = this::limitsChanged;
    private final WidgetPropertyListener<Boolean> enablementChangedListener = this::enablementChanged;
    private final WidgetPropertyListener<Boolean> orientationChangedListener = this::orientationChanged;
    private final WidgetPropertyListener<Instant> runtimeConfChangedListener = (p, o, n) -> openConfigurationPanel();
    private final WidgetPropertyListener<VType> valueChangedListener = this::valueChanged;

    private volatile double min = 0.0;
    private volatile double max = 100.0;
    private volatile double lolo = Double.NaN;
    private volatile double low = Double.NaN;
    private volatile double high = Double.NaN;
    private volatile double hihi = Double.NaN;
    private volatile double value = 50.0;
    private volatile double increment = 1.0;
    private volatile double tick_unit = 20;

    private volatile boolean active = false;
    private volatile boolean enabled = false;

    private final Slider slider;
    private final SliderMarkers markers;

    /** Constructor */
    public ScaledSliderRepresentation()
    {
        slider = JFXPreferences.inc_dec_slider
               ? new IncDecSlider()
               : new Slider();
       markers = new SliderMarkers(slider);
    }

    @Override
    protected GridPane createJFXNode() throws Exception
    {
        slider.setFocusTraversable(true);
        slider.setOnKeyPressed((final KeyEvent event) ->
        {
            switch (event.getCode())
            {
            case PAGE_UP:
                slider.increment();
                event.consume();
                break;
            case PAGE_DOWN:
                slider.decrement();
                event.consume();
                break;
            default: break;
            }
        });

        // Disable the contemporary triggering of a value change and of the
        // opening of contextual menu when right-clicking on the slider's
        // thumb lane.
        slider.addEventFilter(MouseEvent.ANY, e ->
        {
            if (e.getButton() == MouseButton.SECONDARY)
                e.consume();
        });

        slider.setValue(value);

        final GridPane pane = new GridPane();
        // pane.setGridLinesVisible(true);
        pane.add(markers, 0, 0);
        pane.getChildren().add(slider);

        // This code manages layout,
        // because otherwise for example border changes would trigger
        // expensive Node.notifyParentOfBoundsChange() recursing up the scene graph
        pane.setManaged(false);

        return pane;
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
        model_widget.propWidth().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propHeight().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propForegroundColor().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propTransparent().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propFont().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propShowScale().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propScaleFormat().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propShowMinorTicks().addUntypedPropertyListener(layoutChangedListener);
        model_widget.propIncrement().addUntypedPropertyListener(layoutChangedListener);

        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);

        model_widget.propEnabled().addPropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().addPropertyListener(enablementChangedListener);

        model_widget.propLevelHi().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propLevelHiHi().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propLevelLo().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propLevelLoLo().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propShowHigh().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propShowHiHi().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propShowLow().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propShowLoLo().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propMinimum().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propMaximum().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propMajorTickStepHint().addUntypedPropertyListener(limitsChangedListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(limitsChangedListener);

        // Since both the widget's PV value and the JFX node's value property might be
        // written to independently during runtime, both must have listeners.
        slider.valueProperty().addListener(this::handleSliderMove);
        slider.setOnMouseReleased(this::handleSliderMouseRelease);

        if (toolkit.isEditMode()) {
            dirty_value.checkAndClear();
        }
        else
        {
            model_widget.runtimePropValue().addPropertyListener(valueChangedListener);
            model_widget.runtimePropConfigure().addPropertyListener(runtimeConfChangedListener);
        }
        enablementChanged(null, null, null);
        limitsChanged(null, null, null);
        layoutChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(layoutChangedListener);
        model_widget.propHeight().removePropertyListener(layoutChangedListener);
        model_widget.propForegroundColor().removePropertyListener(layoutChangedListener);
        model_widget.propBackgroundColor().removePropertyListener(layoutChangedListener);
        model_widget.propTransparent().removePropertyListener(layoutChangedListener);
        model_widget.propFont().removePropertyListener(layoutChangedListener);
        model_widget.propShowScale().removePropertyListener(layoutChangedListener);
        model_widget.propScaleFormat().removePropertyListener(layoutChangedListener);
        model_widget.propShowMinorTicks().removePropertyListener(layoutChangedListener);
        model_widget.propIncrement().removePropertyListener(layoutChangedListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
        model_widget.propEnabled().removePropertyListener(enablementChangedListener);
        model_widget.runtimePropPVWritable().removePropertyListener(enablementChangedListener);
        model_widget.propLevelHi().removePropertyListener(limitsChangedListener);
        model_widget.propLevelHiHi().removePropertyListener(limitsChangedListener);
        model_widget.propLevelLo().removePropertyListener(limitsChangedListener);
        model_widget.propLevelLoLo().removePropertyListener(limitsChangedListener);
        model_widget.propShowHigh().removePropertyListener(limitsChangedListener);
        model_widget.propShowHiHi().removePropertyListener(limitsChangedListener);
        model_widget.propShowLow().removePropertyListener(limitsChangedListener);
        model_widget.propShowLoLo().removePropertyListener(limitsChangedListener);
        model_widget.propMinimum().removePropertyListener(limitsChangedListener);
        model_widget.propMaximum().removePropertyListener(limitsChangedListener);
        model_widget.propMajorTickStepHint().removePropertyListener(limitsChangedListener);
        model_widget.propLimitsFromPV().removePropertyListener(limitsChangedListener);
        if ( !toolkit.isEditMode() )
        {
            model_widget.runtimePropValue().removePropertyListener(valueChangedListener);
            model_widget.runtimePropConfigure().removePropertyListener(runtimeConfChangedListener);
        }
        super.unregisterListeners();
    }

    private void orientationChanged(final WidgetProperty<Boolean> prop, final Boolean old, final Boolean horizontal)
    {
        // When interactively changing orientation, swap width <-> height.
        // This will only affect interactive changes once the widget is represented on the screen.
        // Initially, when the widget is loaded from XML, the representation
        // doesn't exist and the original width, height and orientation are applied
        // without triggering a swap.
        if (toolkit.isEditMode())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            model_widget.propWidth().setValue(h);
            model_widget.propHeight().setValue(w);
        }
        layoutChanged(prop, old, horizontal);
    }

    private void layoutChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        increment = model_widget.propIncrement().getValue();
        if (increment <= 0.0)
            increment = 1.0;
        dirty_layout.mark();
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
        // Start with widget config
        double new_min = model_widget.propMinimum().getValue();
        double new_max = model_widget.propMaximum().getValue();
        double new_lolo = model_widget.propLevelLoLo().getValue();
        double new_low = model_widget.propLevelLo().getValue();
        double new_high = model_widget.propLevelHi().getValue();
        double new_hihi = model_widget.propLevelHiHi().getValue();

        if (model_widget.propLimitsFromPV().getValue())
        {
            // Try to get display range from PV
            final Display display_info = Display.displayOf(model_widget.runtimePropValue().getValue());
            if (display_info != null)
            {
                // Should use the 'control' range but fall back to 'display' range
                if (display_info.getControlRange().isFinite())
                {
                    new_min = display_info.getControlRange().getMinimum();
                    new_max = display_info.getControlRange().getMaximum();
                }
                else
                {
                    new_min = display_info.getDisplayRange().getMinimum();
                    new_max = display_info.getDisplayRange().getMaximum();
                    // May also be empty, will fall back to 0..100
                }

                new_lolo = display_info.getAlarmRange().getMinimum();

                new_low = display_info.getWarningRange().getMinimum();
                new_high = display_info.getWarningRange().getMaximum();

                new_hihi = display_info.getAlarmRange().getMaximum();
            }
        }
        if (! model_widget.propShowLoLo().getValue())
            new_lolo = Double.NaN;
        if (! model_widget.propShowLow().getValue())
            new_low = Double.NaN;
        if (! model_widget.propShowHigh().getValue())
            new_high = Double.NaN;
        if (! model_widget.propShowHiHi().getValue())
            new_hihi = Double.NaN;

        // If invalid limits, fall back to 0..100 range
        if (! (new_min < new_max))
        {
            new_min = 0.0;
            new_max = 100.0;
        }

        boolean changes = false;
        if (Double.compare(min, new_min) != 0)
        {
            min = new_min;
            changes = true;
        }
        if (Double.compare(max, new_max) != 0)
        {
            max = new_max;
            changes = true;
        }

        final double pixel_span = model_widget.propHorizontal().getValue()
                                ? model_widget.propWidth().getValue()
                                : model_widget.propHeight().getValue();
        double tick_dist = model_widget.propMajorTickStepHint().getValue() / pixel_span * (max - min);
        tick_dist = selectNiceStep(tick_dist);
        if (Double.compare(tick_dist, tick_unit) != 0)
        {
            tick_unit = tick_dist;
            changes = true;
        }
        if (Double.compare(lolo, new_lolo) != 0)
        {
            lolo = new_lolo;
            changes = true;
        }
        if (Double.compare(low, new_low) != 0)
        {
            low = new_low;
            changes = true;
        }
        if (Double.compare(high, new_high) != 0)
        {
            high = new_high;
            changes = true;
        }
        if (Double.compare(hihi, new_hihi) != 0)
        {
            hihi = new_hihi;
            changes = true;
        }

        if (changes)
            layoutChanged(null, null, null);
    }

    /** Nice looking steps for the distance between tick,
     *  and the threshold for using them.
     *  In general, the computed steps "fill" the axis.
     *  The nice looking steps should be wider apart,
     *  because tighter steps would result in overlapping label.
     *  The thresholds thus favor the larger steps:
     *  A computed distance of 6.1 turns into 10.0, not 5.0.
     *  @see #selectNiceStep(double)
     */
    final private static double[] NICE_STEPS = { 10.0, 5.0, 2.0, 1.0 },
                             NICE_THRESHOLDS = {  6.0, 3.0, 1.2, 0.0 };

    /** To a human viewer, tick distances of 5.0 are easier to see
     *  than for example 7.
     *
     *  <p>This method tries to adjust a computed tick distance
     *  to one that is hopefully 'nicer'
     *
     *  @param distance Original step distance
     *  @return Optimal step size
     */
    public static double selectNiceStep(final double distance)
    {
        final double log = Math.log10(distance);
        final double order_of_magnitude = Math.pow(10, Math.floor(log));
        final double step = distance / order_of_magnitude;
        for (int i=0; i<NICE_STEPS.length; ++i)
            if (step >= NICE_THRESHOLDS[i])
                return NICE_STEPS[i] * order_of_magnitude;
        return step * order_of_magnitude;
    }

    private void handleSliderMove(final ObservableValue<? extends Number> property, final Number old_value, final Number new_value)
    {
        if (!active)
            toolkit.fireWrite(model_widget, new_value);
    }

    private void handleSliderMouseRelease(MouseEvent event) {
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    private void valueChanged(final WidgetProperty<? extends VType> property, final VType old_value, final VType new_value)
    {
        if (model_widget.propLimitsFromPV().getValue())
            limitsChanged(null, null, null);
        dirty_value.mark();
        toolkit.scheduleUpdate(this);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_enablement.checkAndClear())
            slider.setDisable(!enabled);
        if (dirty_layout.checkAndClear())
        {
            final boolean horizontal = model_widget.propHorizontal().getValue();
            slider.setOrientation(horizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);

            final boolean any_markers = ! (Double.isNaN(lolo) && Double.isNaN(low) &&
                                           Double.isNaN(high) &&Double.isNaN(hihi));
            if (any_markers)
            {
                if (! jfx_node.getChildren().contains(markers))
                    jfx_node.add(markers, 0, 0);
                if (horizontal)
                {
                    GridPane.setConstraints(slider, 0, 1);
                    GridPane.setHgrow(slider, Priority.ALWAYS);
                    GridPane.setVgrow(slider, Priority.NEVER);
                    GridPane.setVgrow(markers, Priority.NEVER);
                }
                else
                {
                    GridPane.setConstraints(slider, 1, 0);
                    GridPane.setHgrow(slider, Priority.NEVER);
                    GridPane.setHgrow(markers, Priority.NEVER);
                    GridPane.setVgrow(slider, Priority.ALWAYS);
                }
            }
            else
            {
                if (jfx_node.getChildren().contains(markers))
                    jfx_node.getChildren().remove(markers);
                GridPane.setConstraints(slider, 0, 0);
                if (horizontal)
                {
                    GridPane.setHgrow(slider, Priority.ALWAYS);
                    GridPane.setVgrow(slider, Priority.NEVER);
                }
                else
                {
                    GridPane.setHgrow(slider, Priority.NEVER);
                    GridPane.setVgrow(slider, Priority.ALWAYS);
                }
            }

            final double width = model_widget.propWidth().getValue();
            final double height = model_widget.propHeight().getValue();
            jfx_node.resize(width, height);
            if (model_widget.propHorizontal().getValue())
                slider.setMaxSize(width, Double.MAX_VALUE);
            else
                slider.setMaxSize(Double.MAX_VALUE, height);

            final Color background_color;
            if (model_widget.propTransparent().getValue())
                background_color = Color.TRANSPARENT;
            else
                background_color = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
            final Background background = new Background(new BackgroundFill(background_color, CornerRadii.EMPTY, Insets.EMPTY));
            jfx_node.setBackground(background);
            markers.setBackground(background);

            final Font font = JFXUtil.convert(model_widget.propFont().getValue());
            markers.setFont(font);

            final String style = // Text color (and border around the 'track')
                                 "-fx-text-background-color: " + JFXUtil.webRgbOrHex(model_widget.propForegroundColor().getValue()) +
                                 // Axis tick marks
                                 "; -fx-background: " + JFXUtil.webRgbOrHex(model_widget.propForegroundColor().getValue()) +
                                 // Font; NOTE only the shorthand font style is supported for fx-tick-label-font;
                                 // e.g. fx-tick-label-font-size etc are not supported!
                                 "; " + JFXUtil.cssFontShorthand("-fx-tick-label-font", font);

            jfx_node.setStyle(style);
            if (model_widget.propShowScale().getValue())
            {
                String format = model_widget.propScaleFormat().getValue();
                if (format.isEmpty())
                	format = "#.#";
            	slider.setLabelFormatter(new FormatStringConverter<Double>(new DecimalFormat(format)));
                slider.setShowTickLabels(true);
                slider.setShowTickMarks(model_widget.propShowMinorTicks().getValue());
            }
            else
            {
                slider.setShowTickLabels(false);
                slider.setShowTickMarks(false);
            }
            active = true;
            try
            {
                // This triggers a 'slider move'
                slider.setMin(min);
                slider.setMax(max);
            }
            finally
            {
                active = false;
            }

            slider.setMajorTickUnit(tick_unit);
            slider.setBlockIncrement(increment);
            // Create minor ticks that mimic the increments,
            // but limit to 9 minor ticks between major ticks
            slider.setMinorTickCount(Math.min((int) Math.round(tick_unit / increment) - 1, 9));

            if (any_markers)
                markers.setAlarmMarkers(lolo, low, high, hihi);
        }
        if (dirty_value.checkAndClear())
        {
            active = true;
            try
            {
                final VType vtype = model_widget.runtimePropValue().getValue();
                double newval = VTypeUtil.getValueNumber(vtype).doubleValue();
                if (newval < min)
                    newval = min;
                else if (newval > max)
                    newval = max;
                if (!slider.isValueChanging())
                {
                    if (Double.isNaN(newval))
                    {
                        logger.log(Level.FINE, model_widget + " PV has invalid value " + vtype);
                        // Setting slider to NaN will hide the 'knob', so user can never
                        // set it back to a normal value.
                        // In addition, the UI can lock up (see SliderGlitchDemo).
                        // --> Set to min
                        slider.setValue(min);
                    }
                    else {
                        slider.setValue(newval);
                    }
                }
                value = newval;
            }
            finally
            {
                active = false;
            }
        }
        jfx_node.layout();
    }

    private SliderConfigPopOver config_popover = null;

    private void openConfigurationPanel()
    {
        if (config_popover == null)
            config_popover = new SliderConfigPopOver(model_widget.propIncrement());
        if (config_popover.isShowing())
            config_popover.hide();
        else
            config_popover.show(slider);
    }
}