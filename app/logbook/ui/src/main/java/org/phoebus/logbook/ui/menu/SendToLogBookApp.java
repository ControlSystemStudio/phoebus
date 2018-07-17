package org.phoebus.logbook.ui.menu;

import org.phoebus.framework.spi.AppDescriptor;
import org.phoebus.framework.spi.AppInstance;
import org.phoebus.logbook.ui.write.LogEntryDialog;

public class SendToLogBookApp implements AppDescriptor
{

    public static final String DISPLAY_NAME = "Send To Log Book";
    public static final String NAME = "sendToLogBook";
    
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
        LogEntryDialog logEntryDialog = new LogEntryDialog(null, null);
        logEntryDialog.show();
        return null;
    }

}
