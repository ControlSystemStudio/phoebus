/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
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
package org.csstudio.scan.data;

import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.csstudio.scan.util.TextTable;

/** Iterate over {@link ScanData} as spreadsheet-type table
 *
 *  <p>Each column represents the samples for a device.
 *  {@link ScanSample} serials are used to correlate samples.
 *  Each "line" of the spreadsheet contains samples for the
 *  same serial.
 *  Values for a device are repeated on following lines until
 *  a sample with new serial is received.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanDataIterator
{
    /** "Comma Separator" is actually comma,
     *  but could in the future be changed to '\t' or other
     */
    final private static String CSV_SEPARATOR = ",";

    /** Device names, i.e. columns in spreadsheet */
    final private String[] device_names;

    /** Raw data for each device */
    final private List<ScanSample>[] data;

    /** Index within data for each device */
    final private int[] index;

    /** Timestamp of current spreadsheet line */
    private Instant timestamp;

    /** Values for current spreadsheet line */
    final private ScanSample[] value;

    /** Initialize for all devices in the {@link ScanData}
     *  @param scan_data Scan data
     */
    public ScanDataIterator(final ScanData scan_data)
    {
        // Determine for which devices we have samples
        this(scan_data, scan_data.getDevices());
    }

    /** Initialize for specific devices
     *  @param scan_data Scan data
     *  @param device_names Devices that must be in the scan data
     */
    @SuppressWarnings("unchecked")
    public ScanDataIterator(final ScanData scan_data, final String... device_names)
    {
        this.device_names = device_names;
        final int N = device_names.length;

        data = new List[N];
        value = new ScanSample[N];
        index = new int[N];
        for (int i=0; i<N; ++i)
        {
            data[i] = scan_data.getSamples(device_names[i]);
            if (data[i] == null)
                data[i] = Collections.emptyList();
            index[i] = 0;
            value[i] = null;
        }
    }

    /** @param i Device index
     *  @return data[index[i]]
     */
    private ScanSample getCurrentSample(final int i)
    {
        if (data[i].size() > index[i])
            return data[i].get(index[i]);
        return null;
    }

    /** @return Device names, i.e. spreadsheet columns */
    public String[] getDevices()
    {
        return device_names;
    }

    /** @return <code>true</code> if there is another line in the spreadsheet */
    public boolean hasNext()
    {
        // Find oldest serial
        final int N = device_names.length;
        long oldest = Long.MAX_VALUE;
        timestamp = null;
        for (int i=0; i<N; ++i)
        {
            final ScanSample sample = getCurrentSample(i);
            if (sample == null)
                continue;
            if (sample.getSerial() < oldest)
                oldest = sample.getSerial();
        }
        if (oldest == Long.MAX_VALUE)
            return false;

        // 'oldest' now defines the current spreadsheet line.
        // Determine value for that line.
        for (int i=0; i<N; ++i)
        {
            final ScanSample sample = getCurrentSample(i);
            if (sample == null)
            {    // No more data for device #i
                // Leave value[i] as is, "still valid"
                continue;
            }
            // Device #i has data
            if (sample.getSerial() <= oldest)
            {
                // Use that as the 'value' for this line
                value[i] = sample;
                // Set index to the next sample, which will
                // be used as 'value' once we reach that time slot.
                ++index[i];
            }
            // else: sample[i] already points to a sample
            // _after_ the current line, so leave value[i] as is

            // For time stamp, use the newest valid stamp on current line
            if (value[i] != null  &&
                (timestamp == null  ||  timestamp.isBefore(sample.getTimestamp())))
                timestamp = sample.getTimestamp();
        }

        return true;
    }

    /** @return Time stamp of the current spreadsheet line */
    public Instant getTimestamp()
    {
        return timestamp;
    }

    /** @return Samples on the current spreadsheet line */
    public ScanSample[] getSamples()
    {
        // Copy because value[] will be overridden with next line
        return Arrays.copyOf(value, value.length);
    }

    /** Write spreadsheet to stream with fixed-sized columns
     *  @param out {@link PrintStream}
     */
    public void printTable(final PrintStream out)
    {
        final TextTable table = new TextTable(out);
        // Header
        table.addColumn("Time");
        for (String device : getDevices())
            table.addColumn(device);

        // Iterate over device data in 'spreadsheet' manner
        while (hasNext())
        {
            table.addCell(ScanSampleFormatter.format(getTimestamp()));
            // Print current line
            for (ScanSample sample : getSamples())
            {
                if (sample == null)
                    table.addCell("#N/A");
                else
                    table.addCell(ScanSampleFormatter.asString(sample));
            }
        }

        table.flush();
    }

    /** Write spreadsheet to stream in CVS format
     *  @param out {@link PrintStream}
     */
    public void printCSV(final PrintStream out)
    {
        // Header
        out.append("Time");
        for (String device : getDevices())
            out.append(CSV_SEPARATOR).append(device);
        out.println();

        // Iterate over device data in 'spreadsheet' manner
        while (hasNext())
        {
            out.append(ScanSampleFormatter.format(getTimestamp()));
            // Print current line
            for (ScanSample sample : getSamples())
            {
                out.append(CSV_SEPARATOR);
                if (sample == null)
                    out.append("#N/A");
                else
                    out.append(ScanSampleFormatter.asString(sample));
            }
            out.println();
        }
    }
}
