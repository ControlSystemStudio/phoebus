/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv.vtype_pv;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.csstudio.display.builder.runtime.Preferences;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.phoebus.pv.PV;
import org.phoebus.vtype.VType;

import io.reactivex.disposables.Disposable;

/** Implements {@link RuntimePV} for {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypePV implements RuntimePV
{
    private final PV pv;
    private final Disposable value_flow, writable_flow;
    private final List<RuntimePVListener> listeners = new CopyOnWriteArrayList<>();

    VTypePV(final PV pv)
    {
        this.pv = pv;
        value_flow = pv.onValueEvent()
                       .throttleLast(Preferences.update_throttle_ms, TimeUnit.MILLISECONDS)
                       .subscribe(this::valueChanged);
        writable_flow = pv.onAccessRightsEvent()
                          .subscribe(this::writableChanged);
    }

    @Override
    public String getName()
    {
        return pv.getName();
    }

    @Override
    public void addListener(final RuntimePVListener listener)
    {
        // If there is a known value, perform initial update
        final VType value = pv.read();
        if (value != null)
            listener.valueChanged(this, value);
        listeners.add(listener);
    }

    @Override
    public void removeListener(final RuntimePVListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public VType read()
    {
        return pv.read();
    }

    @Override
    public boolean isReadonly()
    {
        return pv.isReadonly();
    }

    @Override
    public void write(final Object new_value) throws Exception
    {
        try
        {
            pv.write(new_value);
        }
        catch (Exception ex)
        {
            throw new Exception("Cannot write " + new_value + " to PV " + getName(), ex);
        }
    }

    public void writableChanged(final boolean writable)
    {
        for (RuntimePVListener listener : listeners)
            listener.permissionsChanged(this, !writable);
    }

    public void valueChanged(final VType value)
    {
        if (PV.isDisconnected(value))
            for (RuntimePVListener listener : listeners)
                listener.disconnected(this);
        else
            for (RuntimePVListener listener : listeners)
                listener.valueChanged(this, value);
    }

    PV getPV()
    {
        return pv;
    }

    void close()
    {
        writable_flow.dispose();
        value_flow.dispose();
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
