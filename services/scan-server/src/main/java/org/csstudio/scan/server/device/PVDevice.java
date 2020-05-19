/*******************************************************************************
 * Copyright (c) 2011-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The scan engine idea is based on the "ScanEngine" developed
 * by the Software Services Group (SSG),  Advanced Photon Source,
 * Argonne National Laboratory,
 * Copyright (c) 2011 , UChicago Argonne, LLC.
 *
 * This implementation, however, contains no SSG "ScanEngine" source code
 * and is not endorsed by the SSG authors.
 ******************************************************************************/
package org.csstudio.scan.server.device;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.csstudio.scan.device.DeviceInfo;
import org.epics.vtype.Alarm;
import org.epics.vtype.Time;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VString;
import org.epics.vtype.VType;
import org.phoebus.core.vtypes.VTypeHelper;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.util.time.TimeDuration;

import io.reactivex.disposables.Disposable;


/** {@link Device} that is connected to a Process Variable,
 *  supporting read and write access to that PV.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVDevice extends Device
{
    /** 'compile time' option to treat byte arrays as string */
    private static final boolean TREAT_BYTES_AS_STRING = true; // XXX Make treat-byte-as-string configurable?

    /** Is the underlying PV type a BYTE[]?
     *  @see #TREAT_BYTES_AS_STRING
     */
    private volatile boolean is_byte_array = false;

    /** Underlying control system PV */
    private final AtomicReference<PV> pv = new AtomicReference<>();

    private volatile Disposable pv_flow;

    /** Most recent value of the PV, updated by subscription as well as read(timeout)  */
    private final AtomicReference<VType> value = new AtomicReference<>(getDisconnectedValue());

    /** Initialize
     *  @param info {@link DeviceInfo}
     *  @throws Exception on error during PV setup
     */
    public PVDevice(final DeviceInfo info) throws Exception
    {
        super(info);
    }

    /** {@inheritDoc} */
    @Override
    public void start() throws Exception
    {
        final PV new_pv = PVPool.getPV(getName());
        if (pv.getAndSet(new_pv) != null)
        {
            PVPool.releasePV(new_pv);
            throw new Exception(getName() + " already started");
        }
        pv_flow = new_pv.onValueEvent().subscribe(this::handleValueUpdate);
    }

    private void handleValueUpdate(final VType new_value)
    {
        logger.log(Level.FINE, "PV {0} received {1}", new Object[] { getName(), new_value });
        value.set(wrapReceivedValue(new_value));
        fireDeviceUpdate();
    }

    private VType wrapReceivedValue(VType new_value)
    {
        if (new_value == null)
            return getDisconnectedValue();
        else if (TREAT_BYTES_AS_STRING  && new_value instanceof VByteArray)
        {
            is_byte_array = true;
            final VByteArray barray = (VByteArray) new_value;
            new_value = VString.of(ByteHelper.toString(barray), barray.getAlarm(), barray.getTime());
            logger.log(Level.FINE,
                    "PV BYTE[] converted to {0}", new_value);
            return new_value;
        }
        else
            return new_value;

    }


    /** {@inheritDoc} */
    @Override
    public boolean isReady()
    {
        return ! PV.isDisconnected(value.get());
    }

    /** @return Human-readable device status */
    @Override
    public String getStatus()
    {
        if (pv.get() == null)
            return "no PV";
        else
        {
            final VType v = value.get();
            if (PV.isDisconnected(v))
                return Alarm.disconnected().getName();
            return VTypeHelper.toString(v);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop()
    {
        final PV copy  = pv.getAndSet(null);
        if (copy == null)
            logger.log(Level.SEVERE, getName() + " stopped but never started");
        else
        {
            pv_flow.dispose();
            PVPool.releasePV(copy);
        }
        value.set(getDisconnectedValue());
    }

    /** {@inheritDoc} */
    @Override
    public VType read() throws Exception
    {
        final VType current = value.get();
        logger.log(Level.FINER, () -> "Reading: PV " + getName() + " = " + current);
        return current;
    }

    /** Turn {@link TimeDuration} into millisecs for {@link TimeUnit} API
     *  @param timeout {@link TimeDuration}
     *  @return Milliseconds or 0
     */
    private static long getMillisecs(final Duration timeout)
    {
        if (timeout == null)
            return 0;
        return Math.max(0, timeout.toMillis());
    }

    /** {@inheritDoc} */
    @Override
    public VType read(final Duration timeout) throws Exception
    {
        final PV save_pv = pv.get(); // Copy to access PV outside of lock
        final VType orig = value.get();
        try
        {
            final Future<VType> read_result = save_pv.asyncRead();
            final long millisec = getMillisecs(timeout);
            final VType received_value = (millisec > 0)
                ? read_result.get(millisec, TimeUnit.MILLISECONDS)
                : read_result.get();
            final VType got = wrapReceivedValue(received_value);
            synchronized (this)
            {
                // If value is still == orig, update to what we got from read.
                // Else: Get-callback was superseded by monitor, so keep the last monitor.
                value.compareAndSet(orig, got);
                return value.get();
            }
        }
        catch (Exception ex)
        {
            value.set(getDisconnectedValue());
            // Report InterruptedException (from abort) as such
            if (ex instanceof InterruptedException)
                throw new InterruptedException("Failed to read " + getName());
            throw new Exception("Failed to read " + getName(), ex);
        }
    }

    /** @return 'Disconnected' Value with current time stamp */
    final private static VType getDisconnectedValue()
    {
        return VString.of(Alarm.disconnected().getName(), Alarm.disconnected(), Time.now());
    }

    /** Handle write conversions
     *  @param value to write
     *  @return Actual value to write
     */
    private Object wrapSentValue(Object value)
    {
        if (is_byte_array && TREAT_BYTES_AS_STRING)
        {
            // If value is a scalar, turn into string
            if (value instanceof Number)
                value = value.toString();
            // String in general written as array of bytes
            if (value instanceof String)
                value = ByteHelper.toBytes((String) value);
        }
        return value;
    }

    /** Write value to device, with special handling of EPICS BYTE[] as String
     *  @param value Value to write (Double, String)
     *  @throws Exception on error: Cannot write, ...
     */
    @Override
    public void write(Object value) throws Exception
    {
        logger.log(Level.FINER, "Writing: PV {0} = {1}",
                   new Object[] { getName(), value });
        try
        {
            value = wrapSentValue(value);
            pv.get().write(value);
        }
        catch (Exception ex)
        {
            throw new Exception("Failed to write " + value + " to " + getName(), ex);
        }
    }

    /** Write value to device, with special handling of EPICS BYTE[] as String
     *  @param value Value to write (Double, String)
     *  @param timeout Timeout, <code>null</code> as "forever"
     *  @throws Exception on error: Cannot write, ...
     */
    @Override
    public void write(final Object value, final Duration timeout) throws Exception
    {
        final Object actual = wrapSentValue(value);
        final long millisec = getMillisecs(timeout);
        if (millisec > 0)
            logger.log(Level.FINE, () -> "Writing PV " + getName() + " = " + actual + " with completion in " + millisec + " ms");
        else
            logger.log(Level.FINE, () -> "Writing PV " + getName() + " = " + actual);
        try
        {
            final Future<?> write_result = pv.get().asyncWrite(actual);
            if (millisec > 0)
                write_result.get(millisec, TimeUnit.MILLISECONDS);
            else
                write_result.get();
        }
        catch (InterruptedException ex)
        {   // Report InterruptedException (from abort) as such
            throw new InterruptedException("Interrupted while writing " + actual + " to " + getName());
        }
        catch (Exception ex)
        {
            if (millisec > 0  &&  ex instanceof TimeoutException)
                throw new Exception("Completion timeout for " + getName() + " = " + actual, ex);
            else
                throw new Exception("Failed to write " + actual + " to " + getName(), ex);
        }
    }
}
