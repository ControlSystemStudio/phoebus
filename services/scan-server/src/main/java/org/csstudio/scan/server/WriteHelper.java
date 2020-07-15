/*******************************************************************************
 * Copyright (c) 2015-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.csstudio.scan.command.Comparison;
import org.csstudio.scan.device.ScanSampleHelper;
import org.csstudio.scan.server.condition.DeviceCondition;
import org.csstudio.scan.server.condition.NumericValueCondition;
import org.csstudio.scan.server.condition.TextValueCondition;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.log.DataLog;
import org.epics.vtype.VType;

/** Helper for writing a PV
 *
 *  <p>Allows for completion, readback, timeout, cancel
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WriteHelper
{
    private final ScanContext context;
    private final Device device, readback;
    private final Object value;
    private final DeviceCondition condition;
    private final boolean completion;
    private final Duration timeout;

    /** Thread that executes the write
     *
     *  SYNC on `this` to prevent race where
     *  executing thread tries to set thread back to null,
     *  while cancel() tries to interrupt.
     */
    private Thread thread = null;

    /** Flag to indicate thread was interrupted because of cancellation */
    private volatile boolean is_cancelled = false;

    /** Write to device with optional completion and/or readback,
     *  logging if the context was configured to auto-log
     *
     *  @param context Scan context
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param completion Await completion to the write?
     *  @param wait Wait for readback to match?
     *  @param readback_name Readback device name (default will be main device_name)
     *  @param readback_value Readback value ("" to use written value)
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout for callback as well as readback; <code>null</code> as "forever"
     *  @throws Exception on error
     */
    public WriteHelper(final ScanContext context,
                       final String device_name, final Object value,
                       final boolean completion,
                       final boolean wait,
                       final String readback_name,
                       Object readback_value,
                       final double tolerance, final Duration timeout) throws Exception
    {
        this.context = context;
        this.completion = completion;
        device = context.getDevice(context.getMacros().resolveMacros(device_name));
        this.timeout = timeout;

        this.value = value;

        // Separate read-back device, or use 'set' device?
        if (readback_name == null  ||  readback_name.isEmpty()  ||  !wait)
            readback = device;
        else
            readback = context.getDevice(context.getMacros().resolveMacros(readback_name));

        //  Wait for the device to reach the value?
        if (wait)
        {
            if (readback_value == null  ||
                readback_value.toString().isEmpty())
                readback_value = value;

            // When using completion, readback needs to match "right away"
            final Duration check_timeout = completion ? Duration.ofSeconds(1) : timeout;
            if (readback_value instanceof Number)
            {
                final double desired = ((Number)readback_value).doubleValue();
                condition = new NumericValueCondition(readback, Comparison.EQUALS, desired,
                                                      tolerance, check_timeout);
            }
            else
            {
                final String desired = readback_value.toString();
                condition = new TextValueCondition(readback, Comparison.EQUALS, desired, check_timeout);
            }
        }
        else
            condition = null;
    }

    /** Perform the write, maybe awaiting completion,
     *  then check readback, and maybe log
     *  @throws Exception on error
     */
    public void perform() throws Exception
    {
        synchronized (this)
        {
            thread = Thread.currentThread();
        }
        logger.log(Level.FINE, "Writing " + device.getName() + " = " + value + (completion ? " with completion" : ""));
        try
        {
            // Perform write
            if (completion)
                device.write(value, timeout);
            else
                device.write(value);

            // Wait?
            if (condition != null)
                condition.await();
        }
        catch (TimeoutException ex)
        {   // Did condition really time out, or did it simply not match after completion,
            // where the timeout is very short?
            if (completion)
                throw new Exception(device + " != " + value + " after completion", ex);
            throw ex;
        }
        catch (InterruptedException ex)
        {
            if (is_cancelled)
                return;
            else
                throw ex;
        }
        finally
        {
            synchronized (this)
            {
                thread = null;
            }
        }

        // Log the value?
        if (context.isAutomaticLogMode())
        {   // If we're waiting on the readback, log the readback.
            // Otherwise readback == device, so log that one
            final VType log_value = readback.read();
            final DataLog log = context.getDataLog().get();
            final long serial = log.getNextScanDataSerial();
            log.log(readback.getAlias(), ScanSampleHelper.createSample(serial, log_value));
        }
    }

    public void cancel()
    {
        is_cancelled = true;
        synchronized (this)
        {
            if (thread != null)
                thread.interrupt();
        }
    }
}
