/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.csstudio.display.builder.runtime.Preferences;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import io.reactivex.disposables.Disposable;

/** Process Variable, API for accessing life control system data.
 *
 *  <p>PVs are to be fetched from the {@link PVPool}
 *  and release to it when no longer used.
 *
 *  <p>The name of the PV is the name by which it was created.
 *  The underlying implementation might use a slightly different name.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class RuntimePV // TODO (Almost) remove. Use vtype.pv, only add setValue for script compatibility?
{
    private final PV pv;
    private final Disposable value_flow, writable_flow;
    private final List<RuntimePVListener> listeners = new CopyOnWriteArrayList<>();

    /** @param pv PV to wrap */
    RuntimePV(final PV pv)
    {
        this.pv = pv;
        value_flow = pv.onValueEvent()
                .throttleLatest(Preferences.update_throttle_ms, TimeUnit.MILLISECONDS)
                .subscribe(this::valueChanged);
        writable_flow = pv.onAccessRightsEvent()
                          .subscribe(this::writableChanged);
    }

    /** @return PV name */
    public String getName()
    {
        return pv.getName();
    }

    /** Request notifications of PV updates.
     *
     *  <p>Note that the PV is shared via the {@link PVPool}.
     *  When updates are no longer desired, caller must
     *  <code>removeListener()</code>.
     *  Simply releasing the PV back to the {@link PVPool}
     *  will <b>not</b> automatically remove listeners!
     *
     *  @param listener Listener that will receive value updates
     *  @see #removeListener(RuntimePVListener)
     */
    public void addListener(final RuntimePVListener listener)
    {
        // If there is a known value, perform initial update
        final VType value = pv.read();
        if (value != null)
            listener.valueChanged(this, value);
        listeners.add(listener);
    }

    /** @param listener Listener that will no longer receive value updates */
    public void removeListener(final RuntimePVListener listener)
    {
        listeners.remove(listener);
    }

    /** Read current value
     *
     *  <p>Should return the most recent value
     *  that listeners have received.
     *
     *  @return Most recent value of the PV. <code>null</code> if no known value.
     */
    public VType read()
    {
        return pv.read();
    }

    /** @return <code>true</code> if PV is read-only */
    public boolean isReadonly()
    {
        return pv.isReadonly();
    }

    /** Write value, no confirmation
     *  @param new_value Value to write to the PV
     *  @exception Exception on error
     */
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

    private void valueChanged(final VType value)
    {
        if (PV.isDisconnected(value))
            for (RuntimePVListener listener : listeners)
                listener.disconnected(this);
        else
            for (RuntimePVListener listener : listeners)
                listener.valueChanged(this, value);
    }

    private void writableChanged(final boolean writable)
    {
        for (RuntimePVListener listener : listeners)
            listener.permissionsChanged(this, !writable);
    }

    /** Has setValue() issued warning about being called? */
    static volatile boolean issued_write_warning = false;

    /** Legacy API that was accessed by some scripts
     *  @param new_value Value to write to the PV
     *  @throws Exception If the new value cannot be set.
     *  @deprecated Use {@link #write(Object)} instead.
     *  @see #write(Object)
     */
    @Deprecated
    public void setValue(final Object new_value) throws Exception
    {
        if (! issued_write_warning)
        {
            issued_write_warning = true;
            // Called quite often for legacy displays, and display still works,
            // so don't log as WARNING
            logger.log(Level.INFO,
                    "Script calls 'setValue(" + new_value +") for PV '" + getName() +
                    "'. Update to 'write'");
        }
        write(new_value);
    }

    public PV getPV()
    {
        return pv;
    }

    void close()
    {
        writable_flow.dispose();
        value_flow.dispose();
        PVPool.releasePV(pv);
    }

    // Should provide PV name in toString() for debug messages that include the PV
    @Override
    public String toString()
    {
        return getName();
    }
}
