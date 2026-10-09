/*******************************************************************************
 * Copyright (c) 2015-2026 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.ScaledPVWidget;
import org.csstudio.display.builder.representation.Preferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.RTTank;
import org.epics.util.stats.Range;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Abstract base for widget representations whose JFX node is an {@link RTTank}.
 *
 *  <p>Handles all logic that depends only on the {@link ScaledPVWidget} contract:
 *  <ul>
 *    <li>Creating and throttle-configuring the {@link RTTank}</li>
 *    <li>Forwarding PV value and display-range changes to the tank</li>
 *    <li>Evaluating alarm limit lines from PV metadata or widget properties</li>
 *    <li>Orientation transform (rotation for horizontal layout)</li>
 *    <li>Scheduling representation updates on property changes</li>
 *  </ul>
 *
 *  <p>Subclasses provide:
 *  <ul>
 *    <li>{@link #isHorizontal()}: the widget's own {@code horizontal} property</li>
 *    <li>{@link #registerLookListeners()} / {@link #unregisterLookListeners()}:
 *        listeners on the widget-specific appearance properties
 *        (colors, scale visibility, font, ...)</li>
 *    <li>{@link #applyLookToTank()}: push the current appearance properties
 *        to the tank after size and orientation have been set</li>
 *    <li>{@link #configureTank()}: optional one-time tank setup</li>
 *  </ul>
 *
 *  <p>{@link #registerScaleLookListeners()} and {@link #applyScaleLook()}
 *  handle the scale look properties that all {@link ScaledPVWidget}s share.
 *
 *  @param <W> concrete {@link ScaledPVWidget} subtype
 */
