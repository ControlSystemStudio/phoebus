package org.phoebus.logbook.ui.menu;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

public class SendToLogBookMenuEntry implements MenuEntry
{

    @Override
    public Void call() throws Exception
    {
        ApplicationService.createInstance(SendToLogBookApp.NAME);
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

}
