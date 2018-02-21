/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.datatable;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.csstudio.scan.client.Preferences;
import org.csstudio.scan.client.ScanClient;
import org.csstudio.scan.data.ScanData;
import org.csstudio.scan.data.ScanDataIterator;
import org.csstudio.scan.data.ScanSample;
import org.csstudio.scan.util.TextTable;
import org.phoebus.framework.jobs.NamedThreadFactory;
import org.phoebus.util.time.TimestampFormats;

/** Table display of logged scan data
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DataTable
{
    private final ScanClient scan_client;
    private final long scan_id;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("Scan Data Table"));
    private ScheduledFuture<?> updates;
    private long last_serial = -1;

    public DataTable(final ScanClient scan_client, final long scan_id)
    {
        this.scan_id = scan_id;
        this.scan_client = scan_client;
        updates = timer.scheduleWithFixedDelay(this::poll, 100, 1000, TimeUnit.MILLISECONDS);
    }

    private Void poll()
    {
        try
        {
            final long serial = scan_client.getLastScanDataSerial(scan_id);
            if (serial > last_serial)
            {
                System.out.println("Data for serial " + serial);
                final ScanData data = scan_client.getScanData(scan_id);
                final ScanDataIterator iterator = new ScanDataIterator(data);

                final TextTable table = new TextTable(System.out);
                table.addColumn("Time");
                for (String device : iterator.getDevices())
                    table.addColumn(device);

                while (iterator.hasNext())
                {
                    table.addCell(TimestampFormats.formatCompactDateTime(iterator.getTimestamp()));
                    for (ScanSample sample : iterator.getSamples())
                        table.addCell(Arrays.toString(sample.getValues()));
                }
                table.flush();

                last_serial = serial;
            }
            if (scan_client.getScanInfo(scan_id).getState().isDone())
            {
                updates.cancel(false);
                timer.shutdown();
            }
        }
        catch (Exception ex)
        {
            System.out.println("No data for " + scan_id + ", " + ex.getMessage());
        }

        return null;
    }

    // TODO Move into demo code
    public static void main(String[] args) throws Exception
    {
        final ScanClient client = new ScanClient(Preferences.host, Preferences.port);
        DataTable table = new DataTable(client, 53);

        while (!table.timer.isShutdown())
            Thread.sleep(200);
    }
}