public abstract class RTScaledWidgetRepresentation<W extends ScaledPVWidget>
    extends RegionBaseRepresentation<Pane, W>
{
    /** The rendering canvas shared by all RTTank-based widgets. */
    protected volatile RTTank tank;

    /** Dirty flag for appearance (color, scale, size).  Value updates do not
     *  set this, they bypass the JFX representation update cycle entirely by
     *  calling {@link RTTank#setValue} directly. */
    protected final DirtyFlag dirtyLook = new DirtyFlag();

    /** Marks appearance dirty and schedules an update. Shared by subclass
     *  listeners on color / scale / font properties. */
    protected final UntypedWidgetPropertyListener lookListener =
            (p, o, n) -> { dirtyLook.mark(); toolkit.scheduleUpdate(this); };

    /** Forwards PV value or display-range changes to the tank immediately. */
    private final UntypedWidgetPropertyListener valueListener  = this::valueChanged;

    /** Re-evaluates and pushes alarm limit lines whenever a limit property changes. */
    private final UntypedWidgetPropertyListener limitsListener = this::limitsChanged;

    /** Swaps width and height in the editor and triggers a look update. */
    protected final WidgetPropertyListener<Boolean> orientationChangedListener =
            this::orientationChanged;

    /** Whether orientation transforms are currently applied to the tank node. */
    private boolean wasTransformed = false;

    @Override
    public Pane createJFXNode() throws Exception
    {
        tank = new RTTank();
        tank.setUpdateThrottle(Preferences.image_update_delay, TimeUnit.MILLISECONDS);
        configureTank();
        return new Pane(tank);
    }

    /** Called once after the tank is created, for one-time settings like a rendering style */
    protected void configureTank()
    {
    }

    /** Register listeners on the {@link ScaledPVWidget} value and limit
     *  properties, then call {@link #registerLookListeners()} for the
     *  subclass to add its widget-specific appearance listeners.
     *  <p>The current range, value and limits are applied once at the end. */
    @Override
    protected void registerListeners()
    {
        super.registerListeners();

        // Value / range
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);

        // Alarm limit lines
        model_widget.propShowAlarmLimits().addUntypedPropertyListener(limitsListener);
        model_widget.propAlarmLimitsFromPV().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLoLo().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLow().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHigh().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHiHi().addUntypedPropertyListener(limitsListener);

        // Alarm color changes only affect appearance, not limits
        model_widget.propMinorAlarmColor().addUntypedPropertyListener(lookListener);
        model_widget.propMajorAlarmColor().addUntypedPropertyListener(lookListener);

        // Widget-specific look properties (colors, scale, font, ...)
        registerLookListeners();

        // Apply the current state, range first, then limits
        valueChanged(null, null, null);
        limitsChanged(null, null, null);
    }

    /** Listen to the widget size, the label format and the scale look
     *  properties shared by all scaled widgets.
     *  Subclasses call this from {@link #registerLookListeners()}. */
    protected void registerScaleLookListeners()
    {
        model_widget.propWidth().addUntypedPropertyListener(lookListener);
        model_widget.propHeight().addUntypedPropertyListener(lookListener);
        model_widget.propFormat().addUntypedPropertyListener(lookListener);
        model_widget.propPrecision().addUntypedPropertyListener(lookListener);
        for (WidgetProperty<?> property : model_widget.getScaleLookProperties())
            property.addUntypedPropertyListener(lookListener);
    }

    /** Undo {@link #registerScaleLookListeners()} */
    protected void unregisterScaleLookListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propFormat().removePropertyListener(lookListener);
        model_widget.propPrecision().removePropertyListener(lookListener);
        for (WidgetProperty<?> property : model_widget.getScaleLookProperties())
            property.removePropertyListener(lookListener);
    }

    /** Register listeners on widget-specific appearance properties.
     *  The implementation should add listeners using {@link #lookListener}
     *  (or a dedicated listener) and call nothing on the tank directly,
     *  that happens in {@link #applyLookToTank()}. */
    protected abstract void registerLookListeners();

    @Override
    protected void unregisterListeners()
    {
        model_widget.propLimitsFromPV().removePropertyListener(valueListener);
        model_widget.propMinimum().removePropertyListener(valueListener);
        model_widget.propMaximum().removePropertyListener(valueListener);
        model_widget.runtimePropValue().removePropertyListener(valueListener);

        model_widget.propShowAlarmLimits().removePropertyListener(limitsListener);
        model_widget.propAlarmLimitsFromPV().removePropertyListener(limitsListener);
        model_widget.propLevelLoLo().removePropertyListener(limitsListener);
        model_widget.propLevelLow().removePropertyListener(limitsListener);
        model_widget.propLevelHigh().removePropertyListener(limitsListener);
        model_widget.propLevelHiHi().removePropertyListener(limitsListener);

        model_widget.propMinorAlarmColor().removePropertyListener(lookListener);
        model_widget.propMajorAlarmColor().removePropertyListener(lookListener);

        unregisterLookListeners();
        super.unregisterListeners();
    }

    /** Unregister the listeners added by {@link #registerLookListeners()}. */
    protected abstract void unregisterLookListeners();

    /** Called on every PV value update and on range-related property changes.
     *  Updates the range, the alarm limits from the PV metadata and the
     *  fill level of the tank. */
    private void valueChanged(final WidgetProperty<?> prop,
                              final Object oldValue, final Object newValue)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();
        final boolean limitsFromPV = model_widget.propLimitsFromPV().getValue();
        updateRange(vtype, limitsFromPV);

        if (model_widget.propAlarmLimitsFromPV().getValue())
            applyAlarmLimits(vtype);

        // In edit mode there is no PV, so the widget range is the effective range
        final double min = model_widget.propMinimum().getValue();
        final double max = model_widget.propMaximum().getValue();
        final double value = toolkit.isEditMode()
            ? (min + max) / 2.0
            : VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);
    }

    /** Push the display range to the tank.
     *  When {@code limitsFromPV} is {@code true}, reads the range from PV
     *  display metadata and falls back to widget properties when metadata is
     *  unavailable.  When {@code false}, uses the widget properties directly.
     *
     *  @param vtype       current PV value (may be {@code null} before connect)
     *  @param limitsFromPV whether the range should come from the PV */
    private void updateRange(final VType vtype, final boolean limitsFromPV)
    {
        double min = model_widget.propMinimum().getValue();
        double max = model_widget.propMaximum().getValue();
        if (limitsFromPV)
        {
            final Display displayInfo = Display.displayOf(vtype);
            if (displayInfo != null  &&  displayInfo.getDisplayRange().isFinite())
            {
                min = displayInfo.getDisplayRange().getMinimum();
                max = displayInfo.getDisplayRange().getMaximum();
            }
        }
        tank.setRange(min, max);
    }

    /** Triggered when any alarm limit property changes; delegates to
     *  {@link #applyAlarmLimits(VType)} with the current PV value. */
    private void limitsChanged(final WidgetProperty<?> property,
                               final Object oldValue, final Object newValue)
    {
        applyAlarmLimits(model_widget.runtimePropValue().getValue());
    }

    /** Resolves alarm limits from PV metadata or widget properties (depending on
     *  {@code alarm_limits_from_pv}) and pushes them to the tank.
     *  Clears all limit lines when {@code show_alarm_limits} is {@code false}. */
    private void applyAlarmLimits(final VType vtype)
    {
        if (!model_widget.propShowAlarmLimits().getValue())
        {
            tank.setLimits(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
            return;
        }
        final double lolo, lo, hi, hihi;
        if (model_widget.propAlarmLimitsFromPV().getValue())
        {
            final Display displayInfo = Display.displayOf(vtype);
            if (displayInfo != null)
            {
                final Range minor = displayInfo.getWarningRange();
                final Range major = displayInfo.getAlarmRange();
                lo   = minor.getMinimum();
                hi   = minor.getMaximum();
                lolo = major.getMinimum();
                hihi = major.getMaximum();
            }
            else
                lolo = lo = hi = hihi = Double.NaN;
        }
        else
        {
            lolo = model_widget.propLevelLoLo().getValue();
            lo   = model_widget.propLevelLow().getValue();
            hi   = model_widget.propLevelHigh().getValue();
            hihi = model_widget.propLevelHiHi().getValue();
        }
        tank.setLimits(lolo, lo, hi, hihi);
        tank.setLimitsFromPV(model_widget.propAlarmLimitsFromPV().getValue());
    }

    /** @return whether this widget is currently in horizontal orientation */
    protected abstract boolean isHorizontal();

    /** Swaps width and height in the editor (so the widget visually rotates
     *  rather than stretching) and triggers a look update. */
    protected void orientationChanged(final WidgetProperty<Boolean> prop,
                                      final Boolean old, final Boolean horizontal)
    {
        if (toolkit.isEditMode())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            model_widget.propWidth().setValue(h);
            model_widget.propHeight().setValue(w);
        }
        dirtyLook.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Push the scale look properties shared by all scaled widgets to the
     *  tank: font, colors, log scale, label format, scale visibility and
     *  tick options, border width.
     *  Subclasses call this from {@link #applyLookToTank()}. */
    protected void applyScaleLook()
    {
        tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
        tank.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
        tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
        tank.setLogScale(model_widget.propLogScale().getValue());
        tank.setLabelFormat(model_widget.propFormat().getValue(),
                            model_widget.propPrecision().getValue());
        tank.setScaleVisible(model_widget.propScaleVisible().getValue());
        tank.setShowMinorTicks(model_widget.propShowMinorTicks().getValue());
        tank.setScaleLabelsVisible(model_widget.propShowScaleLabels().getValue());
        tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
        tank.setPerpendicularTickLabels(model_widget.propPerpendicularTickLabels().getValue());
        tank.setBorderWidth(model_widget.propBorderWidth().getValue());
    }

    /** Push the current widget-specific appearance properties to the tank.
     *  Called from {@link #updateChanges()} after size and orientation
     *  have been applied. */
    protected abstract void applyLookToTank();

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirtyLook.checkAndClear())
        {
            final double width  = model_widget.propWidth().getValue();
            final double height = model_widget.propHeight().getValue();

            // RTTank renders vertically; rotate 90 degrees clockwise for horizontal bars.
            if (isHorizontal())
            {
                tank.getTransforms().setAll(new Translate(width, 0),
                                            new Rotate(90, 0, 0));
                wasTransformed = true;
                tank.setWidth(height);
                tank.setHeight(width);
            }
            else
            {
                if (wasTransformed)
                    tank.getTransforms().clear();
                wasTransformed = false;
                tank.setWidth(width);
                tank.setHeight(height);
            }
            jfx_node.setPrefSize(width, height);

            applyLookToTank();
            tank.setAlarmColors(
                JFXUtil.convert(model_widget.propMinorAlarmColor().getValue()),
                JFXUtil.convert(model_widget.propMajorAlarmColor().getValue()));
        }
    }
}
