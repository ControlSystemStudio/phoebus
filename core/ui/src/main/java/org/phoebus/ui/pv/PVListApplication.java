/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.ui.pv;

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
@SuppressWarnings("nls")
public class PVListApplication
{
    public static final PVListApplication INSTANCE = new PVListApplication();

    public static final String NAME = "PV List";

    private DockItem dock_item = null;

    public void start()
    {
        if (dock_item == null)
        {   // Create the PV List
            final PVList pv_list = new PVList();
            dock_item = new DockItem(NAME, pv_list);
            dock_item.addClosedNotification(this::stop);
            DockPane.getActiveDockPane().addTab(dock_item);
        }
        else
            // Show the existing one
            dock_item.select();
    }

    public void stop()
    {
        dock_item = null;
    }
}
