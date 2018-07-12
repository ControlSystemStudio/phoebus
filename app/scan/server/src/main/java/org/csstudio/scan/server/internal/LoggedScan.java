/*******************************************************************************
 * Copyright (c) 2012 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.internal;

import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.info.Scan;
import org.csstudio.scan.info.ScanInfo;
import org.csstudio.scan.info.ScanState;
import org.csstudio.scan.server.log.DataLog;
import org.csstudio.scan.server.log.DataLogFactory;

/** Scan with logged data
 *
 *  <p>Scan that was executed in the past and has logged data,
 *  or basis for {@link ExecutableScan}
 *  @author Kay Kasemir
 */
public class LoggedScan extends Scan
{
    /** Initialize
     *  @param scan {@link Scan}
     */
    public LoggedScan(final Scan scan)
    {
        super(scan);
    }

    /** @return {@link ScanState} */
    public ScanState getScanState()
    {
        return ScanState.Logged;
    }

    /** @return Info about this scan */
    public ScanInfo getScanInfo()
    {
        return new ScanInfo(this, getScanState());
    }

    /** Get serial of last logged sample.
     *
     *  <p>Can be used to determine if there are new samples
     *  that should be fetched via <code>getScanData()</code>
     *
     *  @return Serial of last sample in scan data or -1 if nothing has been logged
     *  @throws Exception on error
     *  @see #getScanData()
     */
    public long getLastScanDataSerial() throws Exception
    {
        try
        (
            final DataLog logger = DataLogFactory.getDataLog(this);
        )
        {
            return logger.getLastScanDataSerial();
        }
    }

    /** Get logged samples.
     *  @return {@link ScanData}
     *  @throws Exception on error
     */
    public ScanData getScanData() throws Exception
    {
        try
        (
            final DataLog logger = DataLogFactory.getDataLog(this);
        )
        {
            return logger.getScanData();
        }
    }

    // Compare by ID
    @Override
    public boolean equals(final Object obj)
    {
        if (! (obj instanceof LoggedScan))
            return false;
        final LoggedScan other = (LoggedScan) obj;
        return getId() == other.getId();
    }
}
