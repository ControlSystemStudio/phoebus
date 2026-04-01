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
import org.csstudio.display.builder.model.widgets.ProgressBarWidget;
import org.csstudio.display.builder.representation.Preferences;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.RTTank;
import org.epics.util.stats.Range;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import javafx.scene.layout.Pane;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/** Creates JavaFX item for model widget.
 *
 *  <p>Uses {@link RTTank} as the rendering engine so the bar
 *  gains a numeric scale, configurable tick format and precision,
 *  optional second scale, and alarm limit lines — all at no extra
 *  maintenance cost versus the plain JavaFX {@code ProgressBar}.
 *
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 *  @author Heredie Delvalle &mdash; CLS, RTTank-based refactoring, scale support
 */
@SuppressWarnings("nls")
public class ProgressBarRepresentation extends RegionBaseRepresentation<Pane, ProgressBarWidget>
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
        model_widget.propFillColor().addUntypedPropertyListener(lookListener);
        model_widget.propBackgroundColor().addUntypedPropertyListener(lookListener);
        model_widget.propScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propShowMinorTicks().addUntypedPropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().addUntypedPropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().addUntypedPropertyListener(lookListener);
        model_widget.propBorderWidth().addUntypedPropertyListener(lookListener);
        model_widget.propLogScale().addUntypedPropertyListener(lookListener);
        model_widget.propFormat().addUntypedPropertyListener(lookListener);
        model_widget.propPrecision().addUntypedPropertyListener(lookListener);
        model_widget.propMinorAlarmColor().addUntypedPropertyListener(lookListener);
        model_widget.propMajorAlarmColor().addUntypedPropertyListener(lookListener);

        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);

        model_widget.propShowAlarmLimits().addUntypedPropertyListener(limitsListener);
        model_widget.propAlarmLimitsFromPV().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLoLo().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelLow().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHigh().addUntypedPropertyListener(limitsListener);
        model_widget.propLevelHiHi().addUntypedPropertyListener(limitsListener);

        model_widget.propHorizontal().addPropertyListener(orientationChangedListener);

        // Initial apply — range and fill first, then limits
        valueChanged(null, null, null);
        limitsChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propFont().removePropertyListener(lookListener);
        model_widget.propFillColor().removePropertyListener(lookListener);
        model_widget.propBackgroundColor().removePropertyListener(lookListener);
        model_widget.propScaleVisible().removePropertyListener(lookListener);
        model_widget.propShowMinorTicks().removePropertyListener(lookListener);
        model_widget.propOppositeScaleVisible().removePropertyListener(lookListener);
        model_widget.propPerpendicularTickLabels().removePropertyListener(lookListener);
        model_widget.propBorderWidth().removePropertyListener(lookListener);
        model_widget.propLogScale().removePropertyListener(lookListener);
        model_widget.propFormat().removePropertyListener(lookListener);
        model_widget.propPrecision().removePropertyListener(lookListener);
        model_widget.propMinorAlarmColor().removePropertyListener(lookListener);
        model_widget.propMajorAlarmColor().removePropertyListener(lookListener);

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

    private void orientationChanged(final WidgetProperty<Boolean> prop, final Boolean old, final Boolean horizontal)
    {
        // When the user changes orientation in the editor, swap width ↔ height
        // so the widget visually rotates rather than stretching.
        if (toolkit.isEditMode())
        {
            final int w = model_widget.propWidth().getValue();
            final int h = model_widget.propHeight().getValue();
            model_widget.propWidth().setValue(h);
            model_widget.propHeight().setValue(w);
        }
        lookChanged(prop, old, horizontal);
    }

    /** Update the display range and fill level.  Called on every PV value change. */
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

        // Re-read alarm limits from PV metadata on every value update.
        if (model_widget.propAlarmLimitsFromPV().getValue())
            applyAlarmLimits(vtype);

        final double value = toolkit.isEditMode()
            ? (min_val + max_val) / 2
            : VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);
    }

    /** Re-apply alarm limit lines when a limit property changes. */
    private void limitsChanged(final WidgetProperty<?> property, final Object old_value, final Object new_value)
    {
        applyAlarmLimits(model_widget.runtimePropValue().getValue());
    }

    /** Push the current alarm limits to the tank from PV metadata or widget properties. */
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

    /** Track whether orientation transforms are currently applied. */
    private boolean was_transformed = false;

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            double width  = model_widget.propWidth().getValue();
            double height = model_widget.propHeight().getValue();

            // A horizontal bar is rendered by RTTank as if vertical (RTTank is
            // always vertical internally) and then rotated 90° clockwise.
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
                was_transformed = false;
                tank.setWidth(width);
                tank.setHeight(height);
            }
            jfx_node.setPrefSize(width, height);

            tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            // Background is the outer canvas margin; emptyColor is the unfilled bar portion.
            // Map both to background_color so the whole widget has a uniform background.
            final javafx.scene.paint.Color bg = JFXUtil.convert(model_widget.propBackgroundColor().getValue());
            tank.setBackground(bg);
            tank.setEmptyColor(bg);
            tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
            tank.setScaleVisible(model_widget.propScaleVisible().getValue());
            tank.setShowMinorTicks(model_widget.propShowMinorTicks().getValue());
            tank.setRightScaleVisible(model_widget.propOppositeScaleVisible().getValue());
            tank.setPerpendicularTickLabels(model_widget.propPerpendicularTickLabels().getValue());
            tank.setBorderWidth(model_widget.propBorderWidth().getValue());
            tank.setLogScale(model_widget.propLogScale().getValue());
            tank.setLabelFormat(model_widget.propFormat().getValue(),
                                model_widget.propPrecision().getValue());
            tank.setAlarmColors(
                    JFXUtil.convert(model_widget.propMinorAlarmColor().getValue()),
                    JFXUtil.convert(model_widget.propMajorAlarmColor().getValue()));
        }
    }
}
