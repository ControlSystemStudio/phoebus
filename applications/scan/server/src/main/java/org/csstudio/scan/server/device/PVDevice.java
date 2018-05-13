/*******************************************************************************
 * Copyright (c) 2011-2018 Oak Ridge National Laboratory.
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
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.scan.device.DeviceInfo;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVListener;
import org.phoebus.pv.PVPool;
import org.phoebus.util.time.TimeDuration;
import org.phoebus.vtype.Alarm;
import org.phoebus.vtype.AlarmSeverity;
import org.phoebus.vtype.VByteArray;
import org.phoebus.vtype.VType;
import org.phoebus.vtype.ValueFactory;
import org.phoebus.vtype.ValueUtil;


/** {@link Device} that is connected to a Process Variable,
 *  supporting read and write access to that PV.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVDevice extends Device
{
    /** 'compile time' option to treat byte arrays as string */
    final private static boolean TREAT_BYTES_AS_STRING = true; // XXX Make treat-byte-as-string configurable?

    /** Alarm that is used to identify a disconnected PV */
   final private static Alarm DISCONNECTED = ValueFactory.newAlarm(AlarmSeverity.INVALID, "Disconnected");

    /** Is the underlying PV type a BYTE[]?
     *  @see #TREAT_BYTES_AS_STRING
     */
    private boolean is_byte_array = false;

    /** Most recent value of the PV
     *  SYNC on this
     */
    private VType value = getDisconnectedValue();

    /** Underlying control system PV
     *  SYNC on this
     */
    private PV pv;

    final private PVListener pv_listener = new PVListener()
    {
        @Override
        public void valueChanged(final VType new_value)
        {
            logger.log(Level.FINE,
                "PV {0} received {1}", new Object[] { getName(), new_value });
            synchronized (PVDevice.this)
            {
                value = wrapReceivedValue(new_value);
            }
            fireDeviceUpdate();
        }

        @Override
        public void disconnected()
        {
            value = getDisconnectedValue();
            logger.log(Level.WARNING, "PV " + getName() + " disconnected");
            fireDeviceUpdate();
        }
    };

    private VType wrapReceivedValue(VType new_value)
    {
        if (new_value == null)
            return getDisconnectedValue();
        else if (TREAT_BYTES_AS_STRING  && new_value instanceof VByteArray)
        {
            is_byte_array = true;
            final VByteArray barray = (VByteArray) new_value;
            new_value = ValueFactory.newVString(
                    ByteHelper.toString(barray), barray, barray);
            logger.log(Level.FINE,
                    "PV BYTE[] converted to {0}", new_value);
            return new_value;
        }
        else
            return new_value;

    }

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
        synchronized (this)
        {
            pv = PVPool.getPV(getName());
        }
        pv.addListener(pv_listener);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized boolean isReady()
    {
        if (pv == null  ||  value == null)
            return false;
        // A value might _implement_ Alarm to represent DISCONNECTED,
        // but be a different object,
        // so there is no quick way to check alarm == DISCONNECTED.
        final Alarm alarm = ValueUtil.alarmOf(value);
        return alarm.getAlarmSeverity() != DISCONNECTED.getAlarmSeverity()  ||
               ! alarm.getAlarmName().equals(DISCONNECTED.getAlarmName());
    }

    /** @return Human-readable device status */
    @Override
    public synchronized String getStatus()
    {
        if (pv == null)
            return "no PV";
        else
            return VTypeHelper.toString(value);
    }

    /** {@inheritDoc} */
    @Override
    public void stop()
    {
        final PV copy;
        synchronized (this)
        {
            copy = pv;
        }
        copy.removeListener(pv_listener);
        PVPool.releasePV(copy);
        synchronized (this)
        {
            pv = null;
            value = getDisconnectedValue();
        }
    }

    /** {@inheritDoc} */
    @Override
    public VType read() throws Exception
    {
        final VType current;
        synchronized (this)
        {
            current = this.value;
        }
        logger.log(Level.FINER, "Reading: PV {0} = {1}",
                new Object[] { getName(), current });
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
        final PV pv; // Copy to access PV outside of lock
        final VType orig;
        synchronized (this)
        {
            pv = this.pv;
            orig = value;
        }
        try
        {
            final Future<VType> read_result = pv.asyncRead();
            final long millisec = getMillisecs(timeout);
            final VType received_value = (millisec > 0)
                ? read_result.get(millisec, TimeUnit.MILLISECONDS)
                : read_result.get();
            final VType got = wrapReceivedValue(received_value);
            synchronized (this)
            {
                if (value == orig)
                    value = got;
                // else: Get-callback superseeded by monitor
                return value;
            }
        }
        catch (Exception ex)
        {
            synchronized (this)
            {
                value = getDisconnectedValue();
            }
            // Report InterruptedException (from abort) as such
            if (ex instanceof InterruptedException)
                throw new InterruptedException("Failed to read " + getName());
            throw new Exception("Failed to read " + getName(), ex);
        }
    }

    /** @return 'Disconnected' Value with current time stamp */
    final private static VType getDisconnectedValue()
    {
        return ValueFactory.newVString(DISCONNECTED.getAlarmName(), DISCONNECTED, ValueFactory.timeNow());
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

            final PV pv; // Copy to access PV outside of lock
            synchronized (this)
            {
                pv = Objects.requireNonNull(this.pv, "PV not ready");
            }
            pv.write(value);
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
            final PV pv; // Copy to access PV outside of lock
            synchronized (this)
            {
                pv = this.pv;
            }
            final Future<?> write_result = pv.asyncWrite(actual);
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
