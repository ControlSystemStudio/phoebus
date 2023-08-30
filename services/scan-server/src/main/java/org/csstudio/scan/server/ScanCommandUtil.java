/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server;

import java.time.Duration;

/** Utilities for command implementations
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandUtil
{
    /** Write to device without readback
     *
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @throws Exception on error
     */
    public static void write(final ScanContext context,
            final String device_name, final Object value) throws Exception
    {
        write(context, device_name, value, false, false, "", 0.0, null);
    }


    /** Write to device with readback, waiting forever, logging if the context
     *  was configured to auto-log
     *
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout in seconds, <code>null</code> as "forever"
     *  @throws Exception on error
     */
    public static void write(final ScanContext context,
            final String device_name, final Object value,
            final double tolerance, final Duration timeout) throws Exception
    {
        write(context, device_name, value, false, true, device_name, tolerance, timeout);
    }

    /** Write to device with optional completion and/or readback,
     *  logging if the context was configured to auto-log
     *
     *  @param context Scan context
     *  @param device_name Name of device
     *  @param value Value to write to the device
     *  @param completion Await completion to the write?
     *  @param wait Wait for readback to match?
     *  @param readback_name Readback device name (default will be main device_name)
     *  @param tolerance Numeric tolerance when checking value
     *  @param timeout Timeout for callback as well as readback; <code>null</code> as "forever"
     *  @throws Exception on error
     */
    public static void write(final ScanContext context,
            final String device_name, final Object value,
            final boolean completion,
            final boolean wait,
            final String readback_name, final double tolerance, final Duration timeout) throws Exception
    {
        final WriteHelper write = new WriteHelper(context, device_name, value, completion, wait, readback_name, value, tolerance, timeout);
        write.perform();
    }
}
