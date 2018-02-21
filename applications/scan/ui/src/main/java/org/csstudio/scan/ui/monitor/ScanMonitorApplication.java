/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import java.net.URL;

import org.phoebus.framework.spi.AppDescriptor;

/** Application descriptor for "Scan Monitor"
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanMonitorApplication implements AppDescriptor
{
    public static final String NAME = "scan_monitor";
    public static final String DISPLAY_NAME = "Scan Monitor";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }

    @Override
    public URL getIconURL()
    {
        return ScanMonitor.class.getResource("/icons/scan_monitor.png");
    }

    @Override
    public ScanMonitor create()
    {
        if (ScanMonitor.INSTANCE == null)
            ScanMonitor.INSTANCE = new ScanMonitor(this);
        else
            ScanMonitor.INSTANCE.raise();
        return ScanMonitor.INSTANCE;
    }
}
