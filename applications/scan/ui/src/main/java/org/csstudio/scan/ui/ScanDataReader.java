/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui;

import static org.csstudio.scan.ScanSystem.logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.phoebus.framework.jobs.NamedThreadFactory;

/** Periodically read data of a scan until the scan completes
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataReader
{
    private static final ScheduledExecutorService timer;

    static
    {
        timer = Executors.newScheduledThreadPool(0, new NamedThreadFactory("ScanDataReader"));
        ((ScheduledThreadPoolExecutor)timer).setKeepAliveTime(5, TimeUnit.SECONDS);
    }

    public static interface Listener
    {
        /** Called when there's new logged data
         *  @param data Latest scan data
         */
        void dataChanged(ScanData data);

        /** Called when the scan completed, i.e. there won't be any new data */
        void scanCompleted();
    };

    private final ScanClient scan_client;
    private volatile long scan_id = -1;
    private final Consumer<ScanData> data_listener;
    /** Are updates scheduled?
     *  Will be cancelled once the scan has finished.
     *  SYNC on this for access
     */
    private ScheduledFuture<?> updates = null;

    /** Last known scan serial */
    private long last_serial = ScanClient.UNKNOWN_SCAN_SERIAL;

    /** Create reader for scan's log data
     *  @param scan_client {@link ScanClient}
     *  @param data_listener Will be called whenever there's new log data, on background thread
     */
    public ScanDataReader(final ScanClient scan_client, final Consumer<ScanData> data_listener)
    {
        this.scan_client = scan_client;
        this.data_listener = data_listener;
        trigger();
    }

    /** @param scan_id ID of scan for which to read data. Less than 0 to stop reading data */
    public void setScanId(final long scan_id)
    {
        last_serial = ScanClient.UNKNOWN_SCAN_SERIAL;
        this.scan_id = scan_id;
        trigger();
    }

    /** @return Scan ID */
    public long getScanId()
    {
        return scan_id;
    }

    /** Trigger a read right now, not waiting for the next period */
    public synchronized void trigger()
    {
        // If there are no updates, because scan had stopped or this is the very first call, start them.
        if (updates == null)
            updates = timer.scheduleWithFixedDelay(this::poll, 100, 1000, TimeUnit.MILLISECONDS);
        else
            // Updates are running, but we want another update ASAP
            timer.submit(this::poll);
    }

    private Void poll()
    {
        final long id = scan_id;
        if (id >= 0)
            try
            {
                final long serial = scan_client.getLastScanDataSerial(id);
                // Last_serial starts 'unknown'.
                if (serial > last_serial)
                {
                    // As soon as the scan is known, even with 'no data' (serial -1),
                    // fetch the data
                    logger.log(Level.FINE, "Received data for scan {0}", id);
                    final ScanData data = scan_client.getScanData(id);
                    // Inform listener
                    data_listener.accept(data);
                    last_serial = serial;
                }
                // Is this a known scan, and it's done?
                if (serial >= 0  &&
                    scan_client.getScanInfo(id).getState().isDone())
                {
                    shutdown();
                    logger.log(Level.FINE, "Completed reading data for scan {0}", id);
                }
                // else keep polling until the scan is 'done'.
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Scan data poll error for scan " + id, ex);
            }
        return null;
    }

    /** Reader will shut down when the scan completes.
     *  In case the UI needs to close while the scan is still active,
     *  it can shut the reader down at any time.
     */
    public synchronized void shutdown()
    {
        if (updates != null)
        {
            updates.cancel(false);
            updates = null;
        }
    }
}
