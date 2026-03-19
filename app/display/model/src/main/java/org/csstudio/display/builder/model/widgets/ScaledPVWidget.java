/*******************************************************************************
 * Copyright (c) 2015-2025 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.widgets;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newBooleanPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newColorPropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.newDoublePropertyDescriptor;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propLimitsFromPV;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMaximum;
import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.propMinimum;

import java.util.List;

import org.csstudio.display.builder.model.Messages;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyCategory;
import org.csstudio.display.builder.model.WidgetPropertyDescriptor;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.properties.WidgetColor;

/** Base class for PV widgets that display a numeric value on a scale
 *  (Tank, Thermometer, ProgressBar and similar).
 *
 *  <p>Consolidates properties that were historically duplicated across
 *  individual scaled widgets: display range, limits-from-PV toggle,
 *  alarm-level thresholds and their colours.  This follows the pattern
 *  established by CS-Studio BOY's {@code LinearMeterWidget} which exposed
 *  LOLO/LO/HI/HIHI levels with both PV-sourced and manual modes.
 *
 *  <p>Provides:
 *  <ul>
 *    <li>{@code limits_from_pv} &mdash; whether the PV display range
 *        overrides the manual {@code minimum}/{@code maximum}.  This is
 *        the existing upstream property, unchanged.</li>
 *    <li>{@code alarm_limits_from_pv} &mdash; whether PV alarm metadata
 *        overrides the manual LOLO/LO/HI/HIHI levels.  New property;
 *        old Phoebus silently ignores the XML element.</li>
 *    <li>Manual {@code minimum} / {@code maximum} range.</li>
 *    <li>A {@code show_limits} toggle for alarm-limit visual markers.</li>
 *    <li>Manual LOLO / LO / HI / HIHI thresholds (NaN = inactive).</li>
 *    <li>Configurable minor/major alarm colours defaulting to the named
 *        {@code ALARM_MINOR} / {@code ALARM_MAJOR} palette entries.</li>
 *  </ul>
 *
 *  <p>Backward compatibility note: every new property uses a type that
 *  stock Phoebus already knows (boolean, double, color).  Old versions
 *  will silently skip the unknown XML elements; saving in old Phoebus
 *  will simply drop them.  No ordinal-based enums are used, specifically
 *  to avoid the round-trip failure that {@code BooleanWidgetProperty}
 *  causes when it encounters an ordinal &ge; 2.
 *
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, alarm limits, scale refactoring
 */
@SuppressWarnings("nls")
public abstract class ScaledPVWidget extends PVWidget
{
    // ---- Property descriptors -------------------------------------------

