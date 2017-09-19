/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import static org.phoebus.framework.util.ResourceParser.createAppURI;
import static org.phoebus.framework.util.ResourceParser.parseQueryArgs;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.phoebus.framework.spi.AppInstance;
import org.phoebus.framework.spi.AppResourceDescriptor;
import org.phoebus.ui.docking.DockItem;
import org.phoebus.ui.docking.DockPane;

/** Application descriptor for PV Tree
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTreeApplication implements AppResourceDescriptor
{
    public static final Logger logger = Logger.getLogger(PVTree.class.getPackageName());

    public static final String NAME = "pv_tree";

    public static final String DISPLAY_NAME = "PV Tree";

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
    public AppInstance create()
    {
        final PVTree pv_tree = new PVTree(this);
        final DockItem tab = new DockItem(pv_tree, pv_tree.create());
        tab.addClosedNotification(() -> pv_tree.dispose());
        DockPane.getActiveDockPane().addTab(tab);
        return pv_tree;
    }

    @Override
    public AppInstance create(final String resource)
    {
        PVTree pv_tree = null;

        final Map<String, List<String>> args = parseQueryArgs(createAppURI(resource));
        final List<String> pvs = args.get("pv");
        if (pvs == null)
            pv_tree = (PVTree) create();
        else
            for (String pv : pvs)
            {
                pv_tree = (PVTree) create();
                pv_tree.setPVName(pv);
            }

        return pv_tree;
    }
}
