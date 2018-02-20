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

import java.time.Instant;

/** A sample taken by a scan
 *
 *  <p>All samples have a time stamp.
 *
 *  <p>A serial number is used to track the sequence
 *  in which samples were acquired within a scan.
 *  The time stamps alone can <u>not</u> dependably correlate
 *  samples for several reasons:
 *
 *  <ul>
 *  <li>Time stamps can be the actual control system time stamps.
 *      If a value has not changed for a while, the 'old' time stamp
 *      would reflect that.
 *  <li>A scan can run faster than the resolution of the time stamp.
 *  <li>Samples are meant to be logged "together", for example by one
 *      log command within a loop, but their actual time stamps
 *      don't reflect that.
 *  </ul>
 *
 *  @author Kay Kasemir
 *
 *  @see NumberScanSample
 */
abstract public class ScanSample
{
    final private Instant timestamp;
    final private long serial;

    /** Initialize
     *  @param timestamp Time stamp
     *  @param serial Serial to identify when the sample was taken
     */
    public ScanSample(final Instant timestamp, final long serial)
    {
        this.timestamp = timestamp;
        this.serial = serial;
    }

    /** @return Time when this sample was obtained */
    public Instant getTimestamp()
    {
        return timestamp;
    }

    /** @return Serial number of this sample */
    public long getSerial()
    {
        return serial;
    }

    /** Get raw values of the sample
     *  <p>Derived classes can implement access to
     *  the value by other means
     *  @return Values of the sample. Will be an array of {@link Number} or ...
     */
    abstract public Object[] getValues();

    @Override
    public String toString()
    {
        return ScanSampleFormatter.format(timestamp) + " [" + serial + "]";
    }
}
