package org.phoebus.logbook.ui.menu;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.ui.write.LogEntryDialog;
import org.phoebus.ui.docking.DockPane;

public class SendToLogBookApp implements AppDescriptor
{

    public static final String DISPLAY_NAME = "Send To Log Book";
    public static final String NAME = "logbook";

    public String getDisplayName()
    {
        return DISPLAY_NAME;
    }
    
    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public AppInstance create()
    {
        LogEntryDialog logEntryDialog = new LogEntryDialog(DockPane.getActiveDockPane(), null);
        logEntryDialog.show();
        return null;
    }

}
