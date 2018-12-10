/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/**
 * MenuEntry for opening a log entry calendar.
 * @author Kunal Shroff
 */
public class LogEntryCalenderMenuEntry implements MenuEntry
{
    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(LogEntryCalenderApp.NAME);
        return null;
    }

    @Override
    public String getName()
    {
        return LogEntryCalenderApp.DISPLAYNAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Utility";
    }
    
    @Override
    public Image getIcon()
    {
        return LogEntryCalenderApp.icon;
    }

}
