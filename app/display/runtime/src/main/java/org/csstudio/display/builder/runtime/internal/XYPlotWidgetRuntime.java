/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.plots.PlotWidgetProperties.TraceWidgetProperty;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget;
import org.csstudio.display.builder.model.widgets.plots.XYPlotWidget.MarkerProperty;
import org.csstudio.display.builder.runtime.PVNameToValueBinding;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

/** Runtime for the XYPlotWidget
 *
 *  <p>Supports changing the PV names for a trace's X, Y, Error PV.
 *
 *  <p>Binds the marker's PVs to their values.
 *
 *  <p>Does not support adding or removing traces,
 *  does not support changing the number of markers or their PVs.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class XYPlotWidgetRuntime  extends WidgetRuntime<XYPlotWidget>
{
    private final List<RuntimeAction> runtime_actions = new ArrayList<>(2);

    private final List<PVNameToValueBinding> bindings = new ArrayList<>();

    private final List<RuntimePV> marker_pvs = new CopyOnWriteArrayList<>();
    private final Map<WidgetProperty<?>, WidgetPropertyListener<?>> marker_prop_listeners = new ConcurrentHashMap<>();
    private final Map<RuntimePV, RuntimePVListener> marker_pv_listeners = new ConcurrentHashMap<>();

    @Override
    public void initialize(final XYPlotWidget widget)
    {
        super.initialize(widget);
        runtime_actions.add(new ConfigureAction("Configure Plot", widget.runtimePropConfigure()));
        runtime_actions.add(new ToggleToolbarAction(widget));
    }

    @Override
    public Collection<RuntimeAction> getRuntimeActions()
    {
        return runtime_actions;
    }

    @Override
    public void start()
    {
        super.start();

        for (TraceWidgetProperty trace : widget.propTraces().getValue())
        {
            bindings.add(new PVNameToValueBinding(this, trace.traceXPV(), trace.traceXValue()));
            bindings.add(new PVNameToValueBinding(this, trace.traceYPV(), trace.traceYValue()));
            bindings.add(new PVNameToValueBinding(this, trace.traceErrorPV(), trace.traceErrorValue()));
        }

        for (MarkerProperty marker : widget.propMarkers().getValue())
            bindMarker(marker.pv(), marker.value());
    }

    private void bindMarker(final WidgetProperty<String> name_prop, final WidgetProperty<Double> value_prop)
    {
        final String pv_name = name_prop.getValue();
        if (pv_name.isEmpty())
            return;

        logger.log(Level.FINER, "Connecting {0} to Marker PV {1}",  new Object[] { widget, pv_name });
        try
        {
            final RuntimePV pv = PVFactory.getPV(pv_name);
            addPV(pv);
            marker_pvs.add(pv);

            // Write value changes to the PV
            final WidgetPropertyListener<Double> prop_listener = (prop, old, value) ->
            {
                // Ignore if PV already has same value to break update loops
                double pv_value = VTypeUtil.getValueNumber(pv.read()).doubleValue();
                if (value == pv_value)
                    return;
                try
                {
                    // System.out.println("Writing " + value_prop + " to PV " + pv_name);
                    pv.write(value);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Error writing marker value to PV " + pv_name, ex);
                    // Restore property to the unchanged value of the PV
                    value_prop.setValue(pv_value);
                }
            };
            value_prop.addPropertyListener(prop_listener);
            marker_prop_listeners.put(value_prop, prop_listener);

            // Write PV updates to the value
            final RuntimePVListener pv_listener = new RuntimePVListener()
            {
                @Override
                public void valueChanged(final RuntimePV pv, final VType value)
                {
                    final double number = VTypeUtil.getValueNumber(value).doubleValue();
                    if (number == value_prop.getValue())
                        return;
                    // System.out.println("Writing " + number + " from PV " + pv_name + " to " + value_prop);
                    value_prop.setValue(number);
                }
            };
            pv.addListener(pv_listener);
            marker_pv_listeners.put(pv, pv_listener);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error connecting Marker PV " + pv_name, ex);
        }
    }

    @Override
    public void stop()
    {
        // Disconnect Marker PVs and listeners
        for (Map.Entry<WidgetProperty<?>, WidgetPropertyListener<?>> entry : marker_prop_listeners.entrySet())
            entry.getKey().removePropertyListener(entry.getValue());
        marker_prop_listeners.clear();

        for (Map.Entry<RuntimePV, RuntimePVListener> entry : marker_pv_listeners.entrySet())
            entry.getKey().removeListener(entry.getValue());
        marker_pv_listeners.clear();

        for (RuntimePV pv : marker_pvs)
        {
            removePV(pv);
            PVFactory.releasePV(pv);
        }
        marker_pvs.clear();

        for (PVNameToValueBinding binding : bindings)
            binding.dispose();
        super.stop();
    }
}
