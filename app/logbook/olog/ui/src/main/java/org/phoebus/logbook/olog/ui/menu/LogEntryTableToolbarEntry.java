/*
 * Copyright (C) 2024 European Spallation Source ERIC.
 */

package org.phoebus.logbook.olog.ui.menu;

import javafx.scene.image.Image;
import org.phoebus.framework.workbench.ApplicationService;
import org.phoebus.logbook.LogbookPreferences;
import org.phoebus.logbook.olog.ui.LogEntryTableApp;
import org.phoebus.logbook.olog.ui.Messages;
import org.phoebus.ui.dialog.ExceptionDetailsErrorDialog;
import org.phoebus.ui.spi.ToolbarEntry;

public class LogEntryTableToolbarEntry implements ToolbarEntry {

    @Override
    public Void call() throws Exception
    {
        if (LogbookPreferences.is_supported){
            ApplicationService.createInstance(LogEntryTableApp.NAME);
        }
        else{
            ExceptionDetailsErrorDialog.openError(Messages.LogbookNotSupported, Messages.LogbookNotSupported, new Exception("No logbook factory found"));
        }
        return null;
    }

    @Override
    public String getName()
    {
        return LogEntryTableApp.DISPLAY_NAME;
    }

    @Override
    public Image getIcon()
    {
        return LogEntryTableApp.icon;
    }
}
