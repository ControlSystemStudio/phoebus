/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/**
 * Menu entry that starts PV Table
 *
 * @author Kunal Shroff
 */
@SuppressWarnings("nls")
public class PVTableMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return PVTableApplication.DISPLAY_NAME;
    }

    @Override
    public Void call() throws Exception
    {
        new PVTableApplication().create();
        return null;
    }

    @Override
    public Image getIcon()
    {
        return ContextMenuPVTableLauncher.icon;
    }

    @Override
    public String getMenuPath()
    {
        return "Display";
    }
}
