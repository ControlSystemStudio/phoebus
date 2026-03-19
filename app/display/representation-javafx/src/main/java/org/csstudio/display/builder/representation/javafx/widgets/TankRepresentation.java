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
    private final UntypedWidgetPropertyListener lookListener = this::lookChanged;
    private final UntypedWidgetPropertyListener valueListener = this::valueChanged;
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

        model_widget.propShowAlarmLimits().addUntypedPropertyListener(valueListener);
        model_widget.propLevelLoLo().addUntypedPropertyListener(valueListener);
        model_widget.propLevelLow().addUntypedPropertyListener(valueListener);
        model_widget.propLevelHigh().addUntypedPropertyListener(valueListener);
        model_widget.propLevelHiHi().addUntypedPropertyListener(valueListener);
        model_widget.propAlarmLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.propLogScale().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);
        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);
        valueChanged(null, null, null);
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

        model_widget.propShowAlarmLimits().removePropertyListener(valueListener);
        model_widget.propLevelLoLo().removePropertyListener(valueListener);
        model_widget.propLevelLow().removePropertyListener(valueListener);
        model_widget.propLevelHigh().removePropertyListener(valueListener);
        model_widget.propLevelHiHi().removePropertyListener(valueListener);
        model_widget.propAlarmLimitsFromPV().removePropertyListener(valueListener);
        model_widget.propLimitsFromPV().removePropertyListener(valueListener);
        model_widget.propMinimum().removePropertyListener(valueListener);
        model_widget.propMaximum().removePropertyListener(valueListener);
        model_widget.propLogScale().removePropertyListener(valueListener);
        model_widget.runtimePropValue().removePropertyListener(valueListener);
        model_widget.propHorizontal().removePropertyListener(orientationChangedListener);
        super.unregisterListeners();
    }

    private void lookChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        dirty_look.mark();
        toolkit.scheduleUpdate(this);
    }

    private void valueChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        final VType vtype = model_widget.runtimePropValue().getValue();

        // --- Min / Max ---
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

        // --- Alarm limit lines ---
        if (model_widget.propShowAlarmLimits().getValue())
        {
            double lolo, lo, hi, hihi;
            if (model_widget.propAlarmLimitsFromPV().getValue())
            {   // Read from PV alarm metadata
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
            {   // Read from widget properties
                lolo = model_widget.propLevelLoLo().getValue();
                lo   = model_widget.propLevelLow().getValue();
                hi   = model_widget.propLevelHigh().getValue();
                hihi = model_widget.propLevelHiHi().getValue();
            }
            tank.setLimits(lolo, lo, hi, hihi);
            tank.setLimitsFromPV(model_widget.propAlarmLimitsFromPV().getValue());
        }
        else
            tank.setLimits(Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        double value;
        if (toolkit.isEditMode())
            value = (min_val + max_val) / 2;
        else
            value = VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);

        tank.setLogScale(model_widget.propLogScale().getValue());
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
            tank.setLabelFormat(model_widget.propFormat().getValue(),
                                model_widget.propPrecision().getValue());
            tank.setAlarmColors(
                JFXUtil.convert(model_widget.propMinorAlarmColor().getValue()),
                JFXUtil.convert(model_widget.propMajorAlarmColor().getValue()));
            tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
        }
    }
}
