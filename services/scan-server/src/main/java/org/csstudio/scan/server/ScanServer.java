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
package org.csstudio.scan.server;

import java.util.List;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.SimulationResult;

/** Interface to the scan server
 *
 *  <p>Used to be the RMI interface, now only used within scan server.
 *
 *  @author Kay Kasemir
 */
public interface ScanServer
{
    /** @return Info about the scan server
     *  @throws Exception on error
     */
    public ScanServerInfo getInfo() throws Exception;

    /** Query server for devices used by a scan
     *  @param id ID that uniquely identifies a scan
     *            or -1 for default devices
     *  @return Info about devices
     *  @throws Exception on error
     */
    public List<DeviceInfo> getDeviceInfos(long id) throws Exception;

    /** Submit a scan for simulation
     *  @param commands_as_xml Commands to simulate in XML format
     *  @return {@link SimulationResult}
     *  @throws Exception
     */
    public SimulationResult simulateScan(String commands_as_xml) throws Exception;

    /** Submit a sequence of commands as a 'scan' to be executed
     *  @param scan_name Name of the scan
     *  @param commands_as_xml Commands to execute within the scan in XML format
     *  @param queue Queue the scan, or execute as soon as possible?
     *  @param pre_post Perform the pre- and post-scans?
     *  @return ID that uniquely identifies the scan
     *  @throws Exception on error
     */
    public long submitScan(String scan_name, String commands_as_xml, boolean queue, boolean pre_post) throws Exception;

    /** Query server for scans
     *  @return Info for each scan on the server, most recently submitted scan first
     *  @throws Exception on error
     */
    public List<ScanInfo> getScanInfos() throws Exception;

    /** Query server for scan info
     *  @param id ID that uniquely identifies a scan
     *  @return Info for that scan on the server or <code>null</code>
     *  @throws Exception on error
     */
    public ScanInfo getScanInfo(long id) throws Exception;

    /** Query server for the commands in a scan
     *  @param id ID that uniquely identifies a scan
     *  @return Scan commands in XML format for that scan on the server or <code>null</code>
     *  @throws Exception on error
     */
    public String getScanCommands(long id) throws Exception;

    /** Get serial of last logged sample.
     *
     *  <p>Can be used to determine if there are new samples
     *  that should be fetched via <code>getScanData()</code>
     *
     *  @param id ID that uniquely identifies a scan
     *  @return Serial of last sample in scan data or -1 if nothing has been logged
     *  @see #getScanData(long)
     */
    public long getLastScanDataSerial(long id) throws Exception;

    /** Query server for scan data
     *  @param id ID that uniquely identifies a scan
     *  @return Data for that scan on the server or <code>null</code>
     *  @throws Exception on error
     *  @see #getLastScanDataSerial(long)
     */
    public ScanData getScanData(long id) throws Exception;

    /** Ask server to update a command parameter to a new value
     *  @param id ID that uniquely identifies a scan
     *  @param address Address of the command
     *  @param property_id Property to update
     *  @param value New value for the property
     *  @throws Exception on error
     */
    public void updateScanProperty(long id, long address, String property_id, Object value) throws Exception;

    /** Ask server to move idle scan in list
     *
     *  <p>Has no effect if the scan is not idle or target slot in scan list cannot be used.
     *
     *  @param id ID that uniquely identifies a scan
     *  @param steps How far to move the scan 'up' (earlier) for positive steps, otherwise 'down'.
     *  @throws Exception on error
     */
    public void move(long id, int steps) throws Exception;

    /** Ask server to force transition to next command
     *
     *  <p>Has no effect if the scan is not running.
     *
     *  @param id ID that uniquely identifies a scan
     *            -1 to force 'next' on all running scans
     *  @throws Exception on error
     */
    public void next(long id) throws Exception;

    /** Ask server to pause a scan
     *
     *  <p>Note that pausing has no effect if the scan is not running.
     *  It is specifically not possible to pause an idle scan, i.e.
     *  one that hasn't started.
     *  We consider that a feature:
     *  If you don't really want your scan to start, simply don't submit it
     *  in the first place.
     *  You cannot submit a scan and pause it right away, i.e. block
     *  all subsequent scans.
     *
     *  @param id ID that uniquely identifies a scan
     *            -1 to pause all running scans
     *  @throws Exception on error
     */
    public void pause(long id) throws Exception;

    /** Ask server to resume a paused scan
     *  @param id ID that uniquely identifies a scan
     *            -1 to resume all paused scans
     *  @throws Exception on error
     */
    public void resume(long id) throws Exception;

    /** Ask server to abort a scan
     *  @param id ID that uniquely identifies a scan
     *            -1 to abort all scans
     *  @throws Exception on error
     */
    public void abort(long id) throws Exception;

    /** Ask server to remove a (finished) scan
     *  @param id ID that uniquely identifies a scan
     *  @throws Exception on error
     */
    public void remove(long id) throws Exception;

    /** Remove completed scans (NOP if there aren't any)
     *  @throws Exception on error
     */
    public void removeCompletedScans() throws Exception;
}
