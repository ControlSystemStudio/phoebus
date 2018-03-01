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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Data set taken by a scan.
 *
 *  <p>The data in a scan is organized by device.
 *  For each device, a list of samples can be obtained.
 *
 *  @author Kay Kasemir
 */
public class ScanData
{
    /** Map from device name to list of samples for that device */
    final private Map<String, List<ScanSample>> device_logs;

    /** Initialize
     *  @param device_logs Map from device name to list of samples for that device
     */
    public ScanData(final Map<String, List<ScanSample>> device_logs)
    {
        this.device_logs = device_logs;
    }

    /** @return Names of devices for which there are samples */
    public List<String> getDevices()
    {
        final List<String> names = new ArrayList<>(device_logs.keySet());
        Collections.sort(names);
        return names;
    }

    /** @param device_name Name of a device
     *  @return Samples that were taken for this device.
     *          <code>null</code> if the device has no samples.
     */
    public List<ScanSample> getSamples(final String device_name)
    {
        return device_logs.get(device_name);
    }
}
