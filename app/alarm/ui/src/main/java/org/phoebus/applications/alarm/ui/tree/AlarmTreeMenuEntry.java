/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/

package org.phoebus.applications.alarm.ui.tree;

import org.phoebus.applications.alarm.ui.AlarmUI;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

import java.net.URI;

/** Menu entry for alarm tree
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class AlarmTreeMenuEntry implements MenuEntry
{
    /**
     * Identifies alarm configuration.
     */
    private URI resource;

    @Override
    public String getName()
    {
        return AlarmTreeApplication.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(AlarmUI.class, "/icons/alarmtree.png");
    }

    @Override
    public String getMenuPath()
    {
        return "Alarm";
    }

    public void setResource(URI resource){
        this.resource = resource;
    }

    @Override
    public Void call() throws Exception
    {
        if(resource == null){
            ApplicationService.createInstance(AlarmTreeApplication.NAME);
        }
        else{
            ApplicationService.createInstance(AlarmTreeApplication.NAME, resource);
        }
        return null;
    }
}
