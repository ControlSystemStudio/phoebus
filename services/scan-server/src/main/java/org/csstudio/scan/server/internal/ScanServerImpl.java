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
package org.csstudio.scan.server.internal;

import static org.csstudio.scan.server.ScanServerInstance.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.csstudio.scan.command.ScanCommand;
import org.csstudio.scan.command.XMLCommandReader;
import org.csstudio.scan.command.XMLCommandWriter;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.info.MemoryInfo;
import org.csstudio.scan.info.Scan;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanServerInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.info.SimulationResult;
import org.csstudio.scan.server.ScanCommandImpl;
import org.csstudio.scan.server.ScanCommandImplTool;
import org.csstudio.scan.server.ScanContext;
import org.csstudio.scan.server.ScanServer;
import org.csstudio.scan.server.ScanServerInstance;
import org.csstudio.scan.server.SimulationContext;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.device.DeviceContext;

/** Implementation of the {@link ScanServer}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanServerImpl implements ScanServer
{
    /** {@link ScanEngine} used by this server */
    final private ScanEngine scan_engine = new ScanEngine();

    /** Time when this scan server was started */
    private Instant start_time = null;

    /** Start the scan server */
    public void start() throws Exception
    {
        if (start_time != null)
            throw new Exception("Already started");

        scan_engine.start(true);
        start_time = Instant.now();
    }

    /** Stop the scan server */
    public void stop()
    {
        scan_engine.stop();
    }

    /** {@inheritDoc} */
    @Override
    public ScanServerInfo getInfo() throws Exception
    {
        return new ScanServerInfo(ScanServerInstance.VERSION,
                start_time,
                ScanServerInstance.getScanConfigURL().toExternalForm(),
                ScanServerInstance.getScanConfig().getScriptPaths(),
                ScanServerInstance.getScanConfig().getMacros());
    }

    /** Query server for devices used by a scan
     *
     *  <p>Meant to be called only inside the scan server.
     *
     *  @param id ID that uniquely identifies a scan
     *            or -1 for default devices
     *  @return {@link Device}s
     *  @see #getDeviceInfos(long) for similar method that is exposed to clients
     *  @throws Exception on error
     */
     public Device[] getDevices(final long id) throws Exception
    {
        if (id >= 0)
        {   // Get devices for specific scan
            final ExecutableScan scan = scan_engine.getExecutableScan(id);
            if (scan != null)
                return scan.getDevices();
            // else: It's a logged scan, no device info available any more
        }
        else
        {   // Get devices in context
            try
            {
                final DeviceContext context = DeviceContext.getDefault();
                return context.getDevices();
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error reading device context", ex);
            }
        }
        return new Device[0];
    }

    /** {@inheritDoc} */
    @Override
    public List<DeviceInfo> getDeviceInfos(final long id) throws Exception
    {
        final Device[] devices = getDevices(id);
        // Turn Device[] into DeviceInfo[]
        final List<DeviceInfo> infos = new ArrayList<>(devices.length);
        for (Device dev : devices)
            infos.add(dev);
        return infos;
    }

    /** {@inheritDoc} */
    @Override
    public SimulationResult simulateScan(final String commands_as_xml)
            throws Exception
    {
        logger.log(Level.INFO, "Starting simulation...");
        try
        (   // Create Jython interpreter for this scan
            final JythonSupport jython = new JythonSupport();
        )
        {   // Parse scan from XML
            final List<ScanCommand> commands = XMLCommandReader.readXMLString(commands_as_xml);

            // Implement commands
            List<ScanCommandImpl<?>> scan = ScanCommandImplTool.implement(commands, jython);

            // Setup simulation log
            ByteArrayOutputStream log_buf = new ByteArrayOutputStream();
            PrintStream log_out = new PrintStream(log_buf);
            log_out.println("Simulation:");
            log_out.println("--------");

            // Simulate
            final SimulationContext simulation = new SimulationContext(jython, log_out);
            simulation.performSimulation(scan);

            // Close log
            log_out.println("--------");
            log_out.println(simulation.getSimulationTime() + "   Total estimated execution time");
            log_out.close();

            // Fetch simulation log
            final String log_text = log_buf.toString();

            // Help GC to clear copies of log
            log_out = null;
            log_buf = null;
            scan.clear();
            commands.clear();

            logger.log(Level.INFO, "Completed simulation.");
            return new SimulationResult(simulation.getSimulationSeconds(), log_text);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Scan simulation failed", ex);
            throw ex;
        }
    }

    /** {@inheritDoc} */
    @Override
    public long submitScan(final String scan_name,
                           final String commands_as_xml,
                           final boolean queue,
                           final boolean pre_post) throws Exception
    {
        cullScans();

        try
        {   // Parse received 'main' scan from XML
            final List<ScanCommand> commands = XMLCommandReader.readXMLString(commands_as_xml);

            // Read pre- and post-scan commands
            final List<ScanCommand> pre_commands, post_commands;
            if (pre_post)
            {
                pre_commands = new ArrayList<>();
                for (String path : ScanServerInstance.getScanConfig().getPreScanPaths())
                    try
                    {
                        pre_commands.addAll(XMLCommandReader.readXMLStream(PathStreamTool.openStream(path)));
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("Pre-scan parse errro in " + path, ex);
                    }

                post_commands = new ArrayList<>();
                for (String path : ScanServerInstance.getScanConfig().getPostScanPaths())
                    try
                    {
                        post_commands.addAll(XMLCommandReader.readXMLStream(PathStreamTool.openStream(path)));
                    }
                    catch (Exception ex)
                    {
                        throw new Exception("Post-scan parse errro in " + path, ex);
                    }
            }
            else
                pre_commands = post_commands = Collections.emptyList();

            // Create Jython interpreter for this scan
            final JythonSupport jython = new JythonSupport();

            // Obtain implementations for the requested commands as well as pre/post scan
            final List<ScanCommandImpl<?>> pre_impl = ScanCommandImplTool.implement(pre_commands, jython);
            final List<ScanCommandImpl<?>> main_impl = ScanCommandImplTool.implement(commands, jython);
            final List<ScanCommandImpl<?>> post_impl = ScanCommandImplTool.implement(post_commands, jython);

            // Get empty device context
            final DeviceContext devices = new DeviceContext();

            // Submit scan to engine for execution
            final ExecutableScan scan = new ExecutableScan(scan_engine, jython, scan_name, devices, pre_impl, main_impl, post_impl);
            scan_engine.submit(scan, queue);
            logger.log(Level.CONFIG, "Submitted ID " + scan.getId() + " \"" + scan.getName() + "\"");
            return scan.getId();
        }
        catch (Throwable ex)
        {
            logger.log(Level.WARNING, "Scan submission failed", ex);
            throw ex;
        }
    }

    /** If memory consumption is high, remove some older scans */
    private void cullScans() throws Exception
    {
        final double threshold = ScanServerInstance.getScanConfig().getOldScanRemovalMemoryThreshold();
        int count = 0;

        MemoryInfo used = new MemoryInfo();
        while (used.getMemoryPercentage() > threshold && count < 10)
        {
            ++count;
            // Try to turn scan with commands into logged scan
            Scan removed = scan_engine.logOldestCompletedScan();
            if (removed != null)
                logger.log(Level.INFO, "Culling " + count + ", replaced with log: " + removed);
            else
            {   // If not possible, delete oldest scan
                removed = scan_engine.removeOldestCompletedScan();
                if (removed != null)
                    logger.log(Level.INFO, "Culling " + count + ", removed: " + removed);
                else
                    return;
            }
            // Log time stamps of before..after can be used to time the GC
            logger.log(Level.INFO, "Before " + used);
            System.gc();
            final MemoryInfo now = new MemoryInfo();
            logger.log(Level.INFO, "Now    " + now);
            used = now;
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<ScanInfo> getScanInfos() throws Exception
    {
        final List<LoggedScan> scans = scan_engine.getScans();
        final List<ScanInfo> infos = new ArrayList<>(scans.size());
        // Build result with most recent scan first
        for (int i=scans.size()-1; i>=0; --i)
            infos.add(scans.get(i).getScanInfo());
        return infos;
    }

    /** {@inheritDoc} */
    @Override
    public ScanInfo getScanInfo(final long id) throws Exception
    {
        final LoggedScan scan = scan_engine.getScan(id);
        return scan.getScanInfo();
    }

    /** {@inheritDoc} */
    @Override
    public String getScanCommands(final long id) throws Exception
    {
        final ExecutableScan scan = scan_engine.getExecutableScan(id);
        if (scan != null)
        {
            try
            {
                return XMLCommandWriter.toXMLString(scan.getScanCommands());
            }
            catch (Exception ex)
            {
                throw new Exception(ex.getMessage(), ex);
            }
        }
        else
            throw new Exception("Commands not available for logged scan");
    }

    /** Obtain scan context.
     *  @param id ID that uniquely identifies a scan
     *  @return {@link ScanContext} or <code>null</code> if ID does not refer to an active scan
     *  @throws Exception
     */
    public ScanContext getScanContext(final long id) throws Exception
    {
        final ScanContext scan = scan_engine.getExecutableScan(id);
        if (scan != null)
            return scan;
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastScanDataSerial(final long id) throws Exception
    {
        final LoggedScan scan = scan_engine.getScan(id);
        if (scan != null)
            return scan.getLastScanDataSerial();
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public ScanData getScanData(final long id) throws Exception
    {
        try
        {
            final LoggedScan scan = scan_engine.getScan(id);
            return scan.getScanData();
        }
        catch (Exception ex)
        {
            throw new Exception("Error retrieving log data", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateScanProperty(final long id, final long address,
            final String property_id, final Object value) throws Exception
    {
        final ExecutableScan scan = scan_engine.getExecutableScan(id);
        if (scan != null)
            scan.updateScanProperty(address, property_id, value);
    }

    /** {@inheritDoc} */
    @Override
    public void move(long id, int steps) throws Exception
    {
        scan_engine.move(id, steps);
    }

    /** {@inheritDoc} */
    @Override
    public void next(final long id) throws Exception
    {
        if (id >= 0)
        {
            final ExecutableScan scan = scan_engine.getExecutableScan(id);
            if (scan != null)
                scan.next();
        }
        else
        {
            final List<ExecutableScan> scans = scan_engine.getExecutableScans();
            for (ExecutableScan scan : scans)
                scan.next();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void pause(final long id) throws Exception
    {
        if (id >= 0)
        {
            final ExecutableScan scan = scan_engine.getExecutableScan(id);
            if (scan != null)
                scan.pause();
        }
        else
        {
            final List<ExecutableScan> scans = scan_engine.getExecutableScans();
            for (ExecutableScan scan : scans)
                scan.pause();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void resume(final long id) throws Exception
    {
        if (id >= 0)
        {
            final ExecutableScan scan = scan_engine.getExecutableScan(id);
            scan.resume();
        }
        else
        {
            final List<ExecutableScan> scans = scan_engine.getExecutableScans();
            for (ExecutableScan scan : scans)
                scan.resume();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void abort(final long id) throws Exception
    {
        if (id >= 0)
        {
            final ExecutableScan scan = scan_engine.getExecutableScan(id);
            scan.doAbort(scan.prepareAbort());
        }
        else
        {
            logger.log(Level.INFO, "Abort all scans");
            final List<ExecutableScan> scans = scan_engine.getExecutableScans();
            // List is ordered from old (running) to new (idle).
            // First abort idle scans at the top of the queue so they won't ever start.
            // Otherwise, a running scan would get aborted first,
            // then the next scan in the queue would start only to get aborted.
            // Worst case, when the running-then-aborted scan ends,
            // it finds other Idle scans and marks the scan-active PV as 1.
            // To prevent that, first mark all scans to be aborted as such,
            // which once more prevents their start and also avoids them
            // being counted as 'active'.
            final ScanState[] previous = new ScanState[scans.size()];
            for (int i=scans.size()-1; i>=0; --i)
                previous[i] = scans.get(i).prepareAbort();
            // Then actually abort all which are already marked as aborted
            for (int i=scans.size()-1; i>=0; --i)
                scans.get(i).doAbort(previous[i]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void remove(final long id) throws Exception
    {
        final LoggedScan scan = scan_engine.getScan(id);
        try
        {
            scan_engine.removeScan(scan);
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error removing scan", ex);
            throw new Exception("Error removing scan", ex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void removeCompletedScans() throws Exception
    {
        try
        {
            scan_engine.removeCompletedScans();
        }
        catch (Exception ex)
        {
            logger.log(Level.WARNING, "Error removing completed scans", ex);
            throw new Exception("Error removing completed scans", ex);
        }
    }
}
