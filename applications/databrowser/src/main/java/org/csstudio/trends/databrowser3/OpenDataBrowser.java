package org.csstudio.trends.databrowser3;

import org.phoebus.framework.spi.MenuEntry;
import org.phoebus.framework.workbench.ApplicationService;

public class OpenDataBrowser implements MenuEntry
{
    @Override
    public String getName()
    {
        return Messages.DataBrowser;
    }

    @Override
    public String getMenuPath()
    {
        return "Display";
    }

    @Override
    public Void call() throws Exception
    {
        DataBrowserApp app = ApplicationService.findApplication(DataBrowserApp.NAME);
        app.create();
        return null;
    }
}
