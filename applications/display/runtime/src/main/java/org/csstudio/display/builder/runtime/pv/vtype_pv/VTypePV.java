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

import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.vtype.VType;

/** Implements {@link RuntimePV} for {@link PV}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class VTypePV implements RuntimePV, PVListener
{
    private final PV pv;
    private final List<RuntimePVListener> listeners = new CopyOnWriteArrayList<>();

    VTypePV(final PV pv)
    {
        this.pv = pv;
        pv.addListener(this);
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

    @Override
    public void permissionsChanged(final PV pv, final boolean readonly)
    {
        for (RuntimePVListener listener : listeners)
            listener.permissionsChanged(this, readonly);
    }

    @Override
    public void valueChanged(final PV pv, final VType value)
    {
        for (RuntimePVListener listener : listeners)
            listener.valueChanged(this, value);
    }

    @Override
    public void disconnected(final PV pv)
    {
        for (RuntimePVListener listener : listeners)
            listener.disconnected(this);
    }

    PV getPV()
    {
        return pv;
    }

    void close()
    {
        pv.removeListener(this);
    }

    @Override
    public String toString()
    {
        return getName();
    }
}
