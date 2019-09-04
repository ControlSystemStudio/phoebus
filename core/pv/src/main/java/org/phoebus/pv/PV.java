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

import org.epics.vtype.Alarm;
import org.epics.vtype.AlarmSeverity;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VTable;
import org.epics.vtype.VType;

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
public class PV
{
    /** Suggested logger for all vtype.pv packages */
    public static final Logger logger = Logger.getLogger(PV.class.getPackage().getName());

    final private String name;

    final private List<ValueEventHandler.Subscription> value_subs = new CopyOnWriteArrayList<>();

    final private List<AccessRightsEventHandler.Subscription> access_subs = new CopyOnWriteArrayList<>();

    /** Is PV read-only?
     *  Derived class typically updates via {@link #notifyListenersOfPermissions}
     */
    private volatile boolean is_readonly = false;

    /** Most recent value?
     *  Derived class typically updates via {@link #notifyListenersOfValue}
     */
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
     *  <code>removeSubscription()</code>.
     *  Simply releasing the PV back to the {@link PVPool}
     *  will <b>not</b> automatically remove listeners!
     *
     *  @param value_sub Listener that will receive value updates
     *  @see #removeSubscription()
     */
    void addSubscription(final ValueEventHandler.Subscription value_sub)
    {
        // If there is a known value, perform initial update
        final VType value = last_value;
        if (value != null)
            value_sub.update(value);
        value_subs.add(value_sub);
    }

    /** @param value_sub Listener that will no longer receive value updates */
    void removeSubscription(final ValueEventHandler.Subscription value_sub)
    {
        value_subs.remove(value_sub);
    }

    /** @param access_sub Listener that will receive permission updates
     *  @see #removeSubscription()
     */
    void addSubscription(final AccessRightsEventHandler.Subscription access_sub)
    {
        // perform initial update
        access_sub.update(isReadonly());
        access_subs.add(access_sub);
    }

    /** @param access_sub Listener that will no longer receive permission updates */
    void removeSubscription(final AccessRightsEventHandler.Subscription access_sub)
    {
        access_subs.remove(access_sub);
    }

    /** Obtain {@link Flowable} for PV's values.
     *
     *  <p>The {@link Flowable} will receive {@link VType} updates
     *  whenever the PV sends a new value.
     *  When the PV disconnects,
     *  the {@link Flowable} will be of {@link AlarmSeverity#UNDEFINED}
     *  with the alarm message set to {@link PV#DISCONNECTED}.
     *
     *  @return {@link Flowable} that receives {@link VType} for each updated value of the PV
     */
    public Flowable<VType> onValueEvent()
    {
        return onValueEvent(BackpressureStrategy.LATEST);
    }

    /** @param mode {@link BackpressureStrategy}
     *  @return {@link Flowable} that receives {@link VType} for each updated value of the PV
     */
    public Flowable<VType> onValueEvent(final BackpressureStrategy mode)
    {
        return Flowable.create(new ValueEventHandler(this), mode);
    }

    /** Obtain {@link Flowable} for PV's write access.
     *
     *  <p>The {@link Flowable} will receive <code>true</code> when the PV permits write access.
     *  When the PV does not allow write access, or the PV becomes disconnected,
     *  <code>false</code> is emitted.
     *
     *  @return {@link Flowable} that receives <code>true</code>/<code>false</code> to indicate write access
     */
    public Flowable<Boolean> onAccessRightsEvent()
    {
        return Flowable.create(new AccessRightsEventHandler(this), BackpressureStrategy.LATEST);
    }

    /** Check if value indicates a disconnected PV
     *
     *  @param value Value received from PV
     *  @return <code>true</code> if PV is disconnected
     */
    public static boolean isDisconnected(final VType value)
    {
        if (value == null)
            return true;

        // VTable does not implement alarm,
        // but receiving a table means we're not disconnected
        if (value instanceof VTable)
            return false;
        final Alarm alarm = Alarm.alarmOf(value);
        return Alarm.disconnected().equals(alarm);
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
    public void write(final Object new_value) throws Exception
    {
        throw new Exception(this + " is read-only");
    }

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

    /** Helper for PV implementation to notify listeners
     *  @param value New value of the PV
     */
    protected void notifyListenersOfValue(final VType value)
    {
        last_value = value;
        for (ValueEventHandler.Subscription sub : value_subs)
        {
            try
            {
                sub.update(value);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, name + " value update error", ex);
            }
        }
    }

    /** Helper for PV implementation to notify listeners */
    protected void notifyListenersOfDisconnect()
    {
        final VType disconnected = VDouble.of(Double.NaN, Alarm.disconnected(), Time.now(), Display.none());
        notifyListenersOfValue(disconnected);
    }

    /** Helper for PV implementation to notify listeners
     *  @param readonly Read-only state of the PV
     */
    protected void notifyListenersOfPermissions(final boolean readonly)
    {
        is_readonly = readonly;
        for (AccessRightsEventHandler.Subscription sub : access_subs)
        {
            try
            {
                sub.update(readonly);
            }
            catch (Throwable ex)
            {
                logger.log(Level.WARNING, name + " permission update error", ex);
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
