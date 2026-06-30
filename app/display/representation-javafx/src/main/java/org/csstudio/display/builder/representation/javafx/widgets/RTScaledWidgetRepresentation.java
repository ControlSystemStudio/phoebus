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
 *    <li>Orientation transform (90° rotation for horizontal layout)</li>
 *    <li>Scheduling representation updates on property changes</li>
 *  </ul>
 *
 *  <p>Subclasses provide:
 *  <ul>
 *    <li>{@link #isHorizontal()} — read the widget's own {@code horizontal} property</li>
 *    <li>{@link #registerLookListeners()} / {@link #unregisterLookListeners()} —
 *        add / remove listeners on widget-specific appearance properties
 *        (colours, scale visibility, font, …)</li>
 *    <li>{@link #applyLookToTank(double, double)} — push the current appearance
 *        properties to the tank after size and orientation have been set</li>
 *    <li>{@link #configureTank()} — called once after tank creation; override to
 *        perform any one-time tank setup (e.g. enabling a rendering style)</li>
 *  </ul>
 *
 *  <p>Neither this class nor its subclasses have any knowledge of each other's
 *  widget type: {@code TankRepresentation} and {@code ProgressBarRepresentation}
 *  are fully independent.
 *
 *  @param <W> concrete {@link ScaledPVWidget} subtype
 *  @author Heredie Delvalle &mdash; CLS, extracted from TankRepresentation /
 *          ProgressBarRepresentation to eliminate duplication
 */
@SuppressWarnings("nls")
public abstract class RTScaledWidgetRepresentation<W extends ScaledPVWidget>
    extends RegionBaseRepresentation<Pane, W>
{
    // ── shared state ──────────────────────────────────────────────────────────

    /** The rendering canvas shared by all RTTank-based widgets. */
    protected volatile RTTank tank;

    /** Dirty flag for appearance (color, scale, size).  Value updates do not
     *  set this — they bypass the JFX representation update cycle entirely by
     *  calling {@link RTTank#setValue} directly. */
    protected final DirtyFlag dirty_look = new DirtyFlag();

    // ── listeners ─────────────────────────────────────────────────────────────

    /** Marks appearance dirty and schedules an update. Shared by subclass
     *  listeners on color / scale / font properties. */
    protected final UntypedWidgetPropertyListener lookListener =
            (p, o, n) -> { dirty_look.mark(); toolkit.scheduleUpdate(this); };

    /** Forwards PV value or display-range changes to the tank immediately. */
    private final UntypedWidgetPropertyListener valueListener  = this::valueChanged;

    /** Re-evaluates and pushes alarm limit lines whenever a limit property changes. */
    private final UntypedWidgetPropertyListener limitsListener = this::limitsChanged;

    /** Swaps width ↔ height in the editor and triggers a look update. */
    protected final WidgetPropertyListener<Boolean> orientationChangedListener =
            this::orientationChanged;

    /** Whether orientation transforms are currently applied to the tank node. */
    private boolean was_transformed = false;

    // ── JFX node creation ─────────────────────────────────────────────────────

    @Override
    public Pane createJFXNode() throws Exception
    {
        tank = new RTTank();
        tank.setUpdateThrottle(Preferences.image_update_delay, TimeUnit.MILLISECONDS);
        configureTank();
        return new Pane(tank);
    }

    /** Called once after {@link #tank} is created.
     *  Override to apply one-time tank settings (e.g. a rendering style). */
    protected void configureTank()
    {
        // no-op by default
    }

    // ── listener lifecycle ────────────────────────────────────────────────────

    /** Register listeners on the {@link ScaledPVWidget} value and limit
     *  properties, then call {@link #registerLookListeners()} for the
     *  subclass to add its widget-specific appearance listeners.
     *  <p>Initial value and limit state is applied at the end so the tank
     *  shows the correct fill level as soon as the PV connects. */
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

        // Widget-specific look properties (colours, scale, font, …)
        registerLookListeners();

        // Seed initial state — range first, then limits
        valueChanged(null, null, null);
        limitsChanged(null, null, null);
    }

    /** Register listeners on widget-specific appearance properties.
     *  The implementation should add listeners using {@link #lookListener}
     *  (or a dedicated listener) and call nothing on the tank directly —
     *  that happens in {@link #applyLookToTank(double, double)}. */
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

    // ── value / limits handling ───────────────────────────────────────────────

    /** Called on every PV value update and on range-related property changes.
     *  Updates the tank's fill level.  Also updates the display range when the
     *  range may have changed — i.e. when a range property fired, or when
     *  limits come from the PV (range is embedded in every VType).
     *  Alarm limits from PV metadata are also re-evaluated here. */
    private void valueChanged(final WidgetProperty<?> prop,
                              final Object old_value, final Object new_value)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();
        final boolean limits_from_pv = model_widget.propLimitsFromPV().getValue();

        // Skip the scale-range update when a pure PV value arrived and the
        // range is fixed by widget properties: scale.setValueRange() recomputes
        // tick layout on every call, so skipping it at 20 Hz saves real work.
        if (prop != model_widget.runtimePropValue() || limits_from_pv)
            updateRange(vtype, limits_from_pv);

        // Alarm metadata is embedded in each VType — re-evaluate on every update.
        if (model_widget.propAlarmLimitsFromPV().getValue())
            applyAlarmLimits(vtype);

        final double min = model_widget.propMinimum().getValue();
        final double max = model_widget.propMaximum().getValue();
        final double value = toolkit.isEditMode()
            ? (min + max) / 2.0
            : VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);
    }

    /** Push the display range to the tank.
     *  When {@code limits_from_pv} is {@code true}, reads the range from PV
     *  display metadata and falls back to widget properties when metadata is
     *  unavailable.  When {@code false}, uses the widget properties directly.
     *
     *  @param vtype       current PV value (may be {@code null} before connect)
     *  @param limits_from_pv whether the range should come from the PV */
    private void updateRange(final VType vtype, final boolean limits_from_pv)
    {
        double min = model_widget.propMinimum().getValue();
        double max = model_widget.propMaximum().getValue();
        if (limits_from_pv)
        {
            final Display display_info = Display.displayOf(vtype);
            if (display_info != null  &&  display_info.getDisplayRange().isFinite())
            {
                min = display_info.getDisplayRange().getMinimum();
                max = display_info.getDisplayRange().getMaximum();
            }
        }
        tank.setRange(min, max);
    }

    /** Triggered when any alarm limit property changes; delegates to
     *  {@link #applyAlarmLimits(VType)} with the current PV value. */
    private void limitsChanged(final WidgetProperty<?> property,
                               final Object old_value, final Object new_value)
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
            final Display display_info = Display.displayOf(vtype);
            if (display_info != null)
            {
                final Range minor = display_info.getWarningRange();
                final Range major = display_info.getAlarmRange();
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

    // ── orientation & appearance ──────────────────────────────────────────────

    /** @return whether this widget is currently in horizontal orientation */
    protected abstract boolean isHorizontal();

    /** Swaps width ↔ height in the editor (so the widget visually rotates
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
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Push current widget-specific appearance properties to the tank.
     *  Called from {@link #updateChanges()} after size and orientation
     *  transforms have already been applied.
     *
     *  @param width  logical widget width in pixels  (pre-rotation)
     *  @param height logical widget height in pixels (pre-rotation) */
    protected abstract void applyLookToTank(double width, double height);

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            final double width  = model_widget.propWidth().getValue();
            final double height = model_widget.propHeight().getValue();

            // RTTank renders vertically; rotate 90° clockwise for horizontal bars.
            if (isHorizontal())
            {
                tank.getTransforms().setAll(new Translate(width, 0),
                                            new Rotate(90, 0, 0));
                was_transformed = true;
                tank.setWidth(height);
                tank.setHeight(width);
            }
            else
            {
                if (was_transformed)
                    tank.getTransforms().clear();
                was_transformed = false;
                tank.setWidth(width);
                tank.setHeight(height);
            }
            jfx_node.setPrefSize(width, height);

            applyLookToTank(width, height);
            tank.setAlarmColors(
                JFXUtil.convert(model_widget.propMinorAlarmColor().getValue()),
                JFXUtil.convert(model_widget.propMajorAlarmColor().getValue()));
        }
    }
}
