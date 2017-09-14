/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.logging.Logger;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/** Application descriptor for PV Tree
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTreeApplication implements AppDescriptor
{
    public static final Logger logger = Logger.getLogger(PVTree.class.getPackageName());

    public static final String NAME = "PV Tree";

    @Override
    public String getName()
    {
        return "pv_tree";
    }

    @Override
    public String getDisplayName()
    {
        return NAME;
    }

    @Override
    public AppInstance create()
    {
        return openPVTreeTab();
    }

    PVTree openPVTreeTab()
    {
        final PVTree pv_tree = new PVTree(this);
        final DockItem tab = new DockItem(pv_tree, pv_tree.create());
        tab.addClosedNotification(() -> pv_tree.dispose());
        DockPane.getActiveDockPane().addTab(tab);
        return pv_tree;
    }
}
