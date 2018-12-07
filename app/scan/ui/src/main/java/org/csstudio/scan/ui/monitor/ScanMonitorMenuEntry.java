/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.scan.ui.monitor;

import org.csstudio.scan.ScanSystem;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry for scan monitor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScanMonitorMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return ScanMonitorApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(ScanSystem.class, "/icons/scan_monitor.png");
    }

    @Override
    public String getMenuPath()
    {
        return "Scan";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(ScanMonitorApplication.NAME);
        return null;
    }
}
