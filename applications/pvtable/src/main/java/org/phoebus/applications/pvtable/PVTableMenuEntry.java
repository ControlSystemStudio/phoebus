/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.applications.pvtable;

import org.phoebus.framework.spi.MenuEntry;

/**
 * Menu entry that starts PV Table
 *
 * @author Kunal Shroff
 */
public class PVTableMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return PVTableApplication.NAME;
    }

    @Override
    public Void call() throws Exception
    {
        new PVTableApplication().open();
        return null;
    }

    @Override
    public String getMenuPath()
    {
        return "Display.Utility";
    }
}
