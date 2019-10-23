/*******************************************************************************
 * Copyright (c) 2011-2019 Oak Ridge National Laboratory.
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
import java.util.Optional;

import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.server.device.Device;
import org.csstudio.scan.server.internal.ExecutableScan;
import org.csstudio.scan.server.log.DataLog;

/** Context in which the {@link ScanCommandImpl}s of a {@link ExecutableScan} are executed.
 *
 *  <ul>
 *  <li>{@link Device}s with which commands can interact.
 *  <li>Methods to execute commands, supporting Pause/Continue/Abort
 *  <li>Data logger for {@link ScanSample}s
 *  <li>Progress information
 *  </ul>
 *
 *  <p>Scan commands can only interact with this restricted API
 *  of the actual {@link ExecutableScan}.
 *
 *  @author Kay Kasemir
 */
public interface ScanContext
{
    /** @return {@link ScanState} */
    public ScanState getScanState();

    /** @return Macro support */
    public MacroContext getMacros();

    /** Get a device by (alias) name.
     *  @param name (Alias) name of the device.
     *  @return {@link Device} with that name
     *  @throws Exception when device name not known
     */
    public Device getDevice(final String name) throws Exception;

    /** Set log mode
     *  @param automatic Should commands automatically log every change that they perform/observe?
     */
    public void setLogMode(final boolean automatic);

    /** @return Should commands automatically log every change that they perform/observe?
     */
    public boolean isAutomaticLogMode();

    /** Obtain the active data log.
     *
     *  <p>Only present while scan is being executed.
     *  Caller must NOT close this log.
     *
     *  @return {@link DataLog}
     */
    abstract public Optional<DataLog> getDataLog();

    /** Execute a list of commands
     *  @param commands {@link ScanCommandImpl}s to execute
     *  @throws Exception on error in executing a command
     */
    abstract public void execute(final List<ScanCommandImpl<?>> commands) throws Exception;

    /** Execute a single command
     *  @param command {@link ScanCommandImpl} to execute
     *  @throws Exception on error in executing the command
     */
    abstract public void execute(final ScanCommandImpl<?> command) throws Exception;

    /** Inform scan context that work has been performed.
     *  Meant to be called by {@link ScanCommandImpl}s
     *  @param work_units Number of performed work units
     */
    public void workPerformed(final int work_units);
}
