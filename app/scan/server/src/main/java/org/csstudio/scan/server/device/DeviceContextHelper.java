/*******************************************************************************
 * Copyright (c) 2013-2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.server.device;

import java.util.List;

import org.csstudio.scan.device.DeviceInfo;
import org.csstudio.scan.server.MacroContext;
import org.csstudio.scan.server.ScanCommandImpl;

/** Helper for adding devices used by a scan to a context
 *  @author Kay Kasemir
 */
public class DeviceContextHelper
{
    /** @param devices Existing {@link DeviceContext}
     *  @param macros Macros to use when resolving names
     *  @param commands Commands for which missing devices should be added
     *  @throws Exception on error
     */
    public static void addScanDevices(final DeviceContext devices,
            final MacroContext macros,
            final List<ScanCommandImpl<?>> commands) throws Exception
    {
        final List<DeviceInfo> aliases = DeviceContext.getDeviceAliases();

        for (ScanCommandImpl<?> command : commands)
            for (String name : command.getDeviceNames(macros))
                addDevice(devices, aliases, name);
    }

    /** @param devices Existing {@link DeviceContext}
     *  @param aliases Aliases to use
     *  @param name
     *  @throws Exception
     */
    private static void addDevice(final DeviceContext devices, final List<DeviceInfo> aliases,
            final String name) throws Exception
    {
        // Already known?
        try
        {
            final Device device = devices.getDevice(name);
            if (device != null)
                return;
        }
        catch (Exception ex)
        {
            // Not found, need to add
        }

        // Is name an alias?
        for (DeviceInfo alias : aliases)
            if (alias.getAlias().equals(name))
            {
                devices.addPVDevice(alias);
                return;
            }

        devices.addPVDevice(new DeviceInfo(name));
    }
}
