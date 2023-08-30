/*******************************************************************************
 * Copyright (c) 2019 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.server.condition.WaitForDevicesCondition;
import org.csstudio.scan.server.device.DeviceContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;


/** Junit test of {@link WaitForDevicesCondition}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class WaitForDevicesTest
{
    @Test
    public void testWaitForDevices() throws Exception
    {
        final DeviceContext devices = new DeviceContext();
        devices.addPVDevice(new DeviceInfo("loc://x(1)"));
        devices.addPVDevice(new DeviceInfo("loc://y(2)"));

        devices.startDevices();
        try
        {
            final WaitForDevicesCondition connect = new WaitForDevicesCondition(devices.getDevices());
            final long start = System.currentTimeMillis();
            final boolean connected = connect.await(5, TimeUnit.SECONDS);
            final long end = System.currentTimeMillis();
            System.out.println("Connected to local PVs within " + (end-start) + "ms");
            assertThat(connected, equalTo(true));
        }
        finally
        {
            devices.stopDevices();
        }
    }

    @Test
    public void testTimeoutDevices() throws Exception
    {
        final DeviceContext devices = new DeviceContext();
        devices.addPVDevice(new DeviceInfo("loc://x(1)"));
        devices.addPVDevice(new DeviceInfo("bogus_pv_name"));

        devices.startDevices();
        try
        {
            final WaitForDevicesCondition connect = new WaitForDevicesCondition(devices.getDevices());
            final long start = System.currentTimeMillis();
            final boolean connected = connect.await(3, TimeUnit.SECONDS);
            final long end = System.currentTimeMillis();
            System.out.println("Timed out after " + (end-start) + "ms");
            assertThat(connected, equalTo(false));
        }
        finally
        {
            devices.stopDevices();
        }
    }
}
