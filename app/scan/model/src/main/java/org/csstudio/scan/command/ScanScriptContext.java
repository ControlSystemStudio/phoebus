/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.command;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.data.ScanSampleFormatter;

/** Context in which a {@link ScriptCommand}'s script is executed
 *
 *  <p>Script can access the scan data that has been collected to far,
 *  and write to devices.
 *
 *  @author Kay Kasemir
 */
abstract public class ScanScriptContext
{
    /** Access to the {@link ScanData}
     *
     *  <p>Provides access to the scan data in its basic form.
     *  @return {@link ScanData} of currently logged data or <code>null</code>
     *  @throws Exception on error
     *  @see #getData
     */
    abstract public ScanData getScanData() throws Exception;

    /** Get data for devices in a form suitable for computations.
     *
     *  <p>Data will be interpolated onto matching serial numbers
     *  via 'staircase' interpolation, holding a value until it changes.
     *
     *  @param devices Names of devices for which to get logged data
     *  @return 2D array: result[0][...] has data for first device,
     *                    result[1][...] has data for second device and so on
     *  @throws Exception on error
     */
    public double[][] getData(final String... devices) throws Exception
    {
        ScanDataIterator sheet = new ScanDataIterator(getScanData(), devices);
        final List<List<Double>> data = new ArrayList<List<Double>>(devices.length);
        for (int i=0; i<devices.length; ++i)
            data.add(new ArrayList<Double>());
        while (sheet.hasNext())
        {
            final ScanSample[] samples = sheet.getSamples();
            for (int i=0; i<devices.length; ++i)
                data.get(i).add(ScanSampleFormatter.asDouble(samples[i]));
        }
        sheet = null;

        // Convert to plain array
        final double[][] result = new double[devices.length][];
        for (int i=0; i<devices.length; ++i)
        {
            final List<Double> list = data.get(i);
            result[i] = new double[list.size()];
            for (int j=list.size()-1; j>=0; --j)
                result[i][j] = list.get(j);
        }
        return result;
    }

    /** Add data for a device to the log.
     *
     *  <p>The data will typically be the result of a computation
     *  performed in the {@link ScriptCommand}, and it is added to
     *  the log to allow plotting or later analysis
     *
     *  <p>Supported data is
     *  <ul>
     *  <li>NDArray</li>
     *  <li>Anything that NDArray.create(Object) can read: double[], int[], ...
     *  </ul>
     *
     *  <p>The data will be logged as a flat array of samples with
     *  serial numbers equal to the array index.
     *
     *  @param device Device name
     *  @param data Data to log for the device
     *  @throws Exception on error
     */
    abstract public void logData(final String device, final Object data) throws Exception;

    /** Read value from a device
     *  @param device_name Name of device
     *  @return Value of the device: String, Double, ...
     *  @throws Exception on error
     */
    abstract public Object read(final String device_name) throws Exception;

    /** Write to device, not waiting for anything
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @throws Exception on error
     */
    public void write(final String device_name, final Object value) throws Exception
    {
        write(device_name, value, true);
    }

    /** Write to device, waiting for completion, but no readback check
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param completion Wait for completion
     *  @throws Exception on error
     */
    public void write(final String device_name, final Object value, final boolean completion) throws Exception
    {
        write(device_name, value, completion, null, 0.1, null);
    }

    /** Write to device, waiting for readback, but not completion
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param readback Readback device (may be same as device_name)
     *  @throws Exception on error
     */
    public void write(final String device_name, final Object value,
            final String readback) throws Exception
    {
        write(device_name, value, false, readback, 0.1, null);
    }

    /** Write to device
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param completion Wait for completion?
     *  @param readback Readback device, <code>null</code> to not wait for readback
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout in seconds, <code>null</code> as "forever"
     *  @throws Exception on error
     */
    abstract public void write(final String device_name, final Object value,
            final boolean completion,
            final String readback, final double tolerance,
            final Duration timeout) throws Exception;
}
