/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.WidgetPropertyListener;
import org.csstudio.display.builder.model.util.VTypeUtil;
import org.csstudio.display.builder.model.widgets.PVWidget;
import org.csstudio.display.builder.runtime.WidgetRuntime;
import org.csstudio.display.builder.runtime.pv.PVFactory;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.util.array.CircularBufferDouble;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VType;

/** Bind PV name to a circular buffer
 *
 *  .. and value of that buffer is then placed
 *  in a runtime widget 'value' property
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
class PVNameToCircularBufferBinding
{
    private final WidgetRuntime<?> runtime;
    private final WidgetProperty<String> name;
    private final AtomicReference<RuntimePV> pv_ref = new AtomicReference<>();
    private final RuntimePVListener listener;
    private final CircularBufferDouble buffer;

    private final WidgetPropertyListener<String> name_property_listener = (property, old_value, new_value) ->
    {
        // PV name changed: Disconnect existing PV
        disconnect();
        // and connect to new PV
        connect();
        // (even if old_value == new_value)
    };

    private class CircularBufferAppender implements RuntimePVListener
    {
        private final WidgetProperty<VType> property;

        CircularBufferAppender(final WidgetProperty<VType> property)
        {
            this.property = property;
            // Send initial 'disconnected' update so widget shows
            // disconnected state until the first value arrives
            disconnected(null);
        }

        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            double number = VTypeUtil.getValueNumber(value).doubleValue();
            buffer.addDouble(number);
            property.setValue(VDoubleArray.of(buffer, Alarm.none(), Time.now(), Display.none()));
        }

        @Override
        public void disconnected(final RuntimePV pv)
        {
            property.setValue(null);
        }
    }

    public PVNameToCircularBufferBinding(final WidgetRuntime<?> runtime, final WidgetProperty<String> name, final WidgetProperty<VType> value)
    {
        this.runtime = runtime;
        this.name = name;
        this.buffer = new CircularBufferDouble(10); // TODO Capacity

        // TODO
        this.listener = new CircularBufferAppender(value);

        // Fetching the PV name will resolve macros,
        // i.e. set the name property and thus notify listeners
        // -> Do that once in 'connect()' before registering listener
        connect();
        name.addPropertyListener(name_property_listener);
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
        runtime.addPV(pv);
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
