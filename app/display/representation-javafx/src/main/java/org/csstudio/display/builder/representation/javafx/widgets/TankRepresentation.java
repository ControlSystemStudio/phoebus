/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.representation.javafx.widgets;

import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.model.DirtyFlag;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.TankWidget;
import org.csstudio.display.builder.representation.RepresentationUpdateThrottle;
import org.csstudio.display.builder.representation.javafx.JFXUtil;
import org.csstudio.javafx.rtplot.RTTank;
import org.epics.vtype.Display;
import org.epics.vtype.VType;

import javafx.scene.layout.Pane;

/** Creates JavaFX item for model widget
 *  @author Kay Kasemir
 */
public class TankRepresentation extends RegionBaseRepresentation<Pane, TankWidget>
{
    private final DirtyFlag dirty_look = new DirtyFlag();

    private volatile RTTank tank;

    @Override
    public Pane createJFXNode() throws Exception
    {
        tank = new RTTank();
        tank.setUpdateThrottle(RepresentationUpdateThrottle.plot_update_delay, TimeUnit.MILLISECONDS);
        return new Pane(tank);
    }

    @Override
    protected void registerListeners()
    {
        super.registerListeners();
        model_widget.propWidth().addUntypedPropertyListener(this::lookChanged);
        model_widget.propHeight().addUntypedPropertyListener(this::lookChanged);
        model_widget.propFont().addUntypedPropertyListener(this::lookChanged);
        model_widget.propForeground().addUntypedPropertyListener(this::lookChanged);
        model_widget.propBackground().addUntypedPropertyListener(this::lookChanged);
        model_widget.propFillColor().addUntypedPropertyListener(this::lookChanged);
        model_widget.propEmptyColor().addUntypedPropertyListener(this::lookChanged);

        model_widget.propLimitsFromPV().addUntypedPropertyListener(this::valueChanged);
        model_widget.propMinimum().addUntypedPropertyListener(this::valueChanged);
        model_widget.propMaximum().addUntypedPropertyListener(this::valueChanged);
        model_widget.runtimePropValue().addUntypedPropertyListener(this::valueChanged);
        valueChanged(null, null, null);
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
        if (limits_from_pv)
        {
            // Try display range from PV
            final org.epics.vtype.Display display_info = Display.displayOf(vtype);
            if (display_info != null)
            {
                min_val = display_info.getDisplayRange().getMinimum();
                max_val = display_info.getDisplayRange().getMaximum();
            }
        }
        tank.setRange(min_val, max_val);

        double value;
        if (toolkit.isEditMode())
            value = (min_val + max_val) / 2;
        else
            value = VTypeUtil.getValueNumber(vtype).doubleValue();
        tank.setValue(value);
    }

    @Override
    public void updateChanges()
    {
        super.updateChanges();
        if (dirty_look.checkAndClear())
        {
            double width = model_widget.propWidth().getValue();
            double height = model_widget.propHeight().getValue();
            jfx_node.setPrefSize(width, height);
            tank.setWidth(width);
            tank.setHeight(height);
            tank.setFont(JFXUtil.convert(model_widget.propFont().getValue()));
            tank.setBackground(JFXUtil.convert(model_widget.propBackground().getValue()));
            tank.setForeground(JFXUtil.convert(model_widget.propForeground().getValue()));
            tank.setFillColor(JFXUtil.convert(model_widget.propFillColor().getValue()));
            tank.setEmptyColor(JFXUtil.convert(model_widget.propEmptyColor().getValue()));
        }
    }
}
