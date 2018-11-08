/*******************************************************************************
 * Copyright (c) 2015-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime.pv;

import static org.csstudio.display.builder.runtime.WidgetRuntime.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.display.builder.model.util.VTypeUtil;
import org.epics.util.array.ArrayDouble;
import org.epics.util.array.ArrayInteger;
import org.epics.util.array.ListNumber;
import org.epics.vtype.Alarm;
import org.epics.vtype.Display;
import org.epics.vtype.Time;
import org.epics.vtype.VDouble;
import org.epics.vtype.VEnum;
import org.epics.vtype.VEnumArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;

import io.reactivex.disposables.Disposable;

/** Dispatches elements of an array PV to per-element local PVs
 *
 *  <p>Intended use is for the array widget:
 *  Elements of the original array value are sent to separate PVs,
 *  one per array element.
 *  Changing one of the per-element PVs will update the original
 *  array PV.
 *
 *  <p>Treats scalar input PVs as one-element array.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ArrayPVDispatcher implements AutoCloseable
{
    /** Listener interface of the {@link ArrayPVDispatcher} */
    @FunctionalInterface
    public static interface Listener
    {
        /** Notification of new/updated per-element PVs.
         *
         *  <p>Sent on initial connection to the array PV,
         *  and also when the array PV changes its size,
         *  i.e. adds or removes elements.
         *
         *  @param element_pvs One scalar PV for each element of the array
         */
        public void arrayChanged(List<RuntimePV> element_pvs);
    }

    private final RuntimePV array_pv;
    private final Disposable array_flow;

    private final String basename;

    private final Listener listener;

    /** Per-element PVs are local PVs which sent notification "right away".
     *  This flag is used to ignore such updates whenever the dispatcher
     *  itself is writing to the per-element PVs,
     *  or when the per-element PVs update the array.
     */
    private volatile boolean ignore_updates = false;

    private volatile boolean is_string = false;

    private final AtomicReference<List<PV>> element_pvs = new AtomicReference<>(Collections.emptyList());
    private final CopyOnWriteArrayList<Disposable> element_flow = new CopyOnWriteArrayList<>();

    // TODO Update to directly use VType.PV, remove RuntimePV

    /** Construct dispatcher
     *
     *  @param array_pv PV that will be dispatched into per-element PVs
     *  @param basename Base name used to create per-element PVs.
     *  @see #close()
     */
    public ArrayPVDispatcher(final RuntimePV array_pv, final String basename,
                             final Listener listener)
    {
        this.array_pv = array_pv;
        this.basename = basename;
        this.listener = listener;

        array_flow = array_pv.getPV().onValueEvent().subscribe(this::dispatchArrayUpdate);
    }

    /** @param value Value update from array */
    private void dispatchArrayUpdate(final VType value) throws Exception
    {
        if (ignore_updates)
            return;

        if (PV.isDisconnected(value))
            notifyOfDisconnect();
        else
        {
            if (value instanceof VNumberArray)
                dispatchArrayUpdate(((VNumberArray)value).getData());
            else if (value instanceof VEnumArray)
                dispatchArrayUpdate(((VEnumArray)value).getIndexes());
            else if (value instanceof VStringArray)
                dispatchArrayUpdate(((VStringArray)value).getData());
            // Dispatch scalar PVs as one-element arrays
            else if (value instanceof VNumber)
                dispatchArrayUpdate(ArrayDouble.of(((VNumber)value).getValue().doubleValue()));
            else if (value instanceof VEnum)
                dispatchArrayUpdate(ArrayInteger.of(((VEnum)value).getIndex()));
            else if (value instanceof VString)
                dispatchArrayUpdate(Arrays.asList(((VString)value).getValue()));
            else
                throw new Exception("Cannot handle " + value);
        }
    }

    private void notifyOfDisconnect()
    {
        ignore_updates = true;
        try
        {
            for (PV pv : element_pvs.get())
            {
                try
                {
                    pv.write(null);
                }
                catch (Exception ex)
                {
                    logger.log(Level.WARNING, "Cannot notify element PV " + pv.getName() + " of disconnect", ex);
                }
            }
        }
        finally
        {
            ignore_updates = false;
        }
    }

    /** @param value Value update from array of numbers or enum indices */
    private void dispatchArrayUpdate(final ListNumber value) throws Exception
    {
        ignore_updates = true;
        try
        {
            List<PV> pvs = element_pvs.get();
            final int N = value.size();
            if (pvs.size() != N)
            {   // Create new element PVs
                pvs = new ArrayList<>(N);
                for (int i=0; i<N; ++i)
                {
                    final double val = value.getDouble(i);
                    final String name = "loc://" + basename + "_" + i;
                    final PV pv = PVPool.getPV(name);
                    pv.write(val);
                    pvs.add(pv);
                }
                updateElementPVs(false, pvs);
            }
            else
            {   // Update existing element PVs
                final Time now = Time.now();
                for (int i=0; i<N; ++i)
                    pvs.get(i).write(VDouble.of(value.getDouble(i), Alarm.none(), now, Display.none()));
            }
        }
        finally
        {
            ignore_updates = false;
        }
    }

    /** @param value Value update from array of strings */
    private void dispatchArrayUpdate(final List<String> value) throws Exception
    {
        ignore_updates = true;
        try
        {
            List<PV> pvs = element_pvs.get();
            final int N = value.size();
            if (pvs.size() != N)
            {   // Create new element PVs
                pvs = new ArrayList<>(N);
                for (int i=0; i<N; ++i)
                {
                    final String name = "loc://" + basename + i + "(\"\")";
                    final PV pv = PVPool.getPV(name);
                    pv.write(value.get(i));
                    pvs.add(pv);
                }
                updateElementPVs(true, pvs);
            }
            else
            {   // Update existing element PVs
                final Time now = Time.now();
                for (int i=0; i<N; ++i)
                    pvs.get(i).write(VString.of(value.get(i), Alarm.none(), now));
            }
        }
        finally
        {
            ignore_updates = false;
        }
    }

    /** Update the array PV with the current value of all element PVs
     *  @param trigger Value of element PV that triggered the update
     */
    private void updateArrayFromElements(final VType trigger) throws Exception
    {
        if (ignore_updates)
            return;

        ignore_updates = true;
        try
        {
            final List<PV> pvs = element_pvs.get();
            final int N = pvs.size();

            if (N == 1)
            {   // Is 'array' really a scalar?
                final VType array = array_pv.read();
                if (array instanceof VNumber ||
                    array instanceof VString)
                {
                    array_pv.write(pvs.get(0).read());
                    return;
                }
            }

            if (is_string)
            {
                final String[] value = new String[N];
                for (int i=0; i<N; ++i)
                    value[i] = VTypeUtil.getValueString(pvs.get(i).read(), false);
                array_pv.write(value);
            }
            else
            {
                final double[] value = new double[N];
                for (int i=0; i<N; ++i)
                    value[i] = VTypeUtil.getValueNumber(pvs.get(i).read()).doubleValue();
                array_pv.write(value);
            }
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Cannot update array from elements, triggered by " + trigger, ex);
        }
        finally
        {
            ignore_updates = false;
        }
    }

    /** Update per-element PVs.
     *
     *  <p>Disposes old PVs.
     *
     *  <p>Notifies listeners except for special <code>null</code>
     *  parameter used on close
     *
     *  @param new_pvs New per-element PVs
     */
    private void updateElementPVs(final boolean is_string, final List<PV> new_pvs)
    {
        this.is_string = is_string;
        final List<PV> old = element_pvs.getAndSet(new_pvs);
        for (Disposable flow : element_flow)
            flow.dispose();
        element_flow.clear();
        for (PV pv : old)
            PVPool.releasePV(pv);
        if (new_pvs != null)
        {
            // TODO For now each element PV needs to be wrapped as RuntimePV. Remove that
            final List<RuntimePV> rt_pvs = new ArrayList<>(new_pvs.size());
            for (PV pv : new_pvs)
            {
                element_flow.add(pv.onValueEvent().subscribe(this::updateArrayFromElements));
                rt_pvs.add(new RuntimePV(pv));
            }
            listener.arrayChanged(rt_pvs);
        }
    }

    /** Must be called when dispatcher is no longer needed.
     *
     *  <p>Releases the per-element PVs
     */
    @Override
    public void close()
    {
        array_flow.dispose();
        updateElementPVs(false, null);
    }
}