    /** 'alarm_limits_from_pv' — use PV alarm metadata for LOLO/LO/HI/HIHI? */
    public static final WidgetPropertyDescriptor<Boolean> propAlarmLimitsFromPV =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "alarm_limits_from_pv",
                                     Messages.WidgetProperties_AlarmLimitsFromPV);

    /** 'show_alarm_limits' — draw LOLO/LO/HI/HIHI alarm limit markers on the scale */
    public static final WidgetPropertyDescriptor<Boolean> propShowAlarmLimits =
        newBooleanPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "show_alarm_limits",
                                     Messages.WidgetProperties_ShowAlarmLimits);

    /** 'level_lolo' — LOLO (major alarm) lower threshold; {@code NaN} = inactive */
    public static final WidgetPropertyDescriptor<Double> propLevelLoLo =
        newDoublePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "level_lolo",
                                    Messages.WidgetProperties_LevelLoLo);

    /** 'level_lo' — LO (minor warning) lower threshold; {@code NaN} = inactive */
    public static final WidgetPropertyDescriptor<Double> propLevelLow =
        newDoublePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "level_lo",
                                    Messages.WidgetProperties_LevelLow);

    /** 'level_hi' — HI (minor warning) upper threshold; {@code NaN} = inactive */
    public static final WidgetPropertyDescriptor<Double> propLevelHigh =
        newDoublePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "level_hi",
                                    Messages.WidgetProperties_LevelHigh);

    /** 'level_hihi' — HIHI (major alarm) upper threshold; {@code NaN} = inactive */
    public static final WidgetPropertyDescriptor<Double> propLevelHiHi =
        newDoublePropertyDescriptor(WidgetPropertyCategory.BEHAVIOR, "level_hihi",
                                    Messages.WidgetProperties_LevelHiHi);

    /** 'minor_alarm_color' — color for LO / HI lines; defaults to named MINOR alarm color */
    public static final WidgetPropertyDescriptor<WidgetColor> propMinorAlarmColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "minor_alarm_color",
                                   Messages.WidgetProperties_MinorAlarmColor);

    /** 'major_alarm_color' — color for LOLO / HIHI lines; defaults to named MAJOR alarm color */
    public static final WidgetPropertyDescriptor<WidgetColor> propMajorAlarmColor =
        newColorPropertyDescriptor(WidgetPropertyCategory.DISPLAY, "major_alarm_color",
                                   Messages.WidgetProperties_MajorAlarmColor);

    // ---- Instance fields ------------------------------------------------

    private volatile WidgetProperty<Boolean>     limits_from_pv;
    private volatile WidgetProperty<Boolean>     alarm_limits_from_pv;
    private volatile WidgetProperty<Double>      minimum;
    private volatile WidgetProperty<Double>      maximum;
    private volatile WidgetProperty<Boolean>     show_alarm_limits;
    private volatile WidgetProperty<Double>      level_lolo;
    private volatile WidgetProperty<Double>      level_low;
    private volatile WidgetProperty<Double>      level_high;
    private volatile WidgetProperty<Double>      level_hihi;
    private volatile WidgetProperty<WidgetColor> minor_alarm_color;
    private volatile WidgetProperty<WidgetColor> major_alarm_color;

    protected ScaledPVWidget(final String type, final int default_width, final int default_height)
    {
        super(type, default_width, default_height);
    }

    /** Reorder properties so that alarm-related items sit together.
     *  {@code border_alarm_sensitive} (from PVWidget) is moved down to
     *  appear next to {@code alarm_limits_from_pv}.
     */
    @Override
    protected void defineProperties(final List<WidgetProperty<?>> properties)
    {
        super.defineProperties(properties);

        // Move border_alarm_sensitive (added by PVWidget) down so it sits
        // next to the alarm-related limit properties.
        final WidgetProperty<?> alarm_border_prop = propBorderAlarmSensitive();
        properties.remove(alarm_border_prop);

        properties.add(limits_from_pv       = propLimitsFromPV.createProperty(this, true));
        properties.add(minimum              = propMinimum.createProperty(this, 0.0));
        properties.add(maximum              = propMaximum.createProperty(this, 100.0));
        properties.add(alarm_border_prop);
        properties.add(alarm_limits_from_pv = propAlarmLimitsFromPV.createProperty(this, true));
        properties.add(show_alarm_limits    = propShowAlarmLimits.createProperty(this, false));
        properties.add(level_lolo           = propLevelLoLo.createProperty(this, Double.NaN));
        properties.add(level_low            = propLevelLow.createProperty(this, Double.NaN));
        properties.add(level_high           = propLevelHigh.createProperty(this, Double.NaN));
        properties.add(level_hihi           = propLevelHiHi.createProperty(this, Double.NaN));
        properties.add(minor_alarm_color    = propMinorAlarmColor.createProperty(this,
                WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR)));
        properties.add(major_alarm_color    = propMajorAlarmColor.createProperty(this,
                WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR)));
    }

    /** @return 'limits_from_pv' property (min/max display range) */
    public WidgetProperty<Boolean> propLimitsFromPV()        { return limits_from_pv; }

    /** @return 'alarm_limits_from_pv' property (LOLO/LO/HI/HIHI alarm levels) */
    public WidgetProperty<Boolean> propAlarmLimitsFromPV()   { return alarm_limits_from_pv; }

    /** @return 'minimum' property */
    public WidgetProperty<Double> propMinimum()              { return minimum; }

    /** @return 'maximum' property */
    public WidgetProperty<Double> propMaximum()              { return maximum; }

    /** @return 'show_alarm_limits' property */
    public WidgetProperty<Boolean> propShowAlarmLimits()     { return show_alarm_limits; }

    /** @return 'level_lolo' property */
    public WidgetProperty<Double> propLevelLoLo()            { return level_lolo; }

    /** @return 'level_lo' property */
    public WidgetProperty<Double> propLevelLow()             { return level_low; }

    /** @return 'level_hi' property */
    public WidgetProperty<Double> propLevelHigh()            { return level_high; }

    /** @return 'level_hihi' property */
    public WidgetProperty<Double> propLevelHiHi()            { return level_hihi; }

    /** @return 'minor_alarm_color' property */
    public WidgetProperty<WidgetColor> propMinorAlarmColor() { return minor_alarm_color; }

    /** @return 'major_alarm_color' property */
    public WidgetProperty<WidgetColor> propMajorAlarmColor() { return major_alarm_color; }
}
