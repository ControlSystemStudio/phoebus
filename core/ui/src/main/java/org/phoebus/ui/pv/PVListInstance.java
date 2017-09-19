/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/** Application for the PV List
 *
 *  <p>Asserts a singleton instance.
 *  Trying to open more than one will activate the existing instance
 *  until that's closed.
 *
 *  @author Kay Kasemir
 */
public class PVListInstance implements AppInstance
{
    /** At most one instance */
    static PVListInstance INSTANCE = null;

    private final AppDescriptor app;

    private DockItem dock_item = null;

    PVListInstance(final AppDescriptor app)
    {
        this.app = app;

        // Create the PV List
        final PVList pv_list = new PVList();
        dock_item = new DockItem(this, pv_list);
        dock_item.addClosedNotification(this::dispose);
        DockPane.getActiveDockPane().addTab(dock_item);
    }

    @Override
    public AppDescriptor getAppDescriptor()
    {
        return app;
    }

    /** Show the existing singleton instance */
    public void raise()
    {
        dock_item.select();
    }

    private void dispose()
    {
        INSTANCE = null;
    }
}
