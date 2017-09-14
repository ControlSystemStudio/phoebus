/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import java.util.logging.Logger;

import org.phoebus.framework.persistence.Memento;
import org.phoebus.framework.spi.AppDescriptor;
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

    private PVTree pv_tree;

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
    public void create()
    {
        openPVTreeTab();
    }

    PVTree openPVTreeTab()
    {
        pv_tree = new PVTree();
        final DockItem tab = new DockItem(this, pv_tree.create());
        tab.addClosedNotification(() -> pv_tree.dispose());
        DockPane.getActiveDockPane().addTab(tab);
        return pv_tree;
    }

    @Override
    public void restore(final Memento memento)
    {
        memento.getString("pv").ifPresent(name -> pv_tree.setPVName(name));
    }

    @Override
    public void save(final Memento memento)
    {
        memento.setString("pv", pv_tree.getPVName());
    }
}
