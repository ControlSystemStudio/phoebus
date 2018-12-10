/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtree;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Toolbar entry that starts PV Tree Application
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class PVTreeMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return PVTreeApplication.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Display";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(PVTree.class, "/icons/pvtree.png");
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(PVTreeApplication.NAME);
        return null;
    }
}
