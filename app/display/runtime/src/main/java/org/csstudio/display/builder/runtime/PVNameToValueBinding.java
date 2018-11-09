/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

/** Bind a PV 'name' property to a 'value' property
 *
 *  <p>Connects PV for the 'name', then writes
 *  received values to the 'value' property.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVNameToValueBinding
{
    private final WidgetRuntime<?> runtime;
    private final WidgetProperty<String> name;
    private final RuntimePVListener listener;
    private final AtomicReference<RuntimePV> pv_ref = new AtomicReference<>();
    private final boolean need_write_access;

    private final WidgetPropertyListener<String> name_property_listener = (property, old_value, new_value) ->
    {
        // PV name changed: Disconnect existing PV
        disconnect();
        // and connect to new PV
        connect();
        // (even if old_value == new_value)
    };

    /** @param runtime {@link WidgetRuntime}
     *  @param name Property with name of PV
     *  @param value Property to which the value of the PV will be written
     */
    public PVNameToValueBinding(final WidgetRuntime<?> runtime, final WidgetProperty<String> name, final WidgetProperty<VType> value)
    {
        this(runtime, name, value, false);
    }

    /** @param runtime {@link WidgetRuntime}
     *  @param name Property with name of PV
     *  @param value Property to which the value of the PV will be written
     *  @param need_write_access Does the PV need write access?
     */
    public PVNameToValueBinding(final WidgetRuntime<?> runtime, final WidgetProperty<String> name, final WidgetProperty<VType> value, final boolean need_write_access)
    {
        this.runtime = runtime;
        this.name = name;
        this.listener = new PropertyUpdater(value);
        this.need_write_access = need_write_access;

        // Fetching the PV name will resolve macros,
        // i.e. set the name property and thus notify listeners
        // -> Do that once in 'connect()' before registering listener
        connect();
        name.addPropertyListener(name_property_listener);
    }

    /** @return PV or <code>null</code> */
    public RuntimePV getPV()
    {
        return pv_ref.get();
    }

    private void connect()
    {
        final String pv_name = name.getValue();
        if (pv_name.isEmpty())
        {
            listener.valueChanged(null, PVWidget.RUNTIME_VALUE_NO_PV);
            return;
        }
        logger.log(Level.FINE,  "Connecting {0} {1}", new Object[] { name.getWidget(), name });
        final RuntimePV pv;
        try
        {
            pv = PVFactory.getPV(pv_name);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot connect to PV " + pv_name, ex);
            return;
        }
        pv.addListener(listener);
        runtime.addPV(pv, need_write_access);
        pv_ref.set(pv);
    }

    private void disconnect()
    {
        final RuntimePV pv = pv_ref.getAndSet(null);
        if (pv == null)
            return;
        pv.removeListener(listener);
        PVFactory.releasePV(pv);
        runtime.removePV(pv);
    }

    public void dispose()
    {
        name.removePropertyListener(name_property_listener);
        disconnect();
    }
}
