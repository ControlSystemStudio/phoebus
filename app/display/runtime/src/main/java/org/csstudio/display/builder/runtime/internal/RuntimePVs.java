/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.internal;

import static org.csstudio.display.builder.model.properties.CommonWidgetProperties.runtimePropPVWritable;
import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.csstudio.display.builder.model.Widget;
import org.csstudio.display.builder.model.WidgetProperty;
import org.csstudio.display.builder.model.widgets.VisibleWidget;
import org.csstudio.display.builder.runtime.pv.RuntimePV;
import org.csstudio.display.builder.runtime.pv.RuntimePVListener;
import org.epics.vtype.VType;

/** Tracker for all PVs used by a widget.
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RuntimePVs
{
    // Similar to org.csstudio.opibuilder.editparts.ConnectionHandler by Xihui Chen
    private final Widget widget;

    /** PVs and their connection info
     *
     *  <p>PVs are tracked by PV instance.
     *  A widget may use the same PV name in the primary widget PV
     *  and then again as a script PV.
     *  The PV pool will provide the same PV instance,
     *  and the PVInfo keeps a reference count to only
     *  track the PV once.
     */
    private final ConcurrentMap<RuntimePV, PVInfo> pvs = new ConcurrentHashMap<>();

    /** Listener for tracking connection state of individual PV.
     *  Can optionally also check for write access.
     *  Reference counted because widget may use the same PV
     *  multiple times, but connection state is only tracked once.
     */
    private class PVInfo implements RuntimePVListener
    {
        private final AtomicInteger refs = new AtomicInteger();
        private volatile boolean connected = false;
        private volatile boolean need_write_access = false;

        public int addReference()
        {
            return refs.incrementAndGet();
        }

        public int removeReference()
        {
            return refs.decrementAndGet();
        }

        public void trackWriteAccess()
        {
            need_write_access = true;
        }

        public boolean needWriteAccess()
        {
            return need_write_access;
        }

        public boolean isConnected()
        {
            return connected;
        }

        @Override
        public void permissionsChanged(final RuntimePV pv, final boolean readonly)
        {
            if (need_write_access)
                updateWriteAccess();
        }

        @Override
        public void valueChanged(final RuntimePV pv, final VType value)
        {
            if (connected)
                return;
            connected = true;
            updateConnections(true);
        }

        @Override
        public void disconnected(final RuntimePV pv)
        {
            connected = false;
            updateConnections(false);
        }
    }

    /** @param widget PVs of this widget will be tracked */
    public RuntimePVs(final Widget widget)
    {
        this.widget = widget;
    }

    /** @param pv PV to track
     *  @param need_write_access Does widget need write access to this PV?
     */
    public void addPV(final RuntimePV pv, final boolean need_write_access)
    {
        final PVInfo info = pvs.computeIfAbsent(pv, (p) -> new PVInfo());
        if (info.addReference() == 1)
        {
            // Awaiting connections for at least one PV, so widget is for now disconnected
            if (widget instanceof VisibleWidget)
                ((VisibleWidget)widget).runtimePropConnected().setValue(false);
            pv.addListener(info);
        }
        if (need_write_access)
        {
            info.trackWriteAccess();
            // Initial update
            info.permissionsChanged(pv, pv.isReadonly());
        }
    }

    /** @param pv PV to no longer track */
    public void removePV(final RuntimePV pv)
    {
        final PVInfo info = pvs.get(pv);
        if (info == null)
            throw new IllegalStateException("Unknown PV " + pv);
        if (info.removeReference() == 0)
        {
            pvs.remove(pv, info);
            pv.removeListener(info);
        }
    }

    /** Update overall connection state of the widget
     *  @param pv_connected <code>true</code> if one PV just connected, <code>false</code> if one PV disconnected
     */
    private void updateConnections(final boolean pv_connected)
    {
        boolean all_connected = pv_connected;
        if (all_connected)
        {   // Are _all_ PVs connected?
            // If this code turns into a bottleneck, could
            // optimize by simply counting connections +1/-1,
            // but full search of all PVs is less error-prone.
            for (PVInfo info : pvs.values())
                if (! info.isConnected())
                {
                    all_connected = false;
                    break;
                }
        }
        // else: For sure not connected

        if (widget instanceof VisibleWidget)
            ((VisibleWidget)widget).runtimePropConnected().setValue(all_connected);
    }

    /** Update write access indication of the widget */
    private void updateWriteAccess()
    {
        // Widget may have actions that want to write,
        // but no property to track pv_writable..
        final Optional<WidgetProperty<Boolean>> prop = widget.checkProperty(runtimePropPVWritable);
        if (!prop.isPresent())
            return;

        int need_to_write = 0, can_write = 0;
        for (Map.Entry<RuntimePV, PVInfo> entry : pvs.entrySet())
            if (entry.getValue().needWriteAccess())
            {
                ++need_to_write;
                if (! entry.getKey().isReadonly())
                    ++can_write;
            }
        logger.log(Level.FINE, "{0} can write {1} out of {2} PVs", new Object[] { widget, can_write, need_to_write });
        final boolean writable = need_to_write == 0  ||  can_write > 0;
        prop.get().setValue(writable);
    }

    /** @return All PVs of this widget */
    public Collection<RuntimePV> getPVs()
    {   // Create safe copy
        return new ArrayList<>(pvs.keySet());
    }

    /** @return Disconnected PVs of this widget */
    public Collection<RuntimePV> getDisconnectedPVs()
    {
        final List<RuntimePV> disconnected = new ArrayList<>();
        for (Map.Entry<RuntimePV, PVInfo> entry : pvs.entrySet())
            if (! entry.getValue().isConnected())
                disconnected.add(entry.getKey());
        return disconnected;
    }
}
