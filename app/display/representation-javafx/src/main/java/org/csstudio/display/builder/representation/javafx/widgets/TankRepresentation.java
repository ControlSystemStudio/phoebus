/*******************************************************************************
 * Copyright (c) 2015-2023 Oak Ridge National Laboratory.
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
import org.csstudio.display.builder.model.widgets.TankWidget;
import org.csstudio.display.builder.representation.Preferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.RTTank;
import org.epics.util.stats.Range;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 *  @author Heredie Delvalle &mdash; CLS, alarm limits, dual scale,
 *          format/precision wiring
 */
public class TankRepresentation extends RegionBaseRepresentation<Pane, TankWidget>
{
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookListener   = this::lookChanged;
    private final UntypedWidgetPropertyListener valueListener  = this::valueChanged;
    private final UntypedWidgetPropertyListener limitsListener = this::limitsChanged;
    private final WidgetPropertyListener<Boolean> orientationChangedListener = this::orientationChanged;

    private volatile RTTank tank;

    @Override
    public Pane createJFXNode() throws Exception
    {
        tank = new RTTank();
        tank.setUpdateThrottle(Preferences.image_update_delay, TimeUnit.MILLISECONDS);
        return new Pane(tank);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(lookListener);
        model_widget.propHeight().addUntypedPropertyListener(lookListener);
        model_widget.propFont().addUntypedPropertyListener(lookListener);
        model_widget.propForeground().addUntypedPropertyListener(lookListener);
        model_widget.propBackground().addUntypedPropertyListener(lookListener);
        model_widget.propFillColor().addUntypedPropertyListener(lookListener);
        model_widget.propEmptyColor().addUntypedPropertyListener(lookListener);
        model_widget.propScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propShowMinorTicks().addUntypedPropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().addUntypedPropertyListener(lookListener);
        model_widget.propFormat().addUntypedPropertyListener(lookListener);
        model_widget.propPrecision().addUntypedPropertyListener(lookListener);
        model_widget.propMinorAlarmColor().addUntypedPropertyListener(lookListener);
        model_widget.propMajorAlarmColor().addUntypedPropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propBorderWidth().addUntypedPropertyListener(lookListener);
        model_widget.propLogScale().addUntypedPropertyListener(lookListener);

        // Range and fill-level; need re-evaluation on every PV sample
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);
        // Alarm limits; only need re-evaluation when limit properties change.
        // When alarm_limits_from_pv=true, valueChanged() calls applyAlarmLimits() too.
        model_widget.propShowAlarmLimits().addUntypedPropertyListener(limitsListener);
        model_widget.propAlarmLimitsFromPV().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLoLo().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLow().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHigh().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHiHi().addUntypedPropertyListener(limitsListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
        // Initial apply — order matters: range first, then limits, then value
        valueChanged(null, null, null);
        limitsChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propFont().removePropertyListener(lookListener);
        model_widget.propForeground().removePropertyListener(lookListener);
        model_widget.propBackground().removePropertyListener(lookListener);
        model_widget.propFillColor().removePropertyListener(lookListener);
        model_widget.propEmptyColor().removePropertyListener(lookListener);
        model_widget.propScaleVisible().removePropertyListener(lookListener);
        model_widget.propShowMinorTicks().removePropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().removePropertyListener(lookListener);
        model_widget.propFormat().removePropertyListener(lookListener);
        model_widget.propPrecision().removePropertyListener(lookListener);
        model_widget.propMinorAlarmColor().removePropertyListener(lookListener);
        model_widget.propMajorAlarmColor().removePropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().removePropertyListener(lookListener);
        model_widget.propBorderWidth().removePropertyListener(lookListener);
        model_widget.propLogScale().removePropertyListener(lookListener);

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
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
        super.unregisterListeners();
    }

    private void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    /** Update the display range and fill level.  Called on every PV value change.
     *  Alarm limits from PV metadata are also refreshed here (the metadata is
     *  carried inside the VType on every update).  Manually-configured limits
     *  are managed exclusively by {@link #limitsChanged}.
     */
    private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();

        double min_val = model_widget.propMinimum().getValue();
        double max_val = model_widget.propMaximum().getValue();
        if (model_widget.propLimitsFromPV().getValue())
        {
            final Display display_info = Display.displayOf(vtype);
            if (display_info != null && display_info.getDisplayRange().isFinite())
            {
                min_val = display_info.getDisplayRange().getMinimum();
                max_val = display_info.getDisplayRange().getMaximum();
            }
        }
        tank.setRange(min_val, max_val);

        // Alarm metadata is embedded in the VType, so re-check it on every update.
        // When using widget-configured limits, limitsChanged() handles updates instead.
        if (model_widget.propAlarmLimitsFromPV().getValue())
            applyAlarmLimits(vtype);

        final double value = toolkit.isEditMode()
            ? (min_val + max_val) / 2
            : VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);
    }

    /** Re-apply alarm limit lines.  Called when any limit property changes.
     *  Also invoked from {@link #valueChanged} when limits come from the PV.
     */
    private void limitsChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        applyAlarmLimits(model_widget.runtimePropValue().getValue());
    }

    /** Push the current alarm limits to the tank, reading from PV metadata or
     *  widget properties depending on {@code alarm_limits_from_pv}.
     *  Clears all limit lines when {@code show_alarm_limits} is {@code false}.
     */
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
            {   // PV connected but no metadata yet — show nothing
                lolo = lo = hi = hihi = Double.NaN;
            }
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

    private void orientationChanged(final WidgetProperty<Boolean> prop, final Boolean old, final Boolean horizontal)
    {
        if (toolkit.isEditMode())
        {   // Swap width <-> height so widget basically rotates
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            model_widget.propWidth().setValue(h);
            model_widget.propHeight().setValue(w);
        }
        lookChanged(prop, old, horizontal);
    }

    /** Track if we ever set transformations because just 'clearing' would otherwise allocate them  */
    private boolean was_transformed = false;

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            double width = model_widget.propWidth().getValue();
            double height = model_widget.propHeight().getValue();
            if (model_widget.propHorizontal().getValue())
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
                tank.setWidth(width);
                tank.setHeight(height);
            }
            jfx_node.setPrefSize(width, height);
            tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            tank.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
            tank.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
            tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
            tank.setEmptyColor(JFXUtil.convert(model_widget.propEmptyColor().getValue()));
            tank.setScaleVisible(model_widget.propScaleVisible().getValue());
            tank.setShowMinorTicks(model_widget.propShowMinorTicks().getValue());
            tank.setPerpendicularTickLabels(model_widget.propPerpendicularTickLabels().getValue());
            tank.setLogScale(model_widget.propLogScale().getValue());
            tank.setLabelFormat(model_widget.propFormat().getValue(),
                                model_widget.propPrecision().getValue());
            tank.setAlarmColors(
                JFXUtil.convert(model_widget.propMinorAlarmColor().getValue()),
                JFXUtil.convert(model_widget.propMajorAlarmColor().getValue()));
            tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
            tank.setBorderWidth(model_widget.propBorderWidth().getValue());
        }
    }
}
