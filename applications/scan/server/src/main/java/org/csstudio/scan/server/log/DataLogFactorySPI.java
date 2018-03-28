/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log;

import java.util.List;

import org.csstudio.scan.info.Scan;

/** SPI for a {@link DataLog}
 *  @author Kay Kasemir
 */
// Previously called IDataLogFactory
public interface DataLogFactorySPI
{
    /** Called once on startup
     *  @param location Parameter for the log implementation, for example path to directory where log is kept
     *  @throws Exception on error
     */
    public default void startup(String location) throws Exception {};

    /** Create new log for a new scan
     *  @param scan_name Name of the scan (doesn't need to be unique)
     *  @return Scan with ID that can now and later be used to access the data log
     *  @throws Exception on error
     */
    public Scan createDataLog(final String scan_name) throws Exception;

    /** Obtain all available scans
     *  @return Scans that have been logged
     *  @throws Exception on error
     */
    public List<Scan> getScans() throws Exception;

    /** Get log for a scan
     *
     *  <p>Caller needs to <code>close()</code>
     *  @param scan Scan
     *  @return DataLog for Scan, or <code>null</code> if there is none
     *  @throws Exception on error
     *  @see DataLog#close()
     */
    public DataLog getDataLog(final Scan scan) throws Exception;

    /** Delete logged data for a scan
     *  @param scan Scan
     *  @throws Exception on error
     */
    public void deleteDataLog(final Scan scan) throws Exception;

    /** Called on shutdown of scan server to close log infrastructure
     *  @throws Exception on error
     */
    public default void shutdown() throws Exception {};
}
