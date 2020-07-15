/*******************************************************************************
 * Copyright (c) 2012-2020 Oak Ridge National Laboratory.
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
package org.csstudio.scan.command;

import org.csstudio.scan.device.DeviceInfo;

/** Description of a {@link ScanCommand} property
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanCommandProperty
{
    // Tag names for XML and device IDs
    final public static String TAG_ADDRESS = "address";
    final public static String TAG_DEVICE = "device";
    final public static String TAG_VALUE = "value";
    final public static String TAG_READBACK = "readback";
    final public static String TAG_READBACK_VALUE = "readback_value";
    final public static String TAG_COMPLETION = "completion";
    final public static String TAG_WAIT = "wait";
    final public static String TAG_TOLERANCE = "tolerance";
    final public static String TAG_TIMEOUT = "timeout";

    // Properties used by multiple commands
    final public static ScanCommandProperty DEVICE_NAME =
            new ScanCommandProperty("device_name", "Device Name", DeviceInfo.class);

    final public static ScanCommandProperty READBACK =
            new ScanCommandProperty(TAG_READBACK, "Readback Device", DeviceInfo.class);

    final public static ScanCommandProperty COMPLETION =
            new ScanCommandProperty(TAG_COMPLETION, "Wait for completion", Boolean.class);

    final public static ScanCommandProperty WAIT =
            new ScanCommandProperty(TAG_WAIT, "Wait for readback", Boolean.class);

    final public static ScanCommandProperty TOLERANCE =
            new ScanCommandProperty(TAG_TOLERANCE, "Tolerance", Double.class);

    final public static ScanCommandProperty TIMEOUT =
            new ScanCommandProperty(TAG_TIMEOUT, "Time out (seconds; 0 to disable)", Double.class);

    final private String name, id;
    final private Class<?> type;

    /** Initialize
     *  @param id Property ID
     *  @param name Name or label used for GUI
     *  @param type Data type, see {@link ScanCommand} for supported data types
     */
    public ScanCommandProperty(final String id, final String name, final Class<?> type)
    {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    /** @return ID of the property */
    public String getID()
    {
        return id;
    }

    /** @return Name of the property, displayed in a GUI */
    public String getName()
    {
        return name;
    }

    /** @return Type of the property.
     *  @see ScanCommand ScanCommand lists supported data types
     */
    public Class<?> getType()
    {
        return type;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return "Scan command property '" + id + "' (" + name + "), type " + type.getName();
    }
}
