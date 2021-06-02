/*******************************************************************************
 * Copyright (c) 2018 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.phoebus.logbook.ui.menu;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.ui.LogbookUiPreferences;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.spi.MenuEntry;
import org.phoebus.ui.spi.ToolbarEntry;


public class SendToLogBookToolbarEntry implements ToolbarEntry
{
    @Override
    public Void call() throws Exception
    {
        if (LogbookPreferences.is_supported)
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
    public Image getIcon()
    {
        return SendToLogBookApp.icon;
    }
}
