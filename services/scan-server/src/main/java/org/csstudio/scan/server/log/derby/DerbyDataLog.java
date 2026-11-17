/*******************************************************************************
 * Copyright (c) 2012-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.log.derby;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.server.log.DataLog;

/** {@link DataLog} for Derby
 *
 *  <p>Uses the {@link DerbyDataLogger} for a specific scan ID.
 *  Creates a logger on <code>doLog()</code> and keeps that open
 *  until <code>close()</code>
 *
 *  @author Kay Kasemir
 */
public class DerbyDataLog extends DataLog
{
    final private long scan_id;

    private RDBDataLogger logger = null;

    /** Initialize
     *  @param scan_id ID of scan for which this logger should operate
     */
    public DerbyDataLog(final long scan_id) throws Exception
    {
        this.scan_id = scan_id;
        // Initialize sample serial
        final RDBDataLogger logger = new DerbyDataLogger();
        try
        {
            last_serial = logger.getLastScanDataSerial(scan_id);
        }
        finally
        {
            logger.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doLog(final String device, final ScanSample sample) throws Exception
    {
        if (logger == null)
            logger = new DerbyDataLogger();
        logger.log(scan_id, device, sample);
    }

    /** {@inheritDoc} */
    @Override
    public ScanData getScanData() throws Exception
    {
        // Can be called without doLog(), so use separate logger just for this call
        final RDBDataLogger logger = new DerbyDataLogger();
        try
        {
            return logger.getScanData(scan_id);
        }
        finally
        {
            logger.close();
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void close()
    {
        if (logger != null)
        {
            logger.close();
            logger = null;
        }
        super.close();
    }
}
