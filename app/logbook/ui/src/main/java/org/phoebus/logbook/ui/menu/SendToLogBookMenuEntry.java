/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.menu;

import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.javafx.ImageCache;
import org.phoebus.ui.spi.MenuEntry;

import javafx.scene.image.Image;

/**
 * MenuEntry for sending a log book entry outside of a context menu.
 * @author Evan Smith
 */
@SuppressWarnings("nls")
public class SendToLogBookMenuEntry implements MenuEntry
{
    @Override
    public Void call() throws Exception
    {
        if (LogbookUiPreferences.is_supported)
            ApplicationService.createInstance(SendToLogBookApp.NAME);
        else
            ExceptionDetailsErrorDialog.openError("No Logbook Support", "Logbook submissions are not enabled", new Exception("No logbook factory found"));

        return null;
    }

    @Override
    public String getName()
    {
        return SendToLogBookApp.DISPLAY_NAME;
    }

    @Override
    public String getMenuPath()
    {
        return "Utility";
    }

    @Override
    public Image getIcon()
    {
        return ImageCache.getImage(SendToLogBookMenuEntry.class, "/icons/logentry-add-16.png");
    }
}
