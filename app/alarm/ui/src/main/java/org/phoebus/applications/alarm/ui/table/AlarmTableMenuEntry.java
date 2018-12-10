/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.applications.alarm.ui.table;

import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/** Menu entry for alarm table
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTableMenuEntry implements MenuEntry
{
    @Override
    public String getName()
    {
        return AlarmTableApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(AlarmUI.class, "/icons/alarmtable.png");
    }

    @Override
    public String getMenuPath()
    {
        return "Alarm";
    }

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(AlarmTableApplication.NAME);
        return null;
    }
}
