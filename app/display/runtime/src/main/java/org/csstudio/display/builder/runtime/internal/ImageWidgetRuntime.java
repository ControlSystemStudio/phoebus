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
import org.csstudio.display.builder.model.widgets.plots.ImageWidget;
import org.csstudio.display.builder.model.widgets.plots.ImageWidget.ROIWidgetProperty;
import org.csstudio.display.builder.runtime.RuntimeAction;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.csstudio.display.builder.runtime.script.PVUtil;
import org.epics.vtype.VType;

/** Runtime for the ImageWidget
 *
 *  <p>Updates 'Cursor Info PV' with location and value at cursor.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ImageWidgetRuntime extends WidgetRuntime<ImageWidget>
{
    private final List<RuntimeAction> runtime_actions = new ArrayList<>(1);

    private volatile RuntimePV cursor_pv = null, x_pv = null, y_pv = null;

    private final List<RuntimePV> roi_pvs = new CopyOnWriteArrayList<>();

    /** Listen to the 'cursor_info' runtime property, update PV */
    private final WidgetPropertyListener<VType> cursor_info_listener = (prop, old, value) ->
    {
        RuntimePV pv = cursor_pv;
        if (pv != null)
        {
            try
            {
                pv.write(value);
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error writing " + value + " to " + pv, ex);
            }
        }
    };

    /** When user moves the mouse, this will create a flurry of writes to the X and Y crosshair PVs.
     *  That's OK for local PVs, but when using PVs on IOC this flurry of values will be
     *  received with a delay, resulting in an infinite re-run of cursor movements when those
     *  are then used to update the crosshair from the PV.
     *
     *  -> Ignore received updates of the X and Y PVs until the received value matches the
     *  current crosshair, or the timeout expired.
     *  ignore_*_updates is set to the system time ms when updates should again be permitted.
     */
    private volatile long ignore_x_updates = 0, ignore_y_updates = 0;

    private static final long IGNORE_MS = 1000;

    /** Listen to the 'crosshair' runtime property, update X, Y PVs */
    private final WidgetPropertyListener<Double[]> crosshair_listener = (prop, old, value) ->
    {
        RuntimePV pv = x_pv;
        if (pv != null)
        {
            try
            {
                final double existing = PVUtil.getDouble(pv);
                final double new_value = value[0];
                if (Double.doubleToLongBits(existing) != Double.doubleToLongBits(new_value))
                {
                    ignore_x_updates = System.currentTimeMillis() + IGNORE_MS;
                    pv.write(new_value);
                }
            }
            catch (Exception ex)
            {   // Couldn't write, so don't wait for a read back of that value
                ignore_x_updates = 0;
                logger.log(Level.WARNING, "Error writing " + value + " to " + pv, ex);
            }
        }

        pv = y_pv;
        if (pv != null)
        {
            try
            {
                final double existing = PVUtil.getDouble(pv);
                final double new_value = value[1];
                if (Double.doubleToLongBits(existing) != Double.doubleToLongBits(new_value))
                {
                    ignore_y_updates = System.currentTimeMillis() + IGNORE_MS;
                    pv.write(new_value);
                }
            }
            catch (Exception ex)
            {
                ignore_y_updates = 0;
                logger.log(Level.WARNING, "Error writing " + value + " to " + pv, ex);
            }
        }
    };

    /** Listen to the X/Y cursor PVs, update the plot's crosshair */
    private final RuntimePVListener cursor_pv_listener = new RuntimePVListener()
    {
        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            final Double[] current = widget.runtimePropCrosshair().getValue();
            final double x = PVUtil.getDouble(x_pv),
                         y = PVUtil.getDouble(y_pv);
            // Ignore NaN
            if (Double.isNaN(x)  ||  Double.isNaN(y))
                return;
            final long now = System.currentTimeMillis();
            if (pv == x_pv)
            {
                if (current != null  &&  x == current[0])
                {   // Caught up with value we wrote out
                    ignore_x_updates = 0;
                    return;
                }
                // Ignore X updates until caught up or waited long enough
                if (now < ignore_x_updates)
                    return;
            }
            if (pv == y_pv)
            {
                if (current != null  &&  y == current[1])
                {
                    ignore_y_updates = 0;
                    return;
                }
                if (now < ignore_y_updates)
                    return;
            }
            widget.runtimePropCrosshair().setValue(new Double[] { x, y });
        }
    };

    private final Map<WidgetProperty<?>, WidgetPropertyListener<?>> roi_prop_listeners = new ConcurrentHashMap<>();
    private final Map<RuntimePV, RuntimePVListener> roi_pv_listeners = new ConcurrentHashMap<>();

    @Override
    public void initialize(final ImageWidget widget)
    {
        super.initialize(widget);
        runtime_actions.add(new ConfigureAction("Configure Image", widget.runtimePropConfigure()));
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

        // Connect cursor related PVs
        cursor_pv = bindPV(widget.propCursorInfoPV().getValue());
        x_pv = bindPV(widget.propCursorXPV().getValue());
        y_pv = bindPV(widget.propCursorYPV().getValue());
        if (cursor_pv != null)
            widget.runtimePropCursorInfo().addPropertyListener(cursor_info_listener);
        if (x_pv != null  ||  y_pv != null)
            widget.runtimePropCrosshair().addPropertyListener(crosshair_listener);

        if (x_pv != null  &&  y_pv != null)
        {
            x_pv.addListener(cursor_pv_listener);
            y_pv.addListener(cursor_pv_listener);
        }
        // Connect ROI PVs
        for (ROIWidgetProperty roi : widget.propROIs().getValue())
        {
            bindROI(roi.x_pv(), roi.x_value());
            bindROI(roi.y_pv(), roi.y_value());
            bindROI(roi.width_pv(), roi.width_value());
            bindROI(roi.height_pv(), roi.height_value());
        }
    }

    /** @param pv_name Name of cursor related PV
     *  @return {@link RuntimePV} or <code>null</code>
     */
    private RuntimePV bindPV(final String pv_name)
    {
        if (! pv_name.isEmpty())
        {
            logger.log(Level.FINER, "Connecting {0} to {1}",  new Object[] { widget, pv_name });
            try
            {
                final RuntimePV pv = PVFactory.getPV(pv_name);
                addPV(pv);
                return pv;
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error connecting PV " + pv_name, ex);
            }
        }
        return null;
    }

    /** Bind an ROI PV to an ROI value
     *  @param name_prop Property for the PV name
     *  @param value_prop Property for the value
     */
    private void bindROI(final WidgetProperty<String> name_prop, final WidgetProperty<Double> value_prop)
    {
        final String pv_name = name_prop.getValue();
        if (pv_name.isEmpty())
            return;

        logger.log(Level.FINER, "Connecting {0} to ROI PV {1}",  new Object[] { widget, pv_name });
        try
        {
            final RuntimePV pv = PVFactory.getPV(pv_name);
            addPV(pv);
            roi_pvs.add(pv);

            // Write value changes to the PV
            final WidgetPropertyListener<Double> prop_listener = (prop, old, value) ->
            {
                try
                {
                    if (value == VTypeUtil.getValueNumber(pv.read()).doubleValue())
                        return;
                    // System.out.println("Writing " + value_prop + " to PV " + pv_name);
                    pv.write(value);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Error writing ROI value to PV " + pv_name, ex);
                }
            };
            value_prop.addPropertyListener(prop_listener);
            roi_prop_listeners.put(value_prop, prop_listener);

            // Write PV updates to the value
            final RuntimePVListener pv_listener = new RuntimePVListener()
            {
                @Override
                public void valueChanged(final RuntimePV pv, final VType value)
                {
                    final double number = VTypeUtil.getValueNumber(value).doubleValue();
                    if (number == value_prop.getValue())
                        return;
                    // System.out.println("Writing from PV " + pv_name + " to " + value_prop);
                    value_prop.setValue(number);
                }
            };
            pv.addListener(pv_listener);
            roi_pv_listeners.put(pv, pv_listener);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error connecting ROI PV " + pv_name, ex);
        }
    }

    @Override
    public void stop()
    {
        // Disconnect ROI PVs and listeners
        for (Map.Entry<WidgetProperty<?>, WidgetPropertyListener<?>> entry : roi_prop_listeners.entrySet())
            entry.getKey().removePropertyListener(entry.getValue());
        roi_prop_listeners.clear();

        for (Map.Entry<RuntimePV, RuntimePVListener> entry : roi_pv_listeners.entrySet())
            entry.getKey().removeListener(entry.getValue());
        roi_pv_listeners.clear();

        for (RuntimePV pv : roi_pvs)
        {
            removePV(pv);
            PVFactory.releasePV(pv);
        }
        roi_pvs.clear();

        // Disconnect cursor info PV
        if (x_pv != null  &&  y_pv != null)
        {
            y_pv.removeListener(cursor_pv_listener);
            x_pv.removeListener(cursor_pv_listener);
        }
        if (unbind(cursor_pv))
            widget.runtimePropCursorInfo().removePropertyListener(cursor_info_listener);
        boolean x_or_y = unbind(y_pv);
        x_or_y |= unbind(x_pv);
        if (x_or_y)
            widget.runtimePropCrosshair().removePropertyListener(crosshair_listener);
        y_pv = x_pv = cursor_pv = null;
        super.stop();
    }

    private boolean unbind(final RuntimePV pv)
    {
        if (pv == null)
            return false;
        removePV(pv);
        PVFactory.releasePV(pv);
        return true;
    }
}
