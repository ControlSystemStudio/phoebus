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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.csstudio.scan.device.DeviceInfo;
import org.epics.vtype.VType;

/** Base interface for all devices
 *
 *  <p>This is the base for an implementation
 *  of a device described by a {@link DeviceInfo}.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class Device extends DeviceInfo
{
    final private List<DeviceListener> listeners = new CopyOnWriteArrayList<>();

    /** Initialize
     *  @param info {@link DeviceInfo}
     */
    public Device(final DeviceInfo info)
    {
        super(info.getName(), info.getAlias());
    }

    /** @param listener Listener to add */
    public void addListener(final DeviceListener listener)
    {
        listeners.add(listener);
        // Initial update
        if (isReady())
            listener.deviceChanged(this);
    }

    /** @param listener Listener to remove */
    public void removeListener(final DeviceListener listener)
    {
        listeners.remove(listener);
    }

    /** Notify listeners that this device changed */
    protected void fireDeviceUpdate()
    {
        for (DeviceListener listener : listeners)
            listener.deviceChanged(this);
    }

    /** Will be called by scan engine before the scan
     *  so that device can start whatever it needs to start
     *  @throws Exception in case of error that should prohibit the scan
     */
    public void start() throws Exception
    {
        // NOP
    }

    /** Check if the device is ready.
     *  <p>Usually this means it has been started
     *  and is connected to whatever resources it needs
     *  (network, ...)
     *  @return <code>true</code> if ready
     */
    public boolean isReady()
    {
        return true;
    }

    /** Will be called by scan engine at end of the scan
     *  so that device can perform whatever shutdown operation
     *  it needs
     */
    public void stop()
    {
        // NOP
    }

    /** Get current value from the device
     *  @return Current value of the device
     *  @throws Exception on error: Cannot read, ...
     */
    public VType read() throws Exception
    {
        throw new Exception("Device '" + getName() + "' does not support reading");
    }

    /** Read a value from the device.
     *
     *  <p>In contrast to <code>read()</code>, this
     *  will actively perform a read request to assert
     *  that we have the current value, not the last
     *  value that was received over the network.
     *
     *  @param timeout Timeout for awaiting the callback, <code>null</code> to wait "forever"
     *  @return Current value of the device
     *  @throws Exception on error: Cannot read, ...
     */
    public VType read(final Duration timeout) throws Exception
    {
        throw new Exception("Device '" + getName() + "' does not support active reading");
    }

    /** Write a value to the device
     *  @param value Value to write (Double, String)
     *  @throws Exception on error: Cannot write, ...
     */
    public void write(final Object value) throws Exception
    {
        throw new Exception("Device '" + getName() + "' does not support writing");
    }


    /** Write a value to the device with callback.
     *  @param value Value to write (Double, String)
     *  @param timeout Timeout for awaiting the callback, <code>null</code> to wait "forever"
     *  @throws Exception on error: Cannot write, ...
     */
    public void write(final Object value, final Duration timeout) throws Exception
    {
        throw new Exception("Device '" + getName() + "' does not support writing with callback");
    }
}
