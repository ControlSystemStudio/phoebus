/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.UntypedWidgetPropertyListener;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.persist.NamedWidgetColors;
import org.csstudio.display.builder.model.persist.WidgetColorService;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.MeterWidget;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.RTMeter;
import org.epics.util.stats.Range;
import org.epics.vtype.Display;
import org.epics.vtype.VType;
import org.phoebus.ui.vtype.FormatOptionHandler;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class MeterRepresentation extends RegionBaseRepresentation<Pane, MeterWidget>
{
    private final DirtyFlag dirty_look = new DirtyFlag();
    private final UntypedWidgetPropertyListener lookListener = this::lookChanged;
    private final UntypedWidgetPropertyListener valueListener = this::valueChanged;

    private volatile RTMeter meter;

    @Override
    public Pane createJFXNode() throws Exception
    {
        meter = new RTMeter();
        meter.setLimitColors(JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MINOR)),
                             JFXUtil.convert(WidgetColorService.getColor(NamedWidgetColors.ALARM_MAJOR)));


        meter.setManaged(false);
        // Wrapper pane (managed) to get alarm-sensitive border
        return new Pane(meter);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(lookListener);
        model_widget.propHeight().addUntypedPropertyListener(lookListener);
        model_widget.propForeground().addUntypedPropertyListener(lookListener);
        model_widget.propBackground().addUntypedPropertyListener(lookListener);
        model_widget.propFont().addUntypedPropertyListener(lookListener);
        model_widget.propNeedleColor().addUntypedPropertyListener(lookListener);
        model_widget.propKnobColor().addUntypedPropertyListener(lookListener);
        model_widget.propLimitsFromPV().addUntypedPropertyListener(valueListener);
        model_widget.propMinimum().addUntypedPropertyListener(valueListener);
        model_widget.propMaximum().addUntypedPropertyListener(valueListener);
        model_widget.propShowValue().addUntypedPropertyListener(valueListener);
        model_widget.runtimePropValue().addUntypedPropertyListener(valueListener);
        valueChanged(null, null, null);
    }

    @Override
    protected void unregisterListeners()
    {
        model_widget.propWidth().removePropertyListener(lookListener);
        model_widget.propHeight().removePropertyListener(lookListener);
        model_widget.propForeground().removePropertyListener(lookListener);
        model_widget.propBackground().removePropertyListener(lookListener);
        model_widget.propFont().removePropertyListener(lookListener);
        model_widget.propNeedleColor().removePropertyListener(lookListener);
        model_widget.propKnobColor().removePropertyListener(lookListener);
        model_widget.propLimitsFromPV().removePropertyListener(valueListener);
        model_widget.propMinimum().removePropertyListener(valueListener);
        model_widget.propMaximum().removePropertyListener(valueListener);
        model_widget.propShowValue().removePropertyListener(valueListener);
        model_widget.runtimePropValue().removePropertyListener(valueListener);
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

        final boolean limits_from_pv = model_widget.propLimitsFromPV().getValue();
        double min_val = model_widget.propMinimum().getValue();
        double max_val = model_widget.propMaximum().getValue();
        final org.epics.vtype.Display display_info = Display.displayOf(vtype);
        if (limits_from_pv  &&  !toolkit.isEditMode())
        {
            // Try display range from PV
            if (display_info != null)
            {
                min_val = display_info.getDisplayRange().getMinimum();
                max_val = display_info.getDisplayRange().getMaximum();
            }
        }
        meter.setRange(min_val, max_val);

        double value;
        if (toolkit.isEditMode())
            value = (min_val + max_val) / 2;
        else
            value = VTypeUtil.getValueNumber(vtype).doubleValue();

        if (model_widget.propShowLimits().getValue()  &&  display_info != null)
        {
            final Range minor = display_info.getWarningRange();
            final Range major = display_info.getAlarmRange();
            meter.setLimits(major.getMinimum(), minor.getMinimum(), minor.getMaximum(), major.getMaximum());
        }
        else
            meter.setLimits(Double.NaN, Double.NaN, Double.NaN, Double.NaN);

        if (model_widget.propShowValue().getValue())
        {
            final String text = FormatOptionHandler.format(vtype,
                                       model_widget.propFormat().getValue(),
                                       model_widget.propPrecision().getValue(),
                                       model_widget.propShowUnits().getValue());
            meter.setValue(value, text);
        }
        else
            meter.setValue(value, "");
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            meter.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
            meter.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
            meter.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            meter.setNeedle(JFXUtil.convert(model_widget.propNeedleColor().getValue()));
            meter.setKnob(JFXUtil.convert(model_widget.propKnobColor().getValue()));

            // Setting size triggers a redraw
            double width = model_widget.propWidth().getValue();
            double height = model_widget.propHeight().getValue();
            jfx_node.setPrefSize(width, height);
            meter.setSize(width, height);
        }
    }

    @Override
    public void dispose()
    {
        meter.dispose();
        super.dispose();
    }
}
