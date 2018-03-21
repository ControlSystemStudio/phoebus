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
package org.csstudio.scan.server.internal;

import java.time.Instant;
import java.util.List;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerMain;

/** Implementation of the {@link ScanServer}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerImpl implements ScanServer
{
    /** Time when this scan server was started */
    private volatile Instant start_time = null;

    /** Start the scan server */
    public void start() throws Exception
    {
        if (start_time != null)
            throw new Exception("Already started");

        // TODO scan_engine.start(true);
        start_time = Instant.now();
    }

    @Override
    public ScanServerInfo getInfo() throws Exception
    {
        return new ScanServerInfo(ScanServerMain.VERSION, start_time,
                // TODO Show actual parameters
                "Config", "Simu", new String[] { "/a", "/b"},  "X=u");
    }

    @Override
    public DeviceInfo[] getDeviceInfos(long id) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SimulationResult simulateScan(String commands_as_xml)
            throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long submitScan(String scan_name, String commands_as_xml,
            boolean queue) throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<ScanInfo> getScanInfos() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ScanInfo getScanInfo(long id) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getScanCommands(long id) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getLastScanDataSerial(long id) throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ScanData getScanData(long id) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateScanProperty(long id, long address, String property_id,
            Object value) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void next(long id) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void pause(long id) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume(long id) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void abort(long id) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void remove(long id) throws Exception
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeCompletedScans() throws Exception
    {
        // TODO Auto-generated method stub

    }

}
