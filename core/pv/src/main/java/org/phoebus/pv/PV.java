/*******************************************************************************
 * Copyright (c) 2017-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.phoebus.pv;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.phoebus.vtype.VType;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

/** Process Variable, API for accessing life control system data.
 *
 *  <p>PVs are to be fetched from the {@link PVPool}
 *  and released to it when no longer used.
 *
 *  <p>The name of the PV is the name by which it was created.
 *  The underlying implementation might use a slightly different name.
 *
 *  @author Eric Berryman
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
abstract public class PV
{
    /** Suggested logger for all vtype.pv packages */
    public static final Logger logger = Logger.getLogger(PV.class.getName());

    /** Alarm message used with UNDEFINED severity for disconnected state */
    public static final String DISCONNECTED = "Disconnected";

    final private String name;

    final private List<PVListener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean is_readonly = false;

    private volatile VType last_value = null;

    /** Initialize
     *  @param name PV name
     */
    protected PV(final String name)
    {
        this.name = name;
    }

    /** @return PV name */
    public String getName()
    {
        return name;
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
     *  @see #removeListener(PVListener)
     */
    @Deprecated
    public void addListener(final PVListener listener)
    {
        // If there is a known value, perform initial update
        final VType value = last_value;
        if (value != null)
            listener.valueChanged(value);
        listeners.add(listener);
    }

    /** @param listener Listener that will no longer receive value updates */
    @Deprecated
    public void removeListener(final PVListener listener)
    {
        listeners.remove(listener);
    }

    /** @return {@link Flowable} that receives {@link VType} for each updated value of the PV */
    public Flowable<VType> onValueEvent()
    {
        return onValueEvent(BackpressureStrategy.LATEST);
    }

    /** @param mode {@link BackpressureStrategy}
     *  @return {@link Flowable} that receives {@link VType} for each updated value of the PV
     */
    public Flowable<VType> onValueEvent(final BackpressureStrategy mode)
    {
        return Flowable.create(new ValueEventOnSubscribe(this), mode);
    }

    /** @return {@link Flowable} that receives <code>true</code>/<code>false</code> to indicate write access */
    public Flowable<Boolean> onAccessRightsEvent()
    {
        return Flowable.create(new AccessRightsEventOnSubscribe(this), BackpressureStrategy.LATEST);
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
        return last_value;
    }

    /** Issue a read request
     *
     *  <p>{@link Future} allows waiting for
     *  and obtaining the result, or its <code>get()</code>
     *  calls will provide an error.
     *
     *  <p>As a side effect, registered listeners will
     *  also receive the value obtained by this call.
     *
     *  @return {@link Future} for obtaining the result or Exception
     *  @exception Exception on error
     */
    public Future<VType> asyncRead() throws Exception
    {
        // Default: Return last known value
        return CompletableFuture.completedFuture(last_value);
    }

    /** @return <code>true</code> if PV is read-only */
    public boolean isReadonly()
    {
        return is_readonly;
    }

    /** Write value, no confirmation
     *  @param new_value Value to write to the PV
     *  @see PV#write(Object, PVWriteListener)
     *  @exception Exception on error
     */
    abstract public void write(final Object new_value) throws Exception;

    /** Write value with confirmation
     *
     *  <p>{@link Future} can be used to await completion
     *  of the write.
     *  The <code>get()</code> will not return a useful value (null),
     *  but they will throw an error if the write failed.
     *
     *  @param new_value Value to write to the PV
     *  @return {@link Future} for awaiting completion or exception
     *  @exception Exception on error
     */
    public Future<?> asyncWrite(final Object new_value) throws Exception
    {   // Default: Normal write, declare 'done' right away
        write(new_value);
        return CompletableFuture.completedFuture(null);
    }

    /** Helper for PV implementation to notify listeners */
    protected void notifyListenersOfDisconnect()
    {
        last_value = null;
        for (PVListener listener : listeners)
        {
            try
            {
                listener.disconnected();
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, name + " PVListener error", ex);
            }
        }
    }

    /** Helper for PV implementation to notify listeners */
    protected void notifyListenersOfPermissions(final boolean readonly)
    {
        is_readonly = readonly;
        for (PVListener listener : listeners)
        {
            try
            {
                listener.permissionsChanged(readonly);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, name + " PVListener error", ex);
            }
        }
    }

    /** Helper for PV implementation to notify listeners */
    protected void notifyListenersOfValue(final VType value)
    {
        last_value = value;
        for (PVListener listener : listeners)
        {
            try
            {
                listener.valueChanged(value);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, name + " PVListener error", ex);
            }
        }
    }

    /** Close the PV, releasing underlying resources.
     *  <p>
     *  Called by {@link PVPool}.
     *  Users of this class should instead release PV from pool.
     *
     *  @see PVPool#releasePV(PV)
     */
    protected void close()
    {
        // Default implementation has nothing to close
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return getClass().getSimpleName() + " '" + getName() + "' = " + last_value;
    }
}
