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
package org.csstudio.scan.server.condition;

import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.DeviceListener;

/** {@link DeviceCondition} that delays the scan until all {@link Device}s are 'ready'
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WaitForDevicesCondition implements DeviceCondition, DeviceListener
{
    final private Device[] devices;
    private boolean all_ready;

    /** Initialize
     *  @param devices Devices that all need to be 'ready'
     */
    public WaitForDevicesCondition(final Device... devices)
    {
        this.devices = devices;
    }

    /** {@inheritDoc} */
    @Override
    public void await() throws Exception
    {
        for (Device device : devices)
            device.addListener(this);

        synchronized (this)
        {
            all_ready = allReady(devices);
            while (! all_ready)
            {   // Wait for update from device or early completion
                wait();
            }
        }

        for (Device device : devices)
            device.removeListener(this);
    }

    @Override
    public void deviceChanged(final Device device)
    {
        synchronized (this)
        {
            if (allReady(devices))
                all_ready = true;
            // Notify execute() to check all devices again
            notifyAll();
        }
    }

    /** @param devices Devices to check
     *  @return <code>true</code> if all devices are 'ready'
     */
    private boolean allReady(final Device[] devices)
    {
        for (Device device : devices)
            if (! device.isReady())
                return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void complete()
    {
        synchronized (this)
        {
            all_ready = true;
            // Notify await() so it can check again.
            notifyAll();
        }
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        final StringBuilder pending = new StringBuilder();
        for (Device device : devices)
            if (! device.isReady())
            {
                if (pending.length() > 0)
                    pending.append(", ");
                pending.append(device);
            }
        if (pending.length() <= 0)
            return "All devices ready";
        return "Waiting for device " + pending.toString();
    }
}
